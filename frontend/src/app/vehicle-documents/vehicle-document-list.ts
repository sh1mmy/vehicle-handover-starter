import { Component, inject, input, numberAttribute, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { loadErrorMessage, problemDetailMessage } from '../shared/problem-detail';
import {
  VEHICLE_DOCUMENT_STATUS_LABELS,
  VEHICLE_DOCUMENT_TYPE_LABELS,
  VehicleDocument,
  VehicleDocumentApi,
  VehicleDocumentStatus,
} from './vehicle-document-api';

@Component({
  selector: 'app-vehicle-document-list',
  imports: [RouterLink],
  templateUrl: './vehicle-document-list.html',
})
export class VehicleDocumentList {
  private readonly documentApi = inject(VehicleDocumentApi);

  readonly vehicleId = input.required({ transform: numberAttribute });

  protected readonly documents = this.documentApi.list(this.vehicleId);
  protected readonly typeLabels = VEHICLE_DOCUMENT_TYPE_LABELS;
  protected readonly statusLabels = VEHICLE_DOCUMENT_STATUS_LABELS;
  protected readonly statusError = signal<string | null>(null);

  protected loadError(error: unknown): string {
    return loadErrorMessage(error, 'Documents not found');
  }

  protected async changeStatus(
    document: VehicleDocument,
    status: VehicleDocumentStatus,
  ): Promise<void> {
    this.statusError.set(null);
    try {
      await firstValueFrom(this.documentApi.changeStatus(this.vehicleId(), document.id, status));
      this.documents.reload();
    } catch (error) {
      this.statusError.set(problemDetailMessage(error));
    }
  }
}
