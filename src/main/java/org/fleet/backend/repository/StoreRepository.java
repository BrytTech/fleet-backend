package org.fleet.backend.repository;

import org.fleet.backend.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByIsActiveTrue();

    @Query("SELECT s FROM Store s WHERE s.isActive = true AND " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * " +
            "cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(s.latitude)))) < :radius")
    List<Store> findNearbyStores(@Param("lat") Double lat,
                                 @Param("lng") Double lng,
                                 @Param("radius") Double radiusKm);


}
