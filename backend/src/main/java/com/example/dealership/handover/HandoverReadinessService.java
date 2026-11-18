package com.example.dealership.handover;

import java.time.Clock;
import java.time.LocalDate;

import com.example.dealership.error.NotFoundException;
import com.example.dealership.vehicle.Vehicle;
import com.example.dealership.vehicle.VehicleRepository;
import com.example.dealership.vehicledocument.VehicleDocumentRepository;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HandoverReadinessService {

    private final VehicleRepository vehicleRepository;
    private final VehicleDocumentRepository documentRepository;
    private final Clock clock;

    public HandoverReadinessService(VehicleRepository vehicleRepository, VehicleDocumentRepository documentRepository,
            Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.documentRepository = documentRepository;
        this.clock = clock;
    }

    public HandoverReadiness getReadiness(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle with id " + vehicleId + " not found"));
        return HandoverReadinessCheck.evaluate(
                vehicle.isRegisteredByDealer(),
                vehicle.getPlannedHandoverDate(),
                documentRepository.findAllByVehicleId(vehicleId, Sort.by("id")),
                LocalDate.now(clock));
    }
}
