package com.emeal.service;

import com.emeal.config.BusinessTime;

import com.emeal.dto.request.CreateEmployeeRequest;
import com.emeal.dto.request.RecordMealRequest;
import com.emeal.dto.request.UpdateEmployeeRequest;
import com.emeal.dto.response.*;
import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import com.emeal.entity.MealRecord;
import com.emeal.entity.MealStatus;
import com.emeal.exception.BadRequestException;
import com.emeal.exception.DuplicateResourceException;
import com.emeal.exception.ResourceNotFoundException;
import com.emeal.security.SecurityUtils;
import com.emeal.repository.EmployeeRepository;
import com.emeal.repository.MealRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final MealRecordRepository mealRecordRepository;
    private final MealRecordService mealRecordService;
    private final SettingsService settingsService;
    private final AuditLogService auditLogService;
    private final TrashService trashService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           MealRecordRepository mealRecordRepository,
                           MealRecordService mealRecordService,
                           SettingsService settingsService,
                           AuditLogService auditLogService,
                           TrashService trashService) {
        this.trashService = trashService;
        this.employeeRepository = employeeRepository;
        this.mealRecordRepository = mealRecordRepository;
        this.mealRecordService = mealRecordService;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeDTO> searchEmployees(String query, String department, EmployeeStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "employeeCode"));

        org.springframework.data.jpa.domain.Specification<Employee> spec = (root, q, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate code = cb.like(cb.lower(root.get("employeeCode")), pattern);
                jakarta.persistence.criteria.Predicate fn = cb.like(cb.lower(root.get("firstName")), pattern);
                jakarta.persistence.criteria.Predicate ln = cb.like(cb.lower(root.get("lastName")), pattern);
                jakarta.persistence.criteria.Predicate phone = cb.like(cb.lower(root.get("phone")), pattern);
                predicates.add(cb.or(code, fn, ln, phone));
            }
            if (department != null && !department.isBlank()) {
                predicates.add(cb.equal(root.get("department"), department.trim()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Employee> result = employeeRepository.findAll(spec, pageable);
        List<EmployeeDTO> dtos = result.getContent().stream().map(EmployeeDTO::fromEntity).toList();
        return PageResponse.fromPage(result, dtos);
    }

    @Transactional(readOnly = true)
    public EmployeeAttendancePageResponse getEmployeeAttendancePage(LocalDate date, String query, String department,
                                                                    String mealStatusFilter, EmployeeStatus status,
                                                                    int page, int size) {
        LocalDate targetDate = (date != null) ? date : BusinessTime.today();
        String currency = settingsService.getSettingValue("CURRENCY", "RWF");

        // 1. Calculate Top Summary Stats for the selected date
        long totalActiveEmployees = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long dayAteCount = mealRecordRepository.countByMealDateAndMealStatus(targetDate, MealStatus.ATE);
        long dayDidNotEatCount = mealRecordRepository.countByMealDateAndMealStatus(targetDate, MealStatus.DID_NOT_EAT);

        // Always positive; a missing or invalid setting fails clearly in SettingsService.
        BigDecimal standardMealPrice = settingsService.getStandardMealPrice();

        // Exact sum of the stored ATE amounts for the day; never estimated from counts.
        BigDecimal dayMealCost = mealRecordRepository.sumAmountByMealDate(targetDate);
        if (dayMealCost == null) {
            dayMealCost = BigDecimal.ZERO;
        }

        // Summary reflects the selected date only. A day with no records shows real zeros
        // (no substitution of all-time historical counts).
        EmployeeAttendanceSummaryDTO summary = EmployeeAttendanceSummaryDTO.builder()
                .totalActiveEmployees(totalActiveEmployees)
                .ateCount(dayAteCount)
                .didNotEatCount(dayDidNotEatCount)
                .notRecordedCount(Math.max(0, totalActiveEmployees - (dayAteCount + dayDidNotEatCount)))
                .totalMealCost(dayMealCost)
                .currency(currency)
                .standardMealPrice(standardMealPrice)
                .build();

        // 2. Fetch all employees matching query, department, status
        List<Employee> allMatchingEmployees = employeeRepository.findAll(Sort.by(Sort.Direction.ASC, "employeeCode"));
        
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim().toLowerCase() : null;
        String cleanDept = (department != null && !department.isBlank()) ? department.trim().toLowerCase() : null;

        List<Employee> filteredEmployees = allMatchingEmployees.stream()
                .filter(e -> {
                    if (status != null && e.getStatus() != status) return false;
                    if (cleanDept != null && (e.getDepartment() == null || !e.getDepartment().trim().equalsIgnoreCase(cleanDept))) return false;
                    if (cleanQuery != null) {
                        boolean matchCode = e.getEmployeeCode() != null && e.getEmployeeCode().toLowerCase().contains(cleanQuery);
                        boolean matchFirst = e.getFirstName() != null && e.getFirstName().toLowerCase().contains(cleanQuery);
                        boolean matchLast = e.getLastName() != null && e.getLastName().toLowerCase().contains(cleanQuery);
                        boolean matchPhone = e.getPhone() != null && e.getPhone().toLowerCase().contains(cleanQuery);
                        return matchCode || matchFirst || matchLast || matchPhone;
                    }
                    return true;
                })
                .toList();

        // 3. Match each employee with their meal record on the selected date
        List<MealRecord> dayRecords = mealRecordRepository.findByMealDate(targetDate);
        Map<Long, MealRecord> recordMap = new HashMap<>();
        if (dayRecords != null) {
            for (MealRecord mr : dayRecords) {
                if (mr != null && mr.getEmployee() != null && mr.getEmployee().getId() != null) {
                    recordMap.put(mr.getEmployee().getId(), mr);
                }
            }
        }

        List<EmployeeAttendanceDTO> attendanceList = new ArrayList<>();

        // Which rows are listed for the selected date:
        //   default (null / "" / "RECORDED") -> only employees with a real meal record that day,
        //       so deleted or missing records never appear as meals;
        //   "ATE" / "DID_NOT_EAT"           -> real records with that status;
        //   "ALL_STAFF"                      -> every employee; without a record the row is
        //       NOT_RECORDED with no amount (never shown as a meal).
        String filter = mealStatusFilter == null ? "" : mealStatusFilter.trim().toUpperCase();
        boolean allStaff = filter.equals("ALL_STAFF");

        for (Employee emp : filteredEmployees) {
            MealRecord mr = recordMap.get(emp.getId());
            if (mr == null && !allStaff) {
                continue;
            }
            String mStatus;
            BigDecimal amount;
            Long mealRecordId = null;
            String recordedBy = null;
            java.time.LocalDateTime mealRecordedAt = null;

            if (mr != null) {
                mealRecordId = mr.getId();
                recordedBy = mr.getRecordedBy();
                mealRecordedAt = mr.getCreatedAt();
                if (mr.getMealStatus() == MealStatus.ATE) {
                    mStatus = "ATE";
                    amount = (mr.getAmount() != null && mr.getAmount().compareTo(BigDecimal.ZERO) > 0)
                            ? mr.getAmount()
                            : standardMealPrice;
                } else {
                    mStatus = "DID_NOT_EAT";
                    amount = BigDecimal.ZERO;
                }
            } else {
                mStatus = "NOT_RECORDED";
                amount = BigDecimal.ZERO;
            }

            if ((filter.equals("ATE") || filter.equals("DID_NOT_EAT")) && !mStatus.equals(filter)) {
                continue;
            }

            attendanceList.add(EmployeeAttendanceDTO.builder()
                    .id(emp.getId())
                    .employeeCode(emp.getEmployeeCode())
                    .firstName(emp.getFirstName())
                    .lastName(emp.getLastName())
                    .fullName(emp.getFullName())
                    .telephone(emp.getPhone())
                    .email(emp.getEmail())
                    .position(emp.getPosition())
                    .department(emp.getDepartment())
                    .status(emp.getStatus())
                    .mealDate(targetDate)
                    .mealStatus(mStatus)
                    .amount(amount)
                    .currency(currency)
                    .mealRecordId(mealRecordId)
                    .recordedBy(recordedBy)
                    .mealRecordedAt(mealRecordedAt)
                    .build());
        }

        // 4. Paginate attendanceList
        int totalElements = attendanceList.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (totalPages == 0) totalPages = 1;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<EmployeeAttendanceDTO> pagedList = attendanceList.subList(fromIndex, toIndex);

        PageResponse<EmployeeAttendanceDTO> pagedResponse = PageResponse.<EmployeeAttendanceDTO>builder()
                .content(pagedList)
                .pageNo(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(page >= totalPages - 1)
                .build();

        return EmployeeAttendancePageResponse.builder()
                .summary(summary)
                .employees(pagedResponse)
                .build();
    }

    @Transactional(readOnly = true)
    public List<EmployeeDTO> quickSearch(String query) {
        Pageable pageable = PageRequest.of(0, 10);
        return employeeRepository.quickSearchActiveEmployees(query, pageable)
                .stream().map(EmployeeDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeDTO getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return EmployeeDTO.fromEntity(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeDTO getEmployeeByCode(String code) {
        Employee employee = employeeRepository.findByEmployeeCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with code: " + code));
        return EmployeeDTO.fromEntity(employee);
    }

    @Transactional(readOnly = true)
    public List<String> getAllDepartments() {
        return employeeRepository.findAllDepartments();
    }

    @Transactional
    public EmployeeDTO createEmployee(CreateEmployeeRequest request) {
        boolean byUserRole = SecurityUtils.isUserRole();
        validateNewEmployee(request, byUserRole);

        // USER accounts never choose the employee ID; it is generated like any blank ID.
        String code = (!byUserRole && request.getEmployeeCode() != null && !request.getEmployeeCode().isBlank())
                ? request.getEmployeeCode().trim().toUpperCase()
                : generateNextEmployeeCode();

        if (employeeRepository.existsByEmployeeCode(code)) {
            throw new DuplicateResourceException("Employee ID '" + code + "' already exists");
        }

        String fullName = (request.getEmployeeName() != null && !request.getEmployeeName().isBlank())
                ? request.getEmployeeName().trim()
                : ((request.getFirstName() != null ? request.getFirstName() : "") + " " + (request.getLastName() != null ? request.getLastName() : "")).trim();

        String firstName;
        String lastName;
        int spaceIndex = fullName.indexOf(" ");
        if (spaceIndex > 0) {
            firstName = fullName.substring(0, spaceIndex).trim();
            lastName = fullName.substring(spaceIndex + 1).trim();
        } else {
            firstName = fullName;
            lastName = "";
        }

        String dept = (request.getDepartment() != null && !request.getDepartment().isBlank())
                ? request.getDepartment().trim()
                : "General";

        Employee employee = Employee.builder()
                .employeeCode(code)
                .firstName(firstName)
                .lastName(lastName)
                .department(dept)
                .position(request.getPosition() != null ? request.getPosition().trim() : "Employee")
                .phone(request.getPhone() != null ? request.getPhone().trim() : "")
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .status(EmployeeStatus.ACTIVE)
                .build();

        employee.setCreatedBy(SecurityUtils.getCurrentUsername());
        Employee saved = employeeRepository.save(employee);

        // If initial meal attendance is provided
        if (request.getMealStatus() != null) {
            LocalDate date = (request.getMealDate() != null) ? request.getMealDate() : BusinessTime.today();

            if (request.getMealStatus() == MealStatus.DID_NOT_EAT) {
                if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    throw new BadRequestException("Amount must be 0 RWF when Meal Status is DID NOT EAT");
                }
            }

            BigDecimal mealAmount = (request.getMealStatus() == MealStatus.ATE)
                    ? (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0
                        ? request.getAmount()
                        : settingsService.getStandardMealPrice())
                    : BigDecimal.ZERO;

            RecordMealRequest mealReq = new RecordMealRequest();
            mealReq.setEmployeeId(saved.getId());
            mealReq.setMealDate(date);
            mealReq.setMealStatus(request.getMealStatus());
            mealReq.setAmount(mealAmount);
            mealRecordService.recordMeal(mealReq);
        }

        // Synchronous audit: written in this transaction with the real actor (the async variant
        // runs on another thread without the login and would record "SYSTEM").
        auditLogService.logActionSync("CREATE_EMPLOYEE", "EMPLOYEE", saved.getId().toString(),
                "Created employee " + saved.getEmployeeCode() + " (" + saved.getFullName() + ")"
                        + ", position " + saved.getPosition() + ", phone " + saved.getPhone());

        return EmployeeDTO.fromEntity(saved);
    }

    /**
     * Rules for adding a new employee (form: Employee Name, Position, Meal Status, Amount Used).
     * <ul>
     *   <li>All roles: name and position are required; the telephone is optional (stored as empty
     *       text when not given; the column is NOT NULL); the Employee ID is generated when not
     *       given; an ACTIVE employee with the same name (case-insensitive) is rejected, since
     *       without a telephone the name is the only way to spot a duplicate person.</li>
     *   <li>USER only: the employee can be added only together with their meal record (meal status
     *       required, meal date not in the future), and a given telephone must not belong to an
     *       existing employee.</li>
     * </ul>
     */
    private void validateNewEmployee(CreateEmployeeRequest request, boolean byUserRole) {
        String name = (request.getEmployeeName() != null && !request.getEmployeeName().isBlank())
                ? request.getEmployeeName().trim()
                : ((request.getFirstName() != null ? request.getFirstName() : "") + " "
                    + (request.getLastName() != null ? request.getLastName() : "")).trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Employee name is required.");
        }
        if (byUserRole) {
            if (request.getMealStatus() == null) {
                throw new BadRequestException("Select ATE or DID NOT EAT for the new employee.");
            }
            if (request.getMealDate() != null && request.getMealDate().isAfter(BusinessTime.today())) {
                throw new BadRequestException("The meal date cannot be in the future.");
            }
        }
        // Same split as createEmployee(): first word = first name, the rest = last name.
        int space = name.indexOf(' ');
        String first = space > 0 ? name.substring(0, space).trim() : name;
        String last = space > 0 ? name.substring(space + 1).trim() : "";
        if (employeeRepository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndStatus(first, last, EmployeeStatus.ACTIVE)) {
            throw new DuplicateResourceException(
                    "An employee named " + name + " already exists. Search for them in the list instead.");
        }
        if (byUserRole && request.getPhone() != null && !request.getPhone().isBlank()
                && employeeRepository.existsByPhone(request.getPhone().trim())) {
            throw new DuplicateResourceException(
                    "An employee with this phone number already exists. Search for them in the list instead.");
        }
    }

    @Transactional
    public EmployeeDTO updateEmployee(Long id, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (request.getEmployeeName() != null && !request.getEmployeeName().isBlank()) {
            String fullName = request.getEmployeeName().trim();
            int spaceIndex = fullName.indexOf(" ");
            if (spaceIndex > 0) {
                employee.setFirstName(fullName.substring(0, spaceIndex).trim());
                employee.setLastName(fullName.substring(spaceIndex + 1).trim());
            } else {
                employee.setFirstName(fullName);
                employee.setLastName("");
            }
        } else {
            if (request.getFirstName() != null) employee.setFirstName(request.getFirstName().trim());
            if (request.getLastName() != null) employee.setLastName(request.getLastName().trim());
        }

        if (request.getDepartment() != null) employee.setDepartment(request.getDepartment().trim());
        if (request.getPosition() != null) employee.setPosition(request.getPosition().trim());
        if (request.getPhone() != null) employee.setPhone(request.getPhone().trim());
        if (request.getEmail() != null) employee.setEmail(request.getEmail().trim());
        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }

        Employee updated = employeeRepository.save(employee);

        auditLogService.logAction("UPDATE_EMPLOYEE", "EMPLOYEE", updated.getId().toString(),
                "Updated details for employee " + updated.getEmployeeCode() + " (" + updated.getFullName() + ")");

        return EmployeeDTO.fromEntity(updated);
    }

    @Transactional
    public void deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        employee.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.save(employee);

        auditLogService.logAction("DEACTIVATE_EMPLOYEE", "EMPLOYEE", employee.getId().toString(),
                "Deactivated employee " + employee.getEmployeeCode() + " (" + employee.getFullName() + ")");
    }

    /** Moves the selected employees (and their meal records) to the ADMIN Trash; nothing is destroyed. */
    @Transactional
    public int bulkDeleteEmployees(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Select at least one employee.");
        }
        return trashService.trashEmployees(ids, null);
    }

    /** Ids of every employee matching the Employees-page filters, across all pages. */
    @Transactional(readOnly = true)
    public List<Long> getEmployeeAttendanceIds(LocalDate date, String query, String department,
                                               String mealStatusFilter, EmployeeStatus status) {
        return getEmployeeAttendancePage(date, query, department, mealStatusFilter, status, 0, Integer.MAX_VALUE)
                .getEmployees().getContent().stream().map(EmployeeAttendanceDTO::getId).toList();
    }

    private String generateNextEmployeeCode() {
        List<String> codes = employeeRepository.findAllEmployeeCodes();
        int maxSeq = 0;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("EMP(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        for (String code : codes) {
            if (code != null) {
                java.util.regex.Matcher m = pattern.matcher(code.trim());
                if (m.find()) {
                    try {
                        int val = Integer.parseInt(m.group(1));
                        if (val > maxSeq) {
                            maxSeq = val;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return String.format("EMP%03d", maxSeq + 1);
    }
}
