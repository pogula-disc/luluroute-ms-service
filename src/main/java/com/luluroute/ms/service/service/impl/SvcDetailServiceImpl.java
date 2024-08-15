package com.luluroute.ms.service.service.impl;

import com.logistics.luluroute.domain.Shipment.Message.MessageBodyInfo;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.DestinationInfo;
import com.logistics.luluroute.domain.Shipment.Service.OriginInfo;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Shared.LocationItem;
import com.logistics.luluroute.domain.response.entity.EntityProfile;
import com.luluroute.ms.service.dto.*;
import com.luluroute.ms.service.exception.ShipmentPersistenceException;
import com.luluroute.ms.service.model.ServiceDetail;
import com.luluroute.ms.service.model.ServiceLocation;
import com.luluroute.ms.service.repository.ServiceDetailRepository;
import com.luluroute.ms.service.repository.ServiceLocationRepository;
import com.luluroute.ms.service.service.SvcDetailService;
import com.luluroute.ms.service.service.processor.ShipmentMessageBuilder;
import com.luluroute.ms.service.util.HashKeyGenerator;
import com.luluroute.ms.service.util.ObjectMapperUtil;
import com.luluroute.ms.service.util.ShipmentConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.luluroute.ms.service.util.ShipmentConstants.LOCATION_DEST;
import static com.luluroute.ms.service.util.ShipmentConstants.LOCATION_ORIGIN;

@Service
@Slf4j
public class SvcDetailServiceImpl implements SvcDetailService {

    @Autowired
    private ShipmentMessageBuilder messageProcessor;

    @Autowired
    private ServiceLocationRepository locationRepository;

    @Autowired
    private ServiceDetailRepository serviceDetailRepository;

    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${entity.entityServiceUrl}")
    private String entityUrl;
    

    @Autowired
    public SvcDetailServiceImpl(ShipmentMessageBuilder messageProcessor, ServiceLocationRepository locationRepository,
                                ServiceDetailRepository serviceDetailRepository, RestTemplate restTemplate) {
    	this.messageProcessor = messageProcessor;
    	this.locationRepository = locationRepository;
    	this.serviceDetailRepository = serviceDetailRepository;
    	this.restTemplate = restTemplate;
    }

    @Override
    public ShipmentServiceResponse saveShipmentServiceDetails(ShipmentMessage shipmentMessage)
            throws ShipmentPersistenceException {

        Optional<MessageBodyInfo> bodyInfoOptional = Optional.ofNullable(shipmentMessage.getMessageBody());
        try {
            bodyInfoOptional.ifPresent(messageBodyInfo -> {
                List<ShipmentInfo> shipmentInfoList = messageBodyInfo.getShipments();
                shipmentInfoList.forEach(shipmentInfo -> {
                    String shipCorrId = shipmentInfo.getShipmentHeader().getShipmentCorrelationId();
                    OriginInfo originInfo = shipmentInfo.getShipmentHeader().getOrigin();
                    DestinationInfo destinationInfo = shipmentInfo.getShipmentHeader().getDestination();
                    LocationItem origin = originInfo.getAddressFrom();

                    UUID originLocId = lookupSvcLocationIdByAddress(origin);
                    log.info("In SvcDetailServiceImpl processShipmentMessage - originLocId = {}", originLocId);

                    LocationItem destination = destinationInfo.getAddressTo();
                    UUID destLocId = lookupSvcLocationIdByAddress(destination);
                    log.info("In SvcDetailServiceImpl processShipmentMessage - destLocId = {}", destLocId);

                    if (originLocId == null) {
                        ServiceLocation originEntity = ObjectMapperUtil.map(
                                messageProcessor.buildSourceLocation(originInfo), ServiceLocation.class);
                        originLocId = UUID.randomUUID();
                        originEntity.setSvcLocationId(originLocId);
                        originEntity.setLocationType(LOCATION_ORIGIN);
                        locationRepository.save(originEntity);
                    }
                    if (destLocId == null) {
                        ServiceLocation destEntity = ObjectMapperUtil.map(
                                messageProcessor.buildDestinationLocation(destinationInfo), ServiceLocation.class);
                        destLocId = UUID.randomUUID();
                        destEntity.setSvcLocationId(destLocId);
                        destEntity.setLocationType(LOCATION_DEST);
                        locationRepository.save(destEntity);
                    }
                    ServiceDetail serviceDetail = serviceDetailRepository.findServiceDetailByCorrelationId(shipCorrId);
                    if (serviceDetail == null) {
                        // Creating new serviceDetail if it already doesn't exist.
                        serviceDetail = messageProcessor.buildServiceDetails(shipmentMessage);
                    }
                    serviceDetail.setSrcLocationId(originLocId);
                    serviceDetail.setDstLocationId(destLocId);
                    serviceDetail.setSrcEntityCode(originInfo.getEntityCode());
                    serviceDetail.setDstEntityCode(destinationInfo.getEntityCode());

                    if (Objects.isNull(serviceDetail.getSvcDetailId()))
                        // Generate UUID for new record.
                        serviceDetail.setSvcDetailId(UUID.randomUUID());

                    serviceDetailRepository.save(serviceDetail);
                });
            });
        } catch (DataAccessException ex) {
            log.error("Unable to save ShipmentMessage due to DataAccessException.", ex);
            throw new ShipmentPersistenceException(ex.getMessage(), ex);
        }

        return ShipmentServiceResponse.builder().success(Boolean.TRUE).message("Successfully persisted").build();
    }

