package com.example.dealership.vehicledocument;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    List<VehicleDocument> findAllByVehicleId(Long vehicleId, Sort sort);

    Optional<VehicleDocument> findByIdAndVehicleId(Long id, Long vehicleId);

    boolean existsByVehicleIdAndTypeAndStatusNot(Long vehicleId, VehicleDocumentType type,
            VehicleDocumentStatus status);
}
