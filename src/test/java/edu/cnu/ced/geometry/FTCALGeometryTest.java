package edu.cnu.ced.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.junit.jupiter.api.Test;

class FTCALGeometryTest {

	@Test
	void sourceGeometryHasSparseIdsAndReversibleGrid() {
		FTCALGeometry geometry = new FTCALGeometry();
		geometry.initializeFromSource();
		assertEquals(332, geometry.componentIds().size());
		assertTrue(geometry.isValidComponent(8));
		assertTrue(geometry.isValidComponent(475));
		assertFalse(geometry.isValidComponent(1));
		GridIndex grid = geometry.gridIndex(8).orElseThrow();
		assertEquals(8, geometry.componentAt(grid).orElseThrow());
		assertEquals(24, geometry.verticesCm(8).length);
	}

	@Test
	void payloadRestoresRepresentativeComponentsExactly() throws Exception {
		FTCALGeometry source = new FTCALGeometry();
		source.initializeFromSource();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		source.write(new DataOutputStream(bytes));
		FTCALGeometry restored = new FTCALGeometry();
		restored.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
		assertEquals(source.componentIds(), restored.componentIds());
		assertEquals(source.corners(8), restored.corners(8));
		assertEquals(source.corners(475), restored.corners(475));
		assertEquals(source.gridIndex(250), restored.gridIndex(250));
	}

	@Test
	void gridConversionPreservesLegacyZeroGap() {
		assertEquals(-11, FTCALGeometry.valueToGridIndex(-16.0));
		assertEquals(-1, FTCALGeometry.valueToGridIndex(-0.5));
		assertEquals(1, FTCALGeometry.valueToGridIndex(0.5));
		assertEquals(11, FTCALGeometry.valueToGridIndex(16.0));
	}
}
