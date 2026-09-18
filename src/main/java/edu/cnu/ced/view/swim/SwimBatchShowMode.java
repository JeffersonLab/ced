package edu.cnu.ced.view.swim;

/** Which batch swim results {@link SwimBatchDrawer3D} draws, matching legacy CED's own {@code SHOW} enum. */
enum SwimBatchShowMode {
	SUCCESSES, FAILURES, ALL;

	boolean shows(SwimBatchResult result) {
		return switch (this) {
		case ALL -> true;
		case SUCCESSES -> result.success();
		case FAILURES -> !result.success();
		};
	}
}
