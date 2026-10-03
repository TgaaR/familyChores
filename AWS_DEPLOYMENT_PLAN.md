# familyChoreGame — AWS Migration and Deployment Plan

## Purpose

This document describes how to move the current local application to AWS, make the PWA installable on smartphones, and operate it safely. It is a deployment roadmap, not a statement that the application is production-ready today.

The current application consists of:

- Java 21 and Spring Boot, built with Maven
- A Spring Boot-served PWA and `/api/v1` API
- Jdbi with PostgreSQL
- Local resource files for chores, rights, and the school calendar
- An `aws` Spring profile that writes ECS-formatted JSON logs
- A basic health endpoint at `/actuator/health`

Basic member/PIN authentication and parent/child authorization are implemented. Login hardening, automated backups, restore tooling, and production infrastructure are not yet implemented.

## 1. Target AWS architecture

> **Architecture update:** PostgreSQL is now implemented. The AWS target is App Runner or ECS Fargate with a private Amazon RDS for PostgreSQL database. The older EC2/SQLite alternative retained below is historical context and is no longer the recommended deployment.

### Superseded alternative: EC2 with persistent EBS storage

Use one small Linux EC2 instance and keep SQLite on an attached encrypted EBS volume.

```text
Smartphone or computer
        |
      HTTPS
        |
Route 53 domain
        |
Caddy or Nginx on EC2
        |
Spring Boot application
        |
SQLite on encrypted EBS
        |
Encrypted backup in S3
```

This is the shortest safe route because it preserves the existing SQLite/Jdbi implementation. It is appropriate for one family and a small number of devices.

Important constraints:

- Run only one application instance against the SQLite file.
- Store the database on EBS, never in temporary instance storage or inside a container image.
- Do not put the live SQLite database on EFS or S3.
- Back up SQLite using its online backup mechanism, not by blindly copying an active file.

### Recommended deployment: App Runner or ECS with RDS PostgreSQL

Move to this architecture if the application will support multiple families, needs multiple application instances, or requires managed failover.

```text
GitHub Actions -> ECR -> App Runner or ECS Fargate -> RDS PostgreSQL
                                    |
                                CloudWatch
```

The PostgreSQL driver, PostgreSQL-native schema and queries, environment-based datasource configuration, and PostgreSQL-backed integration tests are implemented. AWS work still includes RDS provisioning, verified TLS, secrets, connection-pool sizing, backup/restore drills, and any required one-time transfer of old SQLite development data.

## 2. Complete production-readiness work before public launch

### 2.1 Add authentication and authorization

The API now has application-managed member/PIN login, BCrypt PIN hashes, server sessions, and parent/child roles. Do not expose it publicly until the remaining hardening below is complete.

Implement and test:

1. [x] A family/member login flow.
2. [x] Secure PIN hashing; PINs are never stored in plain text.
3. [x] Parent and child roles.
4. [x] Parent-only protection for the currently implemented member-management and scheduling actions.
5. [x] Server-side authorization on currently implemented protected endpoints, plus matching UI controls.
6. [x] Secure session cookies with `Secure`, `HttpOnly`, and `SameSite=Strict`, plus CSRF protection.
7. [x] Database-backed login throttling and safe structured authentication logs. A durable audit-history table remains optional future work.

The current decision is an application-managed family login. Revisit Amazon Cognito only if the product expands beyond the current single-family scope.

### 2.2 Finalize production API behavior

Add the production features required by `PROJECT_PLAN.md`, including:

- Session and permission information
- Parent approval/cancellation flows where required
- Point corrections and an auditable point ledger
- Vacation overrides
- Stable optimistic-concurrency handling
- Export and backup-health endpoints
- SSE live updates, or retain polling intentionally for the first release

### 2.3 Harden configuration

Production configuration must come from environment variables or AWS Secrets Manager/Systems Manager Parameter Store. Do not commit secrets to GitHub.

At minimum configure:

