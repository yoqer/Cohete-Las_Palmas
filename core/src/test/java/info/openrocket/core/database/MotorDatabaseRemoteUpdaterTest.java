package info.openrocket.core.database;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import info.openrocket.core.communication.ConnectionSourceStub;
import info.openrocket.core.communication.HttpURLConnectionMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.zip.GZIPOutputStream;

public class MotorDatabaseRemoteUpdaterTest {

	@TempDir
	Path tempDir;

	@Test
	public void testInstallAcceptsGzipSha256() throws Exception {
		byte[] dbBytes = "db-bytes".getBytes();
		byte[] gzBytes = gzip(dbBytes);
		String gzSha = sha256Hex(gzBytes);

		MotorDatabaseMetadata metadata = MotorDatabaseMetadata.parse(new ByteArrayInputStream((
				"{\"schema_version\":2,\"database_version\":123,\"sha256\":\"" + gzSha + "\"}").getBytes()));

		HttpURLConnectionMock connection = new HttpURLConnectionMock();
		connection.setDoInput(true);
		connection.setResponseCode(200);
		connection.setContent(gzBytes);

		MotorDatabaseRemoteUpdater updater = new MotorDatabaseRemoteUpdater(new ConnectionSourceStub(connection));
		updater.installRemoteDatabase(tempDir.toFile(), metadata, "http://example/motors.db.gz");

		byte[] installed = Files.readAllBytes(new File(tempDir.toFile(), "motors.db").toPath());
		assertArrayEquals(dbBytes, installed);
	}

	@Test
	public void testInstallAcceptsDbSha256() throws Exception {
		byte[] dbBytes = "db-bytes-2".getBytes();
		byte[] gzBytes = gzip(dbBytes);
		String dbSha = sha256Hex(dbBytes);

		MotorDatabaseMetadata metadata = MotorDatabaseMetadata.parse(new ByteArrayInputStream((
				"{\"schema_version\":2,\"database_version\":123,\"sha256\":\"" + dbSha + "\"}").getBytes()));

		HttpURLConnectionMock connection = new HttpURLConnectionMock();
		connection.setDoInput(true);
		connection.setResponseCode(200);
		connection.setContent(gzBytes);

		MotorDatabaseRemoteUpdater updater = new MotorDatabaseRemoteUpdater(new ConnectionSourceStub(connection));
		updater.installRemoteDatabase(tempDir.toFile(), metadata, "http://example/motors.db.gz");

		byte[] installed = Files.readAllBytes(new File(tempDir.toFile(), "motors.db").toPath());
		assertArrayEquals(dbBytes, installed);
	}

	@Test
	public void testInstallRejectsMismatchedSha256() throws Exception {
		byte[] dbBytes = "db-bytes-3".getBytes();
		byte[] gzBytes = gzip(dbBytes);

		MotorDatabaseMetadata metadata = MotorDatabaseMetadata.parse(new ByteArrayInputStream((
				"{\"schema_version\":2,\"database_version\":123,\"sha256\":\"deadbeef\"}").getBytes()));

		HttpURLConnectionMock connection = new HttpURLConnectionMock();
		connection.setDoInput(true);
		connection.setResponseCode(200);
		connection.setContent(gzBytes);

		MotorDatabaseRemoteUpdater updater = new MotorDatabaseRemoteUpdater(new ConnectionSourceStub(connection));
		assertThrows(Exception.class,
				() -> updater.installRemoteDatabase(tempDir.toFile(), metadata, "http://example/motors.db.gz"));
	}

	private static byte[] gzip(byte[] content) throws Exception {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try (GZIPOutputStream gzOut = new GZIPOutputStream(bout)) {
			gzOut.write(content);
		}
		return bout.toByteArray();
	}

	private static String sha256Hex(byte[] content) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(content);
		StringBuilder sb = new StringBuilder(hash.length * 2);
		for (byte b : hash) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}

