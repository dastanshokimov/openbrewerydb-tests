# OpenBreweryDB — AQA task (Java + REST Assured)

## Prerequisites
- **Java 17+**
- **Maven 3.8+**
- Any IDE (IntelliJ IDEA recommended)
- Internet access (the API is public)

## How to run
```bash
# from project root
mvn clean test \
  -DAPI_BASE_URI="https://api.openbrewerydb.org"   # optional, defaults to this
```

### Notes
- Base path is set to `/v1` in the base test.
- REST Assured logging of request/response is enabled **on validation failure**.
- JSON Schema validation uses files from `src/test/resources/schemas`.

## Project structure (short)
```
src
 └─ test
    ├─ java
    │  ├─ api/
    │  │   └─ ApiClient.java              # thin client for /breweries endpoints
    │  └─ tests/
    │      ├─ BaseApiTest.java            # common setup (baseURI/basePath/logging)
    │      └─ SearchTests.java            # tests for /breweries/search
    └─ resources
       └─ schemas/
           ├─ brewery.schema.json         # single brewery schema
           └─ brewery-list.schema.json    # array of brewery objects
```

---

## Part 1 — “Search Breweries” coverage (what is implemented)

**Endpoint:** `GET /v1/breweries/search?query={search}`

We implemented up to 5 scenarios that cover the method’s core behavior.  
Every test:
- asserts **HTTP 200** (or an expected error for negative)
- validates the **response contract** via JSON Schema
- adds a scenario-specific assertion

### Implemented scenarios
1. **Common query returns results**
   - Query: a popular token (e.g., `"dog"`).
   - **Why:** happy path: service returns a **non-empty list**.
   - Checks: `200`, body matches **array schema**, array not empty.

2. **Find by meaningful part of a name**
   - Get a real brewery name from `/v1/breweries`, take the **first word** token (min length ≥ 3), search by it.
   - **Why:** verifies that **partial matching** works for real production data, not synthetic tokens.
   - Checks: `200`, **array schema**, result **contains the original brewery id**.

3. **Exact match sanity**
   - Take a brewery name from `/v1/breweries`, use the **whole name** as a query (URL-encoded).
   - **Why:** confirms that exact terms don’t regress relative to partial search.
   - Checks: `200`, **array schema**, at least one item has the **same name** (case-insensitive compare).

4. **No matches returns empty list**
   - Query: a long gibberish string (e.g., `"zzzzzzzzzz_qqqqqqqqq_1234567890"`).
   - **Why:** negative data path: backend should not fail and should consistently return **empty array**.
   - Checks: `200`, **array schema**, array is **empty**.

5. **Unsupported method returns 405**
   - `POST /v1/breweries/search`
   - **Why:** protocol/contract negative. Search is **GET-only**; server should respond with **405 Method Not Allowed**.
   - Checks: `405`, non-blank error message.

> Implementation details:
> - Contract is validated with two schemas:
>   - `brewery.schema.json` – single brewery
>   - `brewery-list.schema.json` – `type: array` of `brewery.schema.json` items.

---

## Part 2 — “List Breweries” test design (what & why; effort estimate)

**Endpoint:** `GET /v1/breweries`

Goal: outline how to automate this method comprehensively. Below is a **practical** test set you can implement incrementally.

### A. Contract, smoke & determinism
- **Contract (schema) check** for **list** and for **single** objects.
  - *Why:* catches breaking changes early.
- **Content-Type** → `application/json` and **UTF-8`.
  - *Why:* client compatibility.

### B. Pagination & limits (if supported by API)
- **Default page size** returns a sensible number of items.
- **`per_page` boundaries** (e.g., `1`, maximum allowed).
- **`page` navigation**: page 1 vs page 2 have **no overlap**.
  - *Why:* correctness of pagination & offsets.

### C. Filtering (based on API capabilities)
- **Single filter** (e.g., `by_city=Portland`) returns only records that match.
- **Multiple filters** combined (e.g., `by_state=California&by_type=micro`).
- **Case-insensitive** and **URL-encoded** values (e.g., spaces, commas).

### D. Data quality (spot checks)
- **Geo fields**: `latitude/longitude` are either `null` or parsable numbers within ranges.
- **Website/phone** formats plausible (basic regex).
  - *Why:* data hygiene.

### E. Negative & robustness
- **Invalid params**: `per_page=-1`, `per_page=0`, `per_page=verybig`, unknown filters.
  - *Expected:* `400`/`422` or safe fallback.
- **Unsupported methods**: `POST`/`PUT`/`DELETE` → `405`.
- **Large page number** beyond dataset size → empty list.
- **Rate-limit / throttling** smoke if API applies limits.

### F. Performance (lightweight)
- **P95/P99** time budget smoke (e.g., `GET /breweries?per_page=50` < **X** ms).

### G. Non-functional checks (optional)
- **CORS** headers present (if UI integrations expected).
- **Caching** headers: responses may be cacheable.

---

## Estimation

**MVP coverage for List Breweries** (schema, basic pagination, 2 filters, negative cases): **~6–8 hours**.  
**Full coverage** (all filters, boundaries, perf & data quality): **~1.5–2.5 days**.

---

## Tech stack & rationale
- **REST Assured** — concise HTTP client for tests.
- **TestNG** — flexible parametrization & lifecycle hooks.
- **AssertJ** — fluent assertions.
- **json-schema-validator** — contract testing.

---

## Deliverables
- Source code (tests + schemas).
- This README.
- Optional: test report from `mvn test` (Surefire) and logs if failures occur.
