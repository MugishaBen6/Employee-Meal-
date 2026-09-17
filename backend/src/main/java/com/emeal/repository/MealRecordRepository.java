package com.emeal.repository;

import com.emeal.entity.MealRecord;
import com.emeal.entity.MealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealRecordRepository extends JpaRepository<MealRecord, Long>, JpaSpecificationExecutor<MealRecord> {

    Optional<MealRecord> findByEmployeeIdAndMealDate(Long employeeId, LocalDate mealDate);

    boolean existsByEmployeeIdAndMealDate(Long employeeId, LocalDate mealDate);

    @Query("SELECT m FROM MealRecord m JOIN FETCH m.employee WHERE m.mealDate = :mealDate")
    List<MealRecord> findByMealDate(@Param("mealDate") LocalDate mealDate);

    List<MealRecord> findByMealDateOrderByEmployeeEmployeeCodeAsc(LocalDate mealDate);

    List<MealRecord> findByMealDateAndEmployeeDepartmentOrderByEmployeeEmployeeCodeAsc(LocalDate mealDate, String department);

    List<MealRecord> findByEmployeeIdOrderByMealDateDesc(Long employeeId);

    @Query("SELECT MAX(m.mealDate) FROM MealRecord m")
    LocalDate findLatestMealDate();

    @Query("SELECT MIN(m.mealDate) FROM MealRecord m")
    LocalDate findEarliestMealDate();

    @Query("SELECT COUNT(m) FROM MealRecord m WHERE m.mealStatus = com.emeal.entity.MealStatus.ATE")
    long countTotalMealsAte();

    @Query("SELECT COUNT(m) FROM MealRecord m WHERE m.mealStatus = com.emeal.entity.MealStatus.ATE AND m.employee.status = com.emeal.entity.EmployeeStatus.ACTIVE")
    long countTotalMealsAteForActiveEmployees();

    @Query("SELECT COUNT(m) FROM MealRecord m WHERE m.mealDate = :date AND m.mealStatus = :status")
    long countByMealDateAndMealStatus(@Param("date") LocalDate date, @Param("status") MealStatus status);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM MealRecord m WHERE m.mealDate = :date AND m.mealStatus = 'ATE'")
    BigDecimal sumAmountByMealDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM MealRecord m WHERE m.mealDate BETWEEN :startDate AND :endDate AND m.mealStatus = 'ATE'")
    BigDecimal sumAmountByMealDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM MealRecord m WHERE m.mealStatus = com.emeal.entity.MealStatus.ATE")
    BigDecimal sumTotalAmount();

    @Query("SELECT COALESCE(SUM(CASE WHEN m.amount > 0 THEN m.amount ELSE 600.00 END), 0) FROM MealRecord m WHERE m.mealStatus = com.emeal.entity.MealStatus.ATE AND m.employee.status = com.emeal.entity.EmployeeStatus.ACTIVE")
    BigDecimal sumTotalAmountForActiveEmployees();

    @Query("SELECT COUNT(m) FROM MealRecord m WHERE m.mealDate BETWEEN :startDate AND :endDate AND m.mealStatus = :status")
    long countByMealDateBetweenAndMealStatus(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("status") MealStatus status);

    @Query("SELECT m.mealDate, m.mealStatus, SUM(m.amount), COUNT(m) " +
           "FROM MealRecord m " +
           "WHERE m.mealDate BETWEEN :startDate AND :endDate " +
           "GROUP BY m.mealDate, m.mealStatus")
    List<Object[]> getDailyAggregatesBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT e.department, COUNT(DISTINCT e.id), " +
           "COALESCE(SUM(CASE WHEN m.mealStatus = com.emeal.entity.MealStatus.ATE THEN 1 ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN m.mealStatus = com.emeal.entity.MealStatus.DID_NOT_EAT THEN 1 ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN m.mealStatus = com.emeal.entity.MealStatus.ATE THEN m.amount ELSE 0 END), 0) " +
           "FROM Employee e " +
           "LEFT JOIN MealRecord m ON m.employee.id = e.id AND m.mealDate = :date " +
           "WHERE e.status = com.emeal.entity.EmployeeStatus.ACTIVE " +
           "GROUP BY e.department " +
           "ORDER BY e.department ASC")
    List<Object[]> getDepartmentStatsForDate(@Param("date") LocalDate date);
}
