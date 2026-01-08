# Project To-Do List

## Completed
- [x] **PostgreSQL Setup**: Docker container configured and running.
- [x] **Java Middleware**:
  - [x] Created `ERPNextService.java` for syncing items from ERPNext.
  - [x] Updated `ProductRepo.java` and `AppConstants.java`.
  - [x] Configured `application.properties` to use environment variables.
  - [x] Added bootstrap data for immediate PoC visualization.
  - [x] Verified API endpoint `/api/public/products`.
- [x] **Next.js Storefront**:
  - [x] Created `lib/java` adapter to fetch data from Java backend.
  - [x] Updated UI components (`ThreeItemGrid`, `Carousel`, `ProductPage`) to use Java adapter.
  - [x] Configured `next.config.ts` to allow Unsplash images.
  - [x] Verified storefront rendering.
- [x] **Infrastructure**:
  - [x] Created `docker-compose.yml` for ERPNext.
  - [x] Created central `.env` file for credentials.
- [x] **Version Control**:
  - [x] Initialized git for `ECommerceApplication`.
  - [x] Initialized git for `vaabhi-storefront`.

## Pending / Next Steps
- [ ] **ERPNext Assets**: Fix 404 errors for CSS/JS assets (currently blocking correct rendering of ERPNext UI).
  - *Investigation*: Verify `bench build` output location and Nginx/Gunicorn configuration in the Docker container.
- [ ] **ERPNext Site Initialization**: Complete the `bench new-site` setup to ensure a functional database and login.
- [ ] **Live Sync Verification**: specifically test the flow: *Add Item in ERPNext -> Poll by Java -> Show on Storefront*. (Currently relying on bootstrap data).
- [ ] **Push to Remote**: config remote origin and push the git repositories.
