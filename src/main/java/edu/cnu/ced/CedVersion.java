package edu.cnu.ced;

/** Version information for the MDI-based CED application. */
public final class CedVersion {

	/** Human-readable application name. */
	public static final String APPLICATION_NAME = "CED";

	/** Development version for the new MDI application. */
	public static final String VERSION = "2.0.0-SNAPSHOT";

	private CedVersion() {
	}

	/** @return title displayed by the main application frame */
	public static String title() {
		return APPLICATION_NAME + " " + VERSION;
	}
}
