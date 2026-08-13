package edu.cnu.ced.app;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import edu.cnu.ced.geometry.GeometryService;
import edu.cnu.ced.geometry.GeometryStatus;
import edu.cnu.ced.magfield.MagneticFieldService;
import edu.cnu.ced.magfield.MagneticFieldStatus;
import edu.cnu.ced.resources.Clas12ResourceLocator;
import edu.cnu.ced.resources.Clas12Resources;
import edu.cnu.mdi.app.StartupWindow;
import edu.cnu.mdi.log.Log;

/** Performs heavy CED initialization before the main MDI hierarchy is constructed. */
final class CedBootstrap {
	private CedBootstrap() { }

	static CedBootstrapResult initialize(CedLaunchOptions options, StartupWindow startup) {
		Clas12Resources resources = locateResources(startup);
		MagneticFieldService fields = new MagneticFieldService();
		startup.status("Loading magnetic fields…");
		long started = System.nanoTime();
		MagneticFieldStatus fieldStatus = fields.initialize();
		long fieldMillis = elapsed(started);
		if (fieldStatus.initialized()) {
			Log.getInstance().config("Magnetic fields: " + fieldStatus.description() + " [torus="
					+ fieldStatus.torusMap() + ", solenoid=" + fieldStatus.solenoidMap() + ", " + fieldMillis + " ms]");
		} else Log.getInstance().warning("Magnetic fields unavailable: " + fieldStatus.error());

		GeometryService geometry = new GeometryService();
		startup.status("Loading detector geometry…");
		started = System.nanoTime();
		GeometryStatus geometryStatus = geometry.initialize(options.geometryVariation());
		long geometryMillis = elapsed(started);
		if (geometryStatus.initialized()) {
			Log.getInstance().config("Geometry initialized [cache=" + geometryStatus.cachedDetectors()
					+ ", source=" + geometryStatus.sourceDetectors() + ", " + geometryMillis + " ms]");
		} else Log.getInstance().warning("Geometry unavailable: " + geometryStatus.error());
		startup.status("Opening CED…");
		return new CedBootstrapResult(resources, fields, fieldStatus, geometry, geometryStatus);
	}

	private static Clas12Resources locateResources(StartupWindow startup) {
		startup.status("Locating CLAS12 resources…");
		try {
			Clas12Resources resources = Clas12ResourceLocator.locate();
			System.setProperty("CLAS12DIR", resources.root().toString());
			Log.getInstance().config("CLAS12 resources: " + resources.root());
			return resources;
		} catch (IOException exception) {
			Log.getInstance().warning(exception.getMessage());
			return null;
		}
	}

	private static long elapsed(long started) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started); }
}
