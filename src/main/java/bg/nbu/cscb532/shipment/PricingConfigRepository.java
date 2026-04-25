package bg.nbu.cscb532.shipment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing dynamic pricing configurations.
 */
@Repository
public interface PricingConfigRepository extends JpaRepository<PricingConfig, Long> {

    /**
     * Retrieves the single, currently active pricing configuration.
     * The active configuration is the one where 'activeTo' is null.
     */
    Optional<PricingConfig> findByActiveToIsNull();
}
