package edu.cnu.ced.magfield;

/** Immutable result of magnetic-field initialization. */
public record MagneticFieldStatus(boolean initialized, String description,
		String torusMap, String solenoidMap, String error) {

	public MagneticFieldStatus {
		description = text(description);
		torusMap = text(torusMap);
		solenoidMap = text(solenoidMap);
		error = text(error);
	}

	public static MagneticFieldStatus failed(Throwable cause) {
		String message = cause == null ? "Unknown magnetic-field initialization failure"
				: cause.getMessage();
		return new MagneticFieldStatus(false, "", "", "",
				message == null || message.isBlank() ? cause.getClass().getSimpleName() : message);
	}

	private static String text(String value) {
		return value == null ? "" : value;
	}
}
