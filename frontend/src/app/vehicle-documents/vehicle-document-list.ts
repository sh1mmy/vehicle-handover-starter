import { Component, inject, input, numberAttribute } from '@angular/core';
import { RouterLink } from '@angular/router';
import { loadErrorMessage } from '../shared/problem-detail';
import {
  VEHICLE_DOCUMENT_STATUS_LABELS,
  VEHICLE_DOCUMENT_TYPE_LABELS,
  VehicleDocumentApi,
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

  protected loadError(error: unknown): string {
    return loadErrorMessage(error, 'Documents not found');
  }
}