- `SPRING_PROFILES_ACTIVE=aws`
- `SERVER_ADDRESS=127.0.0.1` when a reverse proxy runs on the same EC2 instance
- `SERVER_PORT=8080`
- `FAMILY_CHORE_DB_URL=jdbc:postgresql://<rds-endpoint>:5432/family_chore_game?sslmode=verify-full`
- `FAMILY_CHORE_DB_USERNAME=<secret value>`
- `FAMILY_CHORE_DB_PASSWORD=<secret value>`
- Authentication secrets and keys
- Allowed public origin/host settings, if introduced

Keep separate development, staging, and production settings.

## 3. Prepare the application artifact

1. Confirm Java 21 compatibility in CI.
2. Run the complete test suite:

   ```bash
   ./mvnw clean verify
   ```

3. Confirm JavaScript syntax:

   ```bash
   node --check app.js
   ```

4. Build the executable JAR:

   ```bash
   ./mvnw clean package
   ```

5. Run the exact artifact locally with the AWS profile:

   ```bash
   SPRING_PROFILES_ACTIVE=aws java -jar target/family-chore-game-0.1.0-SNAPSHOT.jar
   ```

6. Verify `/`, `/api/v1/bootstrap`, and `/actuator/health`.
7. Add a reproducible release version instead of relying permanently on `0.1.0-SNAPSHOT`.

A Docker image is optional for the first EC2 deployment. A systemd-managed JAR is simpler and avoids placing SQLite inside a container filesystem.

## 4. Create AWS accounts and security foundations

1. Use a dedicated AWS account or an appropriately isolated workload account.
2. Enable MFA on the AWS root user and do not use it for routine work.
3. Create administrator access through IAM Identity Center or a tightly controlled IAM role.
4. Enable AWS Budgets and a low-cost billing alarm before creating resources.
5. Select one AWS region close to the family and keep the application, EBS volume, backups, and monitoring there unless backup policy requires another region.
6. Tag resources with application, environment, owner, and cost-center labels.
7. Enable CloudTrail for account activity auditing.

## 5. Provision the first EC2 environment

Create a staging environment before production.

1. Create a VPC or use a carefully reviewed default VPC.
2. Create a small current-generation Linux EC2 instance that supports the selected region and Java 21 workload.
3. Use an encrypted root EBS volume and a separate encrypted EBS data volume for the database and backup staging area.
4. Configure the security group:
   - Allow HTTPS (`443`) from intended clients.
   - Allow HTTP (`80`) only if needed for certificate issuance or redirect it to HTTPS.
   - Do not expose port `8080` publicly.
   - Avoid public SSH. Prefer AWS Systems Manager Session Manager.
5. Attach an IAM instance role with only the permissions required for CloudWatch, S3 backups, Secrets Manager/Parameter Store, and Systems Manager.
6. Assign a stable address through an Elastic IP if the chosen DNS/reverse-proxy design needs one.
7. Install Java 21, the reverse proxy, CloudWatch Agent if needed, and operational utilities.
8. Mount the data volume at a fixed path such as `/var/lib/family-chore-game` and configure it to mount after reboot.
9. Create a non-login operating-system user dedicated to the application.
10. Restrict ownership and permissions on the database, configuration, and deployment directories.

## 6. Configure HTTPS and DNS

A trusted HTTPS origin is required for reliable PWA installation and service-worker behavior on smartphones.

1. Register or choose a domain name.
2. Create its DNS records in Route 53 or the existing DNS provider.
3. Configure Caddy or Nginx as the public reverse proxy to `127.0.0.1:8080`.
4. Obtain and automatically renew a trusted TLS certificate.
5. Redirect HTTP to HTTPS.
6. Add appropriate security headers, including HSTS only after HTTPS is verified.
7. Forward the client/proxy headers Spring Boot needs, and verify that generated URLs and secure cookies behave correctly.
8. Confirm that `/actuator` exposes only the intended endpoints; never expose environment or configuration details publicly.

An Application Load Balancer with an ACM certificate is another option, but it adds cost and complexity for a single-instance application.

## 7. Install the application as a service

1. Copy the tested release JAR to a versioned release directory on the EC2 instance.
2. Keep runtime configuration outside the JAR.
3. Create a systemd service that:
   - Runs as the dedicated application user
   - Activates the `aws` profile
   - Reads secrets from a protected environment source
   - Uses the EBS-backed SQLite path
   - Restarts after recoverable failures
   - Applies memory and filesystem protections appropriate to the application
