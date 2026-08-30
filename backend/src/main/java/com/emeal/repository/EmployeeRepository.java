package com.emeal.repository;

import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCode(String employeeCode);

    long countByStatus(EmployeeStatus status);

    @Query("SELECT DISTINCT e.department FROM Employee e ORDER BY e.department ASC")
    List<String> findAllDepartments();

    @Query("SELECT e FROM Employee e WHERE " +
           "(:query IS NULL OR " +
           "LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.phone) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:department IS NULL OR e.department = :department) AND " +
           "(:status IS NULL OR e.status = :status)")
    Page<Employee> searchEmployees(@Param("query") String query,
                                   @Param("department") String department,
                                   @Param("status") EmployeeStatus status,
                                   Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE e.status = 'ACTIVE' AND (" +
           "LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Employee> quickSearchActiveEmployees(@Param("query") String query, Pageable pageable);
}
