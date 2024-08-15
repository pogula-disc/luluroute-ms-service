package com.luluroute.ms.service.validator;

import com.logistics.luluroute.domain.Shipment.Message.MessageHeaderInfo;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentHeader;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.*;
import com.logistics.luluroute.domain.Shipment.Shared.*;
import com.luluroute.ms.service.config.AppConfig;
import com.luluroute.ms.service.util.OrderType;
import com.luluroute.ms.service.util.RequestType;
import com.luluroute.ms.service.util.ShipmentConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.luluroute.ms.service.util.ShipmentConstants.*;

@Slf4j
@Service
@Validators(command = ShipmentValidatorType.DEFAULT)
public class ShipmentValidatorImpl implements ShipmentValidator {

    private final AppConfig appConfig;

    @Autowired
    public ShipmentValidatorImpl(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public boolean isPayloadValid(ShipmentMessage shipmentMessage) {
        boolean isValid = true;
        List<ItemInfo> responseInfo = new ArrayList<>();
        ResponseItem responseItem = shipmentMessage.getRequestHeader().getResponse();
        long reqDate = shipmentMessage.getRequestHeader().getRequestDate();
        long expDate = shipmentMessage.getRequestHeader().getExpireDate();
        MessageHeaderInfo messageHeaderInfo = shipmentMessage.getMessageHeader();
        long msgDate = messageHeaderInfo.getMessageDate();
        String reqType = shipmentMessage.getRequestHeader().getRequestType();

        if (StringUtils.isBlank(reqType)) {
            ItemInfo itemInfo = ItemInfo.builder().value("RequestType is missing").build();
            responseInfo.add(itemInfo);
        } else {
            boolean isInvalidReq = Stream.of(RequestType.values())
                    .noneMatch(value -> reqType.equalsIgnoreCase(value.toString()));
            if (isInvalidReq) {
                ItemInfo itemInfo = ItemInfo.builder().value("RequestType is invalid").build();
                responseInfo.add(itemInfo);
            } else if (RequestType.PERF_REQ.toString().equals(reqType)) {
                List<ItemInfo> msgHeaderExtList = messageHeaderInfo.getExtended();
                if (!CollectionUtils.isEmpty(msgHeaderExtList)) {
                    boolean perfCheck = msgHeaderExtList.stream().noneMatch(
                            info -> appConfig.getPerfAttrKey().equalsIgnoreCase(info.getKey())
                                    && appConfig.getPerfAttrValue().equalsIgnoreCase(info.getValue()));
                    if (perfCheck) {
                        ItemInfo itemInfo = ItemInfo.builder().value(
                                "Request type is 5500, but mandatory attributes missing in MessageHeader Extended").build();
                        responseInfo.add(itemInfo);
                    }
                }
            }
        }
        if (reqDate <= 0 || isNotValidDate(reqDate)) {
            ItemInfo itemInfo = ItemInfo.builder().value("RequestDate is invalid").build();
            responseInfo.add(itemInfo);
        }
        if (expDate != 0 && isNotValidDate(expDate)) {
            ItemInfo itemInfo = ItemInfo.builder().value("ExpireDate is invalid").build();
            responseInfo.add(itemInfo);
        }

        if (shipmentMessage.getMessageStatus().getStatus() <= 0) {
            ItemInfo itemInfo = ItemInfo.builder().value("MessageStatus Status is missing").build();
            responseInfo.add(itemInfo);
        }

        long statusDate = shipmentMessage.getMessageStatus().getStatusDate();
        if (statusDate <= 0 || isNotValidDate(statusDate)) {
            ItemInfo itemInfo = ItemInfo.builder().value("MessageStatus StatusDate is invalid").build();
            responseInfo.add(itemInfo);
        }

        if (StringUtils.isBlank(messageHeaderInfo.getMessageCorrelationId())) {
            ItemInfo itemInfo = ItemInfo.builder().value("MessageCorrelationId is missing").build();
            responseInfo.add(itemInfo);
        }

        if (messageHeaderInfo.getSequence() == 0) {
            ItemInfo itemInfo = ItemInfo.builder().value("Sequence is missing").build();
            responseInfo.add(itemInfo);
        }

        if (messageHeaderInfo.getTotalSequence() == 0) {
            ItemInfo itemInfo = ItemInfo.builder().value("TotalSequence is missing").build();
            responseInfo.add(itemInfo);
        }

        if (msgDate <= 0 || isNotValidDate(msgDate)) {
            ItemInfo itemInfo = ItemInfo.builder().value("MessageDate is invalid").build();
            responseInfo.add(itemInfo);
        }

        List<EntityRole> msgSources = messageHeaderInfo.getMessageSources();
        String msgEntityCode = "";
        if (CollectionUtils.isEmpty(msgSources)) {
            ItemInfo itemInfo = ItemInfo.builder().value("MessageSources are missing").build();
            responseInfo.add(itemInfo);
        } else {
            for (EntityRole entityRole : msgSources) {
                if (StringUtils.isBlank(entityRole.getRoleType())) {
                    ItemInfo itemInfo = ItemInfo.builder().value("MessageSources roleType is missing").build();
                    responseInfo.add(itemInfo);
                }

                if (StringUtils.isBlank(entityRole.getEntityCode())) {
                    ItemInfo itemInfo = ItemInfo.builder().value("MessageSources entityCode is missing").build();
                    responseInfo.add(itemInfo);
                }
                msgEntityCode = entityRole.getEntityCode();
            }
        }

        List<ShipmentInfo> shipmentInfos = shipmentMessage.getMessageBody().getShipments();
        if (CollectionUtils.isEmpty(shipmentInfos)) {
            ItemInfo itemInfo = ItemInfo.builder().value("Shipments are missing").build();
            responseInfo.add(itemInfo);
        } else {
            validateShipmentInfo(shipmentInfos, responseInfo, msgEntityCode, reqType);
        }

        if (!CollectionUtils.isEmpty(responseInfo)) {
            isValid = false;
            // Added safety check for SFS AU payload
            if (responseItem == null) {
                responseItem = ResponseItem.builder().extended(responseInfo)
                        .responseCode(ShipmentConstants.FAILURE_RESPONSE).build();
            } else {
                responseItem.setResponseCode(ShipmentConstants.FAILURE_RESPONSE);
                responseItem.setExtended(responseInfo);
            }
            shipmentMessage.getRequestHeader().setResponse(responseItem);
        }

        return isValid;
    }

    protected void validateShipmentInfo(List<ShipmentInfo> shipmentInfos, List<ItemInfo> responseInfo,
                                      String msgEntityCode, String reqType) {
        shipmentInfos.forEach(shipmentInfo -> {
            ShipmentHeader shipmentHeader = shipmentInfo.getShipmentHeader();
            OriginInfo originInfo = shipmentHeader.getOrigin();
            DestinationInfo destInfo = shipmentHeader.getDestination();
            ReturnInfo returnInfo = shipmentHeader.getReturnLocation();
            TransitInfo transitInfo = shipmentInfo.getTransitDetails();
            OrderInfo orderInfo = shipmentInfo.getOrderDetails();
            long orderCreation = orderInfo.getOrderCreatedDate();
            String originCountry = originInfo.getAddressFrom().getCountry();
            String destCountry = destInfo.getAddressTo().getCountry();
            String destState = destInfo.getAddressTo().getState();
            double totalAmount = orderInfo.getDeclaredValueDetails() != null ? orderInfo.getDeclaredValueDetails().getValue() : 0;
            String orderType = orderInfo.getOrderType();

            if (StringUtils.isBlank(shipmentHeader.getShipmentCorrelationId())) {
                ItemInfo itemInfo = ItemInfo.builder().value("ShipmentCorrelationId is missing").build();
                responseInfo.add(itemInfo);
            }

            // Validate Origin details
            validateOriginInfo(originInfo, responseInfo, msgEntityCode);

            // Validate Destination details - orderType is optional for SFS
            validateDestinationInfo(destInfo, responseInfo, msgEntityCode, reqType, orderInfo.getOrderType());

            if (Objects.isNull(returnInfo)) {
                responseInfo.add(ItemInfo.builder().value("ReturnLocation is missing").build());
            } else
                validateReturnInfo(returnInfo, responseInfo, msgEntityCode);

            ShipmentDatesInfo datesInfo = transitInfo.getDateDetails();
            if (Objects.isNull(datesInfo)) {
                ItemInfo itemInfo = ItemInfo.builder().value("Transit DateDetails are missing").build();
                responseInfo.add(itemInfo);
            } else {
                long plannedShip = datesInfo.getPlannedShipDate();
                long planDelivery = datesInfo.getPlannedDeliveryDate();
                long estDelivery = datesInfo.getEstimatedDeliveryDate();

                if (plannedShip != 0 && isNotValidDate(plannedShip)) {
                    ItemInfo itemInfo = ItemInfo.builder().value("PlannedShipDate " + INVALID_DATE).build();
                    responseInfo.add(itemInfo);
                }

                if (planDelivery != 0 && isNotValidDate(planDelivery)) {
                    ItemInfo itemInfo = ItemInfo.builder().value("PlannedDeliveryDate " + INVALID_DATE).build();
                    responseInfo.add(itemInfo);
                }

                if (estDelivery != 0 && isNotValidOffsetDate(estDelivery)) {
                    ItemInfo itemInfo = ItemInfo.builder().value("EstimatedDeliveryDate " + INVALID_DATE).build();
                    responseInfo.add(itemInfo);
                }
            }

            if (Objects.isNull(transitInfo.getLabelDetails())) {
                ItemInfo itemInfo = ItemInfo.builder().value("Transit LabelDetails are missing").build();
                responseInfo.add(itemInfo);
            } else {
                if (StringUtils.isBlank(transitInfo.getLabelDetails().getFormat())) {
                    ItemInfo itemInfo = ItemInfo.builder().value("LabelDetails Format is missing").build();
                    responseInfo.add(itemInfo);
                }
            }

            if (Objects.isNull(orderInfo)) {
                ItemInfo itemInfo = ItemInfo.builder().value("OrderDetails are missing").build();
                responseInfo.add(itemInfo);
            } else {
                // Validate mandatory attributes in orderInfo
                validateOrderInfo(orderInfo, responseInfo);

                if (orderCreation != 0 && isNotValidOffsetDate(orderCreation)) {
                    ItemInfo itemInfo = ItemInfo.builder().value("OrderCreatedDate is not valid").build();
                    responseInfo.add(itemInfo);
                }
                if (OrderType.ECOMM.name().equalsIgnoreCase(orderType) && totalAmount > appConfig.getMaxAllowedAmount()
                        && isUsToINTL(originCountry, destCountry, destState)) {
                    responseInfo.add(ItemInfo.builder()
                            .value("Shipments with declared value greater than "+appConfig.getMaxAllowedAmount()+" USD require AES filing, these shipments should be processed through cafe")
                            .build());
                }
            }

            StatusItem statusItem = shipmentInfo.getShipmentStatus();
            if (Objects.isNull(statusItem)) {
                ItemInfo itemInfo = ItemInfo.builder().value("ShipmentStatus is missing").build();
                responseInfo.add(itemInfo);
            } else {
                long statusDate = statusItem.getStatusDate();
                if (statusItem.getStatus() <= 0) {
                    responseInfo.add(ItemInfo.builder().value("ShipmentStatus Status is invalid").build());
                }
                if (statusDate <= 0 || isNotValidDate(statusDate)) {
                    ItemInfo itemInfo = ItemInfo.builder().value("ShipmentStatus StatusDate is invalid").build();
                    responseInfo.add(itemInfo);
                }
            }

            List<ShipmentPieceInfo> shipmentPieces = shipmentInfo.getShipmentPieces();
            if (CollectionUtils.isEmpty(shipmentPieces)) {
                ItemInfo itemInfo = ItemInfo.builder().value("ShipmentPieces are missing").build();
                responseInfo.add(itemInfo);
            } else {
                shipmentPieces.forEach(shipmentPieceInfo -> {
                    if (shipmentPieceInfo.getSequence() == 0) {
                        ItemInfo itemInfo = ItemInfo.builder().value("ShipmentPieces sequence is missing").build();
                        responseInfo.add(itemInfo);
                    }
                    if (shipmentPieceInfo.getTotalSequence() == 0) {
                        ItemInfo itemInfo = ItemInfo.builder()
                                .value("ShipmentPieces TotalSequence is missing").build();
                        responseInfo.add(itemInfo);
                    }
                    MeasureItem weightInfo = shipmentPieceInfo.getWeightDetails();
                    if (Objects.isNull(weightInfo)) {
                        ItemInfo itemInfo = ItemInfo.builder()
                                .value("ShipmentPieces WeightDetails are missing").build();
                        responseInfo.add(itemInfo);
                    } else {
                        if (StringUtils.isBlank(weightInfo.getUom())) {
                            ItemInfo itemInfo = ItemInfo.builder()
                                    .value("WeightDetails UOM is missing").build();
                            responseInfo.add(itemInfo);
                        }
                        if (weightInfo.getValue() == 0) {
                            ItemInfo itemInfo = ItemInfo.builder()
                                    .value("WeightDetails value is missing").build();
                            responseInfo.add(itemInfo);
                        }
                    }

                    List<CartonInfo> cartonsDetails = shipmentPieceInfo.getCartonsDetails();
                    if (CollectionUtils.isEmpty(cartonsDetails)) {
                        ItemInfo itemInfo = ItemInfo.builder()
                                .value("CartonsDetails are missing").build();
                        responseInfo.add(itemInfo);
                    } else {
                        cartonsDetails.forEach(cartonInfo -> {
                            if (StringUtils.isBlank(cartonInfo.getItemCode())) {
                                ItemInfo itemInfo = ItemInfo.builder()
                                        .value("CartonsDetails ItemCode is missing").build();
                                responseInfo.add(itemInfo);
                            }
                            if (cartonInfo.getItemSequence() == 0) {
                                ItemInfo itemInfo = ItemInfo.builder()
                                        .value("CartonsDetails ItemSequence is invalid").build();
                                responseInfo.add(itemInfo);
                            }
                            if (cartonInfo.getItemSequenceTotal() == 0) {
                                ItemInfo itemInfo = ItemInfo.builder()
                                        .value("CartonsDetails ItemSequenceTotal is invalid").build();
                                responseInfo.add(itemInfo);
                            }
                            if (StringUtils.isBlank(cartonInfo.getDescription())) {
                                ItemInfo itemInfo = ItemInfo.builder()
                                        .value("CartonsDetails Description is missing").build();
                                responseInfo.add(itemInfo);
                            }
                            if (cartonInfo.getQuantity() == 0) {
                                ItemInfo itemInfo = ItemInfo.builder()
                                        .value("CartonsDetails Quantity is invalid").build();
                                responseInfo.add(itemInfo);
                            }
                        });
                    }
                });
            }
        });
    }

    public boolean isUsToINTL(String originCountry, String destCountry, String destState) {
        if (US.equalsIgnoreCase(originCountry) && (!US.equalsIgnoreCase(destCountry)
                    || (US.equalsIgnoreCase(destCountry) && PR_COUNTRY.equalsIgnoreCase(destState)))) {
            return true;
        }
        return false;
    }

    /**
     * ISL layer requires destination entity code if retail order type. Otherwise, value is optional
     * @param destinationInfo request payload's destination info
     * @param responseInfo response info object
     * @param orderType shipment's order type
     */
    protected void validateDestEntityCode(DestinationInfo destinationInfo, List<ItemInfo> responseInfo,
                                          String orderType) {
        if (isRetailOrderType(orderType) && StringUtils.isBlank(destinationInfo.getEntityCode())) {
            ItemInfo itemInfo = ItemInfo.builder().value("ShipmentHeader/Destination/EntityCode is missing").build();
            responseInfo.add(itemInfo);
        }
    }

    public boolean isRetailOrderType(String orderType) {
        return OrderType.getRetailOrderTypeList().stream().anyMatch(orderType::equalsIgnoreCase);
    }

    protected void validateAddressInfo(LocationItem locationItem, List<ItemInfo> responseInfo,
                                    int addrType, String entityCode) {

        if (Objects.isNull(locationItem)) {
            ItemInfo itemInfo = ItemInfo.builder().value("Address details are missing").build();
            responseInfo.add(itemInfo);
        } else {
            if (StringUtils.isBlank(locationItem.getContact())) {
                ItemInfo itemInfo = ItemInfo.builder().value("Contact is missing").build();
                responseInfo.add(itemInfo);
            }
            if (addrType == ShipmentConstants.ADDR_TYPE_ORIGIN
                    && StringUtils.isBlank(locationItem.getContactPhone())) {
                locationItem.setContactPhone(DEFAULT_PHONE);
            }
            if (StringUtils.isBlank(locationItem.getDescription1())) {
                ItemInfo itemInfo = ItemInfo.builder().value("Description1 is missing").build();
                responseInfo.add(itemInfo);
            }
            if (StringUtils.isBlank(locationItem.getCity())) {
                ItemInfo itemInfo = ItemInfo.builder().value("City is missing").build();
                responseInfo.add(itemInfo);
            }
            if (!appConfig.getStateNotMandatoryEntityCodes().contains(entityCode.toUpperCase()) && StringUtils.isNotBlank(entityCode)
                    && StringUtils.isBlank(locationItem.getState())) {
                ItemInfo itemInfo = ItemInfo.builder().value("State is missing").build();
                responseInfo.add(itemInfo);
            }
            if (StringUtils.isBlank(locationItem.getCountry())) {
                ItemInfo itemInfo = ItemInfo.builder().value("Country is missing").build();
                responseInfo.add(itemInfo);
            }
            if (StringUtils.isBlank(locationItem.getZipCode())) {
                ItemInfo itemInfo = ItemInfo.builder().value("Zipcode is missing").build();
                responseInfo.add(itemInfo);
            }
        }
    }

    public boolean isNotValidDate(long inputTime) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        return !isValidDate(today, inputTime);
    }

