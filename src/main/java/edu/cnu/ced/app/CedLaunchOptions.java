package edu.cnu.ced.app;

import java.util.Locale;

/** Immutable command-line and system-property configuration for a CED launch. */
public record CedLaunchOptions(boolean experimental, boolean enable3D,
		String geometryVariation) {

	/** Default coatjava geometry variation. */
	public static final String DEFAULT_GEOMETRY_VARIATION = "default";

	/**
	 * Parse legacy-compatible CED command-line options.
	 *
	 * <p>The former {@code -p directory} option is accepted and ignored because
	 * MDI file dialogs remember their own locations.</p>
	 *
	 * @param args command-line arguments, possibly {@code null}
	 * @return parsed immutable options
	 */
	public static CedLaunchOptions parse(String[] args) {
		boolean experimental = false;
		boolean enable3D = true;

		if (args != null) {
			for (int index = 0; index < args.length; index++) {
				String argument = args[index];
				if (argument == null) {
					continue;
				}

				String normalized = argument.trim().toUpperCase(Locale.ROOT);
				if ("-P".equals(normalized)) {
					if (index + 1 < args.length) {
						index++;
					}
				} else if (normalized.contains("EXP")) {
					experimental = true;
				} else if (normalized.contains("NO3D")) {
					enable3D = false;
				}
			}
		}

		return new CedLaunchOptions(experimental, enable3D,
				normalizeVariation(System.getProperty("GEOVARIATION")));
	}

	private static String normalizeVariation(String variation) {
		if (variation == null || variation.isBlank()) {
			return DEFAULT_GEOMETRY_VARIATION;
		}
		return variation.trim();
	}
}
