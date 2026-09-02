package edu.cnu.ced.view.sector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.geometry.DCGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.geometry.Segment3;

class SectorProjectionTest {

	@Test
	void cellUsesContinuousDisplayHexagon() {
		DCGeometry geometry = new DCGeometry();
		geometry.initializeFromSource();
		var cell = SectorProjection.cell(geometry, 1, 3, 3, 56, 0.0);
		assertTrue(cell.length == 6);
	}

	@Test
	void projectedCellMovesContinuouslyNearPhiLimit() {
		DCGeometry geometry = new DCGeometry();
		geometry.initializeFromSource();
		var before = SectorProjection.wire(geometry, 1, 3, 3, 56, 24.9);
		var atLimit = SectorProjection.wire(geometry, 1, 3, 3, 56, 25.0);
		assertTrue(before.distance(atLimit) < 10.0);
	}

	@Test
	void oppositeSectorsProjectToOppositeSides() {
		DCGeometry geometry = new DCGeometry();
		geometry.initializeFromSource();
		var upper = SectorProjection.wire(geometry, 1, 3, 3, 56);
		var lower = SectorProjection.wire(geometry, 4, 3, 3, 56);
		assertTrue(upper.y > 0);
		assertTrue(lower.y < 0);
		assertTrue(Math.abs(upper.x - lower.x) < 1.0e-6);
	}

	@Test
	void clasPointDropsOutOfPlaneOffsetAndTakesAnAbsoluteRadius() {
		// clasPoint's (and tiltedPoint's) shared final step -- bCNU CED's
		// own CedView.projectedPoint intersected with the flat y=0 plane,
		// then CedView.sectorToWorld's sector > 3 sign flip -- drops the
		// out-of-plane sector-y component entirely and takes the in-plane
		// sector-x component's ABSOLUTE VALUE as the on-screen radius (via
		// hypot(x, 0)), not its raw signed value. A lab point whose
		// sector-frame x comes out negative must still land on the same
		// side as its mirror image (180 degrees around, in this same
		// sector's own rotated frame) with positive x.
		int sector = 6;
		double midPlanePhiDeg = 60.0 * (sector - 1);
		double rho = 30.0, z = 40.0;
		Point3 aligned = new Point3(
				rho * Math.cos(Math.toRadians(midPlanePhiDeg)),
				rho * Math.sin(Math.toRadians(midPlanePhiDeg)), z);
		Point3 opposite = new Point3(
				rho * Math.cos(Math.toRadians(midPlanePhiDeg + 180.0)),
				rho * Math.sin(Math.toRadians(midPlanePhiDeg + 180.0)), z);

		var alignedScreen = SectorProjection.clasPoint(aligned, sector, 0.0);
		var oppositeScreen = SectorProjection.clasPoint(opposite, sector, 0.0);
		assertEquals(alignedScreen.x, oppositeScreen.x, 1.0e-9);
		assertEquals(alignedScreen.y, oppositeScreen.y, 1.0e-9);
		assertTrue(alignedScreen.y < 0, "sector 6 (> 3) must land below the divider");
	}

	@Test
	void tiltedPointMatchesClasPointForTheSamePhysicalLocation() {
		// A DC cross and a swum trajectory point at the same physical
		// location must land on the same screen pixel, even though they
		// arrive there through different frames (tiltedPoint from the
		// tilted detector frame a cross's bank values are native to,
		// clasPoint from lab/CLAS coordinates). Ground-truthed against a
		// real bCNU CED session: its feedback panel, for a mouse position
		// at lab xyz (2.52, -4.95, 49.49) cm in sector 6, reported "Sector
		// xyz" (5.55, 0.29, 49.49) and "Tilted sect xyz" (-15.89, 0.29,
		// 47.20) (the panel negates y for sector > 3, so the underlying
		// raw tilted-frame value used below is (-15.89, -0.29, 47.20)).
		double tiltedX = -15.88824640413513;
		double tiltedY = -0.29261598246321485;
		double tiltedZ = 47.19736223655189;
		var viaTiltedPoint = SectorProjection.tiltedPoint(tiltedX, tiltedY, tiltedZ, 6, 0.0);
		assertEquals(49.49, viaTiltedPoint.x, 1.0e-9);
		assertEquals(-5.546825748732971, viaTiltedPoint.y, 1.0e-9);

		Point3 labPoint = new Point3(2.52, -4.95, 49.49);
		var viaClasPoint = SectorProjection.clasPoint(labPoint, 6, 0.0);
		assertEquals(viaClasPoint.x, viaTiltedPoint.x, 1.0e-9);
		assertEquals(viaClasPoint.y, viaTiltedPoint.y, 1.0e-9);
	}

	@Test
	void sectorFromTiltedInvertsTiltedFrame() {
		// sectorFromTilted (tiltedToSector) and tiltedFrame (sectorToTilted)
		// -- both still used for the diagnostic "Sector xyz"/"Tilted sect
		// xyz" location feedback -- must be exact inverses of each other.
		Point3 lab = new Point3(2.52, -4.95, 49.49);
		Point3 sector = SectorProjection.sectorFrame(lab, 6);
		Point3 tilted = SectorProjection.tiltedFrame(sector);
		Point3 recovered = SectorProjection.sectorFromTilted(tilted);
		assertEquals(sector.x(), recovered.x(), 1.0e-9);
		assertEquals(sector.y(), recovered.y(), 1.0e-9);
		assertEquals(sector.z(), recovered.z(), 1.0e-9);
	}

