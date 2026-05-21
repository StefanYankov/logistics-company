package bg.nbu.cscb532.company;

import bg.nbu.cscb532.company.dto.CompanyDto;
import bg.nbu.cscb532.company.dto.CompanyUpdateDto;
import bg.nbu.cscb532.company.dto.CompanyViewDto;
import bg.nbu.cscb532.office.City;
import bg.nbu.cscb532.office.CityRepository;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.location.AddressDetails;
import bg.nbu.cscb532.shared.location.AddressDetailsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyService Unit Tests")
class CompanyServiceTests {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private CompanyServiceImpl companyService;

    @Captor
    private ArgumentCaptor<Company> companyCaptor;

    // --- DATA FACTORIES ---

    private CompanyDto createValidCompanyDto() {
        return CompanyDto.builder()
                .name("Test Company")
                .registrationNumber("REG123")
                .addressDetails(createValidAddressDto())
                .build();
    }

    private CompanyUpdateDto createValidUpdateDto() {
        return CompanyUpdateDto.builder()
                .name("Updated Company")
                .registrationNumber("UPD456")
                .addressDetails(createValidAddressDto())
                .build();
    }

    private AddressDetailsDto createValidAddressDto() {
        return AddressDetailsDto.builder()
                .cityId(1L)
                .street("Main Street 123")
                .build();
    }

    private Company createMockCompany(Long id, String name, String regNumber) {
        AddressDetails address = new AddressDetails();
        address.setCity(createMockCity(1L, "Sofia"));
        address.setStreet("Main Street 123");

        Company company = Company.builder()
                .name(name)
                .registrationNumber(regNumber)
                .addressDetails(address)
                .build();

        company.setId(id);

        return company;
    }

    private City createMockCity(Long id, String name) {
        City city = new City();
        city.setId(id);
        city.setName(name);
        return city;
    }


    @Nested
    @DisplayName("create(CompanyDto) Tests")
    class CreateTests {

