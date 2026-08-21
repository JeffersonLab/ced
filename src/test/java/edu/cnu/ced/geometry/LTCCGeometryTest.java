package edu.cnu.ced.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

class LTCCGeometryTest {

	@Test
	void providesFiniteOppositeSectorCells() {
		LTCCGeometry geometry = new LTCCGeometry();
		Point2D.Double[] upper = geometry.polygon(3, 12, 2);
		Point2D.Double[] lower = geometry.polygon(6, 12, 2);
		assertEquals(4, upper.length);
		assertEquals(4, lower.length);
		for (int index = 0; index < upper.length; index++) {
			assertTrue(Double.isFinite(upper[index].x));
			assertTrue(Double.isFinite(upper[index].y));
			assertEquals(upper[index].x, lower[index].x, 1.0e-9);
			assertEquals(upper[index].y, -lower[index].y, 1.0e-9);
		}
	}
}
