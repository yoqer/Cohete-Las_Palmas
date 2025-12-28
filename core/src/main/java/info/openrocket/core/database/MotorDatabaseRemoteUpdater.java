package info.openrocket.core.database;

import info.openrocket.core.communication.ConnectionSource;
import info.openrocket.core.communication.DefaultConnectionSource;
import info.openrocket.core.util.FileUtils;
import info.openrocket.core.util.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/**
 * Utility class to fetch and install the remote motor database.
 *
 * @author Sibo Van Gool <sibo.vangool@hotmail.com>
 */
public class MotorDatabaseRemoteUpdater {
	private static final Logger log = LoggerFactory.getLogger(MotorDatabaseRemoteUpdater.class);

	public static final String METADATA_URL = "https://openrocket.info/motor-database/metadata.json";
	public static final String DATABASE_GZ_URL = "https://openrocket.info/motor-database/motors.db.gz";

	private static final int CONNECT_TIMEOUT_MS = 10_000;
	private static final int READ_TIMEOUT_MS = 30_000;

	private static final String MOTORS_DB_FILENAME = "motors.db";
	private static final String METADATA_FILENAME = "metadata.json";

	private final ConnectionSource connectionSource;

	public MotorDatabaseRemoteUpdater() {
		this(new DefaultConnectionSource());
	}

	public MotorDatabaseRemoteUpdater(ConnectionSource connectionSource) {
		this.connectionSource = connectionSource;
	}

	/**
	 * Fetch the remote motor database metadata from the default URL.
	 * @return the remote motor database metadata
	 * @throws IOException if an I/O error occurs
	 */
	public MotorDatabaseMetadata fetchRemoteMetadata() throws IOException {
		return fetchRemoteMetadata(METADATA_URL);
	}

	/**
	 * Fetch the remote motor database metadata from the given URL.
	 * @param metadataUrl the URL of the remote motor database metadata
	 * @return the remote motor database metadata
	 * @throws IOException if an I/O error occurs
	 */
	public MotorDatabaseMetadata fetchRemoteMetadata(String metadataUrl) throws IOException {
		try (InputStream in = openStream(metadataUrl)) {
			return MotorDatabaseMetadata.parse(in);
		}
	}

	/**
	 * Check if the remote motor database is newer than the local one.
	 * @param motorLibraryDir the motor library directory
	 * @param remoteMetadata the remote motor database metadata
	 * @return true if the remote database is newer, false otherwise
	 */
	public boolean isRemoteNewer(File motorLibraryDir, MotorDatabaseMetadata remoteMetadata) {
		if (motorLibraryDir == null || remoteMetadata == null) {
			return false;
		}

		File localMetadataFile = new File(motorLibraryDir, METADATA_FILENAME);
		long localVersion = 0;
		if (localMetadataFile.isFile()) {
			try {
				localVersion = MotorDatabaseMetadataIO.readDatabaseVersion(localMetadataFile);
			} catch (IOException e) {
				log.debug("Unable to read local motor database metadata from {}: {}",
						localMetadataFile.getAbsolutePath(), e.getMessage());
			}
		}

		return remoteMetadata.getDatabaseVersion() > localVersion;
	}

	/**
	 * Install the remote motor database into the given motor library directory.
	 * @param motorLibraryDir the motor library directory
	 * @param remoteMetadata the remote motor database metadata
	 * @throws IOException if an I/O error occurs
	 */
	public void installRemoteDatabase(File motorLibraryDir, MotorDatabaseMetadata remoteMetadata) throws IOException {
		installRemoteDatabase(motorLibraryDir, remoteMetadata, DATABASE_GZ_URL);
	}

