package edu.cnu.ced.app;

import edu.cnu.ced.geometry.GeometryService;
import edu.cnu.ced.geometry.GeometryStatus;
import edu.cnu.ced.magfield.MagneticFieldService;
import edu.cnu.ced.magfield.MagneticFieldStatus;
import edu.cnu.ced.resources.Clas12Resources;

/** Services and status prepared before construction of the main CED window. */
public record CedBootstrapResult(Clas12Resources resources,
		MagneticFieldService magneticFields, MagneticFieldStatus magneticFieldStatus,
		GeometryService geometry, GeometryStatus geometryStatus) { }
