package bg.nbu.cscb532.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Employee entities and their specific queries.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    /**
     * Finds an employee by their unique registration number.
     */
    Optional<Employee> findByEmployeeNumber(String employeeNumber);

}