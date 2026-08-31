package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParticleIdTest {

	@Test
	void namesCommonFinalStateSpecies() {
		assertEquals("e-", ParticleId.name(11, -1));
		assertEquals("e+", ParticleId.name(-11, 1));
		assertEquals("gamma", ParticleId.name(22, 0));
		assertEquals("p", ParticleId.name(2212, 1));
		assertEquals("n", ParticleId.name(2112, 0));
		assertEquals("pi+", ParticleId.name(211, 1));
		assertEquals("pi-", ParticleId.name(-211, -1));
		assertEquals("K+", ParticleId.name(321, 1));
		assertEquals("K-", ParticleId.name(-321, -1));
	}

	@Test
	void fallsBackToChargeBasedNameForUnrecognizedPid() {
		assertEquals("unknown+", ParticleId.name(999999, 1));
		assertEquals("unknown-", ParticleId.name(999999, -1));
		assertEquals("unknown0", ParticleId.name(999999, 0));
	}

	@Test
	void zeroPidIsTreatedAsUnrecognized() {
		assertFalse(ParticleId.isKnown(0));
		assertEquals("unknown0", ParticleId.name(0, 0));
	}

	@Test
	void isKnownReflectsTheSameTableUsedByName() {
		assertTrue(ParticleId.isKnown(2212));
		assertFalse(ParticleId.isKnown(-999999));
	}
}
