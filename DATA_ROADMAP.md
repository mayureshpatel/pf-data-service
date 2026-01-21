# Database & Data Operations Roadmap

This roadmap focuses on hardening the runtime behavior of the data layer, addressing concurrency, automation, and security.

## Phase 1: Concurrency & Automation (Immediate Priority)
- [ ] **1. Optimistic Locking (Prevent Data Loss)**
    - [ ] **High Criticality:** Add `@Version private Long version;` to the `Account` entity.
    - [ ] Update `AccountRepository` to ensure queries respect the version.
    - [ ] Verify `ObjectOptimisticLockingFailureException` handling in `TransactionImportService`.
- [ ] **2. Activate Snapshot Scheduler**
    - [ ] Add `@EnableScheduling` to the main application class.
    - [ ] Create a `SnapshotScheduler` class with a Cron expression (e.g., `0 0 0 1 * ?` for monthly).
    - [ ] Wire it to `SnapshotService.createEndOfMonthSnapshot`.

## Phase 2: Security Hardening & Maintenance
- [ ] **3. PII Encryption at Rest**
    - [ ] Implement `AttributeConverter<String, String>` using AES-256.
    - [ ] Apply converter to `User.email`.
    - [ ] *Migration Note:* This requires a migration script to encrypt existing plain-text emails.
- [ ] **4. Production Hygiene**
    - [ ] **Vacuuming:** Ensure PostgreSQL `autovacuum` is tuned for the `transactions` table (high insert/update volume).
    - [ ] **Migration Safety:** Establish a rule to ban `TRUNCATE` / `DROP` in all future migration scripts (V16+).

## Phase 3: Advanced Reporting (Future)
- [ ] **5. Materialized Views**
    - [ ] If `SnapshotService` becomes too slow, replace strictly calculated snapshots with PostgreSQL `MATERIALIZED VIEW` for monthly aggregation.
- [ ] **6. RLS (Row Level Security)**
    - [ ] Move `user_id` filtering from the Application layer (JPA) to the Database layer (Postgres RLS) for defense-in-depth.

---
**Status:** Phase 1 is the active blocker for release.