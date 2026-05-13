package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shared.web.ApiStandardResponses;
import bg.nbu.cscb532.shipment.dto.ServiceCatalogViewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/services")
@ApiStandardResponses
@RequiredArgsConstructor
@Tag(name = "Service Catalog API", description = "Endpoints for retrieving available shipment addons.")
public class ServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    @Operation(
            summary = "Get all available services",
            description = "Retrieves a list of all active shipment addons and their pricing rules."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of services")
    @GetMapping
    public ResponseEntity<List<ServiceCatalogViewDto>> getAllServices() {
        log.info("API GET request for all service catalog entries");
        return ResponseEntity.ok(serviceCatalogService.getAllServices());
    }
}
