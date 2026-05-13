package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shipment.dto.ServiceCatalogViewDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceCatalogServiceImpl implements ServiceCatalogService {

    private final ServiceCatalogRepository serviceCatalogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ServiceCatalogViewDto> getAllServices() {
        log.debug("Fetching all services from the catalog");
        
        return serviceCatalogRepository.findAll().stream()
                .map(this::mapToViewDto)
                .toList();
    }

    private ServiceCatalogViewDto mapToViewDto(ServiceCatalog service) {
        return ServiceCatalogViewDto.builder()
                .id(service.getId())
                .name(service.getName())
                .pricingType(service.getPricingType())
                .pricingValue(service.getPricingValue())
                .build();
    }
}
