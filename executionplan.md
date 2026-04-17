# Execution Plan: Azure Feature Prototype (Phased)

> Goal: Build a **working prototype** (not production-grade) for:
> - Azure SQL Database (Relational Data)
> - Azure Cosmos DB (Catalog & Cart)
> - Azure Service Bus (Async Messaging)
> - Azure AI Search

This plan is designed for your current microservices in this repo:
- `user-service`
- `product-service`
- `inventory-service`
- `order-service`
- Kubernetes manifests in `k8s/`

---

## Phase 0 - Baseline and toggles (Day 0)

### Objective
Keep current functionality working while adding switchable Azure integrations.

### Steps
1. Add feature toggles in each service `application.yaml` (currently empty):
   - `feature.azure-sql-enabled`
   - `feature.cosmos-catalog-enabled`
   - `feature.cosmos-cart-enabled`
   - `feature.servicebus-enabled`
   - `feature.aisearch-enabled`
2. Wire matching env vars in k8s manifests:
   - `k8s/user-service.yaml`
   - `k8s/product-service.yaml`
   - `k8s/inventory-service.yaml`
   - `k8s/order-service.yaml`
3. Keep all toggles `false` by default.
4. Verify current APIs still behave exactly the same.

### Exit criteria
- Existing endpoints under ingress (`/user`, `/product`, `/inventory`, `/order`) still work.
- No behavior changes when toggles are off.

---

## Phase 1 - Azure SQL Database for relational services

### Objective
Move relational persistence from local/Postgres setup to Azure SQL for:
- `user-service`
- `inventory-service`
- `order-service`

### Steps
1. In each target service `pom.xml`, add SQL Server JDBC dependency.
2. Keep existing JPA entities/repositories as-is:
   - `User`, `UserRepository`
   - `Inventory`, `InventoryRepository`
   - `Order`, `OrderItem`, `OrderRepository`
3. Add datasource settings from env vars:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
4. Add optional SQL Server dialect config in `application.yaml`.
5. Create new secret manifest (example): `k8s/azure-sql-secret.yaml`.
6. Update `k8s/*-service.yaml` env sections to pull SQL credentials from new secret.
7. Keep old Postgres config as fallback behind toggle.

### Quick verification
1. Register user.
2. Add inventory.
3. Create order.
4. Fetch the same records via GET endpoints.

### Exit criteria
- CRUD + order flow works with `feature.azure-sql-enabled=true`.
- Services still work with toggle off.

---

## Phase 2 - Cosmos DB for Catalog and Cart

### Objective
Use Cosmos DB for:
- Product catalog documents
- Shopping cart documents

### Steps
1. In `product-service`, add Cosmos model + repository for catalog docs.
2. Keep current JPA `Product` path intact as fallback.
3. In `ProductService`, route read/write to Cosmos when `feature.cosmos-catalog-enabled=true`.
4. In `order-service`, add cart feature (prototype endpoints):
   - `POST /api/orders/cart/{userId}/items`
   - `GET /api/orders/cart/{userId}`
   - `DELETE /api/orders/cart/{userId}`
5. Store cart in Cosmos with partition key `/userId`.
6. Keep order finalization relational (Azure SQL path).
7. On checkout, optionally clear cart.

### Cosmos config to add
- `AZURE_COSMOS_ENDPOINT`
- `AZURE_COSMOS_KEY`
- `AZURE_COSMOS_DATABASE`
- `AZURE_COSMOS_CATALOG_CONTAINER`
- `AZURE_COSMOS_CART_CONTAINER`

### Quick verification
1. Create product -> verify catalog doc exists.
2. Add items to cart -> fetch cart by `userId`.
3. Checkout -> order is created in relational DB.

### Exit criteria
- Catalog and cart workflows function with Cosmos toggles enabled.
- Fallback to existing behavior still works when toggles disabled.

---

## Phase 3 - Service Bus async messaging

### Objective
Emit and consume asynchronous events around order creation.

### Steps
1. In `order-service`, after successful order creation in `OrderService#createOrder`, publish event:
   - `OrderCreatedEvent` with `orderId`, `userId`, `totalAmount`, `items`.
2. Use a queue (simpler for prototype) first.
3. Add consumer in `inventory-service` (or `product-service`) to prove end-to-end consumption.
4. Keep existing synchronous clients (`ProductClient`, `InventoryClient`) unchanged.
5. Add env vars/secrets:
   - `AZURE_SERVICEBUS_CONNECTION_STRING`
   - `AZURE_SERVICEBUS_QUEUE_NAME`

### Quick verification
1. Place an order.
2. Confirm publish log in `order-service`.
3. Confirm consume log in subscriber service.

### Exit criteria
- Order-created message reliably appears and is consumed at least once.

---

## Phase 4 - Azure AI Search integration

### Objective
Provide searchable product catalog using Azure AI Search.

### Steps
1. Define search index fields:
   - `id`, `name`, `description`, `category`, `price`
2. In `product-service`, push documents to AI Search on create/update.
3. Add search endpoint (prototype), for example:
   - `GET /api/products/search?q=...`
4. Query AI Search SDK/API and return top matches.
5. Add env vars/secrets:
   - `AZURE_SEARCH_ENDPOINT`
   - `AZURE_SEARCH_API_KEY`
   - `AZURE_SEARCH_INDEX_NAME`

### Quick verification
1. Create/update product.
2. Run search query.
3. Confirm expected products are returned.

### Exit criteria
- Search endpoint returns results from Azure AI Search index.

---

## Phase 5 - End-to-end demo flow

### Objective
Run a complete demo showing all Azure-backed features together.

### Demo script
1. Register user (`user-service`).
2. Create products (`product-service`, Cosmos-backed).
3. Add inventory (`inventory-service`, Azure SQL-backed).
4. Add products to cart (`order-service`, Cosmos-backed).
5. Checkout order (`order-service`, Azure SQL-backed).
6. Observe order-created message in Service Bus consumer logs.
7. Search products via AI Search endpoint.
8. Toggle off one integration (for example Cosmos cart) and show fallback path still works.

### Exit criteria
- Full business flow runs in one session with Azure integrations enabled.

---

## Suggested implementation order (fastest prototype path)

1. **Phase 0** (toggles + no behavior change)
2. **Phase 1** (Azure SQL)
3. **Phase 2** (Cosmos catalog/cart)
4. **Phase 3** (Service Bus events)
5. **Phase 4** (AI Search)
6. **Phase 5** (final demo)

---

## Minimal secrets/config checklist

Create these Kubernetes secret manifests (prototype):
- `k8s/azure-sql-secret.yaml`
- `k8s/azure-cosmos-secret.yaml`
- `k8s/azure-servicebus-secret.yaml`
- `k8s/azure-search-secret.yaml`

And mount them into service deployments in `k8s/*.yaml` using env vars.

---

## Notes for prototype scope

- Prefer simple direct SDK usage over abstraction layers.
- Avoid refactoring unrelated modules during prototype.
- Keep sync flow active while adding async and search features.
- Use feature toggles to demo both new and fallback behavior quickly.

