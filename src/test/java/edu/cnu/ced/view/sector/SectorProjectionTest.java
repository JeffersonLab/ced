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
	void tiltedCrossCoordinatesRespectSectorSideAndTilt() {
		var upper = SectorProjection.tiltedPoint(12.0, -3.0, 240.0, 2, 0.0);
		var lower = SectorProjection.tiltedPoint(12.0, -3.0, 240.0, 5, 0.0);
		double tilt = Math.toRadians(25.0);
		double expectedZ = -12.0 * Math.sin(tilt) + 240.0 * Math.cos(tilt);
		double expectedTransverse = 12.0 * Math.cos(tilt) + 240.0 * Math.sin(tilt);
		assertEquals(expectedZ, upper.x, 1.0e-12);
		assertEquals(expectedTransverse, upper.y, 1.0e-12);
		assertEquals(expectedZ, lower.x, 1.0e-12);
		assertEquals(-expectedTransverse, lower.y, 1.0e-12);
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
	void clasPointMatchesTiltedPointDirectlyForSectorOne() {
		// Sector 1's own local frame is the lab frame itself (rotation by
		// zero degrees), so clasPoint should agree with calling tiltedPoint
		// directly on the same lab-frame coordinates.
		Point3 point = new Point3(12.0, -3.0, 240.0);
		var viaClasPoint = SectorProjection.clasPoint(point, 1, 0.0);
		var viaTiltedPoint = SectorProjection.tiltedPoint(12.0, -3.0, 240.0, 1, 0.0);
		assertEquals(viaTiltedPoint.x, viaClasPoint.x, 1.0e-12);
		assertEquals(viaTiltedPoint.y, viaClasPoint.y, 1.0e-12);
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
	void sectorForPositionUsesTheSameSixtyDegreeConvention() {
		// Interior points, one per sector -- see RecEventData.Particle's
		// sectorFollowsClas12SixtyDegreeConvention test for why exact
		// boundary values (30, 90, ...) aren't asserted here.
		assertEquals(1, SectorProjection.sectorForPosition(10.0, 0.0));
		assertEquals(2, SectorProjection.sectorForPosition(5.0, 8.7));
		assertEquals(3, SectorProjection.sectorForPosition(-5.0, 8.7));
		assertEquals(4, SectorProjection.sectorForPosition(-10.0, 0.0));
		assertEquals(5, SectorProjection.sectorForPosition(-5.0, -8.7));
		assertEquals(6, SectorProjection.sectorForPosition(5.0, -8.7));
	}

	@Test
	void autoSectorClasPointMatchesExplicitSectorForThatPointsOwnSector() {
		// A point sitting on sector 2's own midplane (phi = 60 degrees):
		// the auto-sector overload should derive sector 2 from the point
		// itself and agree exactly with calling the explicit-sector
		// overload with 2 -- not with sector 1, which is what a track
		// starting in sector 1 but drifting out to this position would
		// wrongly be projected with with a single fixed starting sector.
		double rho = 30.0, z = 100.0;
		Point3 point = new Point3(
				rho * Math.cos(Math.toRadians(60.0)), rho * Math.sin(Math.toRadians(60.0)), z);
		var auto = SectorProjection.clasPoint(point, 0.0);
		var explicitSectorTwo = SectorProjection.clasPoint(point, 2, 0.0);
		var explicitSectorOne = SectorProjection.clasPoint(point, 1, 0.0);
		assertEquals(explicitSectorTwo.x, auto.x, 1.0e-12);
		assertEquals(explicitSectorTwo.y, auto.y, 1.0e-12);
		assertTrue(Math.abs(explicitSectorOne.x - auto.x) > 1.0e-6
				|| Math.abs(explicitSectorOne.y - auto.y) > 1.0e-6);
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
