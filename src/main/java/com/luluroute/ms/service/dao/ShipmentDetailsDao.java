package com.luluroute.ms.service.dao;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.service.dto.ShipmentServiceResponse;
import com.luluroute.ms.service.model.ServiceDetail;
import com.luluroute.ms.service.repository.ServiceDetailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

@Repository
@Slf4j
public class ShipmentDetailsDao {

    @PersistenceContext
    private EntityManager entityManager;

    private final ServiceDetailRepository serviceDetailRepository;

    @Autowired
    public ShipmentDetailsDao(ServiceDetailRepository serviceRepository) {
        this.serviceDetailRepository = serviceRepository;
    }

    public ShipmentServiceResponse processShipmentMessage(ShipmentMessage shipmentMessage) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ServiceDetail> query = cb.createQuery(ServiceDetail.class);
        Root<ServiceDetail> serviceDetail = query.from(ServiceDetail.class);
        Path<String> entCdPath = serviceDetail.get("reqEntityCode");

        return ShipmentServiceResponse.builder().success(Boolean.TRUE).message("Successfully processed").build();
    }
}
