package com.luluroute.ms.service.repository;

import com.luluroute.ms.service.model.ServiceCancel;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.UUID;

public interface ServiceCancelRepository extends JpaRepository<ServiceCancel, UUID> {

    @Transactional
    @Modifying
    @Query(value = "update transit.svccancel set response_code = ?1, response_message = ?2, response_date = ?3, " +
            "updated_date = ?4, updated_by = ?5 where svc_cancel_id = ?6",
            nativeQuery = true)
    void updateCancelResponse(String responseCode, String responseMessage, Date responseDate,
                              Date updatedDate, String updatedBy, UUID svcCancelId);

}
