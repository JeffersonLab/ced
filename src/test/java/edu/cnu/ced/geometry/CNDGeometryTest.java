package edu.cnu.ced.geometry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.junit.jupiter.api.Test;

class CNDGeometryTest {

	@Test
	void sourceGeometryProvidesAllPaddleCorners() {
		CNDGeometry geometry = new CNDGeometry();
		geometry.initializeFromSource();
		assertEquals(8, geometry.corners(1, 1).size());
		assertEquals(4, geometry.xyCorners(2, 24).size());
		assertEquals(24, geometry.verticesCm(3, 48).length);
		assertThrows(IllegalArgumentException.class, () -> geometry.corners(0, 1));
	}

	@Test
	void payloadRestoresRepresentativePaddlesExactly() throws Exception {
		CNDGeometry source = new CNDGeometry();
		source.initializeFromSource();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		source.write(new DataOutputStream(bytes));
		CNDGeometry restored = new CNDGeometry();
		restored.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		assertEquals(source.corners(1, 1), restored.corners(1, 1));
		assertEquals(source.corners(2, 24), restored.corners(2, 24));
		assertEquals(source.corners(3, 48), restored.corners(3, 48));
	}

	@Test
	void numberingConversionsPreserveLegacyMapping() {
		assertArrayEquals(new int[] { 1, 2, 1 }, CNDGeometry.databaseToDetector(2, 48));
		assertArrayEquals(new int[] { 1, 2, 2 }, CNDGeometry.databaseToDetector(2, 1));
		assertEquals(48, CNDGeometry.databasePaddle(1, 2, 1));
		assertEquals(1, CNDGeometry.databasePaddle(1, 2, 2));
	}
}
