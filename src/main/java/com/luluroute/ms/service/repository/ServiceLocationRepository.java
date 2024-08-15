package com.luluroute.ms.service.repository;

import com.luluroute.ms.service.model.ServiceLocation;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ServiceLocationRepository extends JpaRepository<ServiceLocation, UUID> {

    @NotNull List<ServiceLocation> findAll();

    ServiceLocation findServiceLocationBySvcLocationId(UUID svcLocationId);

    @Query("SELECT s.svcLocationId FROM ServiceLocation s where s.hashKey = :hashKey")
    Optional<UUID> findServiceLocationIdByHashKey(@Param("hashKey") String hashKey);
}
