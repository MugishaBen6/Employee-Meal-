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

    List<MealRecord> findByMealDate(LocalDate mealDate);

    List<MealRecord> findByMealDateOrderByEmployeeEmployeeCodeAsc(LocalDate mealDate);

    List<MealRecord> findByMealDateAndEmployeeDepartmentOrderByEmployeeEmployeeCodeAsc(LocalDate mealDate, String department);

    List<MealRecord> findByEmployeeIdOrderByMealDateDesc(Long employeeId);

    @Query("SELECT COUNT(m) FROM MealRecord m WHERE m.mealDate = :date AND m.mealStatus = :status")
    long countByMealDateAndMealStatus(@Param("date") LocalDate date, @Param("status") MealStatus status);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM MealRecord m WHERE m.mealDate = :date AND m.mealStatus = 'ATE'")
    BigDecimal sumAmountByMealDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM MealRecord m WHERE m.mealDate BETWEEN :startDate AND :endDate AND m.mealStatus = 'ATE'")
    BigDecimal sumAmountByMealDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM MealRecord m WHERE m.mealStatus = com.emeal.entity.MealStatus.ATE")
    BigDecimal sumTotalAmount();

    @Query("SELECT COUNT(m) FROM MealRecord m WHERE m.mealDate BETWEEN :startDate AND :endDate AND m.mealStatus = :status")
    long countByMealDateBetweenAndMealStatus(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("status") MealStatus status);

    @Query("SELECT m.mealDate, m.mealStatus, SUM(m.amount), COUNT(m) " +
           "FROM MealRecord m " +
           "WHERE m.mealDate BETWEEN :startDate AND :endDate " +
           "GROUP BY m.mealDate, m.mealStatus")
    List<Object[]> getDailyAggregatesBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
