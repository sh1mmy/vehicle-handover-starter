# Frontend skeletons

## Placeholders
| Placeholder | Meaning | Example |
|---|---|---|
| `<Entity>` / `<entity>` | class name / camelCase variable | `Supplier` / `supplier` |
| `<entities>` | camelCase plural (variables) | `suppliers` |
| `<folder>` | kebab-case plural (folder, route path, URL) | `suppliers` |
| `<file>` | kebab-case singular file prefix | `supplier` |
| `<Label>` / `<label>` | UI label singular / plural, sentence case | `Supplier` / `suppliers` |

Use the same example fields as [backend.md](backend.md): `name`, `email`, optional `rating`, enum `status`.

## `<file>-api.ts`
```ts
import { HttpClient, httpResource } from '@angular/common/http';
import { Injectable, Signal, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type SupplierStatus = 'ACTIVE' | 'SUSPENDED';

export const SUPPLIER_STATUS_LABELS: Record<SupplierStatus, string> = {
  ACTIVE: 'Active',
  SUSPENDED: 'Suspended',
};

export const SUPPLIER_STATUSES = Object.keys(SUPPLIER_STATUS_LABELS) as SupplierStatus[];

export interface <Entity> {
  id: number;
  name: string;
  email: string;
  rating: number | null;
  status: SupplierStatus;
}

export type <Entity>Request = Omit<<Entity>, 'id'>;

@Injectable({ providedIn: 'root' })
export class <Entity>Api {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/<folder>';

  list() {
    return httpResource<<Entity>[]>(() => this.url, { defaultValue: [] });
  }

  get(id: Signal<number | undefined>) {
    return httpResource<<Entity>>(() => {
      const <entity>Id = id();
      return <entity>Id === undefined ? undefined : `${this.url}/${<entity>Id}`;
    });
  }

  create(request: <Entity>Request): Observable<<Entity>> {
    return this.http.post<<Entity>>(this.url, request);
  }

  update(id: number, request: <Entity>Request): Observable<<Entity>> {
    return this.http.put<<Entity>>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
```

## `<file>-list.ts` / `.html`
Copy `vehicle-list.ts` and `vehicle-list.html`, renaming `vehicle` -> `<entity>`. Keep:
- `selector: 'app-<file>-list'`, `imports: [RouterLink]`, `templateUrl`;
- `deleteError` signal, `loadError(error)` with `loadErrorMessage(error, '<Label>s not found')`;
- `delete()` with `firstValueFrom` then `this.<entities>.reload()`, catching into `problemDetailMessage`;
- the template:
  - `<h1><Label>s</h1>`, `<p><a routerLink="/<folder>/new">Add <label></a></p>`;
  - `isLoading` / `error` / `value` branches;
  - a table with one column per field plus `Actions` (Edit link + Delete button);
  - `@empty` row `No <label>s yet.` with `colspan` = number of columns.

Cells:
- an optional value: `{{ x ?? '—' }}`;
- an enum: `{{ statusLabels[x.status] }}`;
- a boolean: `{{ x.flag ? 'Yes' : 'No' }}`.

## `<file>-detail.ts` / `.html`
Copy `vehicle-detail.*`:
- `readonly id = input.required({ transform: numberAttribute });`;
- `loadErrorMessage(error, '<Label> not found')`;
- the template: back link, `<h1>`, `<dl>` with `<dt>`/`<dd>` per field, Edit link.

Do not copy the empty `Documents` section.

## `<file>-form.ts`
```ts
interface <Entity>FormModel {
  name: string;
  email: string;
  rating: number | null;
  status: SupplierStatus | '';
}

function toFormModel(<entity>: <Entity> | undefined): <Entity>FormModel {
  return {
    name: <entity>?.name ?? '',
    email: <entity>?.email ?? '',
    rating: <entity>?.rating ?? null,
    status: <entity>?.status ?? '',
  };
}

function toRequest(model: <Entity>FormModel): <Entity>Request {
  return {
    name: model.name,
    email: model.email,
    rating: model.rating,
    status: model.status as SupplierStatus,
  };
}

@Component({
  selector: 'app-<file>-form',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './<file>-form.html',
})
export class <Entity>Form {
  private readonly <entity>Api = inject(<Entity>Api);
  private readonly router = inject(Router);

  readonly id = input(undefined, { transform: optionalNumberAttribute });
  protected readonly isEdit = computed(() => this.id() !== undefined);
  protected readonly <entity> = this.<entity>Api.get(this.id);
  protected readonly statuses = SUPPLIER_STATUSES;
  protected readonly statusLabels = SUPPLIER_STATUS_LABELS;

  private readonly model = linkedSignal(() =>
    toFormModel(this.<entity>.hasValue() ? this.<entity>.value() : undefined),
  );

  protected readonly <entity>Form = form(
    this.model,
    (path) => {
      required(path.name, { message: 'Name is required' });
      required(path.email, { message: 'Email is required' });
      required(path.status, { message: 'Status is required' });
    },
    {
      submission: {
        action: async (<entity>Form) => {
          const request = toRequest(<entity>Form().value());
          const id = this.id();
          try {
            const saved = await firstValueFrom(
              id === undefined
                ? this.<entity>Api.create(request)
                : this.<entity>Api.update(id, request),
            );
            await this.router.navigate(['/<folder>', saved.id]);
            return undefined;
          } catch (error) {
            return problemDetailToFormErrors(error, <entity>Form);
          }
        },
      },
    },
  );

  protected loadError(error: unknown): string {
    return loadErrorMessage(error, '<Label> not found');
  }
}
```

