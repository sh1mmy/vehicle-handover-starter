package com.example.dealership.vehicledocument;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/documents")
public class VehicleDocumentController {

    private final VehicleDocumentService documentService;

    public VehicleDocumentController(VehicleDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<VehicleDocumentResponse> findAll(@PathVariable Long vehicleId) {
        return documentService.findAll(vehicleId);
    }

    @GetMapping("/{id}")
    public VehicleDocumentResponse findById(@PathVariable Long vehicleId, @PathVariable Long id) {
        return documentService.findById(vehicleId, id);
    }

    @PostMapping
    public ResponseEntity<VehicleDocumentResponse> create(@PathVariable Long vehicleId,
            @Valid @RequestBody VehicleDocumentRequest request) {
        VehicleDocumentResponse response = documentService.create(vehicleId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
