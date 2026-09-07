package edu.cnu.ced.view.dc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.geometry.Point3;

/**
 * Pins the correctness of DCXYView's hand-rolled convex hull (Andrew's
 * monotone chain) -- the same algorithm URWTXYView already relies on for its
 * own panel outlines, duplicated here rather than shared (matching this
 * codebase's existing precedent: SectorView and SectorProjection each carry
 * their own copy too).
 */
class DCXYViewConvexHullTest {

	@Test void reducesADenseSquareOfPointsToJustItsFourCorners() {
		java.util.ArrayList<Point3> points = new java.util.ArrayList<>();
		for (int x = 0; x <= 4; x++) {
			for (int y = 0; y <= 4; y++) {
				points.add(new Point3(x, y, 0));
			}
		}
		List<Point3> hull = DCXYView.convexHull(points);

		assertEquals(4, hull.size());
		assertTrue(hull.contains(new Point3(0, 0, 0)));
		assertTrue(hull.contains(new Point3(4, 0, 0)));
		assertTrue(hull.contains(new Point3(4, 4, 0)));
		assertTrue(hull.contains(new Point3(0, 4, 0)));
	}

	@Test void reducesATrapezoidToItsFourCornersRegardlessOfInteriorPoints() {
		// Roughly matches a real DC superlayer's own shape: narrow near the
		// origin (inner layer), wide far from it (outer layer).
		List<Point3> points = List.of(
				new Point3(20, 5, 0), new Point3(20, -5, 0),
				new Point3(150, 80, 0), new Point3(150, -80, 0),
				new Point3(85, 40, 0), new Point3(60, 0, 0), new Point3(100, -30, 0));
		List<Point3> hull = DCXYView.convexHull(points);

		assertEquals(4, hull.size());
		assertTrue(hull.contains(new Point3(20, 5, 0)));
		assertTrue(hull.contains(new Point3(20, -5, 0)));
		assertTrue(hull.contains(new Point3(150, 80, 0)));
		assertTrue(hull.contains(new Point3(150, -80, 0)));
	}

	@Test void fewerThanThreePointsPassThroughUnchanged() {
		List<Point3> points = List.of(new Point3(1, 1, 0), new Point3(2, 2, 0));
		assertEquals(2, DCXYView.convexHull(points).size());
	}
}
