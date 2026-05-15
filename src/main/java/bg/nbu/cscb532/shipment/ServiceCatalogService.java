package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shipment.dto.ServiceCatalogViewDto;

import java.util.List;

/**
 * Service for managing the Service Catalog.
 */
public interface ServiceCatalogService {

    /**
     * Retrieves all available services in the catalog.
     *
     * @return A list of ServiceCatalogViewDto.
     */
    List<ServiceCatalogViewDto> getAllServices();
}
