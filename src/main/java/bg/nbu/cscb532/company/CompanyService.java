package bg.nbu.cscb532.company;


import bg.nbu.cscb532.company.dto.CompanyDto;
import bg.nbu.cscb532.company.dto.CompanyViewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing {@link bg.nbu.cscb532.company.Company} entities.
 * Provides CRUD operations
 */
public interface CompanyService {

    /**
     * Creates a new company with the provided DTO.
     * @param dto the DTO containing the company creation data
     * @return the created company's view DTO
     */
    CompanyViewDto create (CompanyDto dto);

    /**
     * Updates an existing company with the provided data.
     * @param id the ID of the company to update
     * @param dto the DTO containing the updated company data
     * @return the updated company's view DTO
     */
    CompanyViewDto update(Long id, CompanyDto dto);

    /**
     * Deletes a company by ID.
     * @param id the ID of the company to delete
     */
    void delete(Long id);

    /**
     * Retrieves a company by ID.
     * @param id the ID of the company
     * @return the company's view DTO
     */
    CompanyViewDto getById(Long id);

    /**
     * Retrieves the company by name.
     * @param name the name of the company.
     * @return the company's view DTO
     */
    CompanyViewDto getByName(String name);

    /**
     * Retrieves a paginated list of all companies.
     *
     * @param pageable Contains the requested page number, size, and sorting information.
     * @return A Page containing a slice of CompanyViewDto objects and total element metadata.
     */
    Page<CompanyViewDto> getAll(Pageable pageable);
}