package edu.cnu.ced.geometry.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** SQLite storage for versioned detector-specific primitive geometry payloads. */
public final class SQLiteGeometryCache implements AutoCloseable {

	private static final int SCHEMA_VERSION = 1;
	private final Path path;
	private final String applicationVersion;
	private final String geometryVariation;
	private Connection connection;

	public SQLiteGeometryCache(Path path, String applicationVersion, String geometryVariation) {
		this.path = path.toAbsolutePath().normalize();
		this.applicationVersion = applicationVersion;
		this.geometryVariation = geometryVariation;
	}

	public void open() throws IOException, SQLException {
		Path parent = path.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		connection = DriverManager.getConnection("jdbc:sqlite:" + path);
		createSchema();
		if (!metadataMatches()) {
			close();
			Files.deleteIfExists(path);
			connection = DriverManager.getConnection("jdbc:sqlite:" + path);
			createSchema();
			writeMetadata();
		}
	}

	public boolean read(CacheableGeometry geometry) throws SQLException, IOException {
		String sql = "SELECT format_version, payload FROM geometry_payload WHERE detector_name = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, geometry.name());
			try (ResultSet results = statement.executeQuery()) {
				if (!results.next() || results.getInt(1) != geometry.formatVersion()) {
					return false;
				}
				try (DataInputStream input = new DataInputStream(
						new ByteArrayInputStream(results.getBytes(2)))) {
					geometry.read(input);
					return input.available() == 0;
				}
			}
		}
	}

	public void write(CacheableGeometry geometry) throws SQLException, IOException {
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				DataOutputStream output = new DataOutputStream(bytes)) {
			geometry.write(output);
			output.flush();
			try (PreparedStatement statement = connection.prepareStatement(
					"INSERT OR REPLACE INTO geometry_payload"
					+ " (detector_name, format_version, payload) VALUES (?, ?, ?)")) {
				statement.setString(1, geometry.name());
				statement.setInt(2, geometry.formatVersion());
				statement.setBytes(3, bytes.toByteArray());
				statement.executeUpdate();
			}
		}
	}

	private void createSchema() throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS metadata"
					+ " (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS geometry_payload"
					+ " (detector_name TEXT PRIMARY KEY, format_version INTEGER NOT NULL,"
					+ " payload BLOB NOT NULL)");
		}
	}

	private boolean metadataMatches() throws SQLException {
		return Integer.toString(SCHEMA_VERSION).equals(readMetadata("schema_version"))
				&& applicationVersion.equals(readMetadata("application_version"))
				&& geometryVariation.equals(readMetadata("geometry_variation"));
	}

	private String readMetadata(String key) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT value FROM metadata WHERE key = ?")) {
			statement.setString(1, key);
			try (ResultSet results = statement.executeQuery()) {
				return results.next() ? results.getString(1) : null;
			}
		}
	}

	private void writeMetadata() throws SQLException {
		writeMetadata("schema_version", Integer.toString(SCHEMA_VERSION));
		writeMetadata("application_version", applicationVersion);
		writeMetadata("geometry_variation", geometryVariation);
	}

	private void writeMetadata(String key, String value) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"INSERT OR REPLACE INTO metadata (key, value) VALUES (?, ?)")) {
			statement.setString(1, key);
			statement.setString(2, value);
			statement.executeUpdate();
		}
	}

	@Override
	public void close() throws SQLException {
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}
}
