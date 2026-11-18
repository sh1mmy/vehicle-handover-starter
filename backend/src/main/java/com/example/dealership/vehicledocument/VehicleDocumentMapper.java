package com.example.dealership.vehicledocument;

public final class VehicleDocumentMapper {

    private VehicleDocumentMapper() {
    }

    public static VehicleDocument toEntity(Long vehicleId, VehicleDocumentRequest request) {
        return new VehicleDocument(
                vehicleId,
                request.type(),
                request.number(),
                request.issueDate(),
                request.expiryDate());
    }

    public static VehicleDocumentResponse toResponse(VehicleDocument document) {
        return new VehicleDocumentResponse(
                document.getId(),
                document.getVehicleId(),
                document.getType(),
                document.getNumber(),
                document.getIssueDate(),
                document.getExpiryDate(),
                document.getStatus());
    }
}