4. Start the service and enable it at boot.
5. Verify the local health endpoint before sending public traffic to the release.
6. Verify that a machine reboot remounts the data volume and restarts the application correctly.

## 8. Migrate existing local data

If development data should become production data:

1. Stop local writes.
2. Create a consistent SQLite backup using SQLite's backup command/API.
3. Record a checksum of the backup.
4. Transfer it through an encrypted channel to the EBS-backed database path.
5. Set restrictive file ownership and permissions.
6. Start the application and allow tested schema migrations to run.
7. Verify family, members, assignments, balances, right redemptions, and historical snapshots.
8. Keep the source backup unchanged until production verification is complete.

If development data is disposable, start with a new empty production database and perform family onboarding after deployment.

## 9. Implement backups and restore procedures

Before production use:

1. Create a private S3 backup bucket with public access blocked, versioning enabled, and server-side encryption using KMS.
2. Grant the EC2 role write access only to the required bucket/prefix.
3. Schedule an online SQLite backup at least nightly.
4. Encrypt and upload the completed backup plus its checksum to S3.
5. Apply lifecycle/retention rules for daily, weekly, and monthly backups.
6. Consider replication to another AWS region or a separate backup account.
7. Log backup success, duration, size, and failure without logging family data.
8. Alarm when a backup fails or the latest successful backup becomes stale.
9. Document and automate restoration onto replacement storage.
10. Perform a restore drill in a disposable environment before launch and periodically afterward.

EBS snapshots are useful as an additional recovery layer, but they do not replace application-consistent SQLite backups and restore testing.

## 10. Configure logging, health checks, and alarms

1. Run with the `aws` profile so logs are emitted as ECS JSON.
2. Send application and system logs to CloudWatch Logs.
3. Define log retention instead of retaining logs indefinitely.
4. Preserve the existing request ID in responses and logs.
5. Never log passwords, PINs, cookies, tokens, family/member names, request bodies, or SQL parameters.
6. Monitor:
   - Application health
   - HTTP 5xx responses
   - Process restarts
   - CPU, memory, disk use, and disk space
   - Database/backup failures
   - TLS certificate renewal
7. Send alarms through SNS to a parent-controlled email or another notification channel.

## 11. Add GitHub Actions deployment

Use two workflows or two clearly separated jobs.

### Continuous integration

For every pull request and push:

1. Check out the repository.
2. Install Java 21 with Maven dependency caching.
3. Run `./mvnw clean verify`.
4. Run the JavaScript syntax check and, once added, browser/PWA tests.
5. Upload test reports when failures occur.

### Deployment

For an approved release or protected `main` branch update:

1. Authenticate to AWS using GitHub OpenID Connect (OIDC), not permanent AWS access keys.
2. Build and checksum one immutable JAR artifact.
3. Store the artifact in a versioned S3 deployment bucket or another controlled artifact repository.
4. Deploy to staging first through Systems Manager.
5. Run health and smoke tests.
6. Require approval before production deployment.
7. Place the new JAR in a versioned directory, switch the service to it, and restart.
8. Run post-deployment health checks.
9. Automatically return to the previous JAR if startup or smoke tests fail.

Protect `main`, require passing checks, restrict production environments, and record the deployed Git commit.

## 12. Validate the staging environment

Test from both a computer and a real smartphone:

- HTTPS and certificate trust
- First-family onboarding
- Hebrew/English switching and RTL/LTR layout
- Member creation and archival
- Current- and coming-week scheduling rules
- Chore completion and scoring
- Rights, affordability, availability days, and daily caps
- Concurrent use from two devices
- Server restart and EC2 reboot without data loss
- Offline banner and disabled writes while disconnected
- PWA installation, icon, launch behavior, and application updates
- Logs, request IDs, alerts, backups, and a full restore
- Authorization boundaries for parent and child accounts

Do not promote staging data or credentials into production accidentally.

## 13. Production launch sequence

