package edu.cnu.ced.geometry;

import java.util.ArrayList;
import java.util.List;

import edu.cnu.ced.CedVersion;
import edu.cnu.ced.geometry.cache.CacheableGeometry;
import edu.cnu.ced.geometry.cache.GeometryCacheCoordinator;

/** Owns initialization and publication of migrated detector geometry. */
public final class GeometryService {

	private final CTOFGeometry ctof = new CTOFGeometry();
	private final FTCALGeometry ftcal = new FTCALGeometry();

	public CTOFGeometry ctof() {
		return ctof;
	}

	public FTCALGeometry ftcal() { return ftcal; }

	public GeometryStatus initialize(String variation) {
		List<CacheableGeometry> geometries = List.of(ctof, ftcal);
		try {
			List<String> cached = new GeometryCacheCoordinator(
					GeometryCacheCoordinator.defaultCachePath(), CedVersion.VERSION, variation)
					.initialize(geometries);
			ArrayList<String> source = new ArrayList<>();
			for (CacheableGeometry geometry : geometries) {
				if (!cached.contains(geometry.name())) {
					source.add(geometry.name());
				}
			}
			return new GeometryStatus(true, cached, source, "");
		} catch (Exception exception) {
			return new GeometryStatus(false, List.of(), List.of(), message(exception));
		}
	}

	private static String message(Exception exception) {
		String message = exception.getMessage();
		return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
	}
}
