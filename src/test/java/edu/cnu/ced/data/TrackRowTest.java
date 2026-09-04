package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cnuphys.lund.LundSupport;
import org.junit.jupiter.api.Test;

class TrackRowTest {

	@Test void derivesMomentumThetaPhiInMevAndDegrees() {
		// along +x: theta = 90 deg, phi = 0 deg
		TrackRow row = TrackRow.fromMomentum(1, LundSupport.getInstance().get(2212, 1), 0, 0, 0,
				0.5, 0, 0, 0, "test").orElseThrow();
		assertEquals(500.0, row.momentumMeV(), 1e-9);
		assertEquals(90.0, row.thetaDeg(), 1e-9);
		assertEquals(0.0, row.phiDeg(), 1e-9);
	}

	@Test void alongBeamAxisIsZeroDegreesTheta() {
		TrackRow row = TrackRow.fromMomentum(1, LundSupport.getInstance().get(2212, 1), 0, 0, 0,
				0, 0, 1.0, 0, "test").orElseThrow();
		assertEquals(0.0, row.thetaDeg(), 1e-9);
	}

	@Test void nonPositiveOrNonFiniteMomentumYieldsEmpty() {
		assertTrue(TrackRow.fromMomentum(1, LundSupport.getInstance().get(2212, 1), 0, 0, 0,
				0, 0, 0, 0, "test").isEmpty(), "zero momentum has no direction");
		assertTrue(TrackRow.fromMomentum(1, LundSupport.getInstance().get(2212, 1), 0, 0, 0,
				Double.NaN, 0, 1, 0, "test").isEmpty());
	}

	@Test void energyAndMassFollowFromTheResolvedParticle() {
		// proton: mass ~0.938272 GeV -> ~938.272 MeV
		TrackRow row = TrackRow.fromMomentum(1, LundSupport.getInstance().get(2212, 1), 0, 0, 0,
				0, 0, 1.0, 0, "test").orElseThrow();
		assertEquals(938.272, row.massMeV(), 0.01);
		assertEquals(1, row.charge());
		assertEquals("p", row.name());
		assertEquals(Math.sqrt(1000.0 * 1000.0 + row.massMeV() * row.massMeV()), row.totalEnergyMeV(), 1e-6);
		assertEquals(row.totalEnergyMeV() - row.massMeV(), row.kineticEnergyMeV(), 1e-9);
	}

	@Test void syntheticPidsAreFlaggedAndRealOnesAreNot() {
		assertTrue(syntheticRow(LundSupport.unknownPlus).isSyntheticPid());
		assertTrue(syntheticRow(LundSupport.getTrackbased(1)).isSyntheticPid());
		assertTrue(syntheticRow(LundSupport.getHitbased(-1)).isSyntheticPid());
		assertTrue(syntheticRow(LundSupport.getCVTbased(0)).isSyntheticPid());
		assertFalse(syntheticRow(LundSupport.getInstance().get(2212, 1)).isSyntheticPid());
	}

	private static TrackRow syntheticRow(cnuphys.lund.LundId particle) {
		return TrackRow.fromMomentum(1, particle, 0, 0, 0, 0, 0, 1.0, 0, "test").orElseThrow();
	}
}