1. Confirm that authentication and authorization tests pass.
2. Confirm a successful staging restore drill.
3. Provision production separately from staging.
4. Configure production DNS, TLS, secrets, monitoring, and backups.
5. Deploy the exact artifact already tested in staging.
6. Run smoke tests before family onboarding or data migration.
7. Create or restore the production family data.
8. Install the PWA from its HTTPS URL on each smartphone using **Install app** or **Add to Home screen**.
9. Verify that each device reads and modifies the same server data.
10. Monitor errors, storage, and backups closely after launch.

No Play Store installation is required. The browser installs the HTTPS PWA and future frontend updates are delivered by the deployed web application and service worker.

## 14. Routine operation

### For every release

1. Review and merge through protected GitHub checks.
2. Back up the production database before schema changes.
3. Deploy to staging and test migrations.
4. Deploy the same artifact to production.
5. Check health, logs, and core user flows.
6. Confirm that the service worker exposes the new release as expected.

### Regular maintenance

- Review alarms, dependency updates, operating-system patches, IAM access, costs, disk usage, and backup age.
- Test a restore at least quarterly.
- Rotate secrets and keys on a defined schedule and after suspected exposure.
- Renew school-calendar data before the packaged year expires.
- Review audit events for sensitive parent actions once auditing is implemented.

## 15. Rollback and disaster recovery

### Application rollback

1. Stop routing new work to a failed release if applicable.
2. Switch systemd back to the previous immutable JAR.
3. Restart and run health checks.
4. Do not automatically roll back a database migration unless a tested reverse migration exists.

### Instance or disk failure

1. Provision a replacement EC2 instance and encrypted EBS volume from infrastructure documentation or code.
2. Restore the latest verified SQLite backup from S3.
3. Deploy the matching application version.
4. Validate the data and health locally.
5. Restore DNS/traffic and verify from a client device.

Record the achieved recovery point and recovery time after every drill or real incident.

## 16. Optional migration from SQLite to RDS PostgreSQL

Perform this as a separate project rather than during the first production deployment.

1. Add the PostgreSQL JDBC driver and environment-based datasource configuration.
2. Replace SQLite-specific connection setup and SQL with portable or PostgreSQL variants.
3. Adopt a production migration tool such as Flyway and establish migration ownership.
4. Run the complete repository and concurrency test suites against PostgreSQL.
5. Create a private RDS PostgreSQL instance with encryption, automated backups, deletion protection, and restricted security groups.
6. Store credentials in Secrets Manager and use rotation where practical.
7. Write and rehearse an idempotent SQLite-to-PostgreSQL data migration.
8. Validate row counts, foreign keys, point balances, timestamps, and historical snapshots.
9. Schedule a maintenance window, stop writes, take a final SQLite backup, migrate, validate, and switch the application.
10. Retain the final SQLite backup until the new system has passed the agreed verification period.
11. Only then consider multiple application instances, App Runner, or ECS Fargate.

## 17. Definition of done

AWS production deployment is complete only when:

- The API is protected by tested authentication and server-side authorization.
- The application is reachable only through trusted HTTPS.
- The PWA installs and updates correctly on target phones.
- The database resides on encrypted persistent storage.
- A tested automated backup and restore process exists.
- Secrets are absent from Git and managed through an AWS secret service.
- Logs and metrics reach CloudWatch without sensitive user data.
- Health, error, disk, and stale-backup alarms reach an operator.
- CI and staging checks gate production deployment.
- Rollback and replacement-host recovery have been rehearsed.
- The deployed Git commit, artifact, configuration, and migration version are traceable.

## Suggested implementation order

1. Authentication and parent/child authorization.
2. Production configuration and security hardening.
3. Backup/restore implementation and tests.
4. Reproducible JAR build and systemd service definition.
5. Staging EC2, EBS, DNS, and HTTPS infrastructure.
6. CloudWatch logging, metrics, and alarms.
7. GitHub Actions CI and OIDC-based deployment.
8. Staging functional, mobile PWA, security, reboot, and restore tests.
9. Production provisioning and launch.
10. Optional PostgreSQL/RDS and managed-container migration later.
