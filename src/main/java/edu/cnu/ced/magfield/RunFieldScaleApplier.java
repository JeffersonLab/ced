package edu.cnu.ced.magfield;

import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.Solenoid;
import cnuphys.magfield.Torus;

import edu.cnu.ced.event.RunConfig;

/**
 * Applies each run's own torus/solenoid field-scale factors (from the
 * {@code RUN::config} bank) to the active magnetic field the moment the run
 * number changes.
 * <p>
 * Mirrors bCNU CED's {@code ClasIoEventManager.updateRunData()}, which
 * re-reads {@code RUN::config} on every event and applies its torus/
 * solenoid scale only when the run number actually changes. Without this,
 * the field maps keep whatever scale factor happens to be baked into the
 * loaded field-map file itself, which for many real runs is wrong for the
 * data actually being displayed -- e.g. a run recorded with reversed
 * solenoid polarity needs scale -1, not the file's own default. That
 * mismatch bends every charged trajectory in the wrong direction, and for
 * a nearly-on-axis forward track it most severely distorts the segment
 * closest to the vertex, where the solenoid (not the torus) dominates.
 * </p>
 * <p>
 * Legacy calls {@code MagneticFields.changeFieldsAndMenus(torus, solenoid)}
 * for this, but that method unconditionally touches {@code MagneticFields}'
 * own Swing scale-factor menu panels (e.g. {@code _scaleTorusPanel.fixText()}
 * with no null check) -- safe for legacy, which builds that menu, but an
 * NPE here, since this application never does. Setting the scale directly
 * on {@link MagneticFields#getTorus()}/{@link MagneticFields#getSolenoid()}
 * gets the same effect (including notifying registered
 * {@code MagneticFieldChangeListener}s, via {@code MagneticField
 * .setScaleFactor}'s own call to {@code MagneticFields.changedScale}, which
 * *does* null-check its menu panels) without going anywhere near the menu.
 * </p>
 */
public final class RunFieldScaleApplier {

	@FunctionalInterface
	interface FieldScaleSink {
		void apply(double torusScale, double solenoidScale);
	}

	private final FieldScaleSink sink;
	private int lastAppliedRun = Integer.MIN_VALUE;

	public RunFieldScaleApplier() {
		this(RunFieldScaleApplier::applyToActiveField);
	}

	RunFieldScaleApplier(FieldScaleSink sink) {
		this.sink = sink;
	}

	/**
	 * Applies {@code config}'s torus/solenoid scale, but only if its run
	 * differs from the last run this applier actually applied to the field
	 * -- matching legacy's "only on run change" guard, so this is cheap to
	 * call on every event rather than just once per file open (the run
	 * number, in principle, could change mid-stream).
	 *
	 * @param config the current event's {@code RUN::config} row, or
	 *               {@code null} if the bank isn't present/valid
	 */
	public void apply(RunConfig config) {
		if (config == null || config.run() == lastAppliedRun) return;
		lastAppliedRun = config.run();
		sink.apply(config.torus(), config.solenoid());
	}

	private static void applyToActiveField(double torusScale, double solenoidScale) {
		MagneticFields fields = MagneticFields.getInstance();
		Torus torus = fields.getTorus();
		if (torus != null) torus.setScaleFactor(torusScale);
		Solenoid solenoid = fields.getSolenoid();
		if (solenoid != null) solenoid.setScaleFactor(solenoidScale);
	}
}
