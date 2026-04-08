package bg.nbu.cscb532.office;

import bg.nbu.cscb532.office.dto.CityDto;
import bg.nbu.cscb532.office.dto.CityViewDto;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Core business logic implementation for managing the city entity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
// TODO: (Security) Add @PreAuthorize("hasRole('ADMIN')") to mutating methods once JWT is implemented
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CityViewDto create(CityDto dto) {
        log.debug("Attempting to create a new city");

        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        String normalizedName = dto.name().trim();
        String normalizedPostcode = dto.postcode().trim();

        if (cityRepository.findByNameAndPostcode(normalizedName, normalizedPostcode).isPresent()) {
            log.warn("City creation failed. City [{}] with postcode [{}] already exists.", normalizedName, normalizedPostcode);
            throw new BusinessException(ErrorCode.CITY_DUPLICATE);
        }

        var city = City.builder()
                .name(normalizedName)
                .postcode(normalizedPostcode)
                .build();

        var savedCity = cityRepository.save(city);

        log.info("Successfully created city [{}] with postcode [{}] and ID: {}", savedCity.getName(), savedCity.getPostcode(), savedCity.getId());

        return mapToViewDto(savedCity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CityViewDto update(Long id, CityDto dto) {
        log.debug("Attempting to update city with ID: {}", id);

        Objects.requireNonNull(id, Constants.DeveloperErrors.ENTITY_ID_NULL);
        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        var city = findCityOrThrow(id);

        var newNormalizedName = dto.name().trim();
        var newNormalizedPostcode = dto.postcode().trim();

        boolean isNameChanged = !city.getName().equals(newNormalizedName);
        boolean isPostcodeChanged = !city.getPostcode().equals(newNormalizedPostcode);

        // Proactive duplicate check due to unique constraint
        if (isNameChanged || isPostcodeChanged) {
            cityRepository.findByNameAndPostcode(newNormalizedName, newNormalizedPostcode)
                    .ifPresent(existingCity -> {
                        if (!existingCity.getId().equals(id)) {
                            log.warn("City update failed. Combination of [{}] and [{}] already in use by City ID: {}",
                                    newNormalizedName, newNormalizedPostcode, existingCity.getId());
                            throw new BusinessException(ErrorCode.CITY_DUPLICATE);
                        }
                    });
        }

        city.setName(newNormalizedName);
        city.setPostcode(newNormalizedPostcode);

        var updatedCity = cityRepository.save(city);
        log.info("Successfully updated City ID: {}", id);

        return mapToViewDto(updatedCity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void delete(Long id) {
        log.debug("Attempting to delete city with ID: {}", id);

        Objects.requireNonNull(id, Constants.DeveloperErrors.ENTITY_ID_NULL);

        var company = findCityOrThrow(id);
        cityRepository.delete(company);

        log.info("Successfully deleted city with ID: {}", id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public CityViewDto getById(Long id) {
        log.debug("Fetching city with ID: {}", id);
        Objects.requireNonNull(id, Constants.DeveloperErrors.ENTITY_ID_NULL);

        return mapToViewDto(findCityOrThrow(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<CityViewDto> getByName(String name) {
        log.debug("Fetching cities by exact name: {}", name);

        Objects.requireNonNull(name, Constants.DeveloperErrors.NAME_NULL);

        List<City> cities = cityRepository.findAllByName(name.trim());

        if (cities.isEmpty()) {
            log.warn("Lookup failed. No cities found with name [{}]", name);
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        return cities.stream()
                .map(city -> mapToViewDto(city))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CityViewDto> getAll(Pageable pageable) {
        log.debug("Fetching paginated list of cities");
        Objects.requireNonNull(pageable, Constants.DeveloperErrors.PAGEABLE_NULL);
        var cities = cityRepository.findAll(pageable);
        return cities.map(city -> mapToViewDto(city));
    }

    /**
     * Centralized lookup and exception logic to DRY up the service methods.
     */
    private City findCityOrThrow(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Lookup failed. City with ID [{}] not found.", id);
                    return new BusinessException(ErrorCode.CITY_NOT_FOUND);
                });
    }

    /**
     * Manual mapper.
     */
    private CityViewDto mapToViewDto(City city) {
        if (city == null) {
            return null;
        }
        return new CityViewDto(
                city.getId(),
                city.getName(),
                city.getPostcode()
        );
    }
}