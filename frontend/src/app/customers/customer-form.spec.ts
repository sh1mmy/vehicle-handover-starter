import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { CustomerForm } from './customer-form';

const PHONE_FORMAT_MESSAGE =
  'must be 9 to 20 characters: digits separated by single spaces or hyphens, optionally starting with +';

describe('CustomerForm', () => {
  let fixture: ComponentFixture<CustomerForm>;
  let element: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(CustomerForm);
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

  function fillValidCustomer(): void {
    fill('text', 0, 'Jan');
    fill('text', 1, 'Kowalski');
    fill('text', 2, 'jan.kowalski@example.test');
    fill('text', 3, '+48 600 100 200');
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

    http.expectNone('/api/customers');
    expect(errors()).toEqual([
      'First name is required',
      'Last name is required',
      'Email is required',
      'Phone is required',
    ]);
  });

  it('leaves format and range rules to the backend', async () => {
    fillValidCustomer();
    fill('text', 2, 'not-an-email');
    fill('text', 3, '600--100-200');
    fill('text', 4, '123');
    await submit();

    const request = http.expectOne({ method: 'POST', url: '/api/customers' });
    expect(request.request.body.email).toBe('not-an-email');
    expect(request.request.body.phone).toBe('600--100-200');
    expect(request.request.body.nip).toBe('123');
    request.flush(
      {
        title: 'Bad Request',
        status: 400,
        detail: 'Validation failed',
        errors: [
          { field: 'email', message: 'must be a well-formed email address' },
          { field: 'nip', message: 'must be exactly 10 digits' },
          { field: 'phone', message: PHONE_FORMAT_MESSAGE },
        ],
      },
      { status: 400, statusText: 'Bad Request' },
    );

    await vi.waitFor(() =>
      expect(errors()).toEqual([
        'must be a well-formed email address',
        PHONE_FORMAT_MESSAGE,
        'must be exactly 10 digits',
      ]),
    );
  });

  it('creates the customer and navigates to its details', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fillValidCustomer();
    await submit();

    const request = http.expectOne({ method: 'POST', url: '/api/customers' });
    expect(request.request.body).toEqual({
      firstName: 'Jan',
      lastName: 'Kowalski',
      email: 'jan.kowalski@example.test',
      phone: '+48 600 100 200',
      nip: null,
    });
    request.flush({ id: 7, ...request.request.body });

    await vi.waitFor(() => expect(navigate).toHaveBeenCalledWith(['/customers', 7]));
  });

  it('loads the customer in edit mode and updates it', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const customer = {
      id: 3,
      firstName: 'Piotr',
      lastName: 'Wisniewski',
      email: 'biuro@wisniewski-transport.example.test',
      phone: '+48 22 123 45 67',
      nip: '5260001246',
    };
    fixture.componentRef.setInput('id', '3');
    fixture.detectChanges();
    http.expectOne({ method: 'GET', url: '/api/customers/3' }).flush(customer);
    await fixture.whenStable();

    fill('text', 1, 'Nowicki');
    await submit();

    const request = http.expectOne({ method: 'PUT', url: '/api/customers/3' });
    expect(request.request.body).toEqual({
      firstName: 'Piotr',
      lastName: 'Nowicki',
      email: 'biuro@wisniewski-transport.example.test',
      phone: '+48 22 123 45 67',
      nip: '5260001246',
    });
    request.flush({ ...customer, lastName: 'Nowicki' });

    await vi.waitFor(() => expect(navigate).toHaveBeenCalledWith(['/customers', 3]));
  });

  it('shows a dedicated message when the customer to edit does not exist', async () => {
    fixture.componentRef.setInput('id', '99');
    fixture.detectChanges();
    http
      .expectOne({ method: 'GET', url: '/api/customers/99' })
      .flush(
        { title: 'Not Found', status: 404, detail: 'Customer with id 99 not found' },
        { status: 404, statusText: 'Not Found' },
      );

    await vi.waitFor(() => expect(errors()).toEqual(['Customer not found']));
  });

  it('shows the ProblemDetail detail when loading the customer fails', async () => {
    fixture.componentRef.setInput('id', '3');
    fixture.detectChanges();
    http
      .expectOne({ method: 'GET', url: '/api/customers/3' })
      .flush(
        { title: 'Service Unavailable', status: 503, detail: 'Database is unavailable' },
        { status: 503, statusText: 'Service Unavailable' },
      );

    await vi.waitFor(() => expect(errors()).toEqual(['Database is unavailable']));
  });

  it('shows object-level errors returned by the backend above the form', async () => {
    fillValidCustomer();
    await submit();

    http.expectOne({ method: 'POST', url: '/api/customers' }).flush(
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
    fillValidCustomer();
    await submit();

    http.expectOne({ method: 'POST', url: '/api/customers' }).flush(
      {
        title: 'Bad Request',
        status: 400,
        detail: 'Validation failed',
        errors: [{ field: 'phone', message: PHONE_FORMAT_MESSAGE }],
      },
      { status: 400, statusText: 'Bad Request' },
    );

    await vi.waitFor(() => expect(errors()).toEqual([PHONE_FORMAT_MESSAGE]));
  });

  it('shows the ProblemDetail detail when the backend reports a conflict', async () => {
    fillValidCustomer();
    await submit();

    http.expectOne({ method: 'POST', url: '/api/customers' }).flush(
      {
        title: 'Conflict',
        status: 409,
        detail: 'Customer with email jan.kowalski@example.test already exists',
      },
      { status: 409, statusText: 'Conflict' },
    );

    await vi.waitFor(() =>
      expect(errors()).toEqual(['Customer with email jan.kowalski@example.test already exists']),
    );
  });
});
