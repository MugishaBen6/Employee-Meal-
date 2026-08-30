package com.emeal.repository;

import com.emeal.entity.MealRecord;
import com.emeal.entity.MealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {

    Optional<MealRecord> findByEmployeeIdAndMealDate(Long employeeId, LocalDate mealDate);

    boolean existsByEmployeeIdAndMealDate(Long employeeId, LocalDate mealDate);

    List<MealRecord> findByMealDate(LocalDate mealDate);

    List<MealRecord> findByEmployeeIdOrderByMealDateDesc(Long employeeId);

    @Query("SELECT COUNT(m) FROM MealRecord m WHERE m.mealDate = :date AND m.mealStatus = :status")
    long countByMealDateAndMealStatus(@Param("date") LocalDate date, @Param("status") MealStatus status);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM MealRecord m WHERE m.mealDate = :date AND m.mealStatus = 'ATE'")
    BigDecimal sumAmountByMealDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM MealRecord m WHERE m.mealDate BETWEEN :startDate AND :endDate AND m.mealStatus = 'ATE'")
    BigDecimal sumAmountByMealDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(m) FROM MealRecord m WHERE m.mealDate BETWEEN :startDate AND :endDate AND m.mealStatus = :status")
    long countByMealDateBetweenAndMealStatus(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("status") MealStatus status);

    @Query("SELECT m FROM MealRecord m JOIN m.employee e WHERE " +
           "(:startDate IS NULL OR m.mealDate >= :startDate) AND " +
           "(:endDate IS NULL OR m.mealDate <= :endDate) AND " +
           "(:employeeId IS NULL OR e.id = :employeeId) AND " +
           "(:department IS NULL OR e.department = :department) AND " +
           "(:status IS NULL OR m.mealStatus = :status) AND " +
           "(:recordedBy IS NULL OR LOWER(m.recordedBy) LIKE LOWER(CONCAT('%', :recordedBy, '%')))")
    Page<MealRecord> searchMealRecords(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("employeeId") Long employeeId,
                                       @Param("department") String department,
                                       @Param("status") MealStatus status,
                                       @Param("recordedBy") String recordedBy,
                                       Pageable pageable);

    @Query("SELECT m FROM MealRecord m JOIN m.employee e WHERE " +
           "(:startDate IS NULL OR m.mealDate >= :startDate) AND " +
           "(:endDate IS NULL OR m.mealDate <= :endDate) AND " +
           "(:department IS NULL OR e.department = :department) AND " +
           "(:status IS NULL OR m.mealStatus = :status) " +
           "ORDER BY m.mealDate DESC, e.employeeCode ASC")
    List<MealRecord> findRecordsForReport(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("department") String department,
                                           @Param("status") MealStatus status);
}
