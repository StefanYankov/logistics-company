package bg.nbu.cscb532.company;

import bg.nbu.cscb532.company.dto.CompanyDto;
import bg.nbu.cscb532.company.dto.CompanyUpdateDto;
import bg.nbu.cscb532.company.dto.CompanyViewDto;
import bg.nbu.cscb532.shared.web.ApiStandardResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * RESTful API Controller for managing Company entities.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/companies", produces = MediaType.APPLICATION_JSON_VALUE)
@ApiStandardResponses
@RequiredArgsConstructor
@Tag(name = "Company API", description = "Endpoints for managing logistics companies.")
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "Get all companies", description = "Retrieves a paginated and sortable list of all companies.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of companies")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CompanyViewDto>> getAllCompanies(Pageable pageable) {
        log.info("API GET request for all companies. Pageable: {}", pageable);

        var companies = companyService.getAll(pageable);

        return ResponseEntity.ok(companies);
    }

    @Operation(summary = "Get company by ID", description = "Retrieves the details of a specific company using its unique ID.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the company")
    @ApiResponse(responseCode = "404", description = "Company not found")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyViewDto> getCompanyById(@PathVariable Long id) {
        log.info("API GET request for company ID: {}", id);

        CompanyViewDto company = companyService.getById(id);

        return ResponseEntity.ok(company);
    }

    @Operation(summary = "Get company by Name", description = "Retrieves the details of a specific company by exactly matching its name.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the company")
    @ApiResponse(responseCode = "404", description = "Company not found")
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyViewDto> getCompanyByName(@RequestParam String name) {
        log.info("API GET request to search company by name: {}", name);

        CompanyViewDto company = companyService.getByName(name);

        return ResponseEntity.ok(company);
    }

    @Operation(
            summary = "Create a new company",
            description = "Creates a new logistics company. Returns 201 Created with the Location header pointing to the new resource."
    )
    @PostMapping
    @ApiResponse(responseCode = "201", description = "Company created successfully")
    @ApiResponse(responseCode = "409", description = "Conflict - Registration number already exists")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyViewDto> createCompany(@Valid @RequestBody CompanyDto dto) {
        log.info("API POST request to create company: {}", dto.name());

        CompanyViewDto createdCompany = companyService.create(dto);

        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCompany.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdCompany);

        // location is not consumed by Angular frontend but as course requires restful API I've kept the response location
        // return ResponseEntity.status(HttpStatus.CREATED).body(createdCompany);
    }

    @Operation(summary = "Update a company", description = "Updates the mutable details (e.g., name) of an existing company.")
    @ApiResponse(responseCode = "200", description = "Company updated successfully")
    @ApiResponse(responseCode = "404", description = "Company not found")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyViewDto> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyUpdateDto dto) {

        log.info("API PUT request to update company ID: {}", id);

        CompanyViewDto updatedCompany = companyService.update(id, dto);

        return ResponseEntity.ok(updatedCompany);
    }

    @Operation(summary = "Delete a company", description = "Permanently removes a company from the system.")
    @ApiResponse(responseCode = "204", description = "Company deleted successfully")
    @ApiResponse(responseCode = "404", description = "Company not found")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        log.info("API DELETE request for company with ID: {}", id);

        companyService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
