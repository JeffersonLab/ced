package edu.cnu.ced;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CedVersionTest {

	@Test
	void titleIdentifiesTheDevelopmentVersion() {
		assertEquals("CED 2.0.0-SNAPSHOT", CedVersion.title());
	}
}
