package com.example.dealership.vehicledocument;

import java.util.List;

import com.example.dealership.error.ConflictException;
import com.example.dealership.error.NotFoundException;
import com.example.dealership.vehicle.VehicleRepository;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VehicleDocumentService {

    private final VehicleDocumentRepository documentRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleDocumentService(VehicleDocumentRepository documentRepository, VehicleRepository vehicleRepository) {
        this.documentRepository = documentRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public List<VehicleDocumentResponse> findAll(Long vehicleId) {
        requireVehicle(vehicleId);
        return documentRepository.findAllByVehicleId(vehicleId, Sort.by("id")).stream()
                .map(VehicleDocumentMapper::toResponse)
                .toList();
    }

    public VehicleDocumentResponse findById(Long vehicleId, Long id) {
        requireVehicle(vehicleId);
        return VehicleDocumentMapper.toResponse(getDocument(vehicleId, id));
    }

    @Transactional
    public VehicleDocumentResponse create(Long vehicleId, VehicleDocumentRequest request) {
        requireVehicle(vehicleId);
        VehicleDocument document = documentRepository.save(VehicleDocumentMapper.toEntity(vehicleId, request));
        return VehicleDocumentMapper.toResponse(document);
    }

    @Transactional
    public VehicleDocumentResponse changeStatus(Long vehicleId, Long id, VehicleDocumentStatusRequest request) {
        requireVehicle(vehicleId);
        VehicleDocument document = getDocument(vehicleId, id);
        if (!VehicleDocumentStatusTransitions.isAllowed(document.getStatus(), request.status())) {
            throw new ConflictException("Document with id " + id + " cannot change status from "
                    + document.getStatus() + " to " + request.status());
        }
        document.changeStatus(request.status());
        return VehicleDocumentMapper.toResponse(documentRepository.save(document));
    }

    private void requireVehicle(Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new NotFoundException("Vehicle with id " + vehicleId + " not found");
        }
    }

    private VehicleDocument getDocument(Long vehicleId, Long id) {
        return documentRepository.findByIdAndVehicleId(id, vehicleId)
                .orElseThrow(() -> new NotFoundException("Document with id " + id + " not found"));
    }
}
