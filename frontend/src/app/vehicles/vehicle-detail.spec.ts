import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { VehicleDetail } from './vehicle-detail';

describe('VehicleDetail', () => {
  let fixture: ComponentFixture<VehicleDetail>;
  let element: HTMLElement;
  let http: HttpTestingController;

  const vehicle = {
    id: 3,
    vin: 'TMBJJ7NE8L0000003',
    make: 'Skoda',
    model: 'Octavia',
    productionYear: 2026,
    fuelType: 'HYBRID',
    plannedHandoverDate: null,
  };

  // No whenStable() here: it would wait for the readiness request, which each test flushes itself.
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(VehicleDetail);
    element = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('id', '3');
    fixture.detectChanges();
    http.expectOne({ method: 'GET', url: '/api/vehicles/3' }).flush(vehicle);
  });

  afterEach(() => http.verify());

  async function flushReadiness(body: object, options?: { status: number; statusText: string }) {
    http
      .expectOne({ method: 'GET', url: '/api/vehicles/3/handover-readiness' })
      .flush(body, options);
    await fixture.whenStable();
  }

  function readinessText(): string {
    return element.querySelector('[data-readiness]')!.textContent!.replace(/\s+/g, ' ').trim();
  }

  function items(): string[] {
    return Array.from(element.querySelectorAll('[data-readiness] li'), (item) =>
      item.textContent!.trim(),
    );
  }

  it('shows that the vehicle is ready for handover', async () => {
    await flushReadiness({ ready: true, missingDocumentTypes: [], problems: [] });

    await vi.waitFor(() => expect(readinessText()).toContain('Ready for handover'));
    expect(items()).toEqual([]);
  });

  it('lists missing documents and problems when the vehicle is not ready', async () => {
    await flushReadiness({
      ready: false,
      missingDocumentTypes: ['COC', 'REGISTRATION_POA'],
      problems: [
        { documentType: 'INVOICE', reason: 'NOT_VERIFIED' },
        { documentType: 'OC_POLICY', reason: 'EXPIRED' },
        { documentType: null, reason: 'NO_HANDOVER_DATE' },
      ],
    });

    await vi.waitFor(() => expect(readinessText()).toContain('Not ready for handover'));
    expect(items()).toEqual([
      'COC',
      'Registration POA',
      'Invoice: Not verified',
      'OC policy: Expired',
      'No handover date',
    ]);
  });

  it('shows the ProblemDetail detail when readiness cannot be loaded', async () => {
    await flushReadiness(
      { title: 'Service Unavailable', status: 503, detail: 'Database is unavailable' },
      { status: 503, statusText: 'Service Unavailable' },
    );

    await vi.waitFor(() => expect(readinessText()).toContain('Database is unavailable'));
  });
});
