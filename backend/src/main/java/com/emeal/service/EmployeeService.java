package com.emeal.service;

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

    public EmployeeService(EmployeeRepository employeeRepository,
                           MealRecordRepository mealRecordRepository,
                           MealRecordService mealRecordService,
                           SettingsService settingsService,
                           AuditLogService auditLogService) {
        this.employeeRepository = employeeRepository;
        this.mealRecordRepository = mealRecordRepository;
        this.mealRecordService = mealRecordService;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeDTO> searchEmployees(String query, String department, EmployeeStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "employeeCode"));
        Page<Employee> result = employeeRepository.searchEmployees(query, department, status, pageable);
        List<EmployeeDTO> dtos = result.getContent().stream().map(EmployeeDTO::fromEntity).toList();
        return PageResponse.fromPage(result, dtos);
    }

    @Transactional(readOnly = true)
    public EmployeeAttendancePageResponse getEmployeeAttendancePage(LocalDate date, String query, String department,
                                                                    String mealStatusFilter, EmployeeStatus status,
                                                                    int page, int size) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        String currency = settingsService.getSettingValue("CURRENCY", "RWF");

        // 1. Calculate Top Summary Stats for the selected date from PostgreSQL
        long totalActiveEmployees = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long ateCount = mealRecordRepository.countByMealDateAndMealStatus(targetDate, MealStatus.ATE);
        long didNotEatCount = mealRecordRepository.countByMealDateAndMealStatus(targetDate, MealStatus.DID_NOT_EAT);
        long notRecordedCount = Math.max(0, totalActiveEmployees - ateCount - didNotEatCount);
        BigDecimal totalMealCost = mealRecordRepository.sumAmountByMealDate(targetDate);

        EmployeeAttendanceSummaryDTO summary = EmployeeAttendanceSummaryDTO.builder()
                .totalActiveEmployees(totalActiveEmployees)
                .ateCount(ateCount)
                .didNotEatCount(didNotEatCount)
                .notRecordedCount(notRecordedCount)
                .totalMealCost(totalMealCost != null ? totalMealCost : BigDecimal.ZERO)
                .currency(currency)
                .build();

        // 2. Fetch all employees matching query, department, status
        List<Employee> allMatchingEmployees = employeeRepository.findAll(Sort.by(Sort.Direction.ASC, "employeeCode"));
        
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim().toLowerCase() : null;
        String cleanDept = (department != null && !department.isBlank()) ? department.trim().toLowerCase() : null;

        List<Employee> filteredEmployees = allMatchingEmployees.stream()
                .filter(e -> {
                    if (status != null && e.getStatus() != status) return false;
                    if (cleanDept != null && !e.getDepartment().toLowerCase().equalsIgnoreCase(cleanDept)) return false;
                    if (cleanQuery != null) {
                        boolean matchCode = e.getEmployeeCode().toLowerCase().contains(cleanQuery);
                        boolean matchFirst = e.getFirstName().toLowerCase().contains(cleanQuery);
                        boolean matchLast = e.getLastName().toLowerCase().contains(cleanQuery);
                        boolean matchPhone = e.getPhone() != null && e.getPhone().toLowerCase().contains(cleanQuery);
                        return matchCode || matchFirst || matchLast || matchPhone;
                    }
                    return true;
                })
                .toList();

        // 3. Match each employee with their meal record on the selected date
        List<MealRecord> dayRecords = mealRecordRepository.findByMealDate(targetDate);
        Map<Long, MealRecord> recordMap = dayRecords.stream()
                .collect(Collectors.toMap(r -> r.getEmployee().getId(), r -> r, (r1, r2) -> r1));

        List<EmployeeAttendanceDTO> attendanceList = new ArrayList<>();

        for (Employee emp : filteredEmployees) {
            MealRecord mr = recordMap.get(emp.getId());
            String mStatus;
            BigDecimal amount = null;
            Long mealRecordId = null;
            String recordedBy = null;
            java.time.LocalDateTime mealRecordedAt = null;

            if (mr != null) {
                mealRecordId = mr.getId();
                recordedBy = mr.getRecordedBy();
                mealRecordedAt = mr.getCreatedAt();
                if (mr.getMealStatus() == MealStatus.ATE) {
                    mStatus = "ATE";
                    amount = mr.getAmount();
                } else {
                    mStatus = "DID_NOT_EAT";
                    amount = BigDecimal.ZERO;
                }
            } else {
                mStatus = "NOT_RECORDED";
                amount = null; // Displayed as "—"
            }

            // Apply mealStatusFilter if specified
            if (mealStatusFilter != null && !mealStatusFilter.isBlank() && !mealStatusFilter.equalsIgnoreCase("ALL")) {
                if (!mStatus.equalsIgnoreCase(mealStatusFilter.trim())) {
                    continue;
                }
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
        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode().trim())) {
            throw new DuplicateResourceException("Employee ID '" + request.getEmployeeCode() + "' already exists");
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
                .employeeCode(request.getEmployeeCode().toUpperCase().trim())
                .firstName(firstName)
                .lastName(lastName)
                .department(dept)
                .position(request.getPosition().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .status(EmployeeStatus.ACTIVE)
                .build();

        Employee saved = employeeRepository.save(employee);

        // If initial meal attendance is provided
        if (request.getMealStatus() != null) {
            LocalDate date = (request.getMealDate() != null) ? request.getMealDate() : LocalDate.now();

            if (request.getMealStatus() == MealStatus.DID_NOT_EAT) {
                if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    throw new BadRequestException("Amount must be 0 RWF when Meal Status is DID NOT EAT");
                }
            }

            BigDecimal mealAmount = (request.getMealStatus() == MealStatus.ATE)
                    ? (request.getAmount() != null ? request.getAmount() : new BigDecimal("1500.00"))
                    : BigDecimal.ZERO;

            RecordMealRequest mealReq = new RecordMealRequest();
            mealReq.setEmployeeId(saved.getId());
            mealReq.setMealDate(date);
            mealReq.setMealStatus(request.getMealStatus());
            mealReq.setAmount(mealAmount);
            mealRecordService.recordMeal(mealReq);
        }

        auditLogService.logAction("CREATE_EMPLOYEE", "EMPLOYEE", saved.getId().toString(),
                "Created employee " + saved.getEmployeeCode() + " (" + saved.getFullName() + ")");

        return EmployeeDTO.fromEntity(saved);
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
}
