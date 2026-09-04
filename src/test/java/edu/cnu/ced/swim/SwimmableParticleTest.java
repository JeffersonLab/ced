package edu.cnu.ced.swim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cnuphys.lund.LundSupport;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.data.TrackRow;

class SwimmableParticleTest {

	@Test void ofRecEventDataParticleConvertsRadiansToDegrees() {
		// px=py=0, pz=1 GeV/c -> theta=0; px=1,py=0,pz=0 -> phi=0.
		RecEventData.Particle particle = new RecEventData.Particle(0, 2212, 1,
				1f, 0f, 0f, 1f, 2f, 3f, 0f, 0f, 0f, 4000);

		SwimmableParticle swimmable = SwimmableParticle.of(particle);

		assertEquals(2212, swimmable.pid());
		assertEquals(1, swimmable.charge());
		assertEquals(1.0, swimmable.vx(), 1e-9);
		assertEquals(2.0, swimmable.vy(), 1e-9);
		assertEquals(3.0, swimmable.vz(), 1e-9);
		assertEquals(1.0, swimmable.p(), 1e-6);
		assertEquals(90.0, swimmable.thetaDeg(), 1e-4);
		assertEquals(0.0, swimmable.phiDeg(), 1e-6);
		assertEquals(4000, swimmable.status());
	}

	@Test void ofTrackRowConvertsMevToGevWithoutATrigRoundTrip() {
		TrackRow track = TrackRow.fromMomentum(0, LundSupport.getInstance().get(2212, 1),
				1.0, 2.0, 3.0, 0.5, 0.0, 0.0, 0, "MC::Particle").orElseThrow();

		SwimmableParticle swimmable = SwimmableParticle.of(track);

		assertEquals(track.pid(), swimmable.pid());
		assertEquals(track.charge(), swimmable.charge());
		assertEquals(track.x0(), swimmable.vx(), 1e-9);
		assertEquals(track.y0(), swimmable.vy(), 1e-9);
		assertEquals(track.z0(), swimmable.vz(), 1e-9);
		assertEquals(track.momentumMeV() / 1000.0, swimmable.p(), 1e-9);
		assertEquals(track.thetaDeg(), swimmable.thetaDeg(), 1e-9);
		assertEquals(track.phiDeg(), swimmable.phiDeg(), 1e-9);
		assertEquals(track.status(), swimmable.status());
	}
}
