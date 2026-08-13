package edu.cnu.ced.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CedLaunchOptionsTest {

	@AfterEach
	void clearGeometryVariation() {
		System.clearProperty("GEOVARIATION");
	}

	@Test
	void defaultsEnableProduction3DConfiguration() {
		CedLaunchOptions options = CedLaunchOptions.parse(null);

		assertFalse(options.experimental());
		assertTrue(options.enable3D());
		assertEquals("default", options.geometryVariation());
	}

	@Test
	void parsesLegacyFlagsWithoutTreatingPathAsAnOption() {
		System.setProperty("GEOVARIATION", "  rga_fall2018  ");

		CedLaunchOptions options = CedLaunchOptions.parse(
				new String[] { "-p", "/tmp/EXP-NO3D", "experimental", "NO3D" });

		assertTrue(options.experimental());
		assertFalse(options.enable3D());
		assertEquals("rga_fall2018", options.geometryVariation());
	}

	@Test
	void blankGeometryVariationUsesDefault() {
		System.setProperty("GEOVARIATION", "  ");

		assertEquals("default", CedLaunchOptions.parse(new String[0]).geometryVariation());
	}
}
