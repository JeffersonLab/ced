package edu.cnu.ced.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.junit.jupiter.api.Test;

class URWTGeometryTest {
	@Test void sourceGeometryRoundTripsThroughCache() throws Exception {
		URWTGeometry source = new URWTGeometry(); source.initializeFromSource();
		assertTrue(source.detector(1, 1).strips().size() > 0);
		assertNotEquals(source.strip(1, 1, 1).start(), source.strip(1, 1, 1).end());
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(); source.write(new DataOutputStream(bytes));
		URWTGeometry restored = new URWTGeometry(); restored.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		assertEquals(source.detector(6, 4), restored.detector(6, 4));
		assertEquals(source.detector(3, 2).centroid(), restored.detector(3, 2).centroid());
	}
	@Test void validatesInitializationAndAddresses() {
		URWTGeometry geometry = new URWTGeometry();
		assertThrows(IllegalStateException.class, () -> geometry.detector(1, 1));
		assertThrows(IllegalArgumentException.class, () -> geometry.detector(7, 1));
	}
}