    public boolean isNotValidOffsetDate(long inputTime) {
        LocalDateTime offsetDay = LocalDate.now().atStartOfDay().minusDays(appConfig.getOffsetDays());
        return !isValidDate(offsetDay, inputTime);
    }

    public boolean isValidDate(LocalDateTime offsetDate, long inputTime) {
        LocalDateTime inputDate = Instant.ofEpochSecond(inputTime).atZone(ZoneOffset.UTC).toLocalDateTime();
        // Considered as valid date
        return (inputDate.isEqual(offsetDate) || inputDate.isAfter(offsetDate));
    }

    protected void validateReturnInfo(ReturnInfo returnInfo, List<ItemInfo> responseInfo,
                                      String msgEntityCode) {
        LocationItem returnAddr = returnInfo.getReturnAddress();
        // Validating return address.
        validateAddressInfo(returnAddr, responseInfo,
                ShipmentConstants.ADDR_TYPE_RETURN, msgEntityCode);
    }

    protected void validateOriginInfo(OriginInfo originInfo, List<ItemInfo> responseInfo,
                                      String msgEntityCode) {
        if (Objects.isNull(originInfo)) {
            ItemInfo itemInfo = ItemInfo.builder().value("Origin is missing").build();
            responseInfo.add(itemInfo);
        } else {
            if (StringUtils.isBlank(originInfo.getEntityCode())) {
                ItemInfo itemInfo = ItemInfo.builder().value("Origin EntityCode is missing").build();
                responseInfo.add(itemInfo);
            }

            validateAddressInfo(originInfo.getAddressFrom(), responseInfo,
                    ShipmentConstants.ADDR_TYPE_ORIGIN, msgEntityCode);
        }
    }

