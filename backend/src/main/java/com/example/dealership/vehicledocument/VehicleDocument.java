package com.example.dealership.vehicledocument;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicle_document")
public class VehicleDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleDocumentType type;

    @Column(nullable = false, length = 50)
    private String number;

    @Column(nullable = false)
    private LocalDate issueDate;

    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleDocumentStatus status;

    protected VehicleDocument() {
    }

    public VehicleDocument(Long vehicleId, VehicleDocumentType type, String number, LocalDate issueDate,
            LocalDate expiryDate) {
        this.vehicleId = vehicleId;
        this.type = type;
        this.number = number;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.status = VehicleDocumentStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public VehicleDocumentType getType() {
        return type;
    }

    public String getNumber() {
        return number;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public VehicleDocumentStatus getStatus() {
        return status;
    }

    /** Sets the status; callers check {@link VehicleDocumentStatusTransitions} first. */
    public void changeStatus(VehicleDocumentStatus status) {
        this.status = status;
    }
}
