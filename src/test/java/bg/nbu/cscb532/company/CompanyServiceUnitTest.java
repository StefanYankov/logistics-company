package bg.nbu.cscb532.company;

import bg.nbu.cscb532.company.dto.CompanyDto;
import bg.nbu.cscb532.company.dto.CompanyViewDto;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Company Service Unit Tests")
class CompanyServiceUnitTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyServiceImpl companyService;

    // --- DATA FACTORIES (Object Mother) ---

    private CompanyDto createValidDto(String name, String reg) {
        return CompanyDto.builder()
                .name(name)
                .registrationNumber(reg)
                .build();
    }

    private Company createEntity(Long id, String name, String reg) {
        Company company = Company.builder()
                .name(name)
                .registrationNumber(reg)
                .build();
        company.setId(id);
        return company;
    }

    @Nested
    @DisplayName("create() - Company Registration")
    class CreateCompanyTests {

        @Test
        @DisplayName("Happy Path: Should successfully create a company when given a valid DTO")
        void shouldCreateCompanyWhenDtoIsValid() {
            // Arrange
            CompanyDto createDto = createValidDto("Speedy", "BG123");
            Company savedEntity = createEntity(1L, "Speedy", "BG123");

            given(companyRepository.findByName("Speedy")).willReturn(Optional.empty());
            given(companyRepository.findByRegistrationNumber("BG123")).willReturn(Optional.empty());
            given(companyRepository.save(any(Company.class))).willReturn(savedEntity);

            // Act
            CompanyViewDto result = companyService.create(createDto);

            // Assert
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Speedy");
            verify(companyRepository).save(any(Company.class));
        }

        @Test
        @DisplayName("Business Rule: Should throw BusinessException for duplicate registration number")
        void shouldThrowExceptionWhenCreatingWithDuplicateRegistration() {
            // Arrange
            CompanyDto createDto = createValidDto("Speedy", "DUPLICATE_REG");

            given(companyRepository.findByName("Speedy")).willReturn(Optional.empty());
            given(companyRepository.findByRegistrationNumber("DUPLICATE_REG"))
                    .willReturn(Optional.of(new Company()));

            // Act and Assert
            assertThatThrownBy(() -> companyService.create(createDto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COMPANY_REGISTRATION_DUPLICATE);

            verify(companyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Defense in Depth: Should throw NPE when create DTO is null")
        void shouldThrowNpeWhenCreateDtoIsNull() {
            // Act and Assert
            assertThatThrownBy(() -> companyService.create(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(Constants.DeveloperErrors.DTO_NULL);

            verifyNoInteractions(companyRepository);
        }
    }

    @Nested
    @DisplayName("update() - Company Modification")
    class UpdateCompanyTests {

        @Test
        @DisplayName("Happy Path: Should update company when ID and DTO are valid")
        void shouldUpdateCompanyWhenIdAndDtoAreValid() {
            // Arrange
            Long targetId = 1L;
            CompanyDto updateDto = createValidDto("New Name", "NEW_REG");
            Company existing = createEntity(targetId, "Old", "OLD");

            given(companyRepository.findById(targetId)).willReturn(Optional.of(existing));
            given(companyRepository.findByName("New Name")).willReturn(Optional.empty());
            given(companyRepository.findByRegistrationNumber("NEW_REG")).willReturn(Optional.empty());
            given(companyRepository.save(any(Company.class))).willReturn(existing);

            // Act
            CompanyViewDto result = companyService.update(targetId, updateDto);

            // Assert
            assertThat(result.name()).isEqualTo("New Name");
            verify(companyRepository).save(any(Company.class));
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException for non-existent ID")
        void shouldThrowExceptionWhenUpdatingNonExistentId() {
            // Arrange
            given(companyRepository.findById(999L)).willReturn(Optional.empty());

            // Act and Assert
            assertThatThrownBy(() -> companyService.update(999L, createValidDto("A", "B")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.COMPANY_NOT_FOUND.getDefaultMessage());

            verify(companyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Retrieval Logic - Finders and Pagination")
    class ReadCompanyTests {

        @Test
        @DisplayName("Happy Path: Should return paginated list")
        void shouldReturnPaginatedCompanies() {
            // Arrange
            PageRequest pageRequest = PageRequest.of(0, 10);
            Page<Company> pagedResponse = new PageImpl<>(List.of(createEntity(1L, "Speedy", "BG123")));

            given(companyRepository.findAll(pageRequest)).willReturn(pagedResponse);

            // Act
            Page<CompanyViewDto> result = companyService.getAll(pageRequest);

            // Assert
            assertThat(result.getContent()).hasSize(1);
            verify(companyRepository).findAll(pageRequest);
        }

        @Test
        @DisplayName("Defense in Depth: Should throw NPE when fetching by null name")
        void shouldThrowNpeWhenGetByNameIsNull() {
            // Act and Assert
            assertThatThrownBy(() -> companyService.getByName(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Name must not be null");

            verifyNoInteractions(companyRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when name not found")
        void shouldThrowExceptionWhenCompanyByNameIsNotFound() {
            // Arrange
            given(companyRepository.findByName("Missing")).willReturn(Optional.empty());

            // Act and Assert
            assertThatThrownBy(() -> companyService.getByName("Missing"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
        }
    }
}