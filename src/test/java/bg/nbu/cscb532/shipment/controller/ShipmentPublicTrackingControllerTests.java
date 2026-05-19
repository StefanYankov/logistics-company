package bg.nbu.cscb532.shipment.controller;

import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shipment.dto.PublicShipmentViewDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Controller Framework: Anonymous Public Tracking")
public class ShipmentPublicTrackingControllerTests extends AbstractShipmentControllerTestBase {

    @Test
    @DisplayName("Happy Path: Should successfully retrieve public shipment by tracking number without auth")
    void shouldRetrievePublicShipmentByTrackingNumber() throws Exception {
        String trackingNumber = "TRK-12345";
        PublicShipmentViewDto responseDto = createValidPublicShipmentViewDto(trackingNumber);

        given(shipmentService.getShipmentByTrackingNumber(trackingNumber)).willReturn(responseDto);

        mockMvc.perform(get(BASE_URL + "/track/{trackingNumber}", trackingNumber))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.trackingNumber").value(trackingNumber));

        verify(shipmentService).getShipmentByTrackingNumber(trackingNumber);
    }

    @Test
    @DisplayName("Error Case: Should return 404 Not Found when shipment does not exist")
    void shouldReturn404WhenTrackingNumberNotFound() throws Exception {
        String trackingNumber = "TRK-INVALID";

        given(shipmentService.getShipmentByTrackingNumber(trackingNumber))
                .willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/track/{trackingNumber}", trackingNumber))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Validation Error: Should return 400 Bad Request when tracking number is blank")
    void shouldReturn400WhenTrackingNumberIsBlank() throws Exception {
        String blankTrackingNumber = "";

        given(shipmentService.getShipmentByTrackingNumber(blankTrackingNumber))
                .willThrow(new BusinessException(ErrorCode.VALIDATION_FAILED));

        mockMvc.perform(get(BASE_URL + "/track/{trackingNumber}", blankTrackingNumber))
                .andExpect(status().isNotFound());
    }
}