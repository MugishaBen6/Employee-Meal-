package com.emeal.service;

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
import com.emeal.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

        LocalDate targetDate = (request.getMealDate() != null) ? request.getMealDate() : LocalDate.now();

        if (mealRecordRepository.existsByEmployeeIdAndMealDate(employee.getId(), targetDate)) {
            throw new DuplicateResourceException("Meal already recorded for this employee today.");
        }

        BigDecimal amount = request.getAmount();
        if (amount == null) {
            amount = (request.getMealStatus() == MealStatus.DID_NOT_EAT) 
                    ? BigDecimal.ZERO 
                    : settingsService.getStandardMealPrice();
        }

        String username = getCurrentUsername();

        MealRecord record = MealRecord.builder()
                .employee(employee)
                .mealDate(targetDate)
                .mealStatus(request.getMealStatus() != null ? request.getMealStatus() : MealStatus.ATE)
                .amount(amount)
                .recordedBy(username)
                .build();

        MealRecord saved = mealRecordRepository.save(record);

        auditLogService.logAction("RECORD_MEAL", "MEAL_RECORD", saved.getId().toString(),
                "Recorded meal status " + saved.getMealStatus() + " for employee " + employee.getEmployeeCode() +
                " (" + employee.getFullName() + ") on " + targetDate + " with amount " + saved.getAmount() + " RWF");

        return MealRecordDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public QuickMealCheckResponse quickCheck(String query, LocalDate targetDate) {
        LocalDate date = (targetDate != null) ? targetDate : LocalDate.now();
        
        Employee employee = employeeRepository.findByEmployeeCode(query.trim())
                .orElseGet(() -> employeeRepository.findById(parseId(query))
                        .orElseThrow(() -> new ResourceNotFoundException("Employee not found with code or ID: " + query)));

        Optional<MealRecord> existingOpt = mealRecordRepository.findByEmployeeIdAndMealDate(employee.getId(), date);
        boolean alreadyRecorded = existingOpt.isPresent();
        MealRecordDTO existingDTO = existingOpt.map(MealRecordDTO::fromEntity).orElse(null);
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
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "mealDate", "id"));
        Page<MealRecord> result = mealRecordRepository.searchMealRecords(
                startDate, endDate, employeeId, department, status, recordedBy, pageable
        );
        List<MealRecordDTO> dtos = result.getContent().stream().map(MealRecordDTO::fromEntity).toList();
        return PageResponse.fromPage(result, dtos);
    }

    @Transactional(readOnly = true)
    public MealRecordDTO getMealRecordById(Long id) {
        MealRecord record = mealRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meal record not found with id: " + id));
        return MealRecordDTO.fromEntity(record);
    }

    @Transactional(readOnly = true)
    public List<MealRecordDTO> getEmployeeMealHistory(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }
        return mealRecordRepository.findByEmployeeIdOrderByMealDateDesc(employeeId)
                .stream().map(MealRecordDTO::fromEntity).toList();
    }

    @Transactional
    public MealRecordDTO updateMealRecord(Long id, UpdateMealRecordRequest request) {
        MealRecord record = mealRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meal record not found with id: " + id));

        record.setMealStatus(request.getMealStatus());
        record.setAmount(request.getAmount());
        MealRecord updated = mealRecordRepository.save(record);

        auditLogService.logAction("UPDATE_MEAL_RECORD", "MEAL_RECORD", updated.getId().toString(),
                "Updated meal record ID " + id + " for employee " + record.getEmployee().getEmployeeCode() +
                ": status=" + request.getMealStatus() + ", amount=" + request.getAmount());

        return MealRecordDTO.fromEntity(updated);
    }

    @Transactional
    public void deleteMealRecord(Long id) {
        MealRecord record = mealRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meal record not found with id: " + id));

        mealRecordRepository.delete(record);

        auditLogService.logAction("DELETE_MEAL_RECORD", "MEAL_RECORD", id.toString(),
                "Deleted meal record ID " + id + " for employee " + record.getEmployee().getEmployeeCode());
    }

    private Long parseId(String val) {
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUsername();
        }
        return "admin";
    }
}