	@Test
	void clasPointMatchesLegacyCedReferenceReading() {
		// Same real bCNU CED session as tiltedPointMatchesClasPointForTheSamePhysicalLocation,
		// but starting from the panel's own reported raw lab xyz.
		Point3 labPoint = new Point3(2.52, -4.95, 49.49);
		var projected = SectorProjection.clasPoint(labPoint, 6, 0.0);
		assertEquals(49.49, projected.x, 1.0e-9);
		assertEquals(-5.546825748732971, projected.y, 1.0e-9);
	}

	@Test
	void labPointFromScreenInvertsClasPointOnTheSectorMidplane() {
		// labPointFromScreen assumes zero out-of-plane offset (sectorY = 0),
		// so round-tripping only holds exactly for a lab point that already
		// sits on the sector's own midplane -- exactly the case a cursor
		// click in this 2D slice view is implicitly assuming.
		int sector = 6;
		double midPlanePhiDeg = 60.0 * (sector - 1);
		double rho = 37.0, z = 212.0;
		Point3 onMidplane = new Point3(
				rho * Math.cos(Math.toRadians(midPlanePhiDeg)),
				rho * Math.sin(Math.toRadians(midPlanePhiDeg)), z);

		var screen = SectorProjection.clasPoint(onMidplane, sector, 0.0);
		Point3 recovered = SectorProjection.labPointFromScreen(screen.x, screen.y, sector, 0.0);

		assertEquals(onMidplane.x(), recovered.x(), 1.0e-9);
		assertEquals(onMidplane.y(), recovered.y(), 1.0e-9);
		assertEquals(onMidplane.z(), recovered.z(), 1.0e-9);
	}

	@Test
	void labPointFromScreenMatchesLegacyCedReferenceReading() {
		// Inverse of clasPointMatchesLegacyCedReferenceReading: starting
		// from that same real legacy-CED session's screen point, recover a
		// lab xyz along sector 6's own midplane (phiOffsetDegrees = 0).
		// Since the real lab reading (2.52, -4.95, 49.49) has a nonzero
		// out-of-plane (sector-y) component, this won't exactly reproduce
		// it (labPointFromScreen assumes zero), but it must land close, and
		// re-projecting the recovered point must reproduce the same screen
		// point exactly (round-trip consistency).
		Point3 recovered = SectorProjection.labPointFromScreen(
				49.49, -5.546825748732971, 6, 0.0);
		assertEquals(2.52, recovered.x(), 0.3);
		assertEquals(-4.95, recovered.y(), 0.3);
		assertEquals(49.49, recovered.z(), 1.0e-9);

		var reprojected = SectorProjection.clasPoint(recovered, 6, 0.0);
		assertEquals(49.49, reprojected.x, 1.0e-9);
		assertEquals(-5.546825748732971, reprojected.y, 1.0e-9);
	}

	@Test
	void labCoordinatesProjectOntoSelectedSectorPlane() {
		var sectorOne = SectorProjection.labPoint(new Point3(12.0, 5.0, 240.0), 1, 0.0);
		var sectorFour = SectorProjection.labPoint(new Point3(-12.0, -5.0, 240.0), 4, 0.0);
		assertEquals(240.0, sectorOne.x, 1.0e-12);
		assertEquals(12.0, sectorOne.y, 1.0e-12);
		assertEquals(240.0, sectorFour.x, 1.0e-12);
		assertEquals(-12.0, sectorFour.y, 1.0e-12);
	}

	@Test
	void clasPointSkipsTheTiltEntirely() {
		// clasPoint (lab-frame input, e.g. a swum trajectory point) and
		// tiltedPoint (tilted-frame input, a DC cross's native bank values)
		// converge on the same flat, untilted final projection, but they
		// are NOT interchangeable on the same raw numbers: clasPoint's
		// lab-to-sector rotation (clasToSector) is a plain azimuthal
		// rotation with no tilt, while tiltedPoint's tilted-to-sector
		// conversion does apply the 25-degree tilt. Even in sector 1, whose
		// azimuthal rotation is the identity, the two must disagree unless
		// the input's own x/z happen to sit exactly along the tilt axis.
		Point3 point = new Point3(12.0, -3.0, 240.0);
		var viaClasPoint = SectorProjection.clasPoint(point, 1, 0.0);
		var viaTiltedPoint = SectorProjection.tiltedPoint(12.0, -3.0, 240.0, 1, 0.0);
		assertTrue(Math.abs(viaClasPoint.x - viaTiltedPoint.x) > 1.0
				|| Math.abs(viaClasPoint.y - viaTiltedPoint.y) > 1.0);
	}

