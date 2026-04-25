package bg.nbu.cscb532.office;

import bg.nbu.cscb532.company.Company;
import bg.nbu.cscb532.company.CompanyRepository;
import bg.nbu.cscb532.employee.OfficeClerk;
import bg.nbu.cscb532.employee.OfficeClerkRepository;
import bg.nbu.cscb532.employee.dto.EmployeeViewDto;
import bg.nbu.cscb532.office.dto.OfficeDto;
import bg.nbu.cscb532.office.dto.OfficeViewDto;
import bg.nbu.cscb532.office.dto.OperatingHourDto;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.location.AddressDetails;
import bg.nbu.cscb532.shared.location.AddressDetailsDto;
import bg.nbu.cscb532.user.ApplicationRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Office Service Unit Tests")
public class OfficeServiceUnitTests {

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private OfficeClerkRepository officeClerkRepository;

    @InjectMocks
    private OfficeServiceImpl officeService;

    // --- TEST DATA FACTORY ---

    private Company createValidCompany(Long id) {
        var company = Company.builder()
                .name("Test Company")
                .registrationNumber("123")
                .build();
        company.setId(id);
        return company;
    }

    private City createValidCity(Long id) {
        var city = City.builder()
                .name("Sofia")
                .postcode("1000")
                .build();
        city.setId(id);
        return city;
    }

    private AddressDetailsDto createValidAddressDto(Long cityId) {
        return new AddressDetailsDto(cityId, "Vitosha Blvd", "Mladost", "10", "A", "5", "12", 42.69, 23.32);
    }

