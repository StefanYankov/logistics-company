package bg.nbu.cscb532.office;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfficeRepository extends JpaRepository<Office, Long> {

    // TODO: (SaaS Architecture): If transitioning to a Multi-Tenant platform (e.g., Econt and DHL sharing this DB),
    //  add `Long companyId` to every single query in this repository to prevent cross-tenant data leaks.

    /**
     * Finds all offices located in a specific city.
     */
    List<Office> findAllByAddressDetailsCityId(Long cityId);

    /**
     * Advanced Geospatial Query: Finds the nearest offices to a specific GPS coordinate within a given radius.
     * Uses the Haversine formula in native PostgreSQL to calculate spherical distance in kilometers.
     *
     * @param lat      The latitude of the user's location
     * @param lon      The longitude of the user's location
     * @param radiusKm The maximum distance in kilometers
     * @return A list of offices ordered from closest to furthest
     */
    @Query(value = """
            SELECT * FROM offices o
            WHERE o.latitude IS NOT NULL AND o.longitude IS NOT NULL
              AND (
                6371 * acos(
                    cos(radians(:lat)) * cos(radians(o.latitude))
                    * cos(radians(o.longitude) - radians(:lon))
                    + sin(radians(:lat)) * sin(radians(o.latitude))
                )
              ) <= :radiusKm
            ORDER BY (
                6371 * acos(
                    cos(radians(:lat)) * cos(radians(o.latitude))
                    * cos(radians(o.longitude) - radians(:lon))
                    + sin(radians(:lat)) * sin(radians(o.latitude))
                )
            ) ASC
            """, nativeQuery = true)
    List<Office> findNearestOfficesWithinRadius(@Param("lat") double lat, 
                                                @Param("lon") double lon, 
                                                @Param("radiusKm") double radiusKm);
}