# AQA task (Java + REST Assured + TestNG + AssertJ)

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

## Part 1: “Search Breweries” coverage (what is implemented)

**Endpoint:** `GET /v1/breweries/search?query={search}`

Implemented up to 5 scenarios that cover the method’s core behavior.  
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

## Part 2: “List Breweries” test design (what & why; effort estimate)

**Endpoint:** `GET /v1/breweries`

Goal: outline how to automate this method comprehensively. 

# Test Plan: `GET /breweries` (List Breweries)

## Test Design Techniques (and why)

1. **Equivalence Partitioning**  
   Split input values into valid (known filters, correct types) and invalid (unknown, empty).  
   *Why:* Ensures broad coverage with fewer tests.

2. **Boundary Value Analysis**  
   Validate limits for `per_page` (1, 200, >200), `page`, postal code formats.  
   *Why:* APIs often fail at boundaries.

3. **Combinatorial / Pairwise**  
   Combine 1–2 filters with pagination/sorting (e.g., `by_state + by_type`).  
   *Why:* Most real bugs appear from interaction of 2 params.

4. **Contract Testing (Schema Validation)**  
   Every 2xx response must match `brewery-list.schema.json`.  
   *Why:* Protects against regression in structure/field types.

5. **Error Guessing / Negative Testing**  
   Wrong types (`by_type=foobar`), unsupported methods (`POST /breweries`), conflicting params.  
   *Why:* Covers typical implementation pitfalls.

6. **Collection Invariants**
    - `per_page=N` → exactly N items (or fewer if not enough data).
    - Different `page` values → disjoint sets.
    - Sorting order respected.  
      *Why:* Ensures system-level consistency of results.

---

## Test Cases

### A. Basic / Contract
- **Default call without params** → `200 OK`, ≤50 items, schema valid.
- **Schema validation** is applied on every successful response.

### B. Pagination
- `per_page=1` → exactly 1 item.
- `per_page=200` → ≤200 items.
- `page=1` vs `page=2` (same `per_page`) → no overlap.
- `per_page=201` → either capped to 200 or error (documented behavior to be confirmed).

### C. Attribute Filters
- `by_city=San_Diego` → all `"city" == "San Diego"`.
- `by_state=California` → all `"state" == "California"`.
- `by_country=United_States` → all `"country" == "United States"`.
- `by_postal=94107` (5-digit) → all postal codes start with `94107`.
- `by_postal=94107-1234` (9-digit) → exact match required.
- `by_type=micro` (repeat for a few types) → all have `"brewery_type" == "micro"`.

### D. IDs & Distance
- `by_ids=<id1,id2>` → returns exactly these IDs.
- `by_dist=<lat,lon>` → closest breweries; confirm note: `sort` cannot be combined with `by_dist`.

### E. Sorting
- `sort=name&order=asc&per_page=10` → alphabetical ascending.
- `sort=name&order=desc&per_page=10` → descending.
- Combine filter + sort (`by_state=California&sort=name`) → both respected.

### F. Negative / Resilience
- `by_type=foobar` → empty list.
- `POST /breweries` → `405 Method Not Allowed`.
- Conflicting: `sort` with `by_dist` → confirm actual behavior (ignore or documented error).
- Empty filter values (`by_city=`) → consistent response (like no filter or empty list).

---

## Estimation (only for `/breweries`)

Full coverage of `/breweries` with pagination, filters, sorting, and negative cases would take around **6 hours** of work. A minimal but representative set (1–2 cases per feature + negatives) could be completed in about **3.5–4 hours**.

---

## Notes
- Tests must be **idempotent**: no dependency on specific data, except when fetching live IDs/names and verifying them via `by_ids` or `search`.
- Sorting checks: compare actual list with a locally sorted copy.
- Contract validation (`brewery-list.schema.json`) is always applied for 2xx responses.
- Special case: `by_dist` with `sort` must be explicitly tested as per documentation.