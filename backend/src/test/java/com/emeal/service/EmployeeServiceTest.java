package com.emeal.service;

import com.emeal.dto.request.CreateEmployeeRequest;
import com.emeal.dto.response.EmployeeDTO;
import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import com.emeal.exception.DuplicateResourceException;
import com.emeal.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EmployeeService employeeService;

    private CreateEmployeeRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateEmployeeRequest();
        request.setEmployeeCode("EMP010");
        request.setFirstName("Eric");
        request.setLastName("Nshimiyimana");
        request.setDepartment("IT");
        request.setPosition("Engineer");
        request.setPhone("+250788100010");
    }

    @Test
    void createEmployee_Success() {
        when(employeeRepository.existsByEmployeeCode("EMP010")).thenReturn(false);

        Employee saved = Employee.builder()
                .id(10L)
                .employeeCode("EMP010")
                .firstName("Eric")
                .lastName("Nshimiyimana")
                .department("IT")
                .position("Engineer")
                .phone("+250788100010")
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);

        EmployeeDTO result = employeeService.createEmployee(request);

        assertNotNull(result);
        assertEquals("EMP010", result.getEmployeeCode());
        assertEquals("Eric Nshimiyimana", result.getFullName());
        verify(auditLogService, times(1)).logAction(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void createEmployee_ThrowsDuplicateException_WhenCodeExists() {
        when(employeeRepository.existsByEmployeeCode("EMP010")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> employeeService.createEmployee(request)
        );

        assertTrue(ex.getMessage().contains("already exists"));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void deactivateEmployee_Success() {
        Employee emp = Employee.builder()
                .id(1L)
                .employeeCode("EMP001")
                .firstName("John")
                .lastName("Doe")
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));

        employeeService.deactivateEmployee(1L);

        assertEquals(EmployeeStatus.INACTIVE, emp.getStatus());
        verify(employeeRepository, times(1)).save(emp);
    }
}
