package com.luluroute.ms.service.util;

import java.util.Objects;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class MultiCarrierAttributesUtil {

	static public String PIPE_DELIMITED = "\\|";

	public static void removeMultiCarrierAttributes(ShipmentMessage shipmentMessage) {
		log.debug("Removed multi-carrier attributes (addressCategory, guessServiceSelected, serviceDateCommitments)");
		ShipmentInfo shipmentInfo = shipmentMessage.getMessageBody().getShipments().get(0);
		if (!Objects.isNull(shipmentInfo.getShipmentHeader().getDestination())) {
			shipmentInfo.getShipmentHeader().getDestination().getAddressTo().setAddressCategory(null);
		}
		if (!Objects.isNull(shipmentInfo.getOrderDetails())) {
			shipmentInfo.getOrderDetails().setGuestServiceSelected(null);
		}
		if (!Objects.isNull(shipmentInfo.getTransitDetails())
				&& !Objects.isNull(shipmentInfo.getTransitDetails().getDateDetails())) {
			shipmentInfo.getTransitDetails().getDateDetails().setServiceDateCommitmentMin(null);
			shipmentInfo.getTransitDetails().getDateDetails().setServiceDateCommitmentMax(null);
		}
	}

}
