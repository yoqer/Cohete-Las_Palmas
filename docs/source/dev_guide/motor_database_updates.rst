Motor Database Updates (Security)
================================

OpenRocket can optionally check for a newer motor database at startup and install it into the user's motor library
directory. The update flow is implemented in:

- `core/src/main/java/info/openrocket/core/database/MotorDatabaseRemoteUpdater.java`
- `swing/src/main/java/info/openrocket/swing/startup/MotorDatabaseUpdateChecker.java`

Remote endpoints
----------------

By default, the updater fetches:

- Metadata: `https://openrocket.info/motor-database/metadata.json`
- Database (compressed): `https://openrocket.info/motor-database/motors.db.gz`

The metadata must include `database_version` and may include `sha256`.

Update flow
-----------

1. Download remote `metadata.json` and parse it.
2. Compare `database_version` with the local `metadata.json` in `SystemInfo.getOpenRocketMotorLibraryDirectory()`.
3. If remote is newer, prompt the user (Yes/No) to install.
4. If accepted, download `motors.db.gz`, decompress to `motors.db`, validate it, then atomically replace the local DB
   and write the remote metadata next to it.

Security properties
-------------------

The updater takes several precautions when downloading and installing data:

- HTTPS-only and redirect constraints
  - Downloads use HTTPS.
  - Redirects are handled manually and refused if they switch to non-HTTPS or to a different host than the original
    URL. This reduces the chance of silent host switching.

- Integrity verification (SHA-256)
  - If `sha256` is present and valid (64 hex chars), the download is verified.
  - The published `sha256` may refer to either:
    - the compressed `motors.db.gz` bytes, or
    - the decompressed `motors.db` bytes
    The updater accepts a match against either convention.
  - If the metadata contains a non-empty but invalid SHA-256 value, installation is aborted.

- Schema validation before install
  - The downloaded database is validated via `ThrustCurveMotorSQLiteDatabase.validateDatabase()` before replacing the
    local file. This prevents installing a non-SQLite file or a database missing required tables/columns.

- Resource limits
  - Hard byte limits are enforced for:
    - remote metadata size
    - downloaded compressed size
    - decompressed database size
  - This reduces risk from oversized downloads and decompression bombs.

Limitations and threat model
----------------------------

- No offline authenticity without signatures
  - The SHA-256 value is read from the same server as the database file. If the server is compromised (or TLS is
    intercepted by a trusted/installed root CA), an attacker can serve a matching metadata+database pair.
  - For stronger assurance, add cryptographic signatures (e.g., Ed25519) over `metadata.json` or `motors.db.gz`, and
    verify using a public key embedded in OpenRocket.

- Parsing vulnerabilities
  - The update mechanism installs data files only, and does not execute code from the downloaded artifacts.
  - However, a malicious SQLite file could still attempt to exploit vulnerabilities in the SQLite/JDBC stack. Keeping
    dependencies up to date and adding signature verification are the strongest mitigations.

Testing
-------

- `core/src/test/java/info/openrocket/core/database/MotorDatabaseRemoteUpdaterTest.java` verifies SHA handling (compressed
  vs decompressed) and that mismatches are rejected.
- `core/src/test/java/info/openrocket/core/database/motor/InitialMotorsDatabaseFormatTest.java` ensures the bundled
  `initial_motors.db` can be validated and loaded.

