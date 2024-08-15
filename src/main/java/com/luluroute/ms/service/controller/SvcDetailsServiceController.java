package com.luluroute.ms.service.controller;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.service.config.SwaggerConfig;
import com.luluroute.ms.service.dto.*;
import com.luluroute.ms.service.service.ShipmentRedirectService;
import com.luluroute.ms.service.service.SvcCancelService;
import com.luluroute.ms.service.service.SvcDetailService;
import com.luluroute.ms.service.service.SvcMessageService;
import com.luluroute.ms.service.util.AppLogUtil;
import com.luluroute.ms.service.util.ShipmentConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.concurrent.CompletableFuture;

import static com.luluroute.ms.service.util.ShipmentConstants.MSG_NOT_PROCESSED;

@RestController
@RequestMapping("/v1/api/shipment")
@Slf4j
@Tag(name = SwaggerConfig.SHIPMENT_SVC_TAG, description = "This is used to process Create/Cancel Shipment message")
@RequiredArgsConstructor
public class SvcDetailsServiceController {

    private final SvcDetailService svcDetailService;

    private final SvcMessageService svcMessageService;

    private final SvcCancelService svcCancelService;

    private final ShipmentRedirectService shipmentRedirectService;

    @Operation(tags = SwaggerConfig.SHIPMENT_SVC_TAG,
            description = "RequestHeader.RequestType 2000 is classified as CreateShipment request. \n" +
                    "RequestHeader.RequestType 9989 is CancelShipment request.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully processed the shipment request"),
                    @ApiResponse(responseCode = "400", description = "Unable to process the shipment due to incorrect or missing details in request"),
                    @ApiResponse(responseCode = "401", description = "Cognito token is expired or invalid")
    })
    @PostMapping(value = "/service", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<ShipmentMessage>> processShipmentMessage(
            @RequestBody @Valid ShipmentMessage shipmentMessage) {
        long statTime = System.currentTimeMillis();
        MDC.clear(); // Sanity check - in case a thread w/exception left a trace ID
        log.info(ShipmentConstants.ENTERING_MESSAGE, AppLogUtil.getUniqueTraceId(shipmentMessage));
        try {
            CompletableFuture<ShipmentServiceResponse> futureResponse = shipmentRedirectService.redirectShipmentMessage(shipmentMessage);
            return futureResponse.thenApply(response -> {
                log.info(ShipmentConstants.EXITING_MESSAGE, AppLogUtil.getUniqueTraceId(shipmentMessage), System.currentTimeMillis() - statTime);
                if (response.isSuccess()) {
                    return ResponseEntity.ok(response.getShipmentMessage());
                } else {
                    return ResponseEntity.badRequest().body(response.getShipmentMessage());
                }
            });
        } finally {
            MDC.clear();
        }
    }

//    @PostMapping(value = "/search/svcdetails", produces = MediaType.APPLICATION_JSON_VALUE,
//            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ShipmentQueryResponse> searchShipmentDetails(
            @RequestBody @Valid ShipmentSearchDto searchDto) {
        ShipmentQueryResponse response = svcDetailService.searchShipmentDetails(searchDto);

        if (response.isSuccess())
            return ResponseEntity.ok(response);

        return ResponseEntity.ok(ShipmentQueryResponse.builder()
                .message(MSG_NOT_PROCESSED).build());
    }

//    @PostMapping(value = "/search/svcmessage", produces = MediaType.APPLICATION_JSON_VALUE,
//            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceMessageResponse> searchShipmentServiceMessage(
            @RequestBody @Valid ShipmentSearchDto searchDto) {
        ServiceMessageResponse response = svcMessageService.searchServiceMessage(searchDto);

        if (response.isSuccess())
            return ResponseEntity.ok(response);

        return ResponseEntity.ok(ServiceMessageResponse.builder()
                .message("Shipment message is not processed!").build());
    }

//    @PostMapping(value = "/search/svccancel", produces = MediaType.APPLICATION_JSON_VALUE,
//            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceCancelResponse> searchShipmentServiceCancel(
            @RequestBody @Valid ShipmentSearchDto searchDto) {
        ServiceCancelResponse response = svcCancelService.searchShipmentCancellation(searchDto);

        if (response.isSuccess())
            return ResponseEntity.ok(response);

        return ResponseEntity.ok(ServiceCancelResponse.builder()
                .message(MSG_NOT_PROCESSED).build());
    }
}
