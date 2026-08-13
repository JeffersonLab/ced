package edu.cnu.ced.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

class CedHexViewTest {

	@Test
	void mapsAnglesToClasSectorsAtBoundaries() {
		assertEquals(1, sector(0));
		assertEquals(1, sector(30));
		assertEquals(2, sector(31));
		assertEquals(2, sector(90));
		assertEquals(3, sector(91));
		assertEquals(4, sector(180));
		assertEquals(6, sector(300));
		assertEquals(6, sector(330));
		assertEquals(1, sector(359));
	}

	@Test
	void normalizesNegativePhi() {
		assertEquals(270.0, CedHexView.normalizedPhi(point(-90)), 1.0e-9);
	}

	@Test
	void rotatesSectorOnePointBySixtyDegrees() {
		Point2D.Double rotated = CedHexView.rotateToSector(new Point2D.Double(10, 0), 2);
		assertEquals(5.0, rotated.x, 1.0e-9);
		assertEquals(5.0 * Math.sqrt(3), rotated.y, 1.0e-9);
		assertThrows(IllegalArgumentException.class,
				() -> CedHexView.rotateToSector(new Point2D.Double(), 0));
	}

	private static int sector(double degrees) {
		return CedHexView.sectorAt(point(degrees));
	}

	private static Point2D.Double point(double degrees) {
		double radians = Math.toRadians(degrees);
		return new Point2D.Double(Math.cos(radians), Math.sin(radians));
	}
}
