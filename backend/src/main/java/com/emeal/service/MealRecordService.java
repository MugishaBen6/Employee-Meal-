package com.emeal.service;

import com.emeal.config.BusinessTime;

import com.emeal.dto.request.RecordMealRequest;
import com.emeal.dto.request.UpdateMealRecordRequest;
import com.emeal.dto.response.EmployeeDTO;
import com.emeal.dto.response.MealRecordDTO;
import com.emeal.dto.response.PageResponse;
import com.emeal.dto.response.QuickMealCheckResponse;
import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import com.emeal.entity.MealRecord;
import com.emeal.entity.MealStatus;
import com.emeal.exception.BadRequestException;
import com.emeal.exception.DuplicateResourceException;
import com.emeal.exception.ResourceNotFoundException;
import com.emeal.repository.EmployeeRepository;
import com.emeal.repository.MealRecordRepository;
import com.emeal.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MealRecordService {

    private final MealRecordRepository mealRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final SettingsService settingsService;
    private final AuditLogService auditLogService;

    public MealRecordService(MealRecordRepository mealRecordRepository, EmployeeRepository employeeRepository, SettingsService settingsService, AuditLogService auditLogService) {
        this.mealRecordRepository = mealRecordRepository;
        this.employeeRepository = employeeRepository;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public MealRecordDTO recordMeal(RecordMealRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));

        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new BadRequestException("Employee is deactivated and cannot be selected for new meal records");
        }

        LocalDate targetDate = (request.getMealDate() != null) ? request.getMealDate() : BusinessTime.today();

        if (mealRecordRepository.existsByEmployeeIdAndMealDate(employee.getId(), targetDate)) {
            throw new DuplicateResourceException("A meal record already exists for " + employee.getFullName() + " on " + targetDate + ".");
        }

        MealStatus status = request.getMealStatus() != null ? request.getMealStatus() : MealStatus.ATE;
        BigDecimal amount = resolveAmount(status, request.getAmount(), null);
        String username = SecurityUtils.getCurrentUsername();

        MealRecord record = MealRecord.builder()
                .employee(employee)
                .mealDate(targetDate)
                .mealStatus(status)
                .amount(amount)
                .recordedBy(username)
                .build();

        MealRecord saved = mealRecordRepository.save(record);

        auditLogService.logActionSync("RECORD_MEAL", "MEAL_RECORD", saved.getId().toString(),
                "Recorded meal status " + saved.getMealStatus() + " for employee " + employee.getEmployeeCode() +
                " (" + employee.getFullName() + ") on " + targetDate + " with amount " + saved.getAmount() + " RWF");

        return MealRecordDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public QuickMealCheckResponse quickCheck(String query, LocalDate targetDate) {
        LocalDate date = (targetDate != null) ? targetDate : BusinessTime.today();
        String q = query != null ? query.trim() : "";

        Employee employee = null;
        Long id = parseId(q);
        if (id > 0) {
            employee = employeeRepository.findById(id).orElse(null);
        }
        if (employee == null) {
            employee = employeeRepository.findByEmployeeCode(q).orElse(null);
        }
        if (employee == null) {
            List<Employee> matches = employeeRepository.quickSearchActiveEmployees(q, PageRequest.of(0, 1));
            if (!matches.isEmpty()) {
                employee = matches.get(0);
            }
        }
        if (employee == null) {
            throw new ResourceNotFoundException("Employee not found with code, ID, or name: " + query);
        }

        Optional<MealRecord> existingOpt = mealRecordRepository.findByEmployeeIdAndMealDate(employee.getId(), date);
        boolean alreadyRecorded = existingOpt.isPresent();
        // A USER may learn that a record exists (duplicate prevention) but must not
        // see the details of a record created by another account.
        MealRecordDTO existingDTO = existingOpt
                .filter(record -> !SecurityUtils.isUserRole() || ownsRecord(record))
                .map(MealRecordDTO::fromEntity)
                .orElse(null);
        BigDecimal defaultPrice = settingsService.getStandardMealPrice();

        return QuickMealCheckResponse.builder()
                .employee(EmployeeDTO.fromEntity(employee))
                .alreadyRecordedToday(alreadyRecorded)
                .todayRecord(existingDTO)
                .defaultMealPrice(defaultPrice)
                .targetDate(date)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<MealRecordDTO> searchMealRecords(LocalDate startDate,
                                                         LocalDate endDate,
                                                         Long employeeId,
                                                         String department,
                                                         MealStatus status,
                                                         String recordedBy,
                                                         int page,
                                                         int size) {
        final String ownerFilter;
        if (SecurityUtils.isUserRole()) {
            ownerFilter = SecurityUtils.getCurrentUsername();
        } else {
            ownerFilter = recordedBy;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "mealDate", "id"));

        org.springframework.data.jpa.domain.Specification<MealRecord> spec = (root, q, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("mealDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("mealDate"), endDate));
            }
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            }
            if (department != null && !department.isBlank()) {
                predicates.add(cb.equal(root.get("employee").get("department"), department.trim()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("mealStatus"), status));
            }
            if (ownerFilter != null && !ownerFilter.isBlank()) {
                if (SecurityUtils.isUserRole()) {
                    predicates.add(cb.equal(root.get("recordedBy"), ownerFilter));
                } else {
                    predicates.add(cb.like(cb.lower(root.get("recordedBy")), "%" + ownerFilter.trim().toLowerCase() + "%"));
                }
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<MealRecord> result = mealRecordRepository.findAll(spec, pageable);
        List<MealRecordDTO> dtos = result.getContent().stream().map(MealRecordDTO::fromEntity).toList();
        return PageResponse.fromPage(result, dtos);
    }

    @Transactional(readOnly = true)
    public MealRecordDTO getMealRecordById(Long id) {
        MealRecord record = findRecord(id);
        assertCanView(record);
        return MealRecordDTO.fromEntity(record);
    }

    @Transactional(readOnly = true)
    public List<MealRecordDTO> getEmployeeMealHistory(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }
        List<MealRecord> records = mealRecordRepository.findByEmployeeIdOrderByMealDateDesc(employeeId);
        if (SecurityUtils.isUserRole()) {
            String caller = SecurityUtils.getCurrentUsername();
            records = records.stream()
                    .filter(record -> caller.equals(record.getRecordedBy()))
                    .toList();
        }
        return records.stream().map(MealRecordDTO::fromEntity).toList();
    }

    @Transactional
    public MealRecordDTO updateMealRecord(Long id, UpdateMealRecordRequest request) {
        MealRecord record = findRecord(id);
        assertCanEdit(record);

        MealStatus prevStatus = record.getMealStatus();
        BigDecimal prevAmount = record.getAmount();
        String prevRecordedBy = record.getRecordedBy();

        MealStatus newStatus = request.getMealStatus();
        BigDecimal newAmount = resolveAmount(newStatus, request.getAmount(), record);

        record.setMealStatus(newStatus);
        record.setAmount(newAmount);
        record.setRecordedBy(prevRecordedBy);
        MealRecord updated = mealRecordRepository.save(record);

        auditLogService.logActionSync("UPDATE_MEAL_RECORD", "MEAL_RECORD", updated.getId().toString(),
                "Updated meal record ID " + id + " for employee " + record.getEmployee().getEmployeeCode() +
                " (" + record.getEmployee().getFullName() + ") on " + record.getMealDate() +
                " | PREVIOUS: status=" + prevStatus + ", amount=" + prevAmount + " RWF, recordedBy=" + prevRecordedBy +
                " | NEW: status=" + newStatus + ", amount=" + newAmount + " RWF, recordedBy=" + updated.getRecordedBy());

        return MealRecordDTO.fromEntity(updated);
    }

    @Transactional
    public void deleteMealRecord(Long id) {
        if (SecurityUtils.isUserRole()) {
            throw new AccessDeniedException("USER accounts cannot delete meal records.");
        }
        MealRecord record = findRecord(id);

        mealRecordRepository.delete(record);

        auditLogService.logActionSync("DELETE_MEAL_RECORD", "MEAL_RECORD", id.toString(),
                "Deleted meal record ID " + id + " for employee " + record.getEmployee().getEmployeeCode());
    }

    private MealRecord findRecord(Long id) {
        return mealRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meal record not found with id: " + id));
    }

    private void assertCanView(MealRecord record) {
        if (SecurityUtils.isUserRole() && !ownsRecord(record)) {
            throw new AccessDeniedException("Access denied: you can only view your own meal records.");
        }
    }

    private void assertCanEdit(MealRecord record) {
        if (SecurityUtils.isUserRole() && !ownsRecord(record)) {
            throw new AccessDeniedException("Access denied: you can only edit your own meal records.");
        }
    }

    private boolean ownsRecord(MealRecord record) {
        return SecurityUtils.getCurrentUsername().equals(record.getRecordedBy());
    }

    /**
     * Amount for a new or updated record.
     * <ul>
     *   <li>DID_NOT_EAT is always 0.</li>
     *   <li>USER accounts never choose the amount: a client-supplied value is ignored. ATE uses the
     *       configured standard price, except that a record that is already ATE keeps its stored
     *       amount, so history is not repriced.</li>
     *   <li>Other roles keep the existing rule: a positive requested amount, otherwise the standard price.</li>
     * </ul>
     *
     * @param existing the record being updated, or null when creating a new record
     */
    private BigDecimal resolveAmount(MealStatus status, BigDecimal requestedAmount, MealRecord existing) {
        if (status == MealStatus.DID_NOT_EAT) {
            return BigDecimal.ZERO;
        }

        if (SecurityUtils.isUserRole()) {
            if (existing != null && existing.getMealStatus() == MealStatus.ATE
                    && existing.getAmount() != null && existing.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                return existing.getAmount();
            }
            return settingsService.getStandardMealPrice();
        }

        BigDecimal amount = requestedAmount;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            amount = settingsService.getStandardMealPrice();
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("ATE records must have a valid positive amount.");
        }
        return amount;
    }

    private Long parseId(String val) {
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
