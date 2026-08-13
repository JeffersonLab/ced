package edu.cnu.ced.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.junit.jupiter.api.Test;

class FTOFGeometryTest {

	@Test
	void sourceGeometryProvidesAllPanelsAndSectors() {
		FTOFGeometry geometry = new FTOFGeometry();
		geometry.initializeFromSource();
		assertEquals(23, geometry.paddleCount(FTOFGeometry.PANEL_1A));
		assertEquals(62, geometry.paddleCount(FTOFGeometry.PANEL_1B));
		assertEquals(5, geometry.paddleCount(FTOFGeometry.PANEL_2));
		assertEquals(8, geometry.paddle(1, FTOFGeometry.PANEL_1A, 1).corners().size());
		assertEquals(4, geometry.paddle(6, FTOFGeometry.PANEL_2, 5).projectionEdges().size());
		assertEquals(4, geometry.frontFace(3, FTOFGeometry.PANEL_1B, 30).size());
		assertEquals(24, geometry.verticesCm(4, FTOFGeometry.PANEL_1A, 10).length);
		assertThrows(IllegalArgumentException.class,
				() -> geometry.paddle(1, FTOFGeometry.PANEL_2, 6));
	}

	@Test
	void payloadRestoresRepresentativePaddlesExactly() throws Exception {
		FTOFGeometry source = new FTOFGeometry();
		source.initializeFromSource();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		source.write(new DataOutputStream(bytes));
		FTOFGeometry restored = new FTOFGeometry();
		restored.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		assertEquals(source.paddle(1, FTOFGeometry.PANEL_1A, 1),
				restored.paddle(1, FTOFGeometry.PANEL_1A, 1));
		assertEquals(source.paddle(3, FTOFGeometry.PANEL_1B, 31),
				restored.paddle(3, FTOFGeometry.PANEL_1B, 31));
		assertEquals(source.paddle(6, FTOFGeometry.PANEL_2, 5),
				restored.paddle(6, FTOFGeometry.PANEL_2, 5));
	}
}
