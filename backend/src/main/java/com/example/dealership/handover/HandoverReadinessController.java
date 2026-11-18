package com.example.dealership.handover;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}")
public class HandoverReadinessController {

    private final HandoverReadinessService readinessService;

    public HandoverReadinessController(HandoverReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    @GetMapping("/handover-readiness")
    public HandoverReadiness getReadiness(@PathVariable Long vehicleId) {
        return readinessService.getReadiness(vehicleId);
    }
}