        @Test
        @DisplayName("Happy Path: Should create and save a company with valid data")
        void shouldCreateCompanySuccessfully() {
            // Arrange
            CompanyDto dto = createValidCompanyDto();
            Company savedCompany = createMockCompany(1L, dto.name(), dto.registrationNumber());
            given(cityRepository.findById(1L)).willReturn(Optional.of(createMockCity(1L, "Sofia")));
            given(companyRepository.save(any(Company.class))).willReturn(savedCompany);

            // Act
            CompanyViewDto result = companyService.create(dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(dto.name());
            verify(companyRepository).save(companyCaptor.capture());
            assertThat(companyCaptor.getValue().getAddressDetails().getStreet()).isEqualTo("Main Street 123");
        }

        @Test
        @DisplayName("Error Case: Should throw exception for duplicate name")
        void shouldThrowForDuplicateName() {
            // Arrange
            CompanyDto dto = createValidCompanyDto();
            given(companyRepository.findByName(dto.name())).willReturn(Optional.of(new Company()));

            // Act & Assert
            assertThatThrownBy(() -> companyService.create(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COMPANY_NAME_DUPLICATE);
            verify(companyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException on duplicate registration number")
        void shouldThrowOnDuplicateRegistrationNumber() {
            // Arrange
            CompanyDto dto = createValidCompanyDto();
            given(companyRepository.findByName(dto.name())).willReturn(Optional.empty());
            given(companyRepository.findByRegistrationNumber(dto.registrationNumber())).willReturn(Optional.of(new Company()));

            // Act & Assert
            assertThatThrownBy(() -> companyService.create(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COMPANY_REGISTRATION_DUPLICATE);
            verify(companyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when city not found for address")
        void shouldThrowWhenCityNotFoundForAddress() {
            // Arrange
            CompanyDto dto = createValidCompanyDto();
            given(companyRepository.findByName(dto.name())).willReturn(Optional.empty());
            given(companyRepository.findByRegistrationNumber(dto.registrationNumber())).willReturn(Optional.empty());
            given(cityRepository.findById(dto.addressDetails().cityId())).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> companyService.create(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CITY_NOT_FOUND);
            verify(companyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update(Long, CompanyUpdateDto) Tests")
    class UpdateTests {

        @Test
        @DisplayName("Happy Path: Should update company with valid data")
        void shouldUpdateCompanySuccessfully() {
            // Arrange
            Long companyId = 1L;
            CompanyUpdateDto dto = createValidUpdateDto();
            Company existingCompany = createMockCompany(companyId, "Old Name", "OLD123");
            given(companyRepository.findById(companyId)).willReturn(Optional.of(existingCompany));
            given(cityRepository.findById(1L)).willReturn(Optional.of(createMockCity(1L, "Sofia")));
            given(companyRepository.save(any(Company.class))).willReturn(existingCompany);

            // Act
            CompanyViewDto result = companyService.update(companyId, dto);

            // Assert
            assertThat(result).isNotNull();
            verify(companyRepository).save(companyCaptor.capture());
            assertThat(companyCaptor.getValue().getName()).isEqualTo("Updated Company");
            assertThat(companyCaptor.getValue().getAddressDetails().getStreet()).isEqualTo("Main Street 123");
        }

        @Test
        @DisplayName("Error Case: Should throw exception if company not found")
        void shouldThrowWhenCompanyNotFound() {
            // Arrange
            given(companyRepository.findById(99L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> companyService.update(99L, createValidUpdateDto()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException on duplicate name by another company")
        void shouldThrowOnDuplicateNameByAnotherCompany() {
            // Arrange
            Long companyId = 1L;
            CompanyUpdateDto dto = createValidUpdateDto();
            Company existingCompany = createMockCompany(companyId, "Old Name", "OLD123");
            Company otherCompany = createMockCompany(2L, dto.name(), "OTHER456");

            given(companyRepository.findById(companyId)).willReturn(Optional.of(existingCompany));
            given(companyRepository.findByName(dto.name())).willReturn(Optional.of(otherCompany));

            // Act & Assert
            assertThatThrownBy(() -> companyService.update(companyId, dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COMPANY_NAME_DUPLICATE);
            verify(companyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException on duplicate registration number by another company")
        void shouldThrowOnDuplicateRegistrationNumberByAnotherCompany() {
            // Arrange
            Long companyId = 1L;
            CompanyUpdateDto dto = createValidUpdateDto();
            Company existingCompany = createMockCompany(companyId, "Old Name", "OLD123");
            Company otherCompany = createMockCompany(2L, "Other Name", dto.registrationNumber());

            given(companyRepository.findById(companyId)).willReturn(Optional.of(existingCompany));
            given(companyRepository.findByName(dto.name())).willReturn(Optional.empty());
            given(companyRepository.findByRegistrationNumber(dto.registrationNumber())).willReturn(Optional.of(otherCompany));

            // Act & Assert
            assertThatThrownBy(() -> companyService.update(companyId, dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COMPANY_REGISTRATION_DUPLICATE);
            verify(companyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when city not found for updated address")
        void shouldThrowWhenCityNotFoundForUpdatedAddress() {
            // Arrange
            Long companyId = 1L;
            CompanyUpdateDto dto = createValidUpdateDto();
            Company existingCompany = createMockCompany(companyId, "Old Name", "OLD123");

            given(companyRepository.findById(companyId)).willReturn(Optional.of(existingCompany));
            given(companyRepository.findByName(dto.name())).willReturn(Optional.empty());
            given(companyRepository.findByRegistrationNumber(dto.registrationNumber())).willReturn(Optional.empty());
            given(cityRepository.findById(dto.addressDetails().cityId())).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> companyService.update(companyId, dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CITY_NOT_FOUND);
            verify(companyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete(Long) Tests")
    class DeleteTests {
        @Test
        @DisplayName("Happy Path: Should call deleteById when company exists")
        void shouldDeleteCompany() {
            // Arrange
            Long companyId = 1L;
            Company existingCompany = createMockCompany(companyId, "Test", "123");
            given(companyRepository.findById(companyId)).willReturn(Optional.of(existingCompany));

            // Act
            companyService.delete(companyId);

            // Assert
            verify(companyRepository).delete(existingCompany);
        }

        @Test
        @DisplayName("Error Case: Should throw exception if company to delete is not found")
        void shouldThrowWhenDeletingNonExistentCompany() {
            // Arrange
            given(companyRepository.findById(99L)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> companyService.delete(99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getById(Long) and getAll(Pageable) Tests")
    class ReadTests {
        @Test
        @DisplayName("getById: Should return correct DTO when company exists")
        void shouldReturnDtoWhenCompanyExists() {
            // Arrange
            Long companyId = 1L;
            Company mockCompany = createMockCompany(companyId, "Test", "123");
            given(companyRepository.findById(companyId)).willReturn(Optional.of(mockCompany));

            // Act
            CompanyViewDto result = companyService.getById(companyId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(companyId);
            assertThat(result.address()).contains("Main Street 123");
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when company not found by ID")
        void shouldThrowWhenCompanyNotFoundById() {
            // Arrange
            Long companyId = 99L;
            given(companyRepository.findById(companyId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> companyService.getById(companyId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
        }

        @Test
        @DisplayName("getByName: Should return correct DTO when company exists")
        void shouldReturnDtoWhenCompanyExistsByName() {
            // Arrange
            String companyName = "Test Company";
            Company mockCompany = createMockCompany(1L, companyName, "123");
            given(companyRepository.findByName(companyName)).willReturn(Optional.of(mockCompany));

            // Act
            CompanyViewDto result = companyService.getByName(companyName);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(companyName);
            assertThat(result.address()).contains("Main Street 123");
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when company not found by name")
        void shouldThrowWhenCompanyNotFoundByName() {
            // Arrange
            String companyName = "Non Existent";
            given(companyRepository.findByName(companyName)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> companyService.getByName(companyName))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
        }

        @Test
        @DisplayName("getAll: Should return a page of companies")
        void shouldReturnPageOfCompanies() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            List<Company> companies = List.of(createMockCompany(1L, "Test", "123"));
            Page<Company> companyPage = new PageImpl<>(companies, pageable, 1);
            given(companyRepository.findAll(pageable)).willReturn(companyPage);

            // Act
            Page<CompanyViewDto> result = companyService.getAll(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().name()).isEqualTo("Test");
            assertThat(result.getContent().getFirst().address()).contains("Main Street 123");
        }
    }
}
