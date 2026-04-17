# Microsoft Entra ID Authentication

This project now treats each backend service as a Spring Security OAuth2 resource server backed by Microsoft Entra ID.

## Shared API registration

All four services trust the same Entra issuer and the same API audience:

- `AZURE_ENTRA_TENANT_ID`
- `AZURE_ENTRA_API_AUDIENCE`

The placeholder values are already wired into:

- `docker-compose.yml`
- `k8s/azure-entra-secret.yaml`
- each service `application.yaml`
- each Kubernetes deployment manifest

Use a single Entra app registration for the whole backend and expose the API with an audience like:

- `api://<your-api-app-id>`

## Expected scopes and app roles

The APIs accept these delegated scopes:

- `CloudProject.Access`
- `CloudProject.Write`

The APIs also accept this application role:

- `CloudProject.Admin`

Authorization rules are:

- `GET` endpoints require `CloudProject.Access`, `CloudProject.Write`, or `CloudProject.Admin`
- mutating endpoints require `CloudProject.Write` or `CloudProject.Admin`

## End-to-end flow

1. A client signs in with Microsoft Entra ID and requests an access token for the configured `AZURE_ENTRA_API_AUDIENCE` value.
2. The client calls any backend service with `Authorization: Bearer <token>`.
3. `order-service` relays the same bearer token to `product-service` and `inventory-service`.
4. `/actuator/health` and `/actuator/info` stay public for probes and diagnostics.
5. `/api/users/me` can be used to inspect the authenticated principal and granted authorities.

## Replacing the dummy values

Update the placeholder values in:

- `docker-compose.yml` for local runs
- `k8s/azure-entra-secret.yaml` for Kubernetes

Then create matching delegated scopes and the `CloudProject.Admin` app role in your Entra app registration.

## Example

```bash
curl http://localhost:8081/api/users/me \
  -H "Authorization: Bearer <entra-access-token>"
```
