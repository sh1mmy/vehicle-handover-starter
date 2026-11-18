import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { VehicleDocumentList } from './vehicle-document-list';

describe('VehicleDocumentList', () => {
  let fixture: ComponentFixture<VehicleDocumentList>;
  let element: HTMLElement;
  let http: HttpTestingController;

  const pending = {
    id: 5,
    vehicleId: 1,
    type: 'INVOICE',
    number: 'FV/1',
    issueDate: '2026-11-01',
    expiryDate: null,
    status: 'PENDING',
  };
  const verified = { ...pending, id: 6, type: 'COC', number: 'COC/1', status: 'VERIFIED' };

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(VehicleDocumentList);
    element = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('vehicleId', '1');
    fixture.detectChanges();
    http.expectOne({ method: 'GET', url: '/api/vehicles/1/documents' }).flush([pending, verified]);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function rowButtons(rowIndex: number): string[] {
    const row = element.querySelectorAll('tbody tr')[rowIndex];
    return Array.from(row.querySelectorAll('button'), (button) => button.textContent!.trim());
  }

  function click(rowIndex: number, label: string): void {
    const row = element.querySelectorAll('tbody tr')[rowIndex];
    Array.from(row.querySelectorAll('button'))
      .find((button) => button.textContent!.trim() === label)!
      .click();
  }

  function errors(): string[] {
    return Array.from(element.querySelectorAll('.error'), (error) => error.textContent!.trim());
  }

  it('offers Verify and Reject only for pending documents', () => {
    expect(rowButtons(0)).toEqual(['Verify', 'Reject']);
    expect(rowButtons(1)).toEqual([]);
  });

  it('verifies a pending document and reloads the list', async () => {
    click(0, 'Verify');

    const request = http.expectOne({ method: 'PUT', url: '/api/vehicles/1/documents/5/status' });
    expect(request.request.body).toEqual({ status: 'VERIFIED' });
    request.flush({ ...pending, status: 'VERIFIED' });

    await vi.waitFor(() =>
      http
        .expectOne({ method: 'GET', url: '/api/vehicles/1/documents' })
        .flush([{ ...pending, status: 'VERIFIED' }, verified]),
    );
    await fixture.whenStable();
    expect(rowButtons(0)).toEqual([]);
  });

  it('rejects a pending document', async () => {
    click(0, 'Reject');

    const request = http.expectOne({ method: 'PUT', url: '/api/vehicles/1/documents/5/status' });
    expect(request.request.body).toEqual({ status: 'REJECTED' });
    request.flush({ ...pending, status: 'REJECTED' });

    await vi.waitFor(() =>
      http
        .expectOne({ method: 'GET', url: '/api/vehicles/1/documents' })
        .flush([{ ...pending, status: 'REJECTED' }, verified]),
    );
  });

  it('shows the ProblemDetail detail when the status change conflicts', async () => {
    click(0, 'Verify');

    http.expectOne({ method: 'PUT', url: '/api/vehicles/1/documents/5/status' }).flush(
      {
        title: 'Conflict',
        status: 409,
        detail: 'Document with id 5 cannot change status from REJECTED to VERIFIED',
      },
      { status: 409, statusText: 'Conflict' },
    );

    await vi.waitFor(() =>
      expect(errors()).toEqual([
        'Document with id 5 cannot change status from REJECTED to VERIFIED',
      ]),
    );
  });
});
