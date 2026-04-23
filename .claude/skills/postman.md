# Postman Collections

## When to Use

When adding a new service, adding or removing an endpoint, or changing a request DTO or auth requirement. Every service must have a Postman collection kept in sync with its REST API.

## File Layout

```
.postman/
└── resources.yaml           # workspace pointer — do not edit

postman/
├── collections/
│   └── <service-name>/      # kebab-case, one folder per service
│       ├── collection.yaml  # collection definition + variables
│       ├── .resources/
│       │   └── definition.yaml
│       └── <operation>.request.yaml  # one file per endpoint
├── environments/
│   ├── local.yaml           # base URL per service for local dev
│   └── Dev Environment.environment.yaml  # shared auth vars
└── globals/
    └── workspace.globals.yaml
```

## Collection File (`collection.yaml`)

```yaml
$kind: collection
name: '<Service Name> API'
variables:
  - key: <service>BaseUrl
    value: 'http://localhost:<port>'
  - key: accessToken
    value: ''
  - key: refreshToken
    value: ''
  - key: userId
    value: ''
```

Declare every variable the requests in this collection reference. The `<service>BaseUrl` variable is overridden by the environment.

## Request File Format

One file per endpoint, named `<operation>.request.yaml` in kebab-case:

```yaml
$kind: http-request
name: <Human readable name>
method: GET|POST|PUT|DELETE
url: "{{<service>BaseUrl}}/api/..."
order: <number>
headers:
  Content-Type: application/json
auth:
  type: bearer
  credentials:
    token: "{{accessToken}}"
body:
  type: json
  content: |-
    {
      "field": "value"
    }
scripts:
  - type: afterResponse
    code: |-
      if (pm.response.code === 200) {
          const body = JSON.parse(pm.response.body);
          pm.environment.accessToken = body.accessToken;
          pm.environment.refreshToken = body.refreshToken;
          pm.environment.expiresIn = body.expiresIn;
      }
    language: text/javascript
```

**Rules:**
- Omit `auth:` on public endpoints (signup, signin, refresh, actuator).
- Omit `body:` on GET and DELETE.
- Omit `scripts:` unless the response carries data worth capturing.
- Use `pm.environment.<var>` for auth tokens (shared across collections); use `pm.variables.<var>` for IDs scoped to this collection run.

## Ordering Convention

`order:` controls display order within the collection. Use 1000-increment slots:

| Order | Slot |
|---|---|
| 1000 | Sign Up |
| 2000 | Sign In |
| 3000 | Refresh Token |
| 4000+ | Service-specific operations |

Leave gaps so new requests can be inserted without renumbering.

## Environments

### `local.yaml`

Add a `<service>BaseUrl` entry for each service:

```yaml
name: 'Local'
values:
  - key: userServiceBaseUrl
    value: 'http://localhost:8081'
    enabled: true
  - key: companyServiceBaseUrl
    value: 'http://localhost:8082'
    enabled: true
```

### `Dev Environment.environment.yaml`

Shared auth vars — do not add service-specific URLs here:

```yaml
name: Dev Environment
values:
  - key: accessToken
    enabled: true
    value: ''
  - key: refreshToken
    enabled: true
    value: ''
  - key: expiresIn
    enabled: true
    value: ''
```

## Adding a New Service

1. Create `postman/collections/<service-name>/collection.yaml` — set the name, port, and variables.
2. Create `postman/collections/<service-name>/.resources/definition.yaml`.
3. Add a `.request.yaml` file for every endpoint (see the user-service collection as reference).
4. Add `<service>BaseUrl` to `postman/environments/local.yaml`.
5. If the service issues tokens, add an afterResponse script on the signin/signup request to capture them into `pm.environment`.

## Adding a New Endpoint

1. Create `postman/collections/<service-name>/<operation>.request.yaml`.
2. Pick an `order:` value in the right 1000-slot.
3. Set correct method, URL, auth, and body matching the current DTO.
4. If the response creates a resource, capture its `id` into a `pm.variables` entry for use by downstream requests.

## Keeping Requests in Sync with DTOs

When a DTO field is added, removed, or renamed — update the matching request body in the collection. The Postman body is the living example of what the endpoint accepts.
