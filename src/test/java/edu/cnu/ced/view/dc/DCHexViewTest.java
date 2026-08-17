package edu.cnu.ced.view.dc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

class DCHexViewTest {

	@Test
	void wireCellsPartitionEachLayerWithoutGaps() {
		var first = DCHexView.wirePolygon(1, 2, 3, 1);
		var second = DCHexView.wirePolygon(1, 2, 3, 2);
		assertEquals(first[1].x, second[0].x, 1.0e-12);
		assertEquals(first[1].y, second[0].y, 1.0e-12);
		assertEquals(first[2].x, second[3].x, 1.0e-12);
		assertEquals(first[2].y, second[3].y, 1.0e-12);
	}

	@Test
	void rejectsInvalidWireAddresses() {
		assertThrows(IllegalArgumentException.class,
				() -> DCHexView.wirePolygon(0, 1, 1, 1));
		assertThrows(IllegalArgumentException.class,
				() -> DCHexView.wirePolygon(1, 1, 1, 113));
	}

	@Test
	void findsAnEmptyCellFromWorldCoordinates() {
		var polygon = DCHexView.wirePolygon(4, 5, 2, 73);
		var center = new Point2D.Double();
		for (var point : polygon) {
			center.x += point.x / polygon.length;
			center.y += point.y / polygon.length;
		}
		assertEquals(73, DCHexView.findCell(center).wire());
	}
}
