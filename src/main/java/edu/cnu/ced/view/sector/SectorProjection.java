package edu.cnu.ced.view.sector;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

	/** Orthogonally project a point in lab coordinates onto the selected sector plane. */
	static Point2D.Double labPoint(Point3 point, int sector, double phiOffsetDegrees) {
		double angle = projectionAngle(sector, phiOffsetDegrees);
		return project(point, sector, angle);
	}

	/**
	 * Project a point given in true lab ("CLAS") coordinates -- e.g. a swum
	 * particle trajectory, which is genuine lab-frame data, unlike a DC
	 * cross's raw bank coordinates which are already sector-local.
	 * <p>
	 * Rotates into {@code sector}'s own local frame first (matching bCNU
	 * CED's {@code GeometryManager.clasToSector}: rotate by {@code
	 * -60*(sector-1)} degrees around z, exactly undoing that sector's
	 * placement around the beamline), then defers to {@link #tiltedPoint}
	 * for the same tilted-plane projection used for crosses. Using {@link
	 * #labPoint} directly on lab-frame data here would skip that rotation
	 * entirely, which matches crosses only very close to the beamline and
	 * increasingly diverges from them with distance -- exactly the growing
	 * mismatch this method fixes.
	 * </p>
	 */
	static Point2D.Double clasPoint(Point3 point, int sector, double phiOffsetDegrees) {
		double midPlanePhi = Math.toRadians(60.0 * (sector - 1));
		double cosPhi = Math.cos(midPlanePhi);
		double sinPhi = Math.sin(midPlanePhi);
		double sectorX = cosPhi * point.x() + sinPhi * point.y();
		double sectorY = -sinPhi * point.x() + cosPhi * point.y();
		return tiltedPoint(sectorX, sectorY, point.z(), sector, phiOffsetDegrees);
	}

	/**
	 * Intersects the selected phi plane with the four long edges of a paddle.
	 * The returned quadrilateral uses intersections with the infinitely extended
	 * edge lines, as the legacy CED projection did, so its shape remains stable
	 * near the end of a paddle. An empty array means that fewer than three of the
	 * intersections lie on the finite edges and the paddle should not be drawn.
	 */
	static Point2D.Double[] paddleSlice(List<Segment3> projectionEdges, int sector,
			double phiOffsetDegrees) {
		if (projectionEdges == null || projectionEdges.size() != 4) {
			throw new IllegalArgumentException("A paddle projection requires four edges");
		}
		double angle = projectionAngle(sector, phiOffsetDegrees);
		Point2D.Double[] polygon = new Point2D.Double[projectionEdges.size()];
		int finiteIntersections = 0;
		for (int index = 0; index < projectionEdges.size(); index++) {
			Segment3 edge = projectionEdges.get(index);
			double da = planeDistance(edge.start(), angle);
			double db = planeDistance(edge.end(), angle);
			double denominator = da - db;
			if (Math.abs(denominator) < EPSILON) return new Point2D.Double[0];
			double fraction = da / denominator;
			if (fraction >= -EPSILON && fraction <= 1.0 + EPSILON) finiteIntersections++;
			polygon[index] = project(interpolate(edge.start(), edge.end(), fraction), sector, angle);
		}
		return finiteIntersections > 2 ? polygon : new Point2D.Double[0];
	}

	/**
	 * Returns the convex polygon cut from a detector volume by the selected phi
	 * plane. The input may use any corner ordering; intersecting every corner pair
	 * and taking the two-dimensional hull makes this suitable for the PCAL and
	 * ECAL strip volumes without encoding detector-specific edge topology.
	 */
	static Point2D.Double[] volumeSlice(List<Point3> corners, int sector,
			double phiOffsetDegrees) {
		if (corners == null || corners.size() < 4) return new Point2D.Double[0];
		double angle = projectionAngle(sector, phiOffsetDegrees);
		List<Point2D.Double> intersections = new ArrayList<>();
		for (int i = 0; i < corners.size(); i++) {
			Point3 a = corners.get(i);
			double da = planeDistance(a, angle);
			if (Math.abs(da) < EPSILON) addUnique(intersections, project(a, sector, angle));
			for (int j = i + 1; j < corners.size(); j++) {
				Point3 b = corners.get(j);
				double db = planeDistance(b, angle);
				if (da * db > EPSILON) continue;
				double denominator = da - db;
				if (Math.abs(denominator) < EPSILON) continue;
				double fraction = da / denominator;
				if (fraction < -EPSILON || fraction > 1.0 + EPSILON) continue;
				addUnique(intersections, project(interpolate(a, b, fraction), sector, angle));
			}
		}
		return convexHull(intersections);
	}

	private static void addUnique(List<Point2D.Double> points, Point2D.Double candidate) {
		for (Point2D.Double point : points)
			if (point.distanceSq(candidate) < 1.0e-12) return;
		points.add(candidate);
	}

	private static Point2D.Double[] convexHull(List<Point2D.Double> points) {
		if (points.size() < 3) return new Point2D.Double[0];
		List<Point2D.Double> sorted = points.stream()
				.sorted(Comparator.comparingDouble((Point2D.Double p) -> p.x)
						.thenComparingDouble(p -> p.y)).toList();
		List<Point2D.Double> hull = new ArrayList<>();
		for (Point2D.Double point : sorted) {
			while (hull.size() >= 2 && cross(hull.get(hull.size() - 2),
					hull.get(hull.size() - 1), point) <= 0.0) hull.remove(hull.size() - 1);
			hull.add(point);
		}
		int lowerSize = hull.size();
		for (int i = sorted.size() - 2; i >= 0; i--) {
			Point2D.Double point = sorted.get(i);
			while (hull.size() > lowerSize && cross(hull.get(hull.size() - 2),
					hull.get(hull.size() - 1), point) <= 0.0) hull.remove(hull.size() - 1);
			hull.add(point);
		}
		if (hull.size() > 1) hull.remove(hull.size() - 1);
		return hull.toArray(Point2D.Double[]::new);
	}

	private static double cross(Point2D.Double a, Point2D.Double b, Point2D.Double c) {
		return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
	}

	/**
	 * Projects a point given in the (untilted) sector-local frame -- e.g. a
	 * DC cross's raw bank coordinates, or {@link #clasPoint}'s output after
	 * its own lab-to-sector rotation -- onto the tilted display plane.
	 * <p>
	 * Despite its name (kept for now to avoid a wider rename; the
	 * parameters here are sector-frame, not already-tilted, coordinates),
	 * this previously implemented the rotation in the wrong direction --
	 * {@code sectorX*cos + sectorZ*sin} / {@code -sectorX*sin + sectorZ*cos}
	 * is bCNU CED's {@code CedView.tiltedToSector} (tilted frame back to
	 * sector frame), the inverse of the transform actually needed here.
	 * The correct forward transform, matching bCNU CED's own {@code
	 * CedView.sectorToTilted} exactly, flips both cross-term signs. Verified
	 * against a real feedback-panel reading from legacy CED itself (sector
	 * 6, lab xyz (2.52, -4.95, 49.49) cm): its own reported "Sector xyz"
	 * (5.55, -0.29, 49.49) and "Tilted sect xyz" (-15.89, -0.29, 47.20) --
	 * the panel displays y negated for sector &gt; 3, like the final
	 * transverse output here -- reproduce to the displayed precision only
	 * under this corrected formula; the previous formula was off by tens of
	 * screen-cm at this same point, not a rounding-sized discrepancy.
	 * </p>
	 */
	static Point2D.Double tiltedPoint(double sectorX, double sectorY, double sectorZ,
			int sector, double phiOffsetDegrees) {
		double tilt = Math.toRadians(25.0);
		double tiltX = sectorX * Math.cos(tilt) - sectorZ * Math.sin(tilt);
		double tiltY = sectorY;
		double tiltZ = sectorX * Math.sin(tilt) + sectorZ * Math.cos(tilt);
		double phi = Math.toRadians(phiOffsetDegrees);
		double transverse = tiltX * Math.cos(phi) + tiltY * Math.sin(phi);
		return new Point2D.Double(tiltZ, sector <= 3 ? transverse : -transverse);
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
