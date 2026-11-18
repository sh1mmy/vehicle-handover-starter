import { HttpClient, httpResource } from '@angular/common/http';
import { Injectable, Signal, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type VehicleDocumentType =
  'SALES_AGREEMENT' | 'INVOICE' | 'COC' | 'OC_POLICY' | 'REGISTRATION_POA';

export const VEHICLE_DOCUMENT_TYPE_LABELS: Record<VehicleDocumentType, string> = {
  SALES_AGREEMENT: 'Sales agreement',
  INVOICE: 'Invoice',
  COC: 'COC',
  OC_POLICY: 'OC policy',
  REGISTRATION_POA: 'Registration POA',
};

export const VEHICLE_DOCUMENT_TYPES = Object.keys(
  VEHICLE_DOCUMENT_TYPE_LABELS,
) as VehicleDocumentType[];

export type VehicleDocumentStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export const VEHICLE_DOCUMENT_STATUS_LABELS: Record<VehicleDocumentStatus, string> = {
  PENDING: 'Pending',
  VERIFIED: 'Verified',
  REJECTED: 'Rejected',
};

export interface VehicleDocument {
  id: number;
  vehicleId: number;
  type: VehicleDocumentType;
  number: string;
  issueDate: string;
  expiryDate: string | null;
  status: VehicleDocumentStatus;
}

export type VehicleDocumentRequest = Omit<VehicleDocument, 'id' | 'vehicleId' | 'status'>;

@Injectable({ providedIn: 'root' })
export class VehicleDocumentApi {
  private readonly http = inject(HttpClient);

  private url(vehicleId: number): string {
    return `/api/vehicles/${vehicleId}/documents`;
  }

  list(vehicleId: Signal<number>) {
    return httpResource<VehicleDocument[]>(() => this.url(vehicleId()), { defaultValue: [] });
  }

  get(vehicleId: Signal<number>, id: Signal<number>) {
    return httpResource<VehicleDocument>(() => `${this.url(vehicleId())}/${id()}`);
  }

  create(vehicleId: number, request: VehicleDocumentRequest): Observable<VehicleDocument> {
    return this.http.post<VehicleDocument>(this.url(vehicleId), request);
  }

  changeStatus(
    vehicleId: number,
    id: number,
    status: VehicleDocumentStatus,
  ): Observable<VehicleDocument> {
    return this.http.put<VehicleDocument>(`${this.url(vehicleId)}/${id}/status`, { status });
  }
}
