import { Component, inject, input, numberAttribute } from '@angular/core';
import { RouterLink } from '@angular/router';
import { loadErrorMessage, problemDetailMessage } from '../shared/problem-detail';
import { VEHICLE_DOCUMENT_TYPE_LABELS } from '../vehicle-documents/vehicle-document-api';
import {
  FUEL_TYPE_LABELS,
  HANDOVER_PROBLEM_REASON_LABELS,
  HandoverProblem,
  VehicleApi,
} from './vehicle-api';

@Component({
  selector: 'app-vehicle-detail',
  imports: [RouterLink],
  templateUrl: './vehicle-detail.html',
})
export class VehicleDetail {
  private readonly vehicleApi = inject(VehicleApi);

  readonly id = input.required({ transform: numberAttribute });

  protected readonly vehicle = this.vehicleApi.get(this.id);
  protected readonly readiness = this.vehicleApi.readiness(this.id);
  protected readonly fuelTypeLabels = FUEL_TYPE_LABELS;
  protected readonly documentTypeLabels = VEHICLE_DOCUMENT_TYPE_LABELS;

  protected loadError(error: unknown): string {
    return loadErrorMessage(error, 'Vehicle not found');
  }

  protected readinessError(error: unknown): string {
    return problemDetailMessage(error);
  }

  protected problemLabel(problem: HandoverProblem): string {
    const reason = HANDOVER_PROBLEM_REASON_LABELS[problem.reason];
    return problem.documentType === null
      ? reason
      : `${this.documentTypeLabels[problem.documentType]}: ${reason}`;
  }
}
