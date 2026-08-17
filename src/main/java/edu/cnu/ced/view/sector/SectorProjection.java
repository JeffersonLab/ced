package edu.cnu.ced.view.sector;

import java.awt.geom.Point2D;

import edu.cnu.ced.geometry.DCGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.geometry.Segment3;

/** Projects DC wire geometry onto a selectable radial plane. */
final class SectorProjection {

	private static final double EPSILON = 1.0e-8;

	private SectorProjection() { }

	/** Returns world coordinates {@code (z, signed transverse radius)} in cm. */
	static Point2D.Double wire(DCGeometry geometry, int sector, int superlayer,
			int layer, int wire) {
		return wire(geometry, sector, superlayer, layer, wire, 0.0);
	}

	static Point2D.Double wire(DCGeometry geometry, int sector, int superlayer,
			int layer, int wire, double phiOffsetDegrees) {
		Segment3 line = geometry.wireLine(sector, superlayer, layer, wire);
		double angle = projectionAngle(sector, phiOffsetDegrees);
		Point3 point = intersection(line.start(), line.end(), angle);
		return project(point, sector, angle);
	}

	static Point2D.Double[] cell(DCGeometry geometry, int sector, int superlayer,
			int layer, int wire) {
		return cell(geometry, sector, superlayer, layer, wire, 0.0);
	}

	/** Project a point expressed in sector coordinates with {@code y=0}. */
	static Point2D.Double sectorPoint(double x, double z, int sector,
			double phiOffsetDegrees) {
		double transverse = x * Math.cos(Math.toRadians(phiOffsetDegrees));
		return new Point2D.Double(z, sector <= 3 ? transverse : -transverse);
	}

	/**
	 * Returns a display hexagon centered on the intersection of the selected phi
	 * plane and an infinitely extended wire. Using the same construction at every
	 * phi avoids the discontinuity that occurs when a finite wire volume ceases to
	 * intersect the plane.
	 */
	static Point2D.Double[] cell(DCGeometry geometry, int sector, int superlayer,
			int layer, int wire, double phiOffsetDegrees) {
		Point2D.Double center = wire(geometry, sector, superlayer, layer, wire, phiOffsetDegrees);
		Point2D.Double wireBefore = wire(geometry, sector, superlayer, layer,
				wire == 1 ? 1 : wire - 1, phiOffsetDegrees);
		Point2D.Double wireAfter = wire(geometry, sector, superlayer, layer,
				wire == DCGeometry.WIRE_COUNT ? DCGeometry.WIRE_COUNT : wire + 1,
				phiOffsetDegrees);
		Point2D.Double layerBefore = wire(geometry, sector, superlayer,
				layer == 1 ? 1 : layer - 1, wire, phiOffsetDegrees);
		Point2D.Double layerAfter = wire(geometry, sector, superlayer,
				layer == DCGeometry.LAYER_COUNT ? DCGeometry.LAYER_COUNT : layer + 1,
				wire, phiOffsetDegrees);

		double ux = wireAfter.x - wireBefore.x;
		double uy = wireAfter.y - wireBefore.y;
		double wireDivisor = wire == 1 || wire == DCGeometry.WIRE_COUNT ? 2.0 : 4.0;
		ux /= wireDivisor;
		uy /= wireDivisor;
		double vx = layerAfter.x - layerBefore.x;
		double vy = layerAfter.y - layerBefore.y;
		double layerDivisor = layer == 1 || layer == DCGeometry.LAYER_COUNT ? 2.0 : 4.0;
		vx /= layerDivisor;
		vy /= layerDivisor;

		return new Point2D.Double[] {
				new Point2D.Double(center.x - ux, center.y - uy),
				new Point2D.Double(center.x - ux / 2 - vx, center.y - uy / 2 - vy),
				new Point2D.Double(center.x + ux / 2 - vx, center.y + uy / 2 - vy),
				new Point2D.Double(center.x + ux, center.y + uy),
				new Point2D.Double(center.x + ux / 2 + vx, center.y + uy / 2 + vy),
				new Point2D.Double(center.x - ux / 2 + vx, center.y - uy / 2 + vy)};
	}

	private static double projectionAngle(int sector, double phiOffsetDegrees) {
		return Math.toRadians(60.0 * (sector - 1) + phiOffsetDegrees);
	}

	private static Point3 intersection(Point3 start, Point3 end, double angle) {
		double da = planeDistance(start, angle);
		double db = planeDistance(end, angle);
		double denominator = da - db;
		double fraction = Math.abs(denominator) < EPSILON ? 0.5 : da / denominator;
		return interpolate(start, end, fraction);
	}

	private static double planeDistance(Point3 point, double angle) {
		return -Math.sin(angle) * point.x() + Math.cos(angle) * point.y();
	}

	private static Point3 interpolate(Point3 a, Point3 b, double fraction) {
		return new Point3(a.x() + fraction * (b.x() - a.x()),
				a.y() + fraction * (b.y() - a.y()),
				a.z() + fraction * (b.z() - a.z()));
	}

	private static Point2D.Double project(Point3 point, int sector, double angle) {
		double radius = point.x() * Math.cos(angle) + point.y() * Math.sin(angle);
		return new Point2D.Double(point.z(), sector <= 3 ? radius : -radius);
	}

}