Form model per type (from SKILL.md step 3):

| Type | Model field | `toFormModel` | `toRequest` |
|---|---|---|---|
| text | `string` | `x?.f ?? ''` | `model.f` |
| optional text | `string` | `x?.f ?? ''` | `model.f \|\| null` |
| number | `number \| null` | `x?.f ?? null` | required: `model.f!`; optional: `model.f` |
| boolean | `boolean` | `x?.f ?? false` | `model.f` |
| date | `string` | `x?.f ?? ''` | required: `model.f`; optional: `model.f \|\| null` |
| enum | `<Enum> \| ''` | `x?.f ?? ''` | required: `model.f as <Enum>`; optional: `model.f \|\| null` |

`required()` goes on required non-boolean fields only, with the message `'<Field label> is required'`.

## `<file>-form.html`
Copy `vehicle-form.html`:
- the heading `{{ isEdit() ? 'Edit <label>' : 'New <label>' }}`;
- the loading and error branches;
- form-level errors first, then one block per field;
- Save and Cancel at the end.

Field block:
```html
<label>
  Name
  <input type="text" [formField]="<entity>Form.name" />
</label>
@if (<entity>Form.name().touched()) {
  @for (error of <entity>Form.name().errors(); track error) {
    <p class="error" role="alert">{{ error.message }}</p>
  }
}
```

Controls by type:
- number: `<input type="number" />`;
- decimal: `<input type="number" step="0.01" />`;
- date: `<input type="date" />`;
- boolean: `<input type="checkbox" [formField]="..." />`;
- enum:
  ```html
  <select [formField]="<entity>Form.status">
    <option value="">Select status</option>
    @for (status of statuses; track status) {
      <option [value]="status">{{ statusLabels[status] }}</option>
    }
  </select>
  ```

No `maxlength`, `pattern`, `type="email"` or `min`/`max` attributes: validation messages come from the backend.

## `<folder>.routes.ts`
```ts
export const <entities>Routes: Routes = [
  { path: '', component: <Entity>List },
  { path: 'new', component: <Entity>Form },
  { path: ':id/edit', component: <Entity>Form },
  { path: ':id', component: <Entity>Detail },
];
```

## `app.routes.ts` and `app.html` (top-level)
Append to `routes` after the existing feature routes:
```ts
{
  path: '<folder>',
  loadChildren: () => import('./<folder>/<folder>.routes').then((m) => m.<entities>Routes),
},
```
Append to `<nav>` after the existing links:
```html
<a routerLink="/<folder>" routerLinkActive="active"><Label>s</a>
```

## Nested
Placeholders: `<parents>` (parent folder and URL), `<parent>Id` (route param and input).

API: every method takes the parent id first. The URL is built per call:
```ts
private url(<parent>Id: number): string {
  return `/api/<parents>/${<parent>Id}/<folder>`;
}

list(<parent>Id: Signal<number>) {
  return httpResource<<Entity>[]>(() => this.url(<parent>Id()), { defaultValue: [] });
}

get(<parent>Id: Signal<number>, id: Signal<number | undefined>) {
  return httpResource<<Entity>>(() => {
    const <entity>Id = id();
    return <entity>Id === undefined ? undefined : `${this.url(<parent>Id())}/${<entity>Id}`;
  });
}
```

Components:
- Each component gets `readonly <parent>Id = input.required({ transform: numberAttribute });`.
- Every link and navigation is `['/<parents>', <parent>Id(), '<folder>', ...]`.
- The list's back link goes to the parent detail.

Routes:
- `<folder>.routes.ts` keeps the same four paths.
- In `app.routes.ts`, register the child **before** the parent's lazy route:
  ```ts
  {
    path: '<parents>/:<parent>Id/<folder>',
    loadChildren: () => import('./<folder>/<folder>.routes').then((m) => m.<entities>Routes),
  },
  ```
  The lazy route is componentless, so `<parent>Id` reaches the child components through `withComponentInputBinding()`.

Parent link:
- No nav link.
- Add one line to the parent detail template, after its Edit link:
  `<p><a [routerLink]="['/<parents>', <parent>.id, '<folder>']"><Label>s</a></p>`.
