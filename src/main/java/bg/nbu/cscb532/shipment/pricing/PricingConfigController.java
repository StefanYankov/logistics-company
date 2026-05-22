package bg.nbu.cscb532.shipment.pricing;

import bg.nbu.cscb532.shared.web.ApiStandardResponses;
import bg.nbu.cscb532.shipment.PricingService;
import bg.nbu.cscb532.shipment.dto.PricingConfigViewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/api/pricing", produces = MediaType.APPLICATION_JSON_VALUE)
@ApiStandardResponses
@RequiredArgsConstructor
@Tag(name = "Pricing API", description = "Endpoints for managing pricing configurations.")
public class PricingConfigController {

    private final PricingService pricingService;

    @Operation(
            summary = "Get the active pricing configuration",
            description = "Retrieves the currently active pricing rules (base price, price per kg, surcharges)."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the active pricing configuration")
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PricingConfigViewDto> getActivePricingConfig() {
        log.info("API GET request for active pricing configuration");
        return ResponseEntity.ok(pricingService.getActiveConfig());
    }
}
