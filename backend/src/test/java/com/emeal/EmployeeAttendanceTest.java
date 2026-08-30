package com.emeal;

import com.emeal.dto.request.CreateEmployeeRequest;
import com.emeal.dto.request.RecordMealRequest;
import com.emeal.dto.response.EmployeeAttendanceDTO;
import com.emeal.dto.response.EmployeeAttendancePageResponse;
import com.emeal.dto.response.EmployeeDTO;
import com.emeal.entity.MealStatus;
import com.emeal.exception.BadRequestException;
import com.emeal.exception.DuplicateResourceException;
import com.emeal.repository.EmployeeRepository;
import com.emeal.repository.MealRecordRepository;
import com.emeal.service.EmployeeService;
import com.emeal.service.MealRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class EmployeeAttendanceTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private MealRecordService mealRecordService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MealRecordRepository mealRecordRepository;

    @BeforeEach
    void setUp() {
        mealRecordRepository.deleteAll();
        employeeRepository.deleteAll();
    }

    @Test
    void testEmployeeAttendance_Ate_DidNotEat_AndNotRecorded() {
        LocalDate testDate = LocalDate.of(2026, 8, 30);

        // 1. Create 3 Employees
        CreateEmployeeRequest emp1Req = new CreateEmployeeRequest();
        emp1Req.setEmployeeCode("EMP001");
        emp1Req.setFirstName("John");
        emp1Req.setLastName("Doe");
        emp1Req.setDepartment("Production");
        emp1Req.setPosition("Operator");
        emp1Req.setPhone("+250788100001");
        EmployeeDTO emp1 = employeeService.createEmployee(emp1Req);

        CreateEmployeeRequest emp2Req = new CreateEmployeeRequest();
        emp2Req.setEmployeeCode("EMP002");
        emp2Req.setFirstName("Eric");
        emp2Req.setLastName("Mugabo");
        emp2Req.setDepartment("Logistics");
        emp2Req.setPosition("Driver");
        emp2Req.setPhone("+250788100002");
        EmployeeDTO emp2 = employeeService.createEmployee(emp2Req);

        CreateEmployeeRequest emp3Req = new CreateEmployeeRequest();
        emp3Req.setEmployeeCode("EMP003");
        emp3Req.setFirstName("Alice");
        emp3Req.setLastName("Uwase");
        emp3Req.setDepartment("Finance");
        emp3Req.setPosition("Accountant");
        emp3Req.setPhone("+250788100003");
        EmployeeDTO emp3 = employeeService.createEmployee(emp3Req);

        // 2. Record EMP001 as ATE (1500 RWF)
        RecordMealRequest meal1 = new RecordMealRequest();
        meal1.setEmployeeId(emp1.getId());
        meal1.setMealDate(testDate);
        meal1.setMealStatus(MealStatus.ATE);
        meal1.setAmount(new BigDecimal("1500.00"));
        mealRecordService.recordMeal(meal1);

        // 3. Record EMP002 as DID_NOT_EAT (0 RWF)
        RecordMealRequest meal2 = new RecordMealRequest();
        meal2.setEmployeeId(emp2.getId());
        meal2.setMealDate(testDate);
        meal2.setMealStatus(MealStatus.DID_NOT_EAT);
        meal2.setAmount(BigDecimal.ZERO);
        mealRecordService.recordMeal(meal2);

        // EMP003 has NO record for testDate (NOT_RECORDED)

        // 4. Query Attendance endpoint service for testDate
        EmployeeAttendancePageResponse response = employeeService.getEmployeeAttendancePage(
                testDate, null, null, null, null, 0, 10);

        assertNotNull(response);
        assertNotNull(response.getSummary());

        // Verify Top Summary Cards
        assertEquals(3, response.getSummary().getTotalActiveEmployees());
        assertEquals(1, response.getSummary().getAteCount());
        assertEquals(1, response.getSummary().getDidNotEatCount());
        assertEquals(1, response.getSummary().getNotRecordedCount());
        assertEquals(0, new BigDecimal("1500.00").compareTo(response.getSummary().getTotalMealCost()));

        // Verify Individual Employee Rows
        List<EmployeeAttendanceDTO> list = response.getEmployees().getContent();
        assertEquals(3, list.size());

        EmployeeAttendanceDTO row1 = list.stream().filter(e -> e.getEmployeeCode().equals("EMP001")).findFirst().orElseThrow();
        assertEquals("John Doe", row1.getFullName());
        assertEquals("+250788100001", row1.getTelephone());
        assertEquals("Operator", row1.getPosition());
        assertEquals("ATE", row1.getMealStatus());
        assertEquals(0, new BigDecimal("1500.00").compareTo(row1.getAmount()));

        EmployeeAttendanceDTO row2 = list.stream().filter(e -> e.getEmployeeCode().equals("EMP002")).findFirst().orElseThrow();
        assertEquals("Eric Mugabo", row2.getFullName());
        assertEquals("DID_NOT_EAT", row2.getMealStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(row2.getAmount()));

        EmployeeAttendanceDTO row3 = list.stream().filter(e -> e.getEmployeeCode().equals("EMP003")).findFirst().orElseThrow();
        assertEquals("Alice Uwase", row3.getFullName());
        assertEquals("NOT_RECORDED", row3.getMealStatus());
        assertNull(row3.getAmount());
    }

    @Test
    void testCreateEmployeeForm_WithSingleEmployeeName_AndMealRecording() {
        LocalDate testDate = LocalDate.of(2026, 8, 30);

        // Test 1: Create Employee with ATE status and 1500 RWF
        CreateEmployeeRequest req1 = new CreateEmployeeRequest();
        req1.setEmployeeCode("EMP001");
        req1.setEmployeeName("John Doe");
        req1.setPhone("0788123456");
        req1.setPosition("Machine Operator");
        req1.setMealStatus(MealStatus.ATE);
        req1.setAmount(new BigDecimal("1500.00"));
        req1.setMealDate(testDate);

        EmployeeDTO created1 = employeeService.createEmployee(req1);
        assertEquals("EMP001", created1.getEmployeeCode());
        assertEquals("John Doe", created1.getFullName());
        assertEquals("Machine Operator", created1.getPosition());

        // Verify attendance on testDate
        EmployeeAttendancePageResponse resp1 = employeeService.getEmployeeAttendancePage(
                testDate, null, null, null, null, 0, 10);
        assertEquals(1, resp1.getSummary().getAteCount());
        assertEquals(0, new BigDecimal("1500.00").compareTo(resp1.getSummary().getTotalMealCost()));

        // Test 2: Create Employee with DID_NOT_EAT status and 0 RWF
        CreateEmployeeRequest req2 = new CreateEmployeeRequest();
        req2.setEmployeeCode("EMP002");
        req2.setEmployeeName("Alice Uwase");
        req2.setPhone("0798123456");
        req2.setPosition("Driver");
        req2.setMealStatus(MealStatus.DID_NOT_EAT);
        req2.setAmount(BigDecimal.ZERO);
        req2.setMealDate(testDate);

        EmployeeDTO created2 = employeeService.createEmployee(req2);
        assertEquals("EMP002", created2.getEmployeeCode());
        assertEquals("Alice Uwase", created2.getFullName());

        EmployeeAttendancePageResponse resp2 = employeeService.getEmployeeAttendancePage(
                testDate, null, null, null, null, 0, 10);
        assertEquals(1, resp2.getSummary().getDidNotEatCount());

        // Test 3: Validation failure when DID_NOT_EAT has positive amount
        CreateEmployeeRequest invalidReq = new CreateEmployeeRequest();
        invalidReq.setEmployeeCode("EMP003");
        invalidReq.setEmployeeName("Eric Mugabo");
        invalidReq.setPhone("0728123456");
        invalidReq.setPosition("Technician");
        invalidReq.setMealStatus(MealStatus.DID_NOT_EAT);
        invalidReq.setAmount(new BigDecimal("1500.00"));

        assertThrows(BadRequestException.class, () -> employeeService.createEmployee(invalidReq));

        // Test 4: Duplicate Employee ID validation
        CreateEmployeeRequest duplicateReq = new CreateEmployeeRequest();
        duplicateReq.setEmployeeCode("EMP001");
        duplicateReq.setEmployeeName("Another John");
        duplicateReq.setPhone("0788999999");
        duplicateReq.setPosition("Operator");

        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(duplicateReq));
    }
}
