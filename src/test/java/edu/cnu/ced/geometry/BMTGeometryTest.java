package edu.cnu.ced.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.junit.jupiter.api.Test;

class BMTGeometryTest {

	@Test
	void sourceGeometryRoundTripsThroughCache() throws Exception {
		BMTGeometry source = new BMTGeometry();
		source.initializeFromSource();
		assertEquals(1, source.layer(1).number());
		assertEquals(3, source.layer(6).region());
		assertTrue(source.pitchGroups(1).size() > 1);

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		source.write(new DataOutputStream(bytes));
		BMTGeometry restored = new BMTGeometry();
		restored.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

		assertEquals(source.layer(4), restored.layer(4));
		assertEquals(source.pitchGroups(3), restored.pitchGroups(3));
	}

	@Test
	void validatesAddressesAndInitialization() {
		BMTGeometry geometry = new BMTGeometry();
		assertThrows(IllegalStateException.class, () -> geometry.layer(1));
		assertThrows(IllegalStateException.class, () -> geometry.pitchGroups(1));
	}
}
