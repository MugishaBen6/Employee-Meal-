package com.emeal.service;

import com.emeal.dto.request.RecordMealRequest;
import com.emeal.dto.response.MealRecordDTO;
import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import com.emeal.entity.MealRecord;
import com.emeal.entity.MealStatus;
import com.emeal.exception.BadRequestException;
import com.emeal.exception.DuplicateResourceException;
import com.emeal.repository.EmployeeRepository;
import com.emeal.repository.MealRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealRecordServiceTest {

    @Mock
    private MealRecordRepository mealRecordRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SettingsService settingsService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private MealRecordService mealRecordService;

    private Employee activeEmployee;
    private Employee inactiveEmployee;

    @BeforeEach
    void setUp() {
        activeEmployee = Employee.builder()
                .id(1L)
                .employeeCode("EMP001")
                .firstName("John")
                .lastName("Doe")
                .department("Production")
                .position("Operator")
                .phone("+250788100001")
                .status(EmployeeStatus.ACTIVE)
                .build();

        inactiveEmployee = Employee.builder()
                .id(2L)
                .employeeCode("EMP002")
                .firstName("Jane")
                .lastName("Smith")
                .department("Finance")
                .position("Analyst")
                .phone("+250788100002")
                .status(EmployeeStatus.INACTIVE)
                .build();
    }

    @Test
    void recordMeal_Success() {
        LocalDate today = LocalDate.now();
        RecordMealRequest request = new RecordMealRequest();
        request.setEmployeeId(1L);
        request.setMealDate(today);
        request.setMealStatus(MealStatus.ATE);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(activeEmployee));
        when(mealRecordRepository.existsByEmployeeIdAndMealDate(1L, today)).thenReturn(false);
        when(settingsService.getStandardMealPrice()).thenReturn(new BigDecimal("1500.00"));

        MealRecord savedRecord = MealRecord.builder()
                .id(100L)
                .employee(activeEmployee)
                .mealDate(today)
                .mealStatus(MealStatus.ATE)
                .amount(new BigDecimal("1500.00"))
                .recordedBy("admin")
                .build();

        when(mealRecordRepository.save(any(MealRecord.class))).thenReturn(savedRecord);

        MealRecordDTO result = mealRecordService.recordMeal(request);

        assertNotNull(result);
        assertEquals("EMP001", result.getEmployeeCode());
        assertEquals(MealStatus.ATE, result.getMealStatus());
        assertEquals(new BigDecimal("1500.00"), result.getAmount());
        verify(auditLogService, times(1)).logAction(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void recordMeal_ThrowsDuplicateException_WhenAlreadyRecorded() {
        LocalDate today = LocalDate.now();
        RecordMealRequest request = new RecordMealRequest();
        request.setEmployeeId(1L);
        request.setMealDate(today);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(activeEmployee));
        when(mealRecordRepository.existsByEmployeeIdAndMealDate(1L, today)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> mealRecordService.recordMeal(request)
        );

        assertEquals("Meal already recorded for this employee today.", exception.getMessage());
        verify(mealRecordRepository, never()).save(any());
    }

    @Test
    void recordMeal_ThrowsBadRequestException_WhenEmployeeInactive() {
        RecordMealRequest request = new RecordMealRequest();
        request.setEmployeeId(2L);
        request.setMealDate(LocalDate.now());

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(inactiveEmployee));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> mealRecordService.recordMeal(request)
        );

        assertTrue(exception.getMessage().contains("deactivated"));
        verify(mealRecordRepository, never()).save(any());
    }
}
