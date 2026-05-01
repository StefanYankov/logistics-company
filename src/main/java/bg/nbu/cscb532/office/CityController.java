package bg.nbu.cscb532.office;

import bg.nbu.cscb532.office.dto.CityDto;
import bg.nbu.cscb532.office.dto.CityViewDto;
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
 * RESTful API Controller for managing City entities.
 */
@Slf4j
@RestController
@RequestMapping("/api/cities")
@ApiStandardResponses
@RequiredArgsConstructor
@Tag(name = "City API", description = "Endpoints for managing cities.")
public class CityController {

    private final CityService cityService;

    @Operation(
            summary = "Create a new city",
            description = "Creates a new city. Returns 201 Created with the Location header pointing to the new resource."
    )
    @ApiResponse(responseCode = "201", description = "City created successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g. missing name or postcode format)")
    @ApiResponse(responseCode = "409", description = "Conflict - City already exists")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CityViewDto> createCity(@Valid @RequestBody CityDto dto) {
        log.info("API POST Request to create a city :{} with a postcode: {}", dto.name(), dto.postcode());

        var createdCity = cityService.create(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCity.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdCity);

        // location is not consumed by Angular frontend but as course requires restful API I've kept the response location
        // return ResponseEntity.status(HttpStatus.CREATED).body(createdCity);
    }

    @Operation(
            summary = "Update a city",
            description = "Updates the mutable details (e.g., name, postcode) of an existing city.")
    @ApiResponse(responseCode = "200", description = "City updated successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g. missing name or postcode format)")
    @ApiResponse(responseCode = "404", description = "City not found")
    @ApiResponse(responseCode = "409", description = "Conflict - The new name and postcode combination already exists")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CityViewDto> updateCity(
            @PathVariable Long id,
            @Valid @RequestBody CityDto dto
    ) {
        log.info("API PUT request to update city with ID: {}", id);

        var updatedCity = cityService.update(id, dto);

        return ResponseEntity.ok(updatedCity);
    }

    @Operation(
            summary = "Delete a city",
            description = "Deletes an existing city by its ID."
    )
    @ApiResponse(responseCode = "204", description = "City deleted successfully")
    @ApiResponse(responseCode = "404", description = "City not found")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCity(@PathVariable Long id) {
        log.info("API DELETE request to delete city with ID {}", id);

        cityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get city by ID",
            description = "Retrieves the details of a specific city using its unique ID."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the city")
    @ApiResponse(responseCode = "404", description = "City not found")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CityViewDto> getCityById(@PathVariable Long id) {
        log.info("API GET request for city ID: {}", id);

        CityViewDto city = cityService.getById(id);

        return ResponseEntity.ok(city);
    }

    @Operation(
            summary = "Get all cities",
            description = "Retrieves a paginated and sortable list of all cities."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of cities")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<CityViewDto>> getAllCities(Pageable pageable) {
        log.info("API GET requests for all cities. Pageable: {}", pageable);

        var cities = cityService.getAll(pageable);

        return ResponseEntity.ok(cities);
    }

    @Operation(
            summary = "Search cities by exact name",
            description = "Retrieves all cities matching the provided name. Handles identical names (e.g., 'Troyan') by returning multiple cities grouped by postcode."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved matching cities")
    @ApiResponse(responseCode = "404", description = "No cities found matching the requested name")
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CityViewDto>> getCityByName(@RequestParam String name) {
        log.info("API GET request to search cities by exact name: {}", name);

        List<CityViewDto> cities = cityService.getByName(name);

        return ResponseEntity.ok(cities);
    }
}
