package edu.cnu.ced.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CedApplicationConfigurationTest {

	@Test
	void shellMatchesThePlannedCedDesktop() {
		assertEquals("mdi-ced", CedApplication.APPLICATION_ID);
		assertEquals(18, CedApplication.VIRTUAL_DESKTOP_COLUMNS);
		assertEquals("images/cnu.png", CedApplication.BACKGROUND_RESOURCE);
	}
}
