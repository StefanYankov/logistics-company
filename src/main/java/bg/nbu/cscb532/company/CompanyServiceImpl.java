package bg.nbu.cscb532.company;

import bg.nbu.cscb532.company.dto.CompanyDto;
import bg.nbu.cscb532.company.dto.CompanyViewDto;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Core business logic implementation for the Company domain.
 * Enforces transactional boundaries and strictly typed error handling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
// TODO: (Security) Add @PreAuthorize("hasRole('ADMIN')") to mutating methods once JWT is implemented
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CompanyViewDto create(CompanyDto dto) {
        log.debug("Attempting to create a new company");

        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        if (companyRepository.findByRegistrationNumber(dto.registrationNumber()).isPresent()) {
            log.warn("Company creation failed. Registration number [{}] already exists.", dto.registrationNumber());
            throw new BusinessException(ErrorCode.COMPANY_REGISTRATION_DUPLICATE);
        }

        Company company = Company.builder()
                .name(dto.name().trim())
                .registrationNumber(dto.registrationNumber().trim())
                .build();

        Company savedCompany = companyRepository.save(company);
        
        log.info("Successfully created company [{}] with ID: {}", savedCompany.getName(), savedCompany.getId());
        
        return mapToViewDto(savedCompany);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CompanyViewDto update(Long id, CompanyDto dto) {
        log.debug("Attempting to update company with ID: {}", id);
        
        Objects.requireNonNull(id, Constants.DeveloperErrors.ENTITY_ID_NULL);
        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        Company company = findCompanyOrThrow(id);

        company.setName(dto.name().trim());

        company = companyRepository.save(company);
        
        log.info("Successfully updated company [{}] with ID: {}", company.getName(), id);
        
        return mapToViewDto(company);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(Long id) {
        log.debug("Attempting to delete company with ID: {}", id);
        
        Objects.requireNonNull(id, Constants.DeveloperErrors.ENTITY_ID_NULL);

        Company company = findCompanyOrThrow(id);
        companyRepository.delete(company);
        
        log.info("Successfully deleted company with ID: {}", id);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public CompanyViewDto getById(Long id) {
        log.debug("Fetching company by ID: {}", id);
        
        Objects.requireNonNull(id, Constants.DeveloperErrors.ENTITY_ID_NULL);
        
        return mapToViewDto(findCompanyOrThrow(id));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public CompanyViewDto getByName(String name) {
        log.debug("Fetching company by Name: {}", name);

        Objects.requireNonNull(name, Constants.DeveloperErrors.NAME_NULL);

        Company company = companyRepository.getCompanyByName(name);
        
        if (company == null) {
            log.warn("Lookup failed. Company with Name [{}] not found.", name);
            throw new BusinessException(ErrorCode.COMPANY_NOT_FOUND);
        }

        return mapToViewDto(company);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<CompanyViewDto> getAll(Pageable pageable) {
        log.debug("Fetching paginated list of companies");
        Objects.requireNonNull(pageable, Constants.DeveloperErrors.PAGEABLE_NULL);

        Page<Company> companies = companyRepository.findAll(pageable);
        
        return companies.map(this::mapToViewDto);
    }

    /**
     * Centralized lookup and exception logic to DRY up the service methods.
     */
    private Company findCompanyOrThrow(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Lookup failed. Company with ID [{}] not found.", id);
                    return new BusinessException(ErrorCode.COMPANY_NOT_FOUND);
                });
    }

    /**
     * Manual mapper.
     */
    private CompanyViewDto mapToViewDto(Company company) {
        if (company == null) {
            return null;
        }
        return new CompanyViewDto(
                company.getId(),
                company.getName(),
                company.getRegistrationNumber()
        );
    }
}