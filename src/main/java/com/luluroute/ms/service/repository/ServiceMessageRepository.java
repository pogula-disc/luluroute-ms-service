package com.luluroute.ms.service.repository;

import com.luluroute.ms.service.model.ServiceMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface ServiceMessageRepository extends JpaRepository<ServiceMessage, UUID> {

    ServiceMessage findByActiveAndShipmentCorrelationId(int active, String shipmentCorrelationId);

    @Query("select s from ServiceMessage s where s.active=1"
            + "and (:shipmentCorrelationId is null or s.shipmentCorrelationId = :shipmentCorrelationId)"
            + "and (:svcMessageDate is null or s.svcMessageDate = :svcMessageDate)"
    )
    List<ServiceMessage> searchServiceMessage(@Param("shipmentCorrelationId") UUID shipmentCorrelationId,
                                              @Param("svcMessageDate") Date svcMessageDate);

    @Modifying
    @Query(value = "update ServiceMessage set shipmentStatus= :shipmentStatus"
            + ", updatedDate= :updatedDate"
            + ", updatedBy= :updatedBy" +
            " where shipmentCorrelationId = :shipmentCorrelationId")
    @Transactional
    void updateServiceMessageStatus(@Param("shipmentStatus") String shipmentStatus,
                                    @Param("shipmentCorrelationId") String shipmentCorrelationId,
                                    @Param("updatedDate") Date updatedDate,
                                    @Param("updatedBy") String updatedBy);

    @Modifying
    @Query(value = """
            update ServiceMessage set
            message = :shipmentMessage
            , shipmentStatus = :shipmentStatus
            , originEntity = :originEntity
            , carrierCode = :carrierCode
            , updatedDate = :updatedDate
            , updatedBy = :updatedBy
            where shipmentCorrelationId = :shipmentCorrelationId
            """)
    @Transactional
    void updateServiceMessageResponse(@Param("shipmentMessage") String shipmentMessage,
                                      @Param("shipmentStatus") String shipmentStatus,
                                      @Param("originEntity") String originEntity,
                                      @Param("carrierCode") String carrierCode,
                                      @Param("shipmentCorrelationId") String shipmentCorrelationId,
                                      @Param("updatedDate") Date updatedDate,
                                      @Param("updatedBy") String updatedBy);
}
