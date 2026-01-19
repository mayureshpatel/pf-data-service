# Database & Data Operations Roadmap

This roadmap focuses on hardening the runtime behavior of the data layer, addressing concurrency, automation, and security.

## Phase 1: Concurrency & Automation (Immediate Priority)
- [ ] **1. Optimistic Locking (Prevent Data Loss)**
    - [ ] Add `@Version private Long version;` to the `Account` entity.
    - [ ] Verify `ObjectOptimisticLockingFailureException` handling in `TransactionImportService`.
- [ ] **2. Activate Snapshot Scheduler**
    - [ ] Add `@EnableScheduling` to the main application class.
    - [ ] Create a `SnapshotScheduler` class with a Cron expression (e.g., `0 0 0 1 * ?` for monthly).
    - [ ] Wire it to `SnapshotService`.

## Phase 2: Security Hardening
- [ ] **3. PII Encryption at Rest**
    - [ ] Implement `AttributeConverter<String, String>` using AES-256.
    - [ ] Apply converter to `User.email`.
    - [ ] *Migration Note:* This requires a migration scr/ipt to encrypt existing plain-text emails.

## Phase 3: Operational Tuning
- [ ] **4. Connection Pool Tuning**
    - [ ] Enable HikariCP `leak-detection-threshold`.
    - [ ] Adjust `validation-timeout`.
- [ ] **5. Database Maintenance**
    - [ ] Create a script for `VACUUM ANALYZE` (PostgreSQL specific) to run periodically (or rely on AutoVacuum, but verify config).

---
**Status:** Phases 1-4 of previous roadmap complete. Starting new Cycle focused on Runtime Stability.
