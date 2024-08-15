package com.luluroute.ms.service.service.impl;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Shared.LocationItem;
import com.logistics.luluroute.domain.Shipment.Shared.RateShopResponse;
import com.logistics.luluroute.domain.rateshop.CarrierPool;
import com.logistics.luluroute.domain.rateshop.Destination;
import com.logistics.luluroute.domain.rateshop.EntityCaller;
import com.logistics.luluroute.domain.rateshop.Origin;
import com.logistics.luluroute.domain.rateshop.RateShop;
import com.logistics.luluroute.domain.rateshop.TransitDetails;
import com.logistics.luluroute.domain.response.entity.EntityProfile;
import com.logistics.luluroute.domain.response.entity.Mode;
import com.luluroute.ms.service.dto.RateShopResponseDto;
import com.luluroute.ms.service.dto.ShipmentS3Input;
import com.luluroute.ms.service.service.SvcRequestRateShopService;
import com.luluroute.ms.service.util.ShipmentConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SvcRequestRateShopServiceImpl implements SvcRequestRateShopService {

	@Value("${lulu-route.shipment-svc.scope.request}")
	private String scope;

	private final RestTemplate restTemplate;

	@Value("${rateshop.rateShopURL}")
	private String rateShopUrl;

	@Override
	public RateShopResponseDto requestRateShop(String authorizationHeader, ShipmentS3Input messageDto)
			throws JSONException, JsonMappingException, JsonProcessingException, URISyntaxException {
		
		log.info("Inside SvcRequestRateShopServiceImpl executing requestRateShop");
		Boolean isAuthorized = getClientIdAfterToken(authorizationHeader);
		if (isAuthorized) {
			List<RateShop> rateShopReqList = convertToRateShopReq(messageDto);
			HttpHeaders headers = new HttpHeaders();
			headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
			RateShopResponseDto rateShopResponse = getCallEntityService(headers, rateShopReqList);
			return rateShopResponse;
		} else {
			return null;
		}
	}

	private List<RateShop> convertToRateShopReq(ShipmentS3Input messageDto) {
		log.info("Inside SvcRequestRateShopServiceImpl executing convertToRateShopReq");
		ShipmentMessage message = messageDto.getMessage();
		EntityProfile entityProfile = messageDto.getEntityProfile();

		List<ShipmentInfo> shipmentList = message.getMessageBody().getShipments();
		List<RateShop> rateShopReqList = new ArrayList<>();

		for (ShipmentInfo shipmentInfo : shipmentList) {
			RateShop rateShop = new RateShop();
			EntityCaller entityCaller = new EntityCaller();
			List<CarrierPool> carrierPoolList = new ArrayList<>();
			TransitDetails transitDetails = new TransitDetails();
			Origin origin = new Origin();
			Destination destination = new Destination();

			// set entity obj
			entityCaller.setEntityCode(shipmentInfo.getShipmentHeader().getOrigin().getEntityCode());
			entityCaller.setEntityType(entityProfile.getSourceEntityInfo().getEntityType() != null
					? Integer.valueOf(entityProfile.getSourceEntityInfo().getEntityType())
					: 0);

			// set carrier pool
			List<Mode> modes = entityProfile.getSourceEntityInfo().getModes();
			for (Mode modeObj : modes) {
				CarrierPool carrierPool = new CarrierPool();
				carrierPool.setCarrierCode(modeObj.getCarrierCode());
				carrierPool.setCarrierTimeZone(entityProfile.getSourceEntityInfo().getTimezone());
				carrierPool.setAccountToUse(modeObj.getAccountNo());
				carrierPoolList.add(carrierPool);
			}

			// set transit details
			transitDetails.setShipmentCorrelationId(shipmentInfo.getShipmentHeader().getShipmentCorrelationId());
			transitDetails.setPlannedShipDate(shipmentInfo.getTransitDetails().getDateDetails().getPlannedShipDate());
			transitDetails
					.setPlannedDeliveryDate(shipmentInfo.getTransitDetails().getDateDetails().getPlannedDeliveryDate());
			transitDetails.setTransitMode(shipmentInfo.getTransitDetails().getTransitMode());
			transitDetails.setRouteNo(shipmentInfo.getTransitDetails().getRouteRuleCode());
			transitDetails.setSortCode(shipmentInfo.getTransitDetails().getSortCode());
			transitDetails.setAirportCode(shipmentInfo.getTransitDetails().getAirportCode());
			transitDetails.setHubCode(shipmentInfo.getTransitDetails().getHubCode());
			transitDetails.setIsResidential(shipmentInfo.getTransitDetails().isResidential());
			transitDetails.setIsAuthorityLeave(shipmentInfo.getTransitDetails().isAuthorityLeave());
			transitDetails.setIsSaturdayDelivery(shipmentInfo.getTransitDetails().isSaturdayDelivery());
			transitDetails.setIsSignatureRequired(shipmentInfo.getTransitDetails().isSignatureRequired());

			// set Origin
			origin.setEntityId(null);
			origin.setEntityType(entityProfile.getSourceEntityInfo().getEntityType() != null
					? Integer.valueOf(entityProfile.getSourceEntityInfo().getEntityType())
					: 0);
			origin.setEntityCode(shipmentInfo.getShipmentHeader().getOrigin().getEntityCode());
			origin.setEntityLegacyCode(null);
			origin.setFacility(null);
			origin.setFacilityName(null);
			origin.setFacilityTimeZone(null);

			// origin location AddressFrom
			LocationItem originLocation = new LocationItem();
			originLocation.setLocationId(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getHashKey());
			originLocation.setContact(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getContact());
			originLocation
					.setContactPhone(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getContactPhone());
			originLocation
					.setDescription1(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getDescription1());
			originLocation
					.setDescription2(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getDescription2());
			originLocation
					.setDescription3(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getDescription3());
			originLocation
					.setDescription4(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getDescription4());
			originLocation
					.setDescription5(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getDescription5());
			originLocation.setCity(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getCity());
			originLocation.setState(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getState());
			originLocation.setCountry(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getCountry());
			originLocation.setZipCode(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getZipCode());
			originLocation.setValidated(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().isValidated());
			originLocation.setValidatedEntity(
					shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getValidatedEntity());
			originLocation
					.setValidatedDate(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getValidatedDate());

			origin.setAddressFrom(originLocation);

			// set Destination
			destination.setEntityId(null);
			destination.setEntityType(entityProfile.getDestEntityInfo().getEntityType() != null
					? Integer.valueOf(entityProfile.getDestEntityInfo().getEntityType())
					: 0);
			destination.setEntityCode(shipmentInfo.getShipmentHeader().getDestination().getEntityCode());
			destination.setEntityLegacyCode(null);
			destination.setFacility(null);
			destination.setFacilityName(null);
			destination.setFacilityTimeZone(null);

			// origin location AddressFrom
			LocationItem dstLocation = new LocationItem();
			dstLocation.setLocationId(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getHashKey());
			dstLocation.setContact(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getContact());
			dstLocation.setContactPhone(
					shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getContactPhone());
			dstLocation.setDescription1(
					shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getDescription1());
			dstLocation.setDescription2(
					shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getDescription2());
			dstLocation.setDescription3(
					shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getDescription3());
			dstLocation.setDescription4(
					shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getDescription4());
			dstLocation.setDescription5(
					shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getDescription5());
			dstLocation.setCity(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getCity());
			dstLocation.setState(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getState());
			dstLocation.setCountry(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getCountry());
			dstLocation.setZipCode(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getZipCode());
			dstLocation.setValidated(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().isValidated());
			dstLocation.setValidatedEntity(
					shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getValidatedEntity());
			dstLocation.setValidatedDate(
					shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getValidatedDate());

			destination.setAddressTo(dstLocation);

			rateShop.setEntityCaller(entityCaller);
			rateShop.setCarrierPool(carrierPoolList);
			rateShop.setTransitDetails(transitDetails);
			rateShop.setOrigin(origin);
			rateShop.setDestination(destination);

			rateShopReqList.add(rateShop);

		}

		return rateShopReqList;

	}

	public Boolean getClientIdAfterToken(String authorizationHeader) throws JSONException {
		String[] parts = authorizationHeader.split("\\.");
		String base64EncodedBody = parts[1];
		Base64 base64Url = new Base64(true);
		String payload = new String(base64Url.decode(base64EncodedBody));
		JSONObject jsonPayload = new JSONObject(payload);
		String client_id = jsonPayload.getString("client_id");
		log.info("client_id---------\n" + client_id);
		return null != client_id ? true : false;

	}

	public RateShopResponseDto getCallEntityService(HttpHeaders headers, List<RateShop> rateShopList)
			throws JsonMappingException, JsonProcessingException, URISyntaxException {
		log.info("Inside SvcRequestRateShopServiceImpl executing getCallEntityService");
		HttpHeaders headers1 = new HttpHeaders();
		String baererToken = headers.getFirst(HttpHeaders.AUTHORIZATION);
		headers1.set(HttpHeaders.AUTHORIZATION, baererToken);
		headers1.setContentType(MediaType.APPLICATION_JSON);
		headers1.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		HttpEntity<List<RateShop>> request = new HttpEntity<>(rateShopList, headers1);

		ResponseEntity<RateShopResponseDto> response = restTemplate.postForEntity(rateShopUrl, request,
				RateShopResponseDto.class);
		log.info("Successfully posted to RateShop");
		log.info("Response from legacyShipmentService - response: {}", response);
		if(response == null || response.getBody() == null)
			return null;
		return response.getBody();
	}
}
