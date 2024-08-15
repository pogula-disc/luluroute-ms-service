package com.luluroute.ms.service.repository;

import com.luluroute.ms.service.model.ServiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ServiceDetailRepository extends JpaRepository<ServiceDetail, UUID> {

    List<ServiceDetail> findAll();

    ServiceDetail findBySvcDetailId(UUID serviceId);

    @Query("select s from ServiceDetail s where s.active=1 and (:reqEntityCode is null or s.reqEntityCode = :reqEntityCode)"
            + "and (:carrierCode is null or s.carrierCode = :carrierCode)"
            + "and (:transitMode is null or s.transitMode = :transitMode)"
            + "and (:trackingNo is null or s.trackingNo = :trackingNo)"
            + "and (:orderId is null or s.orderId = :orderId)"
            + "and (:lpn is null or s.lpn = :lpn)"
            + "and (:trailerNo is null or s.trailerNo = :trailerNo)"
    )
    List<ServiceDetail> searchShipmentDetails(@Param("reqEntityCode") String reqEntityCode,
        @Param("carrierCode") String carrierCode, @Param("transitMode") String transitMode,
        @Param("trackingNo") String trackingNo, @Param("orderId") String orderId,
        @Param("lpn") String lpn, @Param("trailerNo") String trailerNo);

    ServiceDetail findServiceDetailByCorrelationId(String correlationId);
}
