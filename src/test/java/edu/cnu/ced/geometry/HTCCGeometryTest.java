package edu.cnu.ced.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

class HTCCGeometryTest {

	@Test
	void providesFiniteOppositeSectorCells() {
		HTCCGeometry geometry = new HTCCGeometry();
		Point2D.Double[] upper = geometry.polygon(1, 2, 1, 15.0);
		Point2D.Double[] lower = geometry.polygon(4, 2, 1, 15.0);
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
