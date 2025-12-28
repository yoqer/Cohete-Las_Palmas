package info.openrocket.core.database;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Parsed metadata for the motor database.
 */
public class MotorDatabaseMetadata {
	private static final String KEY_SCHEMA_VERSION = "schema_version";
	private static final String KEY_DATABASE_VERSION = "database_version";
	private static final String KEY_SHA256 = "sha256";
	private static final String KEY_DOWNLOAD_URL = "download_url";
	private static final String KEY_GENERATED_AT = "generated_at";

	private final int schemaVersion;
	private final long databaseVersion;
	private final String sha256;
	private final String downloadUrl;
	private final String generatedAt;
	private final String rawJson;

	private MotorDatabaseMetadata(int schemaVersion, long databaseVersion, String sha256, String downloadUrl,
			String generatedAt, String rawJson) {
		this.schemaVersion = schemaVersion;
		this.databaseVersion = databaseVersion;
		this.sha256 = sha256;
		this.downloadUrl = downloadUrl;
		this.generatedAt = generatedAt;
		this.rawJson = rawJson;
	}

	public int getSchemaVersion() {
		return schemaVersion;
	}

	public long getDatabaseVersion() {
		return databaseVersion;
	}

	public String getSha256() {
		return sha256;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public String getGeneratedAt() {
		return generatedAt;
	}

	public String getRawJson() {
		return rawJson;
	}

	public static MotorDatabaseMetadata parse(InputStream is) throws IOException {
		String jsonContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject json = JsonParser.parseString(jsonContent).getAsJsonObject();

		if (!json.has(KEY_DATABASE_VERSION)) {
			throw new IOException("metadata.json does not contain " + KEY_DATABASE_VERSION);
		}

		int schemaVersion = json.has(KEY_SCHEMA_VERSION) ? json.get(KEY_SCHEMA_VERSION).getAsInt() : 0;
		long databaseVersion = json.get(KEY_DATABASE_VERSION).getAsLong();
		String sha256 = json.has(KEY_SHA256) ? json.get(KEY_SHA256).getAsString() : null;
		String downloadUrl = json.has(KEY_DOWNLOAD_URL) ? json.get(KEY_DOWNLOAD_URL).getAsString() : null;
		String generatedAt = json.has(KEY_GENERATED_AT) ? json.get(KEY_GENERATED_AT).getAsString() : null;

		return new MotorDatabaseMetadata(schemaVersion, databaseVersion, sha256, downloadUrl, generatedAt, jsonContent);
	}
}

