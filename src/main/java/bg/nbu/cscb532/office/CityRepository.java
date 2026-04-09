package bg.nbu.cscb532.office;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    /**
     * Finds a city by its exact postcode.
     * This query is backed by the uk_city_postcode database unique constraint.
     */
    Optional<City> findByPostcode(String postcode);

    /**
     * Finds all cities with the exact given name.
     */
    List<City> findAllByName(String name);
}