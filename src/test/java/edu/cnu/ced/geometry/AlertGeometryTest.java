package edu.cnu.ced.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.junit.jupiter.api.Test;

class AlertGeometryTest {
	@Test void sourceGeometryRoundTripsThroughCache() throws Exception {
		AlertGeometry source = new AlertGeometry(); source.initializeFromSource();
		assertTrue(source.dcLayerCount() > 0); assertTrue(source.tofLayerCount() > 0);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(); source.write(new DataOutputStream(bytes));
		AlertGeometry restored = new AlertGeometry(); restored.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		assertEquals(source.dcLayerCount(), restored.dcLayerCount());
		assertEquals(source.tofLayerCount(), restored.tofLayerCount());
		assertEquals(source.dcWires(0, 0, 0), restored.dcWires(0, 0, 0));
		assertEquals(source.tofPaddles(0, 0, 0), restored.tofPaddles(0, 0, 0));
	}
	@Test void requiresInitialization() { assertThrows(IllegalStateException.class, () -> new AlertGeometry().dcLayerCount()); }
}
