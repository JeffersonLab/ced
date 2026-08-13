package edu.cnu.ced.geometry.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

/** Coordinates cold/source and warm/cache detector initialization. */
public final class GeometryCacheCoordinator {

	private static final String CACHE_FILE = "geometry-cache.sqlite";
	private final Path cachePath;
	private final String applicationVersion;
	private final String geometryVariation;

	public GeometryCacheCoordinator(Path cachePath, String applicationVersion,
			String geometryVariation) {
		this.cachePath = cachePath.toAbsolutePath().normalize();
		this.applicationVersion = applicationVersion;
		this.geometryVariation = geometryVariation;
	}

	public static Path defaultCachePath() {
		return Path.of(System.getProperty("user.home"), ".ced", CACHE_FILE);
	}

	/** @return names loaded from SQLite; all others were initialized from source */
	public List<String> initialize(List<? extends CacheableGeometry> geometries)
			throws IOException, SQLException {
		try (SQLiteGeometryCache cache = new SQLiteGeometryCache(
				cachePath, applicationVersion, geometryVariation)) {
			cache.open();
			java.util.ArrayList<String> cached = new java.util.ArrayList<>();
			for (CacheableGeometry geometry : geometries) {
				if (cache.read(geometry)) {
					cached.add(geometry.name());
				} else {
					geometry.initializeFromSource();
					cache.write(geometry);
				}
			}
			return List.copyOf(cached);
		}
	}

	public boolean delete() throws IOException {
		boolean deleted = Files.deleteIfExists(cachePath);
		Files.deleteIfExists(Path.of(cachePath + "-wal"));
		Files.deleteIfExists(Path.of(cachePath + "-shm"));
		return deleted;
	}
}
