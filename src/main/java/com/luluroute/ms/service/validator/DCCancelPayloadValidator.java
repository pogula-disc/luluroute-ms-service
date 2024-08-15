package com.luluroute.ms.service.validator;

import com.logistics.luluroute.domain.Shipment.Message.MessageBodyInfo;
import com.logistics.luluroute.domain.Shipment.Message.MessageHeaderInfo;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentHeader;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Shared.EntityRole;
import com.logistics.luluroute.domain.Shipment.Shared.ItemInfo;
import com.logistics.luluroute.domain.Shipment.Shared.ResponseItem;
import com.logistics.luluroute.domain.Shipment.Shared.StatusItem;
import com.luluroute.ms.service.util.RequestType;
import com.luluroute.ms.service.util.ShipmentConstants;
import org.apache.commons.lang3.StringUtils;
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

@Service
@Validators(command = ShipmentValidatorType.SHIPMENT_CANCEL_VALIDATOR)
public class DCCancelPayloadValidator implements ShipmentValidator {

    /**
     * Payload validation for DC shipment payload.
     *
     * @param shipmentMessage - Input shipment payload
     * @return boolean - validation status
     */
    public boolean isPayloadValid(ShipmentMessage shipmentMessage) {
        boolean isValid = true;
        List<ItemInfo> responseInfo = new ArrayList<>();
        ResponseItem responseItem = shipmentMessage.getRequestHeader().getResponse();
        long reqDate = shipmentMessage.getRequestHeader().getRequestDate();
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
            }
        }
        if (reqDate <= 0 || isNotValidDate(reqDate)) {
            ItemInfo itemInfo = ItemInfo.builder().value("RequestDate is invalid").build();
            responseInfo.add(itemInfo);
        }

        if (shipmentMessage.getMessageStatus().getStatus() <= 0) {
            ItemInfo itemInfo = ItemInfo.builder().value("MessageStatus Status is invalid").build();
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
            }
        }

        MessageBodyInfo messageBodyInfo = shipmentMessage.getMessageBody();
        if (Objects.isNull(messageBodyInfo)) {
            ItemInfo itemInfo = ItemInfo.builder().value("MessageBody is missing").build();
            responseInfo.add(itemInfo);
        } else {
            List<ShipmentInfo> shipmentInfos = messageBodyInfo.getShipments();
            if (CollectionUtils.isEmpty(shipmentInfos)) {
                ItemInfo itemInfo = ItemInfo.builder().value("Shipments are missing").build();
                responseInfo.add(itemInfo);
            } else {
                validateShipmentInfo(shipmentInfos, responseInfo);
            }
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

    public boolean isNotValidDate(long inputTime) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        return !isValidDate(today, inputTime);
    }

    public boolean isValidDate(LocalDateTime offsetDate, long inputTime) {
        LocalDateTime inputDate = Instant.ofEpochSecond(inputTime).atZone(ZoneOffset.UTC).toLocalDateTime();
        // Considered as valid date
        return (inputDate.isEqual(offsetDate) || inputDate.isAfter(offsetDate));
    }

    protected void validateShipmentInfo(List<ShipmentInfo> shipmentInfos, List<ItemInfo> responseInfo) {
        shipmentInfos.forEach(shipmentInfo -> {
            ShipmentHeader shipmentHeader = shipmentInfo.getShipmentHeader();

            if (Objects.isNull(shipmentHeader))
                responseInfo.add(ItemInfo.builder().value("ShipmentHeader is missing").build());
            else if (StringUtils.isBlank(shipmentHeader.getShipmentCorrelationId()))
                responseInfo.add(ItemInfo.builder().value("ShipmentCorrelationId is missing").build());

            StatusItem statusItem = shipmentInfo.getShipmentStatus();
            if (Objects.isNull(statusItem)) {
                ItemInfo itemInfo = ItemInfo.builder().value("ShipmentStatus is missing").build();
                responseInfo.add(itemInfo);
            }

            long statusDate = statusItem.getStatusDate();
            if (statusItem.getStatus() <= 0) {
                responseInfo.add(ItemInfo.builder().value("ShipmentStatus Status is invalid").build());
            }
            if (statusDate <= 0 || isNotValidDate(statusDate)) {
                ItemInfo itemInfo = ItemInfo.builder().value("ShipmentStatus StatusDate is invalid").build();
                responseInfo.add(itemInfo);
            }

        });
    }
}
