package bg.nbu.cscb532.office;

import bg.nbu.cscb532.employee.dto.EmployeeViewDto;
import bg.nbu.cscb532.office.dto.OfficeDto;
import bg.nbu.cscb532.office.dto.OfficeViewDto;
import bg.nbu.cscb532.shared.web.ApiStandardResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * RESTful API Controller for managing Office entities.
 */
@Slf4j
@RestController
@RequestMapping("/api/offices")
@ApiStandardResponses
@RequiredArgsConstructor
@Tag(name = "Office API", description = "Endpoints for managing logistics offices.")
public class OfficeController {

    private final OfficeService officeService;

    @Operation(
            summary = "Create a new office",
            description = "Creates a new office. Returns 201 Created with the Location header pointing to the new resource."
    )
    @ApiResponse(responseCode = "201", description = "Office created successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g., invalid data or Operating Hours logic)")
    @ApiResponse(responseCode = "404", description = "Referenced Company or City not found")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OfficeViewDto> createOffice(@Valid @RequestBody OfficeDto dto) {
        log.info("API POST request to create a new office for Company ID: {} in City ID: {}", dto.companyId(), dto.address().cityId());

        var createdOffice = officeService.create(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdOffice.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdOffice);
    }

    @Operation(
            summary = "Update an office",
            description = "Updates the details, address, and operating hours of an existing office."
    )
    @ApiResponse(responseCode = "200", description = "Office updated successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g., invalid data or Operating Hours logic)")
    @ApiResponse(responseCode = "404", description = "Office, Company, or City not found")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OfficeViewDto> updateOffice(
            @PathVariable Long id,
            @Valid @RequestBody OfficeDto dto
    ) {
        log.info("API PUT request to update office with ID: {}", id);

        var updatedOffice = officeService.update(id, dto);

        return ResponseEntity.ok(updatedOffice);
    }

    @Operation(
            summary = "Delete an office",
            description = "Deletes an existing office by its ID."
    )
    @ApiResponse(responseCode = "204", description = "Office deleted successfully")
    @ApiResponse(responseCode = "404", description = "Office not found")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOffice(@PathVariable Long id) {
        log.info("API DELETE request to delete office with ID: {}", id);

        officeService.delete(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get office by ID",
            description = "Retrieves the details of a specific office using its unique ID."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the office")
    @ApiResponse(responseCode = "404", description = "Office not found")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OfficeViewDto> getOfficeById(@PathVariable Long id) {
        log.info("API GET request for office ID: {}", id);

        var office = officeService.getById(id);

        return ResponseEntity.ok(office);
    }

    @Operation(
            summary = "Get all offices",
            description = "Retrieves a paginated and sortable list of all offices."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of offices")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OfficeViewDto>> getAllOffices(Pageable pageable) {
        log.info("API GET requests for all offices. Pageable: {}", pageable);

        var offices = officeService.getAll(pageable);

        return ResponseEntity.ok(offices);
    }

    @Operation(
            summary = "Get offices by City",
            description = "Retrieves all offices located in a specific city."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of offices")
    @GetMapping("/city/{cityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OfficeViewDto>> getOfficesByCityId(@PathVariable Long cityId) {
        log.info("API GET request for offices in City ID: {}", cityId);

        var offices = officeService.getOfficesByCityId(cityId);

        return ResponseEntity.ok(offices);
    }

    @Operation(
            summary = "Find nearest offices",
            description = "Retrieves a list of offices within a given radius of a GPS coordinate, sorted by distance."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the nearest offices")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g., invalid GPS coordinates)")
    @GetMapping("/nearest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OfficeViewDto>> getNearestOffices(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam double radiusKm
    ) {
        log.info("API GET request for nearest offices. Lat: {}, Lon: {}, Radius: {}", lat, lon, radiusKm);

        var offices = officeService.getNearestOffices(lat, lon, radiusKm);

        return ResponseEntity.ok(offices);
    }

    @Operation(
            summary = "Get clerks for an office",
            description = "Retrieves a paginated list of OfficeClerk employees assigned to a specific office."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of clerks")
    @ApiResponse(responseCode = "404", description = "Office not found")
    @GetMapping("/{id}/clerks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<EmployeeViewDto>> getClerksForOffice(
            @PathVariable Long id, 
            Pageable pageable) {
        log.info("API GET request for clerks in Office ID: {}", id);

        Page<EmployeeViewDto> clerks = officeService.getClerksByOfficeId(id, pageable);

        return ResponseEntity.ok(clerks);
    }
}
