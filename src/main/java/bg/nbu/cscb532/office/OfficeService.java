package bg.nbu.cscb532.office;

import bg.nbu.cscb532.employee.dto.EmployeeViewDto;
import bg.nbu.cscb532.office.dto.OfficeDto;
import bg.nbu.cscb532.office.dto.OfficeViewDto;
import bg.nbu.cscb532.shared.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for managing {@link bg.nbu.cscb532.office.Office} entities.
 * Provides CRUD operations and advanced search capabilities.
 */
public interface OfficeService {

    /**
     * Creates a new office with the provided DTO.
     *
     * @param dto the DTO containing the office creation data
     * @return the created office's view DTO
     * @throws BusinessException if the requested Company ID is not found (ErrorCode.COMPANY_NOT_FOUND)
     * @throws BusinessException if the requested City ID is not found (ErrorCode.CITY_NOT_FOUND)
     */
    OfficeViewDto create(OfficeDto dto);

    /**
     * Updates an existing office with the provided data.
     *
     * @param id  the ID of the office to update
     * @param dto the DTO containing the updated office data
     * @return the updated office's view DTO
     * @throws BusinessException if the office ID is not found (ErrorCode.OFFICE_NOT_FOUND)
     * @throws BusinessException if the new Company ID is not found (ErrorCode.COMPANY_NOT_FOUND)
     * @throws BusinessException if the new City ID is not found (ErrorCode.CITY_NOT_FOUND)
     */
    OfficeViewDto update(Long id, OfficeDto dto);

    /**
     * Deletes an office by ID.
     *
     * @param id the ID of the office to delete
     * @throws BusinessException if the office ID is not found (ErrorCode.OFFICE_NOT_FOUND)
     */
    void delete(Long id);

    /**
     * Retrieves an office by ID.
     *
     * @param id the ID of the office
     * @return the office's view DTO
     * @throws BusinessException if the office ID is not found (ErrorCode.OFFICE_NOT_FOUND)
     */
    OfficeViewDto getById(Long id);

    /**
     * Retrieves a paginated list of all offices.
     *
     * @param pageable Contains the requested page number, size, and sorting information.
     * @return A Page containing a slice of the office's view DTO objects and total element metadata.
     */
    Page<OfficeViewDto> getAll(Pageable pageable);

    /**
     * Retrieves all offices located in a specific city.
     * Useful for filtering operations on the frontend.
     *
     * @param cityId the ID of the city to filter by
     * @return A list of OfficeViewDto objects representing the offices in that city
     */
    List<OfficeViewDto> getOfficesByCityId(Long cityId);

    /**
     * Finds the nearest offices to a specific GPS coordinate within a given radius.
     *
     * @param lat      The latitude of the user's location
     * @param lon      The longitude of the user's location
     * @param radiusKm The maximum distance to search in kilometers
     * @return A list of OfficeViewDto objects ordered from closest to furthest
     * @throws BusinessException if the provided coordinates or radius are mathematically invalid (ErrorCode.VALIDATION_FAILED)
     */
    List<OfficeViewDto> getNearestOffices(double lat, double lon, double radiusKm);

    /**
     * Retrieves a paginated list of all OfficeClerks assigned to a specific office.
     * This is a cross-aggregate query orchestrated by the OfficeService.
     *
     * @param officeId The physical office location ID.
     * @param pageable The pagination criteria.
     * @return A paginated list of Employee DTOs representing the staff.
     * @throws BusinessException if the office ID is not found (ErrorCode.OFFICE_NOT_FOUND)
     */
    Page<EmployeeViewDto> getClerksByOfficeId(Long officeId, Pageable pageable);
}
