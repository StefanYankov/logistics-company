package bg.nbu.cscb532.office;

import bg.nbu.cscb532.company.Company;
import bg.nbu.cscb532.company.CompanyRepository;
import bg.nbu.cscb532.employee.OfficeClerk;
import bg.nbu.cscb532.employee.OfficeClerkRepository;
import bg.nbu.cscb532.employee.dto.EmployeeViewDto;
import bg.nbu.cscb532.office.dto.OfficeDto;
import bg.nbu.cscb532.office.dto.OfficeViewDto;
import bg.nbu.cscb532.office.dto.OperatingHourViewDto;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.location.AddressDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Core business logic implementation for managing the office entity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
// TODO: (Security) Add @PreAuthorize("hasRole('ADMIN')") to mutating methods once JWT is implemented
public class OfficeServiceImpl implements OfficeService {

    private final OfficeRepository officeRepository;
    private final CityRepository cityRepository;
    private final CompanyRepository companyRepository;
    private final OfficeClerkRepository officeClerkRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public OfficeViewDto create(OfficeDto dto) {
        log.debug("Attempting to create a new office");

        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> {
                    log.warn("Office creation failed. Company ID [{}] not found.", dto.companyId());
                    return new BusinessException(ErrorCode.COMPANY_NOT_FOUND);
                });

        var city = cityRepository.findById(dto.address().cityId())
                .orElseThrow(() -> {
                    log.warn("Office creation failed. City ID [{}] not found.", dto.address().cityId());
                    return new BusinessException(ErrorCode.CITY_NOT_FOUND);
                });

        var addressDetails = AddressDetails.builder()
                .city(city)
                .street(dto.address().street().trim())
                .district(dto.address().district() != null ? dto.address().district().trim() : null)
                .building(dto.address().building() != null ? dto.address().building().trim() : null)
                .entrance(dto.address().entrance() != null ? dto.address().entrance().trim() : null)
                .floor(dto.address().floor() != null ? dto.address().floor().trim() : null)
                .apartment(dto.address().apartment() != null ? dto.address().apartment().trim() : null)
                .latitude(dto.address().latitude())
                .longitude(dto.address().longitude())
                .build();

        var operatingHours = dto.operatingHours().stream()
                .map(hourDto -> OperatingHour.builder()
                        .dayOfWeek(hourDto.dayOfWeek())
                        .openTime(hourDto.openTime())
                        .closeTime(hourDto.closeTime())
                        .isClosed(hourDto.isClosed())
                        .build())
                .collect(Collectors.toSet());

        var office = Office.builder()
                .company(company)
                .addressDetails(addressDetails) 
                .operatingHours(operatingHours) 
                .build();

        var savedOffice = officeRepository.save(office);

        log.info("Successfully created an office for company [{}], located in City [{}], with ID: {}",
                        savedOffice.getCompany().getName(),
                        savedOffice.getAddressDetails().getCity().getName(),
                        savedOffice.getId());

