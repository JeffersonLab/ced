package edu.cnu.ced.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.junit.jupiter.api.Test;

class FMTGeometryTest {

	@Test
	void sourceGeometryRoundTripsThroughCache() throws Exception {
		FMTGeometry source = new FMTGeometry();
		source.initializeFromSource();
		assertEquals(FMTGeometry.STRIP_COUNT, source.layer(0).strips().size());
		assertEquals(8, source.stripVertices(5, 1023).size());
		assertNotEquals(source.stripLine(0, 0).start(), source.stripLine(0, 0).end());

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		source.write(new DataOutputStream(bytes));
		FMTGeometry restored = new FMTGeometry();
		restored.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

		assertEquals(source.stripVertices(3, 511), restored.stripVertices(3, 511));
		assertEquals(source.localToGlobal(2, new Point3(1, 2, 3)),
				restored.localToGlobal(2, new Point3(1, 2, 3)));
	}

	@Test
	void preservesLegacyRegionsAndValidatesAddresses() {
		assertEquals(1, FMTGeometry.region(1));
		assertEquals(4, FMTGeometry.region(1024));
		assertEquals(0, FMTGeometry.region(0));
		FMTGeometry geometry = new FMTGeometry();
		assertThrows(IllegalStateException.class, () -> geometry.layer(0));
		assertThrows(IllegalArgumentException.class, () -> geometry.stripVertices(0, 1024));
	}
}
