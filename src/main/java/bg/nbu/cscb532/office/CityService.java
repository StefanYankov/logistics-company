package bg.nbu.cscb532.office;

import bg.nbu.cscb532.office.dto.CityDto;
import bg.nbu.cscb532.office.dto.CityViewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for managing {@link bg.nbu.cscb532.office.City} entities.
 * Provides CRUD operations
 */
public interface CityService {

    /**
     * Creates a new city with the provided DTO.
     *
     * @param dto the DTO containing the city creation data
     * @return the created city's view DTO
     */
    CityViewDto create(CityDto dto);


    /**
     * Updates an existing city with the provided data.
     *
     * @param id  the ID of the city to update
     * @param dto the DTO containing the updated city data
     * @return the updated city's view DTO
     */
    CityViewDto update(Long id, CityDto dto);

    /**
     * Deletes a city by ID.
     *
     * @param id the ID of the city to delete
     */
    void delete(Long id);

    /**
     * Retrieves a city by ID.
     *
     * @param id the ID of the city
     * @return the city's view DTO
     */
    CityViewDto getById(Long id);

    /**
     * Retrieves all cities exactly matching the given name.
     *
     * @param name the exact name of the city.
     * @return A list of matching CityViewDto objects
     */
    List<CityViewDto> getByName(String name);

    /**
     * Retrieves a paginated list of all cities.
     *
     * @param pageable Contains the requested page number, size, and sorting information.
     * @return A Page containing a slice of the city's view DTO objects and total element metadata.
     */
    Page<CityViewDto> getAll(Pageable pageable);
}