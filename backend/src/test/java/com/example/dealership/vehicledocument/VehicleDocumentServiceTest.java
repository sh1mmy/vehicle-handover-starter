package com.example.dealership.vehicledocument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.dealership.error.ConflictException;
import com.example.dealership.error.NotFoundException;
import com.example.dealership.vehicle.VehicleRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class VehicleDocumentServiceTest {

    private static final String NUMBER = "OC/2026/0001";
    private static final String OTHER_NUMBER = "FV/2026/0002";

    @Mock
    private VehicleDocumentRepository documentRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleDocumentService documentService;

    @Test
    void findAllReturnsMappedVehicleDocuments() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.findAllByVehicleId(1L, Sort.by("id")))
                .thenReturn(List.of(document(NUMBER), document(OTHER_NUMBER)));

        List<VehicleDocumentResponse> responses = documentService.findAll(1L);

        assertThat(responses).extracting(VehicleDocumentResponse::number).containsExactly(NUMBER, OTHER_NUMBER);
    }

    @Test
    void findAllThrowsNotFoundWhenVehicleDoesNotExist() {
        when(vehicleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> documentService.findAll(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Vehicle with id 99 not found");
    }

    @Test
    void findByIdReturnsMappedVehicleDocument() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.findByIdAndVehicleId(5L, 1L)).thenReturn(Optional.of(document(NUMBER)));

        VehicleDocumentResponse response = documentService.findById(1L, 5L);

        assertThat(response.number()).isEqualTo(NUMBER);
    }

    @Test
    void findByIdThrowsNotFoundWhenVehicleDoesNotExist() {
        when(vehicleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> documentService.findById(99L, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Vehicle with id 99 not found");
    }

    @Test
    void findByIdThrowsNotFoundWhenVehicleDocumentBelongsToAnotherVehicle() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.findByIdAndVehicleId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.findById(1L, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Document with id 5 not found");
    }

    @Test
    void createSavesVehicleDocumentWithPendingStatus() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.save(any(VehicleDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleDocumentResponse response = documentService.create(1L, request(NUMBER));

        assertThat(response.vehicleId()).isEqualTo(1L);
        assertThat(response.type()).isEqualTo(VehicleDocumentType.OC_POLICY);
        assertThat(response.number()).isEqualTo(NUMBER);
        assertThat(response.issueDate()).isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(response.expiryDate()).isEqualTo(LocalDate.of(2027, 11, 17));
        assertThat(response.status()).isEqualTo(VehicleDocumentStatus.PENDING);
    }

    @Test
    void createThrowsConflictWhenActiveDocumentOfTypeExists() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.existsByVehicleIdAndTypeAndStatusNot(1L, VehicleDocumentType.OC_POLICY,
                VehicleDocumentStatus.REJECTED)).thenReturn(true);

        assertThatThrownBy(() -> documentService.create(1L, request(NUMBER)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Vehicle with id 1 already has an active document of type OC_POLICY");
        verify(documentRepository, never()).save(any());
    }

    @Test
    void createSavesDocumentWhenOnlyRejectedDocumentsOfTypeExist() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.existsByVehicleIdAndTypeAndStatusNot(1L, VehicleDocumentType.OC_POLICY,
                VehicleDocumentStatus.REJECTED)).thenReturn(false);
        when(documentRepository.save(any(VehicleDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleDocumentResponse response = documentService.create(1L, request(NUMBER));

        assertThat(response.status()).isEqualTo(VehicleDocumentStatus.PENDING);
        verify(documentRepository).save(any(VehicleDocument.class));
    }

    @Test
    void createThrowsNotFoundWhenVehicleDoesNotExist() {
        when(vehicleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> documentService.create(99L, request(NUMBER)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Vehicle with id 99 not found");
        verify(documentRepository, never()).save(any());
    }

    @Test
    void changeStatusVerifiesPendingDocument() {
        VehicleDocument document = document(NUMBER);
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.findByIdAndVehicleId(5L, 1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(document)).thenReturn(document);

        VehicleDocumentResponse response = documentService.changeStatus(1L, 5L,
                new VehicleDocumentStatusRequest(VehicleDocumentStatus.VERIFIED));

        assertThat(response.status()).isEqualTo(VehicleDocumentStatus.VERIFIED);
        assertThat(document.getStatus()).isEqualTo(VehicleDocumentStatus.VERIFIED);
        verify(documentRepository).save(document);
    }

    @Test
    void changeStatusThrowsConflictWhenTransitionIsNotAllowed() {
        VehicleDocument document = document(NUMBER);
        document.changeStatus(VehicleDocumentStatus.REJECTED);
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.findByIdAndVehicleId(5L, 1L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.changeStatus(1L, 5L,
                new VehicleDocumentStatusRequest(VehicleDocumentStatus.VERIFIED)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Document with id 5 cannot change status from REJECTED to VERIFIED");
        assertThat(document.getStatus()).isEqualTo(VehicleDocumentStatus.REJECTED);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void changeStatusThrowsNotFoundWhenVehicleDoesNotExist() {
        when(vehicleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> documentService.changeStatus(99L, 5L,
                new VehicleDocumentStatusRequest(VehicleDocumentStatus.VERIFIED)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Vehicle with id 99 not found");
    }

    @Test
    void changeStatusThrowsNotFoundWhenVehicleDocumentBelongsToAnotherVehicle() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.findByIdAndVehicleId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.changeStatus(1L, 5L,
                new VehicleDocumentStatusRequest(VehicleDocumentStatus.VERIFIED)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Document with id 5 not found");
        verify(documentRepository, never()).save(any());
    }

    private static VehicleDocument document(String number) {
        return new VehicleDocument(1L, VehicleDocumentType.OC_POLICY, number, LocalDate.of(2026, 11, 18),
                LocalDate.of(2027, 11, 17));
    }

    private static VehicleDocumentRequest request(String number) {
        return new VehicleDocumentRequest(VehicleDocumentType.OC_POLICY, number, LocalDate.of(2026, 11, 18),
                LocalDate.of(2027, 11, 17));
    }
}
