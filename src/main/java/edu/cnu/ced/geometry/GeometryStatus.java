package edu.cnu.ced.geometry;

import java.util.List;

/** Immutable result of detector geometry initialization. */
public record GeometryStatus(boolean initialized, List<String> cachedDetectors,
		List<String> sourceDetectors, String error) {

	public GeometryStatus {
		cachedDetectors = cachedDetectors == null ? List.of() : List.copyOf(cachedDetectors);
		sourceDetectors = sourceDetectors == null ? List.of() : List.copyOf(sourceDetectors);
		error = error == null ? "" : error;
	}
}