    protected void validateDestinationInfo(DestinationInfo destinationInfo, List<ItemInfo> responseInfo,
                                      String msgEntityCode, String reqType, String orderType) {
        if (Objects.isNull(destinationInfo)) {
            ItemInfo itemInfo = ItemInfo.builder().value("Destination is missing").build();
            responseInfo.add(itemInfo);
        } else {
            LocationItem addressTo = destinationInfo.getAddressTo();
            validateDestEntityCode(destinationInfo, responseInfo, orderType);
            validateAddressInfo(addressTo, responseInfo,
                    ShipmentConstants.ADDR_TYPE_DESTINATION, msgEntityCode);
            validateUsTerritoryCountry(responseInfo, addressTo);
        }
    }

    protected void validateUsTerritoryCountry(List<ItemInfo> responseInfo, LocationItem addressTo) {
        String destCountry = addressTo.getCountry();
        String destState = addressTo.getState().toUpperCase();
        log.debug("Validating US Territory Country | Destination Country: {} | Destination State: {}", destCountry, destState);
        if (US.equalsIgnoreCase(destCountry) && appConfig.getUsTerritories().contains(destState)) {
            ItemInfo itemInfo = ItemInfo.builder().value(String.format(US_TERRITORY_DEST_ERROR_MSG, destState, destCountry, destState)).build();
            responseInfo.add(itemInfo);
        }
    }

    protected void validateOrderInfo(OrderInfo orderInfo, List<ItemInfo> responseInfo) {
        if (StringUtils.isBlank(orderInfo.getOrderId())) {
            ItemInfo itemInfo = ItemInfo.builder().value("OrderId in OrderDetails is missing").build();
            responseInfo.add(itemInfo);
        }
        if (StringUtils.isBlank(orderInfo.getOrderType())) {
            ItemInfo itemInfo = ItemInfo.builder().value("OrderType in OrderDetails is missing").build();
            responseInfo.add(itemInfo);
        }
        if (StringUtils.isBlank(orderInfo.getIntegration())) {
            ItemInfo itemInfo = ItemInfo.builder().value("Integration in OrderDetails is missing").build();
            responseInfo.add(itemInfo);
        }
    }

}