	/**
	 * Install the remote motor database into the given motor library directory.
	 * @param motorLibraryDir the motor library directory
	 * @param remoteMetadata the remote motor database metadata
	 * @param databaseGzUrl the URL of the remote motor database GZIP archive
	 * @throws IOException if an I/O error occurs
	 */
	public void installRemoteDatabase(File motorLibraryDir, MotorDatabaseMetadata remoteMetadata, String databaseGzUrl)
			throws IOException {
		if (motorLibraryDir == null) {
			throw new IOException("Motor library directory is null");
		}
		if (!motorLibraryDir.exists() && !motorLibraryDir.mkdirs()) {
			throw new IOException("Unable to create motor library directory: " + motorLibraryDir.getAbsolutePath());
		}
		if (remoteMetadata == null) {
			throw new IOException("Remote metadata is null");
		}

		File targetDbFile = new File(motorLibraryDir, MOTORS_DB_FILENAME);
		File targetMetadataFile = new File(motorLibraryDir, METADATA_FILENAME);

		File tmpDbFile = new File(motorLibraryDir, MOTORS_DB_FILENAME + ".download");
		File tmpMetadataFile = new File(motorLibraryDir, METADATA_FILENAME + ".download");

		log.info("Downloading motor database from {}", databaseGzUrl);
		String expectedSha256 = normalizeSha256(remoteMetadata.getSha256());

		try (InputStream rawIn = openStream(databaseGzUrl);
			 FileOutputStream out = new FileOutputStream(tmpDbFile)) {

			if (expectedSha256 == null) {
				try (GZIPInputStream gzIn = new GZIPInputStream(rawIn)) {
					FileUtils.copy(gzIn, out);
				}
			} else {
				MessageDigest gzipDigest = sha256Digest();
				MessageDigest dbDigest = sha256Digest();
				String actualGzipSha256;
				String actualDbSha256;

				try (DigestInputStream gzipDigestStream = new DigestInputStream(rawIn, gzipDigest);
					 GZIPInputStream gzIn = new GZIPInputStream(gzipDigestStream);
					 DigestInputStream dbDigestStream = new DigestInputStream(gzIn, dbDigest)) {
					FileUtils.copy(dbDigestStream, out);
				}

				actualGzipSha256 = TextUtil.bytesToHex(gzipDigest.digest());
				actualDbSha256 = TextUtil.bytesToHex(dbDigest.digest());

				// The published SHA-256 may refer to either the compressed archive or the decompressed database.
				if (!expectedSha256.equals(actualGzipSha256) && !expectedSha256.equals(actualDbSha256)) {
					throw new IOException("Downloaded motor database SHA-256 mismatch (expected " + expectedSha256 +
							", got gzip=" + actualGzipSha256 + ", db=" + actualDbSha256 + ")");
				}
			}
		} catch (IOException e) {
			if (tmpDbFile.isFile() && !tmpDbFile.delete()) {
				log.debug("Unable to delete partial download {}", tmpDbFile.getAbsolutePath());
			}
			throw e;
		}

		MotorDatabaseMetadataIO.writeRawJson(tmpMetadataFile, remoteMetadata.getRawJson());

		FileUtils.replaceFile(tmpDbFile, targetDbFile);
		FileUtils.replaceFile(tmpMetadataFile, targetMetadataFile);
		log.info("Installed updated motor database (database_version={})", remoteMetadata.getDatabaseVersion());
	}

	/**
	 * Open an InputStream to the given URL.
	 * @param url the URL
	 * @return the InputStream
	 * @throws IOException if an I/O error occurs
	 */
	private InputStream openStream(String url) throws IOException {
		HttpURLConnection connection = connectionSource.getConnection(url);
		connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(READ_TIMEOUT_MS);
		connection.setRequestProperty("User-Agent", "OpenRocket");
		connection.setRequestProperty("Accept", "application/json, */*");
		connection.connect();

		int code = connection.getResponseCode();
		if (code < 200 || code >= 300) {
			throw new IOException("HTTP " + code + " when fetching " + url);
		}

		return connection.getInputStream();
	}

	/**
	 * Create a SHA-256 MessageDigest instance.
	 * @return the MessageDigest
	 * @throws IOException if SHA-256 is not supported
	 */
	private static MessageDigest sha256Digest() throws IOException {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("SHA-256 digest not supported by JRE", e);
		}
	}

	/**
	 * Normalize a SHA-256 string: trim whitespace and convert to lowercase.
	 * @param sha256 the SHA-256 string
	 * @return the normalized string, or null if the input was null or empty
	 */
	private static String normalizeSha256(String sha256) {
		if (sha256 == null) {
			return null;
		}
		String trimmed = sha256.trim().toLowerCase(Locale.ENGLISH);
		return trimmed.isEmpty() ? null : trimmed;
	}
}
