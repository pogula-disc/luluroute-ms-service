package com.luluroute.ms.service.validator;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.*;
import com.logistics.luluroute.domain.Shipment.Shared.*;
import com.luluroute.ms.service.config.AppConfig;
import com.luluroute.ms.service.util.RequestType;
import com.luluroute.ms.service.util.ShipmentConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.luluroute.ms.service.util.ShipmentConstants.SOAP_CONTEXT;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

@Service
@Validators(command = ShipmentValidatorType.DC_SHIPMENT_VALIDATOR)
public class DCPayloadValidator extends ShipmentValidatorImpl {

    private final AppConfig appConfig;

    public DCPayloadValidator(AppConfig appConfig) {
        super(appConfig);
        this.appConfig = appConfig;
    }

    /**
     * Payload validation for DC shipment payload.
     *
     * @param shipmentMessage - Input shipment payload
     * @return boolean - validation status
     */
    public boolean isPayloadValid(ShipmentMessage shipmentMessage) {
        boolean isValid = super.isPayloadValid(shipmentMessage);
        ResponseItem responseItem = shipmentMessage.getRequestHeader().getResponse();
        List<ItemInfo> responseInfo = (Objects.isNull(responseItem) || Objects.isNull(responseItem.getExtended()))
                ? new ArrayList<>() : responseItem.getExtended();

        Optional<EntityRole> entityRole = Optional.ofNullable(shipmentMessage
                .getMessageHeader().getMessageSources().get(0));
        String entityCode = entityRole.isPresent() ? entityRole.get().getEntityCode() : "";
        String reqType = shipmentMessage.getRequestHeader().getRequestType();
        List<ShipmentInfo> shipmentInfoList = shipmentMessage.getMessageBody().getShipments();
        shipmentInfoList.forEach(shipmentInfo -> {
            List<ShipmentPieceInfo> shipmentPieces = shipmentInfo.getShipmentPieces();
            CurrencyItem declaredValues = shipmentInfo.getOrderDetails().getDeclaredValueDetails();
            OriginInfo originInfo = shipmentInfo.getShipmentHeader().getOrigin();
            DestinationInfo destInfo = shipmentInfo.getShipmentHeader().getDestination();
            boolean isCrossBorder = this.isCrossBorderShipment(originInfo, destInfo, reqType, entityCode);

            if (StringUtils.isBlank(shipmentInfo.getOrderDetails().getShipmentContents())) {
                responseInfo.add(ItemInfo.builder().value("ShipmentContents in OrderDetails are empty").build());
            }

            if (isCrossBorder) {
                if (Objects.isNull(declaredValues)) {
                    responseInfo.add(ItemInfo.builder()
                            .value("DeclaredValueDetails in OrderDetails are required for X-border shipments").build());
                } else {
                    String ordCurrency = declaredValues.getCurrency();
                    double ordVal = declaredValues.getValue();
                    if(StringUtils.isBlank(ordCurrency)) {
                        responseInfo.add(ItemInfo.builder()
                                .value("Currency in OrderDetails/DeclaredValueDetails is empty").build());
                    }
                    if (ordVal <= 0d) {
                        responseInfo.add(ItemInfo.builder()
                                .value("Value in OrderDetails/DeclaredValueDetails is invalid").build());
                    }
                }
            }

            shipmentPieces.forEach(shipmentPieceInfo -> {
                List<CartonInfo> cartonsDetails = shipmentPieceInfo.getCartonsDetails();
                cartonsDetails.forEach(cartonInfo -> {
                    if (Objects.isNull(cartonInfo)) {
                        responseInfo.add(ItemInfo.builder()
                                .value("CartonsDetails is missing").build());
                    }
                    MeasureItem measureItem = cartonInfo.getWeightDetails();
                    if (Objects.isNull(measureItem)) {
                        responseInfo.add(ItemInfo.builder()
                                .value("WeightDetails in CartonsDetails is missing").build());
                    } else {
                        if (StringUtils.isBlank(measureItem.getUom())) {
                            responseInfo.add(ItemInfo.builder().value("Uom in WeightDetails is missing").build());
                        }
                        if (measureItem.getValue() <= 0d) {
                            responseInfo.add(ItemInfo.builder().value("Value in WeightDetails is invalid").build());
                        }
                    }

                    CurrencyItem currencyItem = cartonInfo.getItemValue();
                    if (!equalsIgnoreCase("true", MDC.get(SOAP_CONTEXT))) {
                        if (Objects.isNull(currencyItem)) {
                            responseInfo.add(ItemInfo.builder()
                                    .value("ItemValue in CartonsDetails is missing").build());
                        } else {
                            if (StringUtils.isBlank(currencyItem.getCurrency())) {
                                responseInfo.add(ItemInfo.builder().value("Currency in ItemValue is missing").build());
                            }
                            if (currencyItem.getValue() <= 0d) {
                                responseInfo.add(ItemInfo.builder().value("Value in ItemValue is invalid").build());
                            }
                        }
                    }

                    if (isCrossBorder) {
                        TradeDataInfo tradeInfo = cartonInfo.getTradeDetails();
                        if (Objects.isNull(tradeInfo)) {
                            responseInfo.add(ItemInfo.builder()
                                    .value("TradeDetails are missing in CartonsDetails").build());
                        } else {
                            if (StringUtils.isBlank(tradeInfo.getItemsContents())) {
                                responseInfo.add(ItemInfo.builder()
                                        .value("ItemsContents are missing in TradeDetails in CartonsDetails").build());
                            }

                            if (StringUtils.isBlank(tradeInfo.getManufacturerID())) {
                                responseInfo.add(ItemInfo.builder()
                                        .value("ManufacturerID is missing in TradeDetails in CartonsDetails").build());
                            }

                            if (StringUtils.isBlank(tradeInfo.getCountryOfOriginCode())) {
                                responseInfo.add(ItemInfo.builder()
                                        .value("CountryOfOriginCode is missing in TradeDetails in CartonsDetails")
                                        .build());
                            }

                            if (StringUtils.isBlank(tradeInfo.getHsTariffNumber())) {
                                responseInfo.add(ItemInfo.builder()
                                        .value("HsTariffNumber is missing in TradeDetails in CartonsDetails").build());
                            }

                            if (Objects.isNull(tradeInfo.getDeclaredValueDetails())) {
                                responseInfo.add(ItemInfo.builder()
                                        .value("DeclaredValueDetails are missing in CartonsDetails/TradeDetails")
                                        .build());
                            } else {
                                if (tradeInfo.getDeclaredValueDetails().getValue() <= 0d) {
                                    responseInfo.add(ItemInfo.builder()
                                            .value("Value in CartonsDetails/TradeDetails/DeclaredValueDetails is invalid")
                                            .build());
                                }
                                if (StringUtils.isBlank(tradeInfo.getDeclaredValueDetails().getCurrency())) {
                                    responseInfo.add(ItemInfo.builder()
                                            .value("Currency in CartonsDetails/TradeDetails/DeclaredValueDetails is missing")
                                            .build());
                                }
                            }
                        }
                    }
                });
            });
        });

        if (!CollectionUtils.isEmpty(responseInfo)) {
            isValid = false;
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

    public boolean isCrossBorderShipment(OriginInfo originInfo, DestinationInfo destinationInfo,
                                         String reqType, String entityCode) {
        String originCountry = originInfo.getAddressFrom().getCountry();
        String destCountry = destinationInfo.getAddressTo().getCountry();
        boolean isUSCountry = ShipmentConstants.US.equalsIgnoreCase(originCountry)
                && ShipmentConstants.US.equalsIgnoreCase(destCountry);
        boolean isInternational = ShipmentConstants.CA.equalsIgnoreCase(originCountry)
                && !ShipmentConstants.CA.equalsIgnoreCase(destCountry);
        boolean isCanadaDC = StringUtils.isNotBlank(entityCode)
                && appConfig.getIntlDcEntityCodes().contains(entityCode);

        return RequestType.SHIPMENT_REQ.getValue().equals(reqType)
                && isCanadaDC && (isUSCountry || isInternational);
    }

    @Override
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
            if (addrType != ShipmentConstants.ADDR_TYPE_RETURN
                    && StringUtils.isBlank(locationItem.getContactPhone())) {
                ItemInfo itemInfo = ItemInfo.builder().value("ContactPhone is missing").build();
                responseInfo.add(itemInfo);
            }
            if (StringUtils.isBlank(locationItem.getDescription1()))
                responseInfo.add(ItemInfo.builder().value("Description1 is missing").build());

            if (StringUtils.isBlank(locationItem.getCity()))
                responseInfo.add(ItemInfo.builder().value("City is missing").build());

            if (StringUtils.isBlank(locationItem.getState()))
                responseInfo.add(ItemInfo.builder().value("State is missing").build());

            if (StringUtils.isBlank(locationItem.getCountry()))
                responseInfo.add(ItemInfo.builder().value("Country is missing").build());

            if (StringUtils.isBlank(locationItem.getZipCode()))
                responseInfo.add(ItemInfo.builder().value("Zipcode is missing").build());
        }
    }

