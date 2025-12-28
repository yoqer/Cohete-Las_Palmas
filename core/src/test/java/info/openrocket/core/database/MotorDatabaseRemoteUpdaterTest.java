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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.security.MessageDigest;
import java.util.zip.GZIPOutputStream;

public class MotorDatabaseRemoteUpdaterTest {

	@TempDir
	Path tempDir;

	@Test
	public void testInstallAcceptsGzipSha256() throws Exception {
		byte[] dbBytes = createValidMotorDbBytes();
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
		byte[] dbBytes = createValidMotorDbBytes();
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
		byte[] dbBytes = createValidMotorDbBytes();
		byte[] gzBytes = gzip(dbBytes);

		MotorDatabaseMetadata metadata = MotorDatabaseMetadata.parse(new ByteArrayInputStream((
				"{\"schema_version\":2,\"database_version\":123,\"sha256\":\"0000000000000000000000000000000000000000000000000000000000000000\"}").getBytes()));

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

	private byte[] createValidMotorDbBytes() throws Exception {
		Class.forName("org.sqlite.JDBC");
		File dbFile = tempDir.resolve("motors.db").toFile();
		try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
			 Statement stmt = connection.createStatement()) {
			stmt.execute("PRAGMA foreign_keys = ON");
			stmt.execute("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
			stmt.execute("INSERT INTO meta (key, value) VALUES ('schema_version', '2')");

			stmt.execute("CREATE TABLE manufacturers (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"name TEXT NOT NULL UNIQUE, " +
					"abbrev TEXT" +
					")");

			stmt.execute("CREATE TABLE motors (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"manufacturer_id INTEGER NOT NULL, " +
					"tc_motor_id TEXT, " +
					"designation TEXT NOT NULL, " +
					"common_name TEXT, " +
					"impulse_class TEXT, " +
					"diameter REAL, " +
					"length REAL, " +
					"total_impulse REAL, " +
					"avg_thrust REAL, " +
					"max_thrust REAL, " +
					"burn_time REAL, " +
					"propellant_weight REAL, " +
					"total_weight REAL, " +
					"type TEXT, " +
					"delays TEXT, " +
					"case_info TEXT, " +
					"prop_info TEXT, " +
					"sparky INTEGER, " +
					"info_url TEXT, " +
					"data_files INTEGER, " +
					"updated_on TEXT, " +
					"FOREIGN KEY (manufacturer_id) REFERENCES manufacturers(id)" +
					")");

			stmt.execute("CREATE TABLE thrust_curves (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"motor_id INTEGER NOT NULL, " +
					"tc_simfile_id TEXT, " +
					"source TEXT, " +
					"format TEXT, " +
					"license TEXT, " +
					"info_url TEXT, " +
					"data_url TEXT, " +
					"total_impulse REAL, " +
					"avg_thrust REAL, " +
					"max_thrust REAL, " +
					"burn_time REAL, " +
					"FOREIGN KEY (motor_id) REFERENCES motors(id) ON DELETE CASCADE" +
					")");

			stmt.execute("CREATE TABLE thrust_data (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"curve_id INTEGER NOT NULL, " +
					"time_seconds REAL NOT NULL, " +
					"force_newtons REAL NOT NULL, " +
					"FOREIGN KEY (curve_id) REFERENCES thrust_curves(id) ON DELETE CASCADE" +
					")");
		}

		return Files.readAllBytes(dbFile.toPath());
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
