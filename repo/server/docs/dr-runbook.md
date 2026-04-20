# Meridian — Disaster Recovery Runbook

## Overview

This runbook covers PostgreSQL standby failover and point-in-time recovery for the Meridian system. Keep a printed copy accessible when the primary database is unreachable.

---

## 1. Roles and Contact

| Role | Responsibility |
|------|----------------|
| On-call DBA | Execute failover steps |
| System Admin | Restart application services after failover |
| Security Officer | Authorize access during incident |

---

## 2. Pre-conditions

- Streaming replication is configured from primary to at least one standby.
- `BACKUP_PATH` on the backup host contains the latest full dump (`*.dump`) and WAL archives.
- `DB_PASSWORD` and `DB_USER` are available in the environment or from the secrets vault.

---

## 3. Standby Failover Procedure

### 3.1 Confirm Primary Failure

```bash
psql -h <primary-host> -U meridian -c "SELECT 1;" 2>&1
# Expected: could not connect to server
```

Check that the standby has applied WAL up to the last confirmed LSN:

```bash
psql -h <standby-host> -U meridian -c "SELECT pg_last_wal_replay_lsn();"
```

### 3.2 Promote the Standby

On the **standby host**, run:

```bash
pg_ctl promote -D /var/lib/postgresql/16/main
# OR for PostgreSQL 12+:
psql -h <standby-host> -U postgres -c "SELECT pg_promote();"
```

Confirm the standby is now accepting writes:

```bash
psql -h <standby-host> -U meridian -c "SELECT pg_is_in_recovery();"
# Expected: f
```

### 3.3 Update Application Configuration

Update `application.yml` (or the environment variable) to point to the new primary:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://<standby-host>:5432/meridian
```

Restart the Spring Boot application:

```bash
systemctl restart meridian-server
# or in Docker:
docker-compose restart server
```

### 3.4 Verify Application Health

```bash
curl -sf http://localhost:8080/api/v1/health | jq .status
# Expected: "UP"
```

---

## 4. Point-in-Time Recovery from Backup

Use this procedure when no standby is available or data must be recovered to a specific point.

### 4.1 Identify the Backup File

```bash
ls -lt /app/backups/*.dump | head -5
# Pick the most recent FULL dump or the nearest before the target time
```

### 4.2 Restore to a New Instance

```bash
# Create a fresh database
createdb -U meridian meridian_restored

# Restore from the custom-format dump
pg_restore -h localhost -U meridian -d meridian_restored \
  --no-owner --no-acl /app/backups/<timestamp>-full.dump
```

### 4.3 Apply WAL Archives (if available)

Add `recovery.conf` (PG ≤ 11) or a `recovery_target_time` entry in `postgresql.conf` (PG ≥ 12):

```conf
restore_command = 'cp /wal_archive/%f %p'
recovery_target_time = '2026-04-20 03:00:00+00'
recovery_target_action = 'promote'
```

Start PostgreSQL and wait for recovery to complete:

```bash
pg_ctl start -D /var/lib/postgresql/16/main
tail -f /var/log/postgresql/postgresql.log | grep "recovery"
```

### 4.4 Swap the Restored Database

```bash
psql -U postgres -c "ALTER DATABASE meridian RENAME TO meridian_old;"
psql -U postgres -c "ALTER DATABASE meridian_restored RENAME TO meridian;"
```

Restart the application as in step 3.3.

---

## 5. Verification Checklist

- [ ] `GET /api/v1/health` returns `{"status":"UP"}`
- [ ] Admin login succeeds
- [ ] At least one training session record is readable
- [ ] Audit log shows no unexpected gaps
- [ ] Run a Recovery Drill via `POST /api/v1/admin/backups/recovery-drill` and confirm status = PASSED

---

## 6. Post-Incident Actions

1. Create a new full backup immediately: `POST /api/v1/admin/backups/run?mode=FULL`
2. Re-establish streaming replication to a new standby.
3. File an incident report and update this runbook with lessons learned.

---

*Last reviewed: 2026-04-20*
