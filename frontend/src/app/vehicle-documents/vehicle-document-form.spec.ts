import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { VehicleDocumentForm } from './vehicle-document-form';

describe('VehicleDocumentForm', () => {
  let fixture: ComponentFixture<VehicleDocumentForm>;
  let element: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(VehicleDocumentForm);
    fixture.componentRef.setInput('vehicleId', '1');
    element = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function fill(type: string, index: number, value: string): void {
    const input = element.querySelectorAll<HTMLInputElement>(`input[type="${type}"]`)[index];
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function select(value: string): void {
    const select = element.querySelector('select')!;
    select.value = value;
    select.dispatchEvent(new Event('input'));
  }

  function fillValidVehicleDocument(): void {
    select('OC_POLICY');
    fill('text', 0, 'OC/2026/0001');
    fill('date', 0, '2026-11-18');
  }

  async function submit(): Promise<void> {
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  function errors(): string[] {
    return Array.from(element.querySelectorAll('.error'), (error) => error.textContent!.trim());
  }

  it('shows required errors and sends nothing when the form is empty', async () => {
    await submit();

    http.expectNone('/api/vehicles/1/documents');
    expect(errors()).toEqual(['Type is required', 'Number is required', 'Issue date is required']);
  });

  it('leaves format and range rules to the backend', async () => {
    fillValidVehicleDocument();
    fill('date', 0, '2099-01-01');
    await submit();

    const request = http.expectOne({ method: 'POST', url: '/api/vehicles/1/documents' });
    expect(request.request.body.issueDate).toBe('2099-01-01');
    request.flush(
      {
        title: 'Bad Request',
        status: 400,
        detail: 'Validation failed',
        errors: [{ field: 'issueDate', message: 'must be a date in the past or in the present' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );

    await vi.waitFor(() =>
      expect(errors()).toEqual(['must be a date in the past or in the present']),
    );
  });

  it('creates the document and navigates to its details', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fillValidVehicleDocument();
    await submit();

    const request = http.expectOne({ method: 'POST', url: '/api/vehicles/1/documents' });
    expect(request.request.body).toEqual({
      type: 'OC_POLICY',
      number: 'OC/2026/0001',
      issueDate: '2026-11-18',
      expiryDate: null,
    });
    request.flush({ id: 7, vehicleId: 1, status: 'PENDING', ...request.request.body });

    await vi.waitFor(() => expect(navigate).toHaveBeenCalledWith(['/vehicles', 1, 'documents', 7]));
  });

  it('sends the expiry date when it is filled in', async () => {
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fillValidVehicleDocument();
    fill('date', 1, '2027-11-17');
    await submit();

    const request = http.expectOne({ method: 'POST', url: '/api/vehicles/1/documents' });
    expect(request.request.body.expiryDate).toBe('2027-11-17');
    request.flush({ id: 8, vehicleId: 1, status: 'PENDING', ...request.request.body });
  });

  it('shows the ProblemDetail detail when the vehicle does not exist', async () => {
    fillValidVehicleDocument();
    await submit();

    http
      .expectOne({ method: 'POST', url: '/api/vehicles/1/documents' })
      .flush(
        { title: 'Not Found', status: 404, detail: 'Vehicle with id 1 not found' },
        { status: 404, statusText: 'Not Found' },
      );

    await vi.waitFor(() => expect(errors()).toEqual(['Vehicle with id 1 not found']));
  });

  it('shows object-level errors returned by the backend above the form', async () => {
    fillValidVehicleDocument();
    await submit();

    http.expectOne({ method: 'POST', url: '/api/vehicles/1/documents' }).flush(
      {
        title: 'Bad Request',
        status: 400,
        detail: 'Validation failed',
        errors: [{ field: null, message: 'request body must be valid JSON' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );

    await vi.waitFor(() => expect(errors()).toEqual(['request body must be valid JSON']));
  });

  it('shows field errors returned by the backend as ProblemDetail', async () => {
    fillValidVehicleDocument();
    await submit();

    http.expectOne({ method: 'POST', url: '/api/vehicles/1/documents' }).flush(
      {
        title: 'Bad Request',
        status: 400,
        detail: 'Validation failed',
        errors: [{ field: 'number', message: 'size must be between 0 and 50' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );

    await vi.waitFor(() => expect(errors()).toEqual(['size must be between 0 and 50']));
  });
});
