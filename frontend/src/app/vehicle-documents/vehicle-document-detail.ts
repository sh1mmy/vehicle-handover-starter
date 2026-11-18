import { Component, inject, input, numberAttribute } from '@angular/core';
import { RouterLink } from '@angular/router';
import { loadErrorMessage } from '../shared/problem-detail';
import {
  VEHICLE_DOCUMENT_STATUS_LABELS,
  VEHICLE_DOCUMENT_TYPE_LABELS,
  VehicleDocumentApi,
} from './vehicle-document-api';

@Component({
  selector: 'app-vehicle-document-detail',
  imports: [RouterLink],
  templateUrl: './vehicle-document-detail.html',
})
export class VehicleDocumentDetail {
  private readonly documentApi = inject(VehicleDocumentApi);

  readonly vehicleId = input.required({ transform: numberAttribute });
  readonly id = input.required({ transform: numberAttribute });

  protected readonly document = this.documentApi.get(this.vehicleId, this.id);
  protected readonly typeLabels = VEHICLE_DOCUMENT_TYPE_LABELS;
  protected readonly statusLabels = VEHICLE_DOCUMENT_STATUS_LABELS;

  protected loadError(error: unknown): string {
    return loadErrorMessage(error, 'Document not found');
  }
}