    public UUID lookupSvcLocationIdByAddress(LocationItem locationItem) {
        String hashKey = HashKeyGenerator.generateHashKey(locationItem);
        Optional<UUID> optLocationId = locationRepository.findServiceLocationIdByHashKey(hashKey);
        return optLocationId.orElse(null);
    }

    public ShipmentServiceResponse updateShipmentStatus(ShipmentStatusDto shipmentStatus) {
        Optional<ServiceDetail> optSvcDetail = Optional.ofNullable(
                serviceDetailRepository.findBySvcDetailId(shipmentStatus.getSvcDetailId()));
        if (optSvcDetail.isPresent()) {
            ServiceDetail serviceDetail = optSvcDetail.get();
            serviceDetail.setLstSvcStatus(shipmentStatus.getLstSvcStatus());
            serviceDetail.setLstSvcStatusBy(shipmentStatus.getLstSvcStatusBy());
            serviceDetail.setLstSvcStatusDate(shipmentStatus.getLstSvcStatusDate());
            serviceDetailRepository.save(serviceDetail);
        }
        return ShipmentServiceResponse.builder().success(Boolean.TRUE).message("Successfully processed").build();
    }

    public ShipmentServiceResponse updateShipmentCancellation(ShipmentCancellationDto cancellationDto) {
        Optional<ServiceDetail> optSvcDetail = Optional.ofNullable(
                serviceDetailRepository.findBySvcDetailId(cancellationDto.getSvcDetailId()));
        if (optSvcDetail.isPresent()) {
            ServiceDetail serviceDetail = optSvcDetail.get();
            serviceDetail.setCancellationId(cancellationDto.getCancellationId());
            serviceDetail.setCancellationDate(cancellationDto.getCancellationDate());
            serviceDetailRepository.save(serviceDetail);
        }
        return ShipmentServiceResponse.builder().success(Boolean.TRUE).message("Successfully processed").build();
    }

    public ShipmentServiceResponse updateBillingDetails(ShipmentBillingDto billingDto) {
        Optional<ServiceDetail> optSvcDetail = Optional.ofNullable(
                serviceDetailRepository.findBySvcDetailId(billingDto.getSvcDetailId()));
        if (optSvcDetail.isPresent()) {
            ServiceDetail serviceDetail = optSvcDetail.get();
            serviceDetail.setBillId(billingDto.getBillId());
            //serviceDetail.setBillDate(billingDto.getBillDate());
            serviceDetailRepository.save(serviceDetail);
        }
        return ShipmentServiceResponse.builder().success(Boolean.TRUE).message("Successfully processed").build();
    }

    public ShipmentQueryResponse searchShipmentDetails(ShipmentSearchDto searchDto) {
        List<ServiceDetail> serviceDetails = serviceDetailRepository.searchShipmentDetails(
                searchDto.getReqEntityCode(), searchDto.getCarrierCode(),
                searchDto.getTransitMode(), searchDto.getTrackingNo(), searchDto.getOrderId(),
                searchDto.getLpn(), searchDto.getTrailerNo());
        if (CollectionUtils.isEmpty(serviceDetails)) {
            return ShipmentQueryResponse.builder().message("No shipment details found!").build();
        } else {
            return ShipmentQueryResponse.builder()
                    .serviceDetails(ObjectMapperUtil.mapAll(serviceDetails, ServiceDetailDto.class))
                    .success(true).message("Shipment Details fetched successfully").build();
        }
    }
    
    @Override
   	public boolean getClientIdAfterTokenValidation(HttpHeaders headers, ShipmentMessage shipmentMessage) throws JSONException, UnsupportedEncodingException {
   		String token=headers.getFirst(HttpHeaders.AUTHORIZATION);
   		String[] parts = token.split("\\.");
   		String base64EncodedBody=parts[1];
           Base64 base64Url = new Base64(true);
           String payload = new String(base64Url.decode(base64EncodedBody));
           JSONObject jsonPayload=new JSONObject(payload);
   		String client_id=jsonPayload.getString("client_id");
   		return null != client_id ? true : false;
   	}
    
    @Override 
	public EntityProfile getCallEntityDcService(HttpHeaders headers, ShipmentMessage shipmentMessage)  {
		log.info("Inside SvcRequestRateShopServiceImpl executing getCallEntityService");
		String baererToken=headers.getFirst(HttpHeaders.AUTHORIZATION);
		String srcEntityCode=shipmentMessage.getMessageBody().getShipments().get(0).getShipmentHeader().getOrigin().getEntityCode();
		String dstEntityCode=shipmentMessage.getMessageBody().getShipments().get(0).getShipmentHeader().getDestination().getEntityCode();
		 HttpHeaders headers1 = new HttpHeaders();
	        headers1.setContentType(MediaType.APPLICATION_JSON);
		MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
		map.add("baererToken", baererToken); 
		map.add("srcEntityCode", srcEntityCode);
		map.add("dstEntityCode", dstEntityCode); 

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map,headers1);
		ResponseEntity<EntityProfile> response = restTemplate.postForEntity(entityUrl, request , EntityProfile.class);
		
		log.info(ShipmentConstants.SUCCESS_MESSAGE);
		log.info("Response from svcDetailServiceImpl - response: {}", response);
		if(response == null || response.getBody() == null)
			return null;
		return response.getBody();
		 
	}
}
