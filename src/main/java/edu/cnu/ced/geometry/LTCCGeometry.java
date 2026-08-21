package edu.cnu.ced.geometry;

import java.awt.geom.Point2D;

/** Lightweight analytic LTCC geometry used by the sector projections. */
public final class LTCCGeometry {
	public static final int RING_COUNT = 18;
	public static final int HALF_COUNT = 2;
	private static final double THICKNESS = 8.0;
	private static final double START_RADIUS = 670.0;
	private static final double START_ANGLE = Math.toRadians(3.7);
	private static final double END_ANGLE = Math.toRadians(34.7);
	private static final double SLOPE = Math.tan(Math.toRadians(25.0));
	private static final double X0 = START_RADIUS * Math.cos(START_ANGLE);
	private static final double Y0 = START_RADIUS * Math.sin(START_ANGLE);
	private static final double END_RADIUS = (X0 + Y0 * SLOPE)
			/ (Math.sin(END_ANGLE) * SLOPE + Math.cos(END_ANGLE));
	private static final double DX = (END_RADIUS * Math.cos(END_ANGLE) - X0) / RING_COUNT;
	private static final double DY = (END_RADIUS * Math.sin(END_ANGLE) - Y0) / RING_COUNT;

	/** Returns world {@code (z, signed transverse)} polygon coordinates in cm. */
	public Point2D.Double[] polygon(int sector, int ring, int half) {
		if (sector < 1 || sector > 6 || ring < 1 || ring > RING_COUNT
				|| half < 1 || half > HALF_COUNT) return new Point2D.Double[0];
		double x0 = X0 + (ring - 1) * DX;
		double y0 = Y0 + (ring - 1) * DY;
		double x1 = x0 + DX;
		double y1 = y0 + DY;
		double theta0 = Math.atan2(y0, x0);
		double theta1 = Math.atan2(y1, x1);
		double tx0 = THICKNESS * Math.cos(theta0);
		double ty0 = THICKNESS * Math.sin(theta0);
		double tx1 = THICKNESS * Math.cos(theta1);
		double ty1 = THICKNESS * Math.sin(theta1);
		if (half == 2) { x0 += tx0; y0 += ty0; x1 += tx1; y1 += ty1; }
		double sign = sector <= 3 ? 1.0 : -1.0;
		return new Point2D.Double[] {new Point2D.Double(x0, sign * y0),
				new Point2D.Double(x1, sign * y1), new Point2D.Double(x1 + tx1, sign * (y1 + ty1)),
				new Point2D.Double(x0 + tx0, sign * (y0 + ty0))};
	}
}
