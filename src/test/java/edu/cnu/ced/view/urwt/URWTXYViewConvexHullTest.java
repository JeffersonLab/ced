package edu.cnu.ced.view.urwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.geometry.Point3;

/**
 * Pins the correctness of URWTXYView's hand-rolled convex hull (Andrew's
 * monotone chain), which fixes the "sectors wrong" bug reported against the
 * running app: a wedge/trapezoid panel's outline is exactly the convex hull
 * of its strip midpoints, so a broken hull would silently misdraw every
 * panel again.
 */
class URWTXYViewConvexHullTest {

	@Test void reducesADenseSquareOfPointsToJustItsFourCorners() {
		// A 5x5 grid of points, including many interior/edge points that must
		// be dropped -- only the four true corners should survive.
		java.util.ArrayList<Point3> points = new java.util.ArrayList<>();
		for (int x = 0; x <= 4; x++) {
			for (int y = 0; y <= 4; y++) {
				points.add(new Point3(x, y, 0));
			}
		}
		List<Point3> hull = URWTXYView.convexHull(points);

		assertEquals(4, hull.size());
		assertTrue(hull.contains(new Point3(0, 0, 0)));
		assertTrue(hull.contains(new Point3(4, 0, 0)));
		assertTrue(hull.contains(new Point3(4, 4, 0)));
		assertTrue(hull.contains(new Point3(0, 4, 0)));
	}

	@Test void reducesATrapezoidToItsFourCornersRegardlessOfInteriorPoints() {
		// Roughly matches the real shape (narrow near the origin, wide far
		// from it) that motivated using a hull in the first place.
		List<Point3> points = List.of(
				new Point3(20, 5, 0), new Point3(20, -5, 0),
				new Point3(150, 80, 0), new Point3(150, -80, 0),
				new Point3(85, 40, 0), new Point3(60, 0, 0), new Point3(100, -30, 0));
		List<Point3> hull = URWTXYView.convexHull(points);

		assertEquals(4, hull.size());
		assertTrue(hull.contains(new Point3(20, 5, 0)));
		assertTrue(hull.contains(new Point3(20, -5, 0)));
		assertTrue(hull.contains(new Point3(150, 80, 0)));
		assertTrue(hull.contains(new Point3(150, -80, 0)));
	}

	@Test void fewerThanThreePointsPassThroughUnchanged() {
		List<Point3> points = List.of(new Point3(1, 1, 0), new Point3(2, 2, 0));
		assertEquals(2, URWTXYView.convexHull(points).size());
	}
}
