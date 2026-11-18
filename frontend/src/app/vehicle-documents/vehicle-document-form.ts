import { Component, inject, input, numberAttribute, signal } from '@angular/core';
import { FormField, FormRoot, form, required } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { problemDetailToFormErrors } from '../shared/problem-detail';
import {
  VEHICLE_DOCUMENT_TYPES,
  VEHICLE_DOCUMENT_TYPE_LABELS,
  VehicleDocumentApi,
  VehicleDocumentRequest,
  VehicleDocumentType,
} from './vehicle-document-api';

interface VehicleDocumentFormModel {
  type: VehicleDocumentType | '';
  number: string;
  issueDate: string;
  expiryDate: string;
}

function emptyFormModel(): VehicleDocumentFormModel {
  return {
    type: '',
    number: '',
    issueDate: '',
    expiryDate: '',
  };
}

function toRequest(model: VehicleDocumentFormModel): VehicleDocumentRequest {
  return {
    type: model.type as VehicleDocumentType,
    number: model.number,
    issueDate: model.issueDate,
    expiryDate: model.expiryDate || null,
  };
}

@Component({
  selector: 'app-vehicle-document-form',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './vehicle-document-form.html',
})
export class VehicleDocumentForm {
  private readonly documentApi = inject(VehicleDocumentApi);
  private readonly router = inject(Router);

  readonly vehicleId = input.required({ transform: numberAttribute });

  protected readonly types = VEHICLE_DOCUMENT_TYPES;
  protected readonly typeLabels = VEHICLE_DOCUMENT_TYPE_LABELS;

  private readonly model = signal(emptyFormModel());

  protected readonly documentForm = form(
    this.model,
    (path) => {
      required(path.type, { message: 'Type is required' });
      required(path.number, { message: 'Number is required' });
      required(path.issueDate, { message: 'Issue date is required' });
    },
    {
      submission: {
        action: async (documentForm) => {
          const request = toRequest(documentForm().value());
          const vehicleId = this.vehicleId();
          try {
            const saved = await firstValueFrom(this.documentApi.create(vehicleId, request));
            await this.router.navigate(['/vehicles', vehicleId, 'documents', saved.id]);
            return undefined;
          } catch (error) {
            return problemDetailToFormErrors(error, documentForm);
          }
        },
      },
    },
  );
}
