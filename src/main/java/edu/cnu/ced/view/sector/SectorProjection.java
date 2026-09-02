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
	 * cross's raw bank coordinates which are already in the tilted
	 * detector frame (see {@link #tiltedPoint}).
	 * <p>
	 * Rotates into {@code sector}'s own local frame first (matching bCNU
	 * CED's {@code GeometryManager.clasToSector}: rotate by {@code
	 * -60*(sector-1)} degrees around z, exactly undoing that sector's
	 * placement around the beamline), then defers to {@link
	 * #sectorFrameToScreen} for the same flat (untilted) display
	 * projection used for crosses -- both ultimately land in the sector
	 * frame and share that one final step, matching bCNU CED's own
	 * {@code CedView.projectClasToWorld}: {@code clasToSector} then
	 * {@code projectedPoint} (a flat plane intersection, no tilt).
	 * </p>
	 */
	static Point2D.Double clasPoint(Point3 point, int sector, double phiOffsetDegrees) {
		Point3 sectorPoint = sectorFrame(point, sector);
		return sectorFrameToScreen(sectorPoint, sector, phiOffsetDegrees);
	}

	/**
	 * Rotates a lab-frame point into {@code sector}'s own local frame -- the
	 * same rotation {@link #clasPoint} applies before tilting -- exposed on
	 * its own so callers can display the intermediate value (e.g.
	 * reproducing bCNU CED's "Sector xyz" feedback reading) rather than
	 * only the final 2D screen point.
	 */
	static Point3 sectorFrame(Point3 labPoint, int sector) {
		double midPlanePhi = Math.toRadians(60.0 * (sector - 1));
		double cosPhi = Math.cos(midPlanePhi);
		double sinPhi = Math.sin(midPlanePhi);
		double sectorX = cosPhi * labPoint.x() + sinPhi * labPoint.y();
		double sectorY = -sinPhi * labPoint.x() + cosPhi * labPoint.y();
		return new Point3(sectorX, sectorY, labPoint.z());
	}

	/**
	 * Applies the 25-degree tilt to a sector-frame point -- the same
	 * rotation {@link #tiltedPoint} applies -- returning the full 3D tilted
	 * point rather than collapsing straight to a 2D screen point, so
	 * callers can display it directly (bCNU CED's "Tilted sect xyz"
	 * feedback reading).
	 */
	static Point3 tiltedFrame(Point3 sectorPoint) {
		double tilt = Math.toRadians(25.0);
		double tiltX = sectorPoint.x() * Math.cos(tilt) - sectorPoint.z() * Math.sin(tilt);
		double tiltY = sectorPoint.y();
		double tiltZ = sectorPoint.x() * Math.sin(tilt) + sectorPoint.z() * Math.cos(tilt);
		return new Point3(tiltX, tiltY, tiltZ);
	}

	/**
	 * Inverts {@link #clasPoint}'s chain, recovering a lab-frame point from
	 * a screen {@code (z, transverse)} position -- under the assumption
	 * that the point lies exactly along the {@code phiOffsetDegrees}-
	 * rotated axis within {@code sector}'s own frame (the on-screen
	 * transverse coordinate is that axis's absolute distance, per {@link
	 * #sectorFrameToScreen}; a 2D slice view's mouse position alone can't
	 * otherwise recover the sign lost to that {@code abs()}, or any
	 * perpendicular offset). Used for cursor location feedback and
	 * magnetic-field lookups at the cursor.
	 */
	static Point3 labPointFromScreen(double screenZ, double screenTransverse, int sector,
			double phiOffsetDegrees) {
		double radius = sector <= 3 ? screenTransverse : -screenTransverse;
		double phi = Math.toRadians(phiOffsetDegrees);
		double sectorX = radius * Math.cos(phi);
		double sectorY = radius * Math.sin(phi);
		double midPlanePhi = Math.toRadians(60.0 * (sector - 1));
		double x = Math.cos(midPlanePhi) * sectorX - Math.sin(midPlanePhi) * sectorY;
		double y = Math.sin(midPlanePhi) * sectorX + Math.cos(midPlanePhi) * sectorY;
		return new Point3(x, y, screenZ);
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
	 * Projects a point given in the tilted detector frame -- a DC cross's
	 * raw bank coordinates -- onto the display.
	 * <p>
	 * <b>This is the method that both crosses and swum trajectories were
	 * actually wrong through, for most of a long debugging session, and
	 * this is the real fix.</b> Two earlier fixes in that session corrected
	 * this method's own tilt-rotation formula (direction, then sign) to
	 * match bCNU CED's {@code CedView.sectorToTilted} / {@code
	 * tiltedToSector} -- verified numerically against a real legacy-CED
	 * feedback-panel reading, and that verification was NOT wrong, but it
	 * was answering a narrower question than it seemed to: {@code
	 * sectorToTilted} is genuinely what produces legacy's "Tilted sect xyz"
	 * feedback text (see {@link #tiltedFrame}, still used for that
	 * purpose), but that text is purely diagnostic. Checking legacy's own
	 * {@code CrossDrawer} and {@code SwimTrajectoryDrawer} directly (not a
	 * single feedback-panel number) shows what actually reaches the
	 * screen:
	 * <ul>
	 * <li>Crosses: {@code tiltedToSector} (bank xyz, genuinely tilted-frame,
	 * back to sector frame) -- {@link #sectorFromTilted}.</li>
	 * <li>Swum trajectories: {@code clasToSector} (lab xyz to sector frame)
	 * -- {@link #sectorFrame}.</li>
	 * <li>Both then go through {@code CedView.projectedPoint}: intersect
	 * the sector-frame point with the flat {@code y = 0} plane (i.e. just
	 * drop the out-of-plane sector-y component -- no tilt at all) and take
	 * {@code hypot(x, 0) = abs(x)} as the on-screen radius, then negate for
	 * {@code sector > 3} -- {@link #sectorFrameToScreen}.</li>
	 * </ul>
	 * There is no further tilt-mixing step for either path; the 25-degree
	 * tilt only ever appears converting between a cross's native tilted
	 * frame and the sector frame both paths converge on.
	 * </p>
	 */
	static Point2D.Double tiltedPoint(double tiltedX, double tiltedY, double tiltedZ,
			int sector, double phiOffsetDegrees) {
		Point3 sectorPoint = sectorFromTilted(new Point3(tiltedX, tiltedY, tiltedZ));
		return sectorFrameToScreen(sectorPoint, sector, phiOffsetDegrees);
	}

	/**
	 * Converts a point in the tilted detector frame (a DC cross's raw bank
	 * coordinates) to the sector frame -- bCNU CED's own {@code
	 * CedView.tiltedToSector} -- the inverse of {@link #tiltedFrame}.
	 */
	static Point3 sectorFromTilted(Point3 tilted) {
		double tilt = Math.toRadians(25.0);
		double sectorX = tilted.x() * Math.cos(tilt) + tilted.z() * Math.sin(tilt);
		double sectorY = tilted.y();
		double sectorZ = -tilted.x() * Math.sin(tilt) + tilted.z() * Math.cos(tilt);
		return new Point3(sectorX, sectorY, sectorZ);
	}

	/**
	 * Flat (untilted) projection of a sector-frame point onto the display --
	 * bCNU CED's own {@code CedView.projectedPoint} intersected with the
	 * flat {@code y = 0} plane (equivalent to simply dropping {@code
	 * sectorPoint.y()}) composed with its {@code sector > 3} sign flip.
	 * {@code phiOffsetDegrees} rotates which direction within the sector's
	 * own (x, y) plane counts as "radius" -- projecting onto that rotated
	 * axis rather than dropping y outright -- matching how {@link #project}
	 * folds the same angle into a single combined rotation (the two are
	 * algebraically identical; see this method's own unit tests).
	 */
	private static Point2D.Double sectorFrameToScreen(Point3 sectorPoint, int sector,
			double phiOffsetDegrees) {
		double phi = Math.toRadians(phiOffsetDegrees);
		double radius = Math.abs(sectorPoint.x() * Math.cos(phi) + sectorPoint.y() * Math.sin(phi));
		return new Point2D.Double(sectorPoint.z(), sector <= 3 ? radius : -radius);
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
