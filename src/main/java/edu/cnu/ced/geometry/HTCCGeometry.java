package edu.cnu.ced.geometry;

import java.awt.geom.Point2D;

/** Lightweight analytic HTCC geometry used by the sector projections. */
public final class HTCCGeometry {
	public static final int RING_COUNT = 4;
	public static final int HALF_COUNT = 2;
	private static final double INNER_RADIUS = 150.0;
	private static final double THICKNESS = 5.0;
	private static final double START_ANGLE = 5.0;
	private static final double RING_ANGLE = 7.5;

	/** Returns world {@code (z, signed transverse)} polygon coordinates in cm. */
	public Point2D.Double[] polygon(int sector, int ring, int half, double phiOffsetDegrees) {
		if (sector < 1 || sector > 6 || ring < 1 || ring > RING_COUNT
				|| half < 1 || half > HALF_COUNT) return new Point2D.Double[0];
		double r1 = INNER_RADIUS + (half - 1) * THICKNESS;
		double r2 = r1 + THICKNESS;
		double theta1 = Math.toRadians(START_ANGLE + (ring - 1) * RING_ANGLE);
		double theta2 = theta1 + Math.toRadians(RING_ANGLE);
		double cosinePhi = Math.cos(Math.toRadians(phiOffsetDegrees));
		if (Math.abs(cosinePhi) < 1.0e-8) return new Point2D.Double[0];
		double sign = sector <= 3 ? 1.0 : -1.0;
		return new Point2D.Double[] {
				point(r1, theta1, sign, cosinePhi), point(r2, theta1, sign, cosinePhi),
				point(r2, theta2, sign, cosinePhi), point(r1, theta2, sign, cosinePhi)};
	}

	private static Point2D.Double point(double radius, double theta,
			double sign, double cosinePhi) {
		return new Point2D.Double(radius * Math.cos(theta),
				sign * radius * Math.sin(theta) / cosinePhi);
	}
}