    @Override
    protected void validateReturnInfo(ReturnInfo returnInfo, List<ItemInfo> responseInfo,
                                      String msgEntityCode) {
        if (StringUtils.isBlank(returnInfo.getEntityCode()))
            responseInfo.add(ItemInfo.builder().value("ReturnLocation EntityCode is missing").build());
        // Validating return address.
        this.validateAddressInfo(returnInfo.getReturnAddress(), responseInfo,
                ShipmentConstants.ADDR_TYPE_RETURN, msgEntityCode);
    }

    @Override
    protected void validateDestinationInfo(DestinationInfo destinationInfo, List<ItemInfo> responseInfo,
                                           String msgEntityCode, String reqType, String orderType) {
        if (Objects.isNull(destinationInfo)) {
            responseInfo.add(ItemInfo.builder().value("Destination is missing").build());
        } else {
            if (RequestType.SHIPMENT_REQ.getValue().equals(reqType)
                    && appConfig.getIntlDcEntityCodes().contains(msgEntityCode)
                    && appConfig.getRetailOrderTypes().contains(orderType)
                    && StringUtils.isBlank(destinationInfo.getEntityCode()))
                responseInfo.add(ItemInfo.builder().value("Destination EntityCode is missing").build());

            validateAddressInfo(destinationInfo.getAddressTo(), responseInfo,
                    ShipmentConstants.ADDR_TYPE_DESTINATION, msgEntityCode);
            super.validateUsTerritoryCountry(responseInfo, destinationInfo.getAddressTo());
        }
    }

    @Override
    protected void validateOrderInfo(OrderInfo orderInfo, List<ItemInfo> responseInfo) {
        if (StringUtils.isBlank(orderInfo.getOrderId()))
            responseInfo.add(ItemInfo.builder().value("orderId in OrderDetails is missing").build());
        if (StringUtils.isBlank(orderInfo.getOrderType()))
            responseInfo.add(ItemInfo.builder().value("orderType in OrderDetails is missing").build());
        if (Objects.isNull(orderInfo.getBillingDetails()))
            responseInfo.add(ItemInfo.builder().value("BillingDetails in OrderDetails are required").build());
    }
}