	@Test
	void clasPointRotatesLabCoordinatesIntoSectorLocalFrameFirst() {
		// A lab-frame point straight down sector 2's own midplane (60
		// degrees) should land exactly where a point straight down sector
		// 1's local +x axis lands when projected through sector 1 -- the
		// same "distance from the beamline, no transverse offset" case in a
		// different sector, matching bCNU CED's GeometryManager.clasToSector.
		double midPlanePhiDeg = 60.0;
		double rho = 20.0, z = 150.0;
		Point3 onSector2Midplane = new Point3(
				rho * Math.cos(Math.toRadians(midPlanePhiDeg)),
				rho * Math.sin(Math.toRadians(midPlanePhiDeg)), z);
		var sector2 = SectorProjection.clasPoint(onSector2Midplane, 2, 0.0);
		var sector1Reference = SectorProjection.clasPoint(new Point3(rho, 0.0, z), 1, 0.0);
		assertEquals(sector1Reference.x, sector2.x, 1.0e-9);
		assertEquals(sector1Reference.y, sector2.y, 1.0e-9);
	}

	@Test
	void clasPointUnderOneFixedSectorIsContinuousEvenNearTheBeamline() {
		// clasPoint(Point3, sector, ...) rotates any lab-frame point onto
		// sector's fixed midplane -- a plain, continuous change of basis,
		// not something limited to points physically inside that sector's
		// own 60-degree wedge. Projecting a smooth path (e.g. a swum
		// trajectory) one fixed sector at a time must therefore itself stay
		// smooth, including right near the beamline where x and y are both
		// tiny and their own angle is meaningless. This guards against
		// reintroducing a per-point "which sector is this position really
		// in" re-derivation, which flips the rotation basis by 60 degrees
		// whenever that noisy per-point estimate crosses a boundary and
		// produces exactly this kind of discontinuity -- confirmed against
		// real event data as a jagged, curled-looking track near the vertex
		// that didn't correspond to the particle's real trajectory.
		int sector = 3;
		double stepCm = 0.5;
		Point2D.Double previous = null;
		for (int i = 0; i <= 20; i++) {
			// A path that starts essentially on the beamline (tiny, noisy
			// transverse offset) and moves outward into sector 3.
			double s = i * stepCm;
			Point3 point = new Point3(
					1.0e-6 * ((i % 2 == 0) ? 1 : -1) + s * Math.cos(Math.toRadians(120.0)),
					-3.0e-6 * ((i % 3 == 0) ? 1 : -1) + s * Math.sin(Math.toRadians(120.0)),
					-5.0 + s * 0.1);
			Point2D.Double projected = SectorProjection.clasPoint(point, sector, 0.0);
			if (previous != null) {
				assertTrue(previous.distance(projected) < 2.0 * stepCm,
						"consecutive projected points must not jump further apart than the "
								+ "physical step between them (i=" + i + ")");
			}
			previous = projected;
		}
	}

	@Test
	void nonlinearFieldScaleRoundTripsAndExpandsLowFields() {
		double maximum = 6.58;
		for (double fraction : new double[] {0.0, 0.25, 0.5, 0.75, 1.0}) {
			double value = SectorView.fieldValueAtColorFraction(fraction, maximum);
			assertEquals(fraction, SectorView.fieldColorFraction(value, maximum), 1.0e-12);
		}
		assertTrue(SectorView.fieldColorFraction(0.1, maximum) > 0.25);
	}

	@Test
	void paddleSliceUsesLongEdgeIntersectionsAndRejectsMisses() {
		List<Segment3> crossing = List.of(
				edge(-2, -1, 10, 2, 1, 30), edge(2, -1, 10, -2, 1, 30),
				edge(-2, -1, 12, 2, 1, 32), edge(2, -1, 12, -2, 1, 32));
		Point2D.Double[] polygon = SectorProjection.paddleSlice(crossing, 1, 0.0);
		assertEquals(4, polygon.length);
		assertEquals(20.0, polygon[0].x, 1.0e-9);

		List<Segment3> missed = List.of(
				edge(-2, 1, 10, 2, 2, 30), edge(2, 1, 10, -2, 2, 30),
				edge(-2, 1, 12, 2, 2, 32), edge(2, 1, 12, -2, 2, 32));
		assertTrue(SectorProjection.paddleSlice(missed, 1, 0.0).length == 0);
	}

	@Test
	void volumeSliceBuildsConvexCrossSection() {
		List<Point3> box = List.of(
				new Point3(-2, -2, 10), new Point3(-2, -2, 20),
				new Point3(-2, 2, 10), new Point3(-2, 2, 20),
				new Point3(2, -2, 10), new Point3(2, -2, 20),
				new Point3(2, 2, 10), new Point3(2, 2, 20));
		Point2D.Double[] polygon = SectorProjection.volumeSlice(box, 1, 0.0);
		assertEquals(4, polygon.length);
		for (Point2D.Double point : polygon) {
			assertTrue(Double.isFinite(point.x));
			assertTrue(Double.isFinite(point.y));
		}
	}

	private static Segment3 edge(double x1, double y1, double z1,
			double x2, double y2, double z2) {
		return new Segment3(new Point3(x1, y1, z1), new Point3(x2, y2, z2));
	}
}
