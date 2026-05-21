package bg.nbu.cscb532.company;

import bg.nbu.cscb532.company.dto.CompanyDto;
import bg.nbu.cscb532.company.dto.CompanyUpdateDto;
import bg.nbu.cscb532.company.dto.CompanyViewDto;
import bg.nbu.cscb532.office.CityRepository;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.location.AddressDetails;
import bg.nbu.cscb532.shared.location.AddressDetailsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Core business logic implementation for the Company domain.
 * Enforces transactional boundaries and strictly typed error handling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CityRepository cityRepository;

    /** {@inheritDoc} */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyViewDto create(CompanyDto dto) {
        log.debug("Attempting to create a new company");

        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        if (companyRepository.findByName(dto.name()).isPresent()) {
            log.warn("Company creation failed. Name [{}] already exists.", dto.name());
            throw new BusinessException(ErrorCode.COMPANY_NAME_DUPLICATE);
        }
        if (companyRepository.findByRegistrationNumber(dto.registrationNumber()).isPresent()) {
            log.warn("Company creation failed. Registration number [{}] already exists.", dto.registrationNumber());
            throw new BusinessException(ErrorCode.COMPANY_REGISTRATION_DUPLICATE);
        }

        Company company = Company.builder()
                .name(dto.name().trim())
                .registrationNumber(dto.registrationNumber().trim())
                .addressDetails(buildAddressDetails(dto.addressDetails()))
                .build();

        Company savedCompany = companyRepository.save(company);

        log.info("Successfully created company [{}] with ID: {}", savedCompany.getName(), savedCompany.getId());

        return mapToViewDto(savedCompany);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyViewDto update(Long id, CompanyUpdateDto dto) {
        log.debug("Attempting to update company with ID: {}", id);

        Objects.requireNonNull(id, Constants.DeveloperErrors.ENTITY_ID_NULL);
        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        Company company = findCompanyOrThrow(id);

        // Check for name duplication before updating
        companyRepository.findByName(dto.name().trim()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                log.warn("Company update failed. Name [{}] is already taken by company ID [{}].", dto.name(), existing.getId());
                throw new BusinessException(ErrorCode.COMPANY_NAME_DUPLICATE);
            }
        });

        // Check for registration number duplication before updating
        companyRepository.findByRegistrationNumber(dto.registrationNumber().trim()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                log.warn("Company update failed. Registration number [{}] is already taken by company ID [{}].", dto.registrationNumber(), existing.getId());
                throw new BusinessException(ErrorCode.COMPANY_REGISTRATION_DUPLICATE);
            }
        });

        company.setName(dto.name().trim());
        company.setRegistrationNumber(dto.registrationNumber().trim());
        company.setAddressDetails(buildAddressDetails(dto.addressDetails()));

        company = companyRepository.save(company);

        log.info("Successfully updated company [{}] with ID: {}", company.getName(), id);

        return mapToViewDto(company);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
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

        Optional<Company> company = companyRepository.findByName(name);

        if (company.isEmpty()) {
            log.warn("Lookup failed. Company with Name [{}] not found.", name);
            throw new BusinessException(ErrorCode.COMPANY_NOT_FOUND);
        }

        return mapToViewDto(company.get());
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
                company.getRegistrationNumber(),
                formatAddress(company.getAddressDetails())
        );
    }

    private AddressDetails buildAddressDetails(AddressDetailsDto dto) {
        return cityRepository.findById(dto.cityId())
                .map(city -> AddressDetails.builder()
                        .city(city)
                        .street(dto.street().trim())
                        .district(dto.district() != null ? dto.district().trim() : null)
                        .building(dto.building() != null ? dto.building().trim() : null)
                        .entrance(dto.entrance() != null ? dto.entrance().trim() : null)
                        .floor(dto.floor() != null ? dto.floor().trim() : null)
                        .apartment(dto.apartment() != null ? dto.apartment().trim() : null)
                        .latitude(dto.latitude())
                        .longitude(dto.longitude())
                        .build())
                .orElseThrow(() -> new BusinessException(ErrorCode.CITY_NOT_FOUND));
    }

    private String formatAddress(AddressDetails address) {
        if (address == null) {
            return "";
        }
        return Stream.of(
                        address.getStreet(),
                        address.getDistrict(),
                        formatOptionalPart("bl. ", address.getBuilding()),
                        formatOptionalPart("ent. ", address.getEntrance()),
                        formatOptionalPart("fl. ", address.getFloor()),
                        formatOptionalPart("ap. ", address.getApartment()),
                        address.getCity().getName() + " " + address.getCity().getPostcode()
                )
                .filter(part -> part != null && !part.isBlank())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private String formatOptionalPart(String prefix, String value) {
        if (value != null && !value.isBlank()) {
            return prefix + value;
        }
        return null;
    }
}
