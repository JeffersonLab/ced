package edu.cnu.ced.view.sector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.geometry.DCGeometry;

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
	void nonlinearFieldScaleRoundTripsAndExpandsLowFields() {
		double maximum = 6.58;
		for (double fraction : new double[] {0.0, 0.25, 0.5, 0.75, 1.0}) {
			double value = SectorView.fieldValueAtColorFraction(fraction, maximum);
			assertEquals(fraction, SectorView.fieldColorFraction(value, maximum), 1.0e-12);
		}
		assertTrue(SectorView.fieldColorFraction(0.1, maximum) > 0.25);
	}
}