        return mapToViewDto(savedOffice);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public OfficeViewDto update(Long id, OfficeDto dto) {
        log.debug("Attempting to update office with ID: {}", id);

        Objects.requireNonNull(id, Constants.DeveloperErrors.ENTITY_ID_NULL);
        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        var office = findOfficeOrThrow(id);

        // Check if Company changed
        if (!office.getCompany().getId().equals(dto.companyId())) {
            var newCompany = companyRepository.findById(dto.companyId())
                    .orElseThrow(() -> {
                        log.warn("Office update failed. New Company ID [{}] not found.", dto.companyId());
                        return new BusinessException(ErrorCode.COMPANY_NOT_FOUND);
                    });
            office.setCompany(newCompany);
        }

        // Check if City changed
        var currentCityId = office.getAddressDetails().getCity().getId();
        if (!currentCityId.equals(dto.address().cityId())) {
            var newCity = cityRepository.findById(dto.address().cityId())
                    .orElseThrow(() -> {
                        log.warn("Office update failed. New City ID [{}] not found.", dto.address().cityId());
                        return new BusinessException(ErrorCode.CITY_NOT_FOUND);
                    });
            office.getAddressDetails().setCity(newCity);
        }

        // 3. Update Address Details (blind overwrite is safest for value objects)
        AddressDetails address = office.getAddressDetails();
        address.setStreet(dto.address().street().trim());
        address.setDistrict(dto.address().district() != null ? dto.address().district().trim() : null);
        address.setBuilding(dto.address().building() != null ? dto.address().building().trim() : null);
        address.setEntrance(dto.address().entrance() != null ? dto.address().entrance().trim() : null);
        address.setFloor(dto.address().floor() != null ? dto.address().floor().trim() : null);
        address.setApartment(dto.address().apartment() != null ? dto.address().apartment().trim() : null);
        address.setLatitude(dto.address().latitude());
        address.setLongitude(dto.address().longitude());

        // 4. Update Operating Hours (Overwrite collection)
        var newOperatingHours = dto.operatingHours().stream()
                .map(hourDto -> OperatingHour.builder()
                        .dayOfWeek(hourDto.dayOfWeek())
                        .openTime(hourDto.openTime())
                        .closeTime(hourDto.closeTime())
                        .isClosed(hourDto.isClosed())
                        .build())
                .collect(Collectors.toSet());

        office.getOperatingHours().clear();
        office.getOperatingHours().addAll(newOperatingHours);

        var updatedOffice = officeRepository.save(office);
        log.info("Successfully updated Office ID: {}", id);

        return mapToViewDto(updatedOffice);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void delete(Long id) {
        log.debug("Attempting to delete office with ID: {}", id);

        Objects.requireNonNull(id, Constants.DeveloperErrors.ENTITY_ID_NULL);

        var office = findOfficeOrThrow(id);
        officeRepository.delete(office);

        log.info("Successfully deleted Office ID: {}", id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public OfficeViewDto getById(Long id) {
        log.debug("Fetching office with ID: {}", id);
        
        Objects.requireNonNull(id, Constants.DeveloperErrors.ENTITY_ID_NULL);

        return mapToViewDto(findOfficeOrThrow(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OfficeViewDto> getAll(Pageable pageable) {
        log.debug("Fetching paginated list of offices");
        
        Objects.requireNonNull(pageable, Constants.DeveloperErrors.PAGEABLE_NULL);

        Page<Office> offices = officeRepository.findAll(pageable);
        return offices.map(office -> mapToViewDto(office));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<OfficeViewDto> getOfficesByCityId(Long cityId) {
        log.debug("Fetching all offices for City ID: {}", cityId);
        
        Objects.requireNonNull(cityId, Constants.DeveloperErrors.ENTITY_ID_NULL);

        return officeRepository.findAllByAddressDetailsCityId(cityId).stream()
                .map(this::mapToViewDto)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<OfficeViewDto> getNearestOffices(double lat, double lon, double radiusKm) {
        log.debug("Fetching nearest offices within {} km of ({}, {})", radiusKm, lat, lon);

        // Standard validation for geographic coordinates
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180 || radiusKm <= 0) {
            log.warn("Invalid geospatial parameters provided: lat={}, lon={}, radius={}", lat, lon, radiusKm);
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        return officeRepository.findNearestOfficesWithinRadius(lat, lon, radiusKm).stream()
                .map(this::mapToViewDto)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeViewDto> getClerksByOfficeId(Long officeId, Pageable pageable) {
        log.debug("Fetching paginated clerks for Office ID: {}", officeId);
        Objects.requireNonNull(officeId, Constants.DeveloperErrors.ENTITY_ID_NULL);

        // First verify the office exists. We don't want to return an empty list if the office is invalid.
        Office office = findOfficeOrThrow(officeId);

        // Execute cross-domain query
        Page<OfficeClerk> clerksPage = officeClerkRepository.findOfficeClerksByOfficeId(office.getId(), pageable);

        // Translate the employee entity to the employee view DTO locally
        return clerksPage.map(this::mapClerkToEmployeeViewDto);
    }

    /**
     * Centralized lookup and exception logic to DRY up the service methods.
     */
    private Office findOfficeOrThrow(Long id) {
        return officeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Lookup failed. Office with ID [{}] not found.", id);
                    return new BusinessException(ErrorCode.OFFICE_NOT_FOUND);
                });
    }

    /**
     * Manual mapper.
     */
    private OfficeViewDto mapToViewDto(Office office) {
        if (office == null) {
            return null;
        }

        // 1. Build the flattened full address string using the functional Stream approach
        String fullAddress = formatAddress(office.getAddressDetails());

        // 2. Map the Set of OperatingHour entities to OperatingHourViewDto records
        Set<OperatingHourViewDto> mappedHours = office.getOperatingHours().stream()
                .map(hour -> new OperatingHourViewDto(
                        hour.getDayOfWeek(),
                        hour.getOpenTime(),
                        hour.getCloseTime(),
                        hour.isClosed()
                ))
                .collect(Collectors.toSet());

        // 3. Create the temporary view DTO entity
        OfficeViewDto viewDto = new OfficeViewDto(
                office.getId(),
                office.getCompany().getId(),
                office.getAddressDetails().getCity().getName(),
                office.getAddressDetails().getCity().getPostcode(),
                fullAddress,
                mappedHours
        );

        // 4. Return the temporary entity
        return viewDto;
    }

    /**
     * Translates an OfficeClerk entity from the Employee domain into an EmployeeViewDto.
     */
    private EmployeeViewDto mapClerkToEmployeeViewDto(OfficeClerk clerk) {
        if (clerk == null) return null;

        Long officeId = clerk.getOffice() != null ? clerk.getOffice().getId() : null;

        return EmployeeViewDto.builder()
                .id(clerk.getId())
                .username(clerk.getUsername())
                .email(clerk.getEmail())
                .firstName(clerk.getFirstName())
                .lastName(clerk.getLastName())
                .employeeNumber(clerk.getEmployeeNumber())
                .hireDate(clerk.getHireDate())
                .salary(clerk.getSalary())
                .applicationRole(clerk.getApplicationRole())
                .isActive(clerk.isActive())
                .officeId(officeId)
                .build();
    }

    /**
     * Formats the components of an AddressDetails object into a single, comma-separated string.
     * Utilizing the Stream API allows us to declaratively filter out missing/null fields
     * and automatically manage the comma delimiters without complex if/else logic.
     */
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
                .collect(Collectors.joining(", "));
    }

    /**
     * Helper method to safely prepend a string prefix (like "bl. " or "ap. ")
     * only if the underlying value actually exists.
     */
    private String formatOptionalPart(String prefix, String value) {
        if (value != null && !value.isBlank()) {
            return prefix + value;
        }
        return null;
    }
}