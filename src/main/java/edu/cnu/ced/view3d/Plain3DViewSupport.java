package edu.cnu.ced.view3d;

import java.util.Arrays;

import edu.cnu.mdi.mdi3D.view3D.PlainView3D;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Shared key/value preparation for every CED 3D view built on {@link
 * PlainView3D} -- both {@link CedView3D} subclasses and any standalone,
 * non-event-driven {@code PlainView3D} built directly (e.g. a testing
 * view with no {@code EventNavigator} to bridge).
 */
public final class Plain3DViewSupport {

	/** Default fraction of the main application window a 3D view sizes itself to, absent an explicit FRACTION. */
	private static final double DEFAULT_SIZE_FRACTION = 0.75;

	private Plain3DViewSupport() {
	}

	/**
	 * Prepares a subclass's key/value pairs before they reach {@link
	 * PlainView3D}'s constructor -- the last point any code of ours runs
	 * before that constructor's own {@code make3DPanel(...)} call
	 * eventually constructs a {@code Panel3D} on the EDT.
	 *
	 * <p>
	 * First waits for {@link GLWarmup#awaitReady(long)}: without this, the
	 * very first {@code Panel3D} ever constructed triggers JOGL's first GL
	 * context creation synchronously on the EDT, which on macOS can
	 * deadlock against the AppKit main thread (see {@link GLWarmup}'s own
	 * javadoc). {@link GLWarmup#start()} is called once, early in {@code
	 * main()}, well before this could ever run; five seconds is generous
	 * headroom for a warm-up that normally finishes in well under one.
	 * </p>
	 *
	 * <p>
	 * Then appends {@code USECONTAINER=false}: {@link PlainView3D#
	 * resolveContainer} always returns {@code null} (3D views have no 2D
	 * {@code IContainer}), but {@code BaseView}'s own constructor only
	 * skips using that {@code null} when told not to use a container at
	 * all; its "use container" flag otherwise defaults {@code true} and
	 * unconditionally dereferences the result, crashing every 3D CED view
	 * that forgets to pass this. Appended last so it always wins even if a
	 * caller's own {@code keyVals} happens to set it too.
	 * </p>
	 *
	 * <p>
	 * Finally, unless the caller's own {@code keyVals} already set {@code
	 * FRACTION}, defaults it to {@value #DEFAULT_SIZE_FRACTION} -- without
	 * an explicit {@code WIDTH}/{@code HEIGHT} or {@code FRACTION},
	 * {@code BaseView} falls back to a fixed, small 400x300, which is not
	 * a usable size for a 3D scene.
	 * </p>
	 */
	public static Object[] prepareKeyVals(Object[] keyVals) {
		GLWarmup.awaitReady(5000);
		boolean hasFraction = false;
		for (int i = 0; i < keyVals.length - 1; i += 2) {
			if (PropertyUtils.FRACTION.equals(keyVals[i])) {
				hasFraction = true;
				break;
			}
		}
		Object[] combined = Arrays.copyOf(keyVals, keyVals.length + (hasFraction ? 2 : 4));
		int i = keyVals.length;
		combined[i++] = PropertyUtils.USECONTAINER;
		combined[i++] = false;
		if (!hasFraction) {
			combined[i++] = PropertyUtils.FRACTION;
			combined[i++] = DEFAULT_SIZE_FRACTION;
		}
		return combined;
	}
}
