package edu.cnu.ced.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.junit.jupiter.api.Test;

class CTOFGeometryTest {

	@Test
	void analyticGeometryHasExpectedRadiiAndDefensiveVertices() {
		CTOFGeometry geometry = new CTOFGeometry();
		geometry.initializeFromSource();
		assertEquals(CTOFGeometry.INNER_RADIUS_MM, geometry.quad(1).get(0).x(), 1.0e-9);
		assertEquals(0.0, geometry.quad(1).get(0).y(), 1.0e-9);
		assertEquals(CTOFGeometry.OUTER_RADIUS_MM, geometry.quad(1).get(1).x(), 1.0e-9);
		float[] first = geometry.verticesCm(1);
		float[] second = geometry.verticesCm(1);
		assertNotSame(first, second);
		first[0] = 0;
		assertEquals((float) (CTOFGeometry.INNER_RADIUS_MM / 10), second[0], 1.0e-6f);
		assertThrows(IllegalArgumentException.class, () -> geometry.quad(0));
	}

	@Test
	void cachePayloadRestoresExactGeometry() throws Exception {
		CTOFGeometry source = new CTOFGeometry();
		source.initializeFromSource();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		source.write(new DataOutputStream(bytes));
		CTOFGeometry restored = new CTOFGeometry();
		restored.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		assertEquals(source.quad(17), restored.quad(17));
		assertEquals(source.quad(48), restored.quad(48));
	}
}
