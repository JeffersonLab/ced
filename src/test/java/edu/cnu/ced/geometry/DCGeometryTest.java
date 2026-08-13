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

class DCGeometryTest {

	@Test
	void sourceGeometryRoundTripsAndRotatesSectors() throws Exception {
		DCGeometry source = new DCGeometry();
		source.initializeFromSource();
		assertEquals(12, source.wire(1, 1, 1).volume().size());
		assertTrue(source.absoluteMaxWireX() > 0.0);
		assertNotEquals(source.wireLine(1, 1, 1, 1), source.wireLine(2, 1, 1, 1));

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		source.write(new DataOutputStream(bytes));
		DCGeometry restored = new DCGeometry();
		restored.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

		assertEquals(source.wire(6, 6, 112), restored.wire(6, 6, 112));
		assertEquals(source.absoluteMaxWireX(), restored.absoluteMaxWireX());
	}

	@Test
	void validatesAddressesAndInitialization() {
		DCGeometry geometry = new DCGeometry();
		assertThrows(IllegalStateException.class, () -> geometry.wire(1, 1, 1));
		assertThrows(IllegalArgumentException.class, () -> geometry.wireLine(0, 1, 1, 1));
		assertThrows(IllegalArgumentException.class, () -> geometry.wire(7, 1, 1));
	}
}
