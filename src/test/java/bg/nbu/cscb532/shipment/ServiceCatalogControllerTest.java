package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shared.config.SecurityConfig;
import bg.nbu.cscb532.shared.web.exception.GlobalExceptionHandler;
import bg.nbu.cscb532.shipment.dto.ServiceCatalogViewDto;
import bg.nbu.cscb532.user.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ServiceCatalogController.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class ServiceCatalogControllerTest {

    private static final String BASE_URL = "/api/services";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceCatalogService serviceCatalogService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    @DisplayName("Happy Path: Should successfully retrieve list of services")
    void shouldRetrieveServicesSuccessfully() throws Exception {
        // Arrange
        ServiceCatalogViewDto dto = ServiceCatalogViewDto.builder()
                .id(1L)
                .name("Fragile")
                .pricingType(PricingType.FIXED_AMOUNT)
                .pricingValue(BigDecimal.valueOf(5.00))
                .build();

        given(serviceCatalogService.getAllServices()).willReturn(List.of(dto));

        // Act and Assert
        mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Fragile"));

        verify(serviceCatalogService).getAllServices();
    }
}
