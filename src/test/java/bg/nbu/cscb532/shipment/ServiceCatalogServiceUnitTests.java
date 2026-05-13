package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shipment.dto.ServiceCatalogViewDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceCatalog Service Unit Tests")
class ServiceCatalogServiceUnitTests {

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;

    @InjectMocks
    private ServiceCatalogServiceImpl serviceCatalogService;

    @Test
    @DisplayName("Happy Path: Should retrieve all services and map to DTOs")
    void shouldGetAllServices() {
        // Arrange
        ServiceCatalog s1 = new ServiceCatalog();
        s1.setId(1L);
        s1.setName("Fragile");
        s1.setPricingType(PricingType.FIXED_AMOUNT);
        s1.setPricingValue(BigDecimal.valueOf(5.00));

        ServiceCatalog s2 = new ServiceCatalog();
        s2.setId(2L);
        s2.setName("Heavy");
        s2.setPricingType(PricingType.PERCENTAGE_OF_BASE);
        s2.setPricingValue(BigDecimal.valueOf(0.25));

        given(serviceCatalogRepository.findAll()).willReturn(List.of(s1, s2));

        // Act
        List<ServiceCatalogViewDto> result = serviceCatalogService.getAllServices();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Fragile");
        assertThat(result.get(0).pricingType()).isEqualTo(PricingType.FIXED_AMOUNT);
        assertThat(result.get(1).name()).isEqualTo("Heavy");
        
        verify(serviceCatalogRepository).findAll();
    }
    
    @Test
    @DisplayName("Edge Case: Should return empty list when catalog is empty")
    void shouldReturnEmptyListWhenCatalogEmpty() {
        // Arrange
        given(serviceCatalogRepository.findAll()).willReturn(List.of());

        // Act
        List<ServiceCatalogViewDto> result = serviceCatalogService.getAllServices();

        // Assert
        assertThat(result).isEmpty();
        verify(serviceCatalogRepository).findAll();
    }
}