    private OperatingHourDto createValidOperatingHourDto() {
        return new OperatingHourDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false);
    }

    private OfficeDto createValidOfficeDto(Long companyId, Long cityId) {
        return new OfficeDto(
                companyId,
                createValidAddressDto(cityId),
                Set.of(createValidOperatingHourDto())
        );
    }

    private Office createValidOfficeEntity(Long id, Long companyId, Long cityId) {
        var address = AddressDetails.builder()
                .city(createValidCity(cityId))
                .street("Vitosha Blvd")
                .district("Mladost")
                .building("10")
                .entrance("A")
                .floor("5")
                .apartment("12")
                .latitude(42.69)
                .longitude(23.32)
                .build();

        var operatingHour = OperatingHour.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .isClosed(false)
                .build();

        var office = Office.builder()
                .company(createValidCompany(companyId))
                .addressDetails(address)
                .operatingHours(new HashSet<>(Set.of(operatingHour)))
                .build();
        office.setId(id);
        return office;
    }


    // --- TESTS ---

    @Nested
    @DisplayName("getClerksByOfficeId(Long, Pageable) Tests")
    class GetClerksByOfficeIdTests {

        @Test
        @DisplayName("Happy Path: Should return mapped paginated list of clerks")
        void shouldReturnMappedClerks_HappyPath() {
            // Arrange
            Long officeId = 1L;
            PageRequest pageable = PageRequest.of(0, 10);

            Office mockOffice = createValidOfficeEntity(officeId, 10L, 20L);

            OfficeClerk clerk = new OfficeClerk();
            clerk.setId(UUID.randomUUID());
            clerk.setUsername("janedoe");
            clerk.setFirstName("Jane");
            clerk.setLastName("Doe");
            clerk.setEmail("jane@test.com");
            clerk.setApplicationRole(ApplicationRole.CLERK);
            clerk.setEmployeeNumber("EMP-001");
            clerk.setSalary(BigDecimal.valueOf(3000));
            clerk.setHireDate(LocalDate.now());
            clerk.setOffice(mockOffice);

            Page<OfficeClerk> pagedResponse = new PageImpl<>(List.of(clerk), pageable, 1);

            given(officeRepository.findById(officeId)).willReturn(Optional.of(mockOffice));
            given(officeClerkRepository.findOfficeClerksByOfficeId(officeId, pageable)).willReturn(pagedResponse);

            // Act
            Page<EmployeeViewDto> result = officeService.getClerksByOfficeId(officeId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            
            EmployeeViewDto viewDto = result.getContent().getFirst();
            assertThat(viewDto.username()).isEqualTo("janedoe");
            assertThat(viewDto.officeId()).isEqualTo(officeId);

            verify(officeRepository).findById(officeId);
            verify(officeClerkRepository).findOfficeClerksByOfficeId(officeId, pageable);
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when office is not found")
        void shouldThrowExceptionWhenOfficeNotFound_ErrorCase() {
            // Arrange
            Long invalidOfficeId = 999L;
            PageRequest pageable = PageRequest.of(0, 10);

            given(officeRepository.findById(invalidOfficeId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> officeService.getClerksByOfficeId(invalidOfficeId, pageable))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.OFFICE_NOT_FOUND);

            verify(officeRepository).findById(invalidOfficeId);
            verifyNoInteractions(officeClerkRepository);
        }

        @Test
        @DisplayName("Edge Case: Should handle empty page mapping correctly")
        void shouldReturnEmptyPageWhenNoClerksExist_EdgeCase() {
            // Arrange
            Long officeId = 1L;
            PageRequest pageable = PageRequest.of(0, 10);

            Office mockOffice = createValidOfficeEntity(officeId, 10L, 20L);
            Page<OfficeClerk> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(officeRepository.findById(officeId)).willReturn(Optional.of(mockOffice));
            given(officeClerkRepository.findOfficeClerksByOfficeId(officeId, pageable)).willReturn(emptyPage);

            // Act
            Page<EmployeeViewDto> result = officeService.getClerksByOfficeId(officeId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {

        @Test
        @DisplayName("Should successfully create an office")
        void shouldCreateSuccessfully_HappyPath() {
            // Arrange
            Long companyId = 10L;
            Long cityId = 20L;
            OfficeDto dto = createValidOfficeDto(companyId, cityId);

            Company company = createValidCompany(companyId);
            City city = createValidCity(cityId);
            Office savedOffice = createValidOfficeEntity(1L, companyId, cityId);

            given(companyRepository.findById(companyId)).willReturn(Optional.of(company));
            given(cityRepository.findById(cityId)).willReturn(Optional.of(city));
            given(officeRepository.save(any(Office.class))).willReturn(savedOffice);

            // Act
            OfficeViewDto result = officeService.create(dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.companyId()).isEqualTo(companyId);
            assertThat(result.cityName()).isEqualTo("Sofia");
            assertThat(result.cityPostcode()).isEqualTo("1000");
            assertThat(result.fullAddress()).contains("Vitosha Blvd", "Mladost", "bl. 10", "ent. A", "fl. 5", "ap. 12", "Sofia 1000");
            assertThat(result.operatingHours()).hasSize(1);

            verify(companyRepository).findById(companyId);
            verify(cityRepository).findById(cityId);
            verify(officeRepository).save(any(Office.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when Company is not found")
        void shouldThrowExceptionWhenCompanyNotFound_ErrorCase() {
            // Arrange
            OfficeDto dto = createValidOfficeDto(999L, 20L);
            given(companyRepository.findById(999L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> officeService.create(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.COMPANY_NOT_FOUND.getDefaultMessage());

            verify(companyRepository).findById(999L);
            verifyNoInteractions(cityRepository, officeRepository);
        }

        @Test
        @DisplayName("Should throw BusinessException when City is not found")
        void shouldThrowExceptionWhenCityNotFound_ErrorCase() {
            // Arrange
            OfficeDto dto = createValidOfficeDto(10L, 999L);
            given(companyRepository.findById(10L)).willReturn(Optional.of(createValidCompany(10L)));
            given(cityRepository.findById(999L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> officeService.create(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.CITY_NOT_FOUND.getDefaultMessage());

            verify(companyRepository).findById(10L);
            verify(cityRepository).findById(999L);
            verifyNoInteractions(officeRepository);
        }

        @Test
        @DisplayName("Should throw NPE when DTO is null (Defense in Depth)")
        void shouldThrowNpeWhenDtoIsNull_ErrorCase() {
            assertThatThrownBy(() -> officeService.create(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(Constants.DeveloperErrors.DTO_NULL);

            verifyNoInteractions(officeRepository, cityRepository, companyRepository);
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update office successfully without querying parent entities if IDs are unchanged")
        void shouldUpdateSuccessfullyWithoutFetchingParents_HappyPath() {
            // Arrange
            Long officeId = 1L;
            Long companyId = 10L;
            Long cityId = 20L;

            Office existingOffice = createValidOfficeEntity(officeId, companyId, cityId);
            OfficeDto updateDto = createValidOfficeDto(companyId, cityId); // Same IDs

            given(officeRepository.findById(officeId)).willReturn(Optional.of(existingOffice));
            given(officeRepository.save(any(Office.class))).willReturn(existingOffice);

            // Act
            OfficeViewDto result = officeService.update(officeId, updateDto);

            // Assert
            assertThat(result).isNotNull();

            // Prove micro-optimization: Parents were NOT fetched again
            verifyNoInteractions(companyRepository, cityRepository);
            verify(officeRepository).findById(officeId);
            verify(officeRepository).save(existingOffice);
        }

        @Test
        @DisplayName("Should update office successfully and fetch new parent entities if IDs changed")
        void shouldUpdateSuccessfullyAndFetchNewParents_HappyPath() {
            // Arrange
            Long officeId = 1L;
            Long oldCompanyId = 10L;
            Long oldCityId = 20L;
            Long newCompanyId = 99L;
            Long newCityId = 88L;

            Office existingOffice = createValidOfficeEntity(officeId, oldCompanyId, oldCityId);
            OfficeDto updateDto = createValidOfficeDto(newCompanyId, newCityId); // Different IDs

            Company newCompany = createValidCompany(newCompanyId);
            City newCity = createValidCity(newCityId);

            given(officeRepository.findById(officeId)).willReturn(Optional.of(existingOffice));
            given(companyRepository.findById(newCompanyId)).willReturn(Optional.of(newCompany));
            given(cityRepository.findById(newCityId)).willReturn(Optional.of(newCity));
            given(officeRepository.save(any(Office.class))).willReturn(existingOffice); // The object mutates in place

            // Act
            OfficeViewDto result = officeService.update(officeId, updateDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.companyId()).isEqualTo(newCompanyId); // Proves it updated

            verify(companyRepository).findById(newCompanyId);
            verify(cityRepository).findById(newCityId);
            verify(officeRepository).save(existingOffice);
        }

        @Test
        @DisplayName("Should throw BusinessException when existing Office is not found")
        void shouldThrowExceptionWhenOfficeNotFound_ErrorCase() {
            Long officeId = 999L;
            OfficeDto updateDto = createValidOfficeDto(10L, 20L);

            given(officeRepository.findById(officeId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> officeService.update(officeId, updateDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.OFFICE_NOT_FOUND.getDefaultMessage());

            verifyNoInteractions(companyRepository, cityRepository);
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should successfully delete an office")
        void shouldDeleteSuccessfully_HappyPath() {
            Long officeId = 1L;
            Office office = createValidOfficeEntity(officeId, 10L, 20L);

            given(officeRepository.findById(officeId)).willReturn(Optional.of(office));

            officeService.delete(officeId);

            verify(officeRepository).findById(officeId);
            verify(officeRepository).delete(office);
        }

        @Test
        @DisplayName("Should throw BusinessException when office is not found")
        void shouldThrowExceptionWhenNotFound_ErrorCase() {
            Long officeId = 999L;
            given(officeRepository.findById(officeId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> officeService.delete(officeId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.OFFICE_NOT_FOUND.getDefaultMessage());

            verify(officeRepository, never()).delete(any(Office.class));
        }
    }

    @Nested
    @DisplayName("Geospatial Query Tests")
    class GeospatialTests {

        @Test
        @DisplayName("Should return a list of nearest offices")
        void shouldReturnNearestOffices_HappyPath() {
            // Arrange
            double lat = 42.69;
            double lon = 23.32;
            double radius = 10.0;
            Office office = createValidOfficeEntity(1L, 10L, 20L);

            given(officeRepository.findNearestOfficesWithinRadius(lat, lon, radius)).willReturn(List.of(office));

            // Act
            List<OfficeViewDto> results = officeService.getNearestOffices(lat, lon, radius);

            // Assert
            assertThat(results).hasSize(1);
            verify(officeRepository).findNearestOfficesWithinRadius(lat, lon, radius);
        }

        @ParameterizedTest(name = "lat: {0}, lon: {1}, radius: {2}")
        @CsvSource({
                "100.0, 23.32, 10.0",   // Invalid Latitude (> 90)
                "-100.0, 23.32, 10.0",  // Invalid Latitude (< -90)
                "42.69, 200.0, 10.0",   // Invalid Longitude (> 180)
                "42.69, -200.0, 10.0",  // Invalid Longitude (< -180)
                "42.69, 23.32, 0.0",    // Invalid Radius (Zero)
                "42.69, 23.32, -5.0"    // Invalid Radius (Negative)
        })
        @DisplayName("Edge Case: Should throw VALIDATION_FAILED when coordinates are mathematically invalid")
        void shouldThrowExceptionWhenCoordinatesInvalid_EdgeCase(double lat, double lon, double radius) {

            // Act & Assert
            assertThatThrownBy(() -> officeService.getNearestOffices(lat, lon, radius))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.VALIDATION_FAILED.getDefaultMessage());

            verifyNoInteractions(officeRepository);
        }
    }

    @Nested
    @DisplayName("Standard Read Operation Tests")
    class ReadTests {

        @Test
        @DisplayName("Should retrieve office by ID")
        void shouldGetById_HappyPath() {
            Long officeId = 1L;
            Office office = createValidOfficeEntity(officeId, 10L, 20L);

            given(officeRepository.findById(officeId)).willReturn(Optional.of(office));

            OfficeViewDto result = officeService.getById(officeId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(officeId);
            verify(officeRepository).findById(officeId);
        }

        @Test
        @DisplayName("Should retrieve paginated list of all offices")
        void shouldGetAll_HappyPath() {
            PageRequest pageable = PageRequest.of(0, 10);
            Office office = createValidOfficeEntity(1L, 10L, 20L);
            Page<Office> pagedResponse = new PageImpl<>(List.of(office));

            given(officeRepository.findAll(pageable)).willReturn(pagedResponse);

            Page<OfficeViewDto> result = officeService.getAll(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(officeRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should retrieve offices by City ID")
        void shouldGetByCityId_HappyPath() {
            Long cityId = 20L;
            Office office = createValidOfficeEntity(1L, 10L, cityId);

            given(officeRepository.findAllByAddressDetailsCityId(cityId)).willReturn(List.of(office));

            List<OfficeViewDto> results = officeService.getOfficesByCityId(cityId);

            assertThat(results).hasSize(1);
            verify(officeRepository).findAllByAddressDetailsCityId(cityId);
        }

        @Test
        @DisplayName("Edge Case: Should return empty list when no offices found for City ID")
        void shouldReturnEmptyListWhenCityHasNoOffices_EdgeCase() {
            // Arrange
            Long cityId = 999L;
            given(officeRepository.findAllByAddressDetailsCityId(cityId)).willReturn(List.of());

            // Act
            List<OfficeViewDto> results = officeService.getOfficesByCityId(cityId);

            // Assert
            assertThat(results).isNotNull().isEmpty();
            verify(officeRepository).findAllByAddressDetailsCityId(cityId);
        }
    }
}