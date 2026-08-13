package edu.cnu.ced.view;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.List;

import edu.cnu.ced.event.EventNavigator;
import edu.cnu.mdi.container.IContainer;

/** Common six-sector laboratory-XY behavior for CED hexagonal views. */
@SuppressWarnings("serial")
public abstract class CedHexView extends CedXYView {

	public static final int SECTOR_COUNT = 6;

	protected CedHexView(EventNavigator navigator, Object... properties) {
		super(navigator, properties);
	}

	/** Return the one-based CLAS sector containing a laboratory XY point. */
	public static int sectorAt(Point2D.Double worldPoint) {
		if (worldPoint == null) return 0;
		double phi = normalizedPhi(worldPoint);
		// Suppress harmless trig roundoff at the exact 30-degree boundaries.
		phi = Math.rint(phi * 1.0e9) / 1.0e9;
		if (phi > 30.0 && phi <= 90.0) return 2;
		if (phi > 90.0 && phi <= 150.0) return 3;
		if (phi > 150.0 && phi <= 210.0) return 4;
		if (phi > 210.0 && phi <= 270.0) return 5;
		if (phi > 270.0 && phi <= 330.0) return 6;
		return 1;
	}

	/** Return azimuth in the conventional [0, 360) degree range. */
	public static double normalizedPhi(Point2D.Double worldPoint) {
		if (worldPoint == null) return Double.NaN;
		double phi = Math.toDegrees(Math.atan2(worldPoint.y, worldPoint.x));
		return phi < 0 ? phi + 360.0 : phi;
	}

	/** Rotate a sector-one laboratory XY point into another one-based sector. */
	protected static Point2D.Double rotateToSector(Point2D.Double sectorOnePoint, int sector) {
		if (sectorOnePoint == null) throw new IllegalArgumentException("point must not be null");
		if (sector < 1 || sector > SECTOR_COUNT) {
			throw new IllegalArgumentException("sector must be in [1, 6]");
		}
		double angle = Math.toRadians(60.0 * (sector - 1));
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		return new Point2D.Double(sectorOnePoint.x * cos - sectorOnePoint.y * sin,
				sectorOnePoint.x * sin + sectorOnePoint.y * cos);
	}

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint,
			Point2D.Double worldPoint, List<String> feedback) {
		super.getFeedbackStrings(container, screenPoint, worldPoint, feedback);
		int sector = sectorAt(worldPoint);
		if (sector > 0) feedback.add("$orange$sector " + sector);
	}
}
