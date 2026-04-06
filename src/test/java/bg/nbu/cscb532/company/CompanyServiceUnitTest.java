package bg.nbu.cscb532.company;

import bg.nbu.cscb532.company.dto.CompanyDto;
import bg.nbu.cscb532.company.dto.CompanyViewDto;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Company Service Unit Tests")
class CompanyServiceUnitTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyServiceImpl companyService;

    // --- CREATE ---

    @Test
    @DisplayName("Should successfully create a company when given a valid DTO")
    void shouldCreateCompanyWhenDtoIsValid() {
        // Arrange
        CompanyDto createDto = CompanyDto.builder()
                .name("Speedy")
                .registrationNumber("BG123")
                .build();

        Company savedEntity = Company.builder()
                .name("Speedy")
                .registrationNumber("BG123")
                .build();
        savedEntity.setId(1L);

        given(companyRepository.findByRegistrationNumber("BG123")).willReturn(Optional.empty());
        given(companyRepository.save(any(Company.class))).willReturn(savedEntity);

        // Act
        CompanyViewDto result = companyService.create(createDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Speedy");
        assertThat(result.registrationNumber()).isEqualTo("BG123");

        verify(companyRepository).findByRegistrationNumber("BG123");
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when creating a company with duplicate registration number")
    void shouldThrowExceptionWhenCreatingWithDuplicateRegistration() {
        // Arrange
        CompanyDto createDto = CompanyDto.builder()
                .name("Speedy")
                .registrationNumber("DUPLICATE_REG")
                .build();

        given(companyRepository.findByRegistrationNumber(anyString()))
                .willReturn(Optional.of(new Company())); // Simulate existing company

        // Act & Assert
        assertThatThrownBy(() -> companyService.create(createDto))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.COMPANY_REGISTRATION_DUPLICATE.getDefaultMessage())
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_REGISTRATION_DUPLICATE);

        verify(companyRepository).findByRegistrationNumber("DUPLICATE_REG");
        verifyNoMoreInteractions(companyRepository); // Ensure save() is never called
    }

    @Test
    @DisplayName("Should throw NullPointerException when create DTO is null (Defense in Depth)")
    void shouldThrowNpeWhenCreateDtoIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> companyService.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Incoming DTO must not be null");

        verifyNoMoreInteractions(companyRepository);
    }

    // --- UPDATE ---

    @Test
    @DisplayName("Should update company name when given valid ID and DTO")
    void shouldUpdateCompanyWhenIdAndDtoAreValid() {
        // Arrange
        Long targetId = 1L;
        CompanyDto updateDto = CompanyDto.builder()
                .name("New Name")
                .registrationNumber("IGNORED_REG_NUM")
                .build();

        Company existingEntity = Company.builder()
                .name("Old Name")
                .registrationNumber("BG123")
                .build();
        existingEntity.setId(targetId);

        given(companyRepository.findById(targetId)).willReturn(Optional.of(existingEntity));
        given(companyRepository.save(any(Company.class))).willReturn(existingEntity);

        // Act
        CompanyViewDto result = companyService.update(targetId, updateDto);

        // Assert
        assertThat(result.name()).isEqualTo("New Name");
        // Ensure the registration number didn't change despite the DTO
        assertThat(result.registrationNumber()).isEqualTo("BG123"); 

        verify(companyRepository).findById(targetId);
        verify(companyRepository).save(existingEntity);
    }

    @Test
    @DisplayName("Should throw BusinessException when updating a non-existent company ID")
    void shouldThrowExceptionWhenUpdatingNonExistentId() {
        // Arrange
        Long invalidId = 999L;
        CompanyDto updateDto = CompanyDto.builder().name("New Name").build();

        given(companyRepository.findById(invalidId)).willReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> companyService.update(invalidId, updateDto))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.COMPANY_NOT_FOUND.getDefaultMessage());

        verify(companyRepository).findById(invalidId);
        verifyNoMoreInteractions(companyRepository);
    }

    // --- READ ---

    @Test
    @DisplayName("Should return paginated list of companies")
    void shouldReturnPaginatedCompanies() {
        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 10);
        Company company = Company.builder().name("Speedy").registrationNumber("BG123").build();
        company.setId(1L);
        Page<Company> pagedResponse = new PageImpl<>(List.of(company));

        given(companyRepository.findAll(pageRequest)).willReturn(pagedResponse);

        // Act
        Page<CompanyViewDto> result = companyService.getAll(pageRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("Speedy");

        verify(companyRepository).findAll(pageRequest);
    }

    @Test
    @DisplayName("Should successfully retrieve a company by its exact name")
    void shouldGetCompanyByNameWhenValidNameProvided() {
        // Arrange
        String targetName = "DHL Express";
        Company existingEntity = Company.builder()
                .name(targetName)
                .registrationNumber("BG123")
                .build();
        existingEntity.setId(1L);

        given(companyRepository.getCompanyByName(targetName)).willReturn(existingEntity);

        // Act
        CompanyViewDto result = companyService.getByName(targetName);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(targetName);
        assertThat(result.registrationNumber()).isEqualTo("BG123");

        verify(companyRepository).getCompanyByName(targetName);
    }

    @Test
    @DisplayName("Should throw BusinessException when attempting to fetch a company by a non-existent name")
    void shouldThrowExceptionWhenCompanyByNameIsNotFound() {
        // Arrange
        String nonExistentName = "Unknown Company";
        given(companyRepository.getCompanyByName(nonExistentName)).willReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> companyService.getByName(nonExistentName))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.COMPANY_NOT_FOUND.getDefaultMessage())
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

        verify(companyRepository).getCompanyByName(nonExistentName);
    }

    @Test
    @DisplayName("Should throw NullPointerException when fetching company by null name (Defense in Depth)")
    void shouldThrowNpeWhenGetByNameIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> companyService.getByName(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Name must not be null");

        verifyNoMoreInteractions(companyRepository);
    }
}