package com.example.dealership.handover;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HandoverReadinessController {

    private final HandoverReadinessService readinessService;

    public HandoverReadinessController(HandoverReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    @GetMapping("/api/vehicles/{vehicleId}/handover-readiness")
    public HandoverReadiness getReadiness(@PathVariable Long vehicleId) {
        return readinessService.getReadiness(vehicleId);
    }
}
