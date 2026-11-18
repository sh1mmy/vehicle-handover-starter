import { HttpClient, httpResource } from '@angular/common/http';
import { Injectable, Signal, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { VehicleDocumentType } from '../vehicle-documents/vehicle-document-api';

export type FuelType = 'PETROL' | 'DIESEL' | 'HYBRID' | 'ELECTRIC';

export const FUEL_TYPE_LABELS: Record<FuelType, string> = {
  PETROL: 'Petrol',
  DIESEL: 'Diesel',
  HYBRID: 'Hybrid',
  ELECTRIC: 'Electric',
};

export const FUEL_TYPES = Object.keys(FUEL_TYPE_LABELS) as FuelType[];

export interface Vehicle {
  id: number;
  vin: string;
  make: string;
  model: string;
  productionYear: number;
  fuelType: FuelType;
  plannedHandoverDate: string | null;
  registeredByDealer: boolean;
}

export type VehicleRequest = Omit<Vehicle, 'id'>;

export type HandoverProblemReason = 'NOT_VERIFIED' | 'EXPIRED' | 'NO_HANDOVER_DATE';

export const HANDOVER_PROBLEM_REASON_LABELS: Record<HandoverProblemReason, string> = {
  NOT_VERIFIED: 'Not verified',
  EXPIRED: 'Expired',
  NO_HANDOVER_DATE: 'No handover date',
};

export interface HandoverProblem {
  documentType: VehicleDocumentType | null;
  reason: HandoverProblemReason;
}

export interface HandoverReadiness {
  ready: boolean;
  missingDocumentTypes: VehicleDocumentType[];
  problems: HandoverProblem[];
}

@Injectable({ providedIn: 'root' })
export class VehicleApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/vehicles';

  list() {
    return httpResource<Vehicle[]>(() => this.url, { defaultValue: [] });
  }

  get(id: Signal<number | undefined>) {
    return httpResource<Vehicle>(() => {
      const vehicleId = id();
      return vehicleId === undefined ? undefined : `${this.url}/${vehicleId}`;
    });
  }

  readiness(id: Signal<number>) {
    return httpResource<HandoverReadiness>(() => `${this.url}/${id()}/handover-readiness`);
  }

  create(request: VehicleRequest): Observable<Vehicle> {
    return this.http.post<Vehicle>(this.url, request);
  }

  update(id: number, request: VehicleRequest): Observable<Vehicle> {
    return this.http.put<Vehicle>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
