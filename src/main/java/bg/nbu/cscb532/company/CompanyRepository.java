package bg.nbu.cscb532.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    /**
     * Finds a company by its unique registration number.
     */
    Optional<Company> findByRegistrationNumber(String registrationNumber);

    /**
     * Finds a company by its unique name.
     */
    Optional<Company> findByName(String name);
}
