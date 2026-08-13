package edu.cnu.ced.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GeometryStatusTest {

	@Test
	void defensivelyCopiesDetectorLists() {
		ArrayList<String> cached = new ArrayList<>(List.of("CTOF"));
		GeometryStatus status = new GeometryStatus(true, cached, List.of(), null);
		cached.clear();
		assertEquals(List.of("CTOF"), status.cachedDetectors());
		assertThrows(UnsupportedOperationException.class,
				() -> status.cachedDetectors().add("other"));
		assertEquals("", status.error());
	}
}
