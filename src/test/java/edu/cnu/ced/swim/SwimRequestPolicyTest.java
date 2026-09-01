package edu.cnu.ced.swim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.data.RecEventData;

class SwimRequestPolicyTest {

	// region bits, matching org.jlab.clas.detector.DetectorParticleStatus
	private static final int REGION = 1000;
	private static final int FORWARD = 2;
	private static final int CENTRAL = 4;

	@Test
	void centralOnlyStatusUsesCentralDetectorPathLength() {
		RecEventData.Particle central = particleWithStatus(CENTRAL * REGION);
		assertEquals(SwimRequestPolicy.CENTRAL_MAX_PATH_CM, SwimRequestPolicy.maxPathLengthCm(central));
	}

	@Test
	void statusSignIsTheTriggerParticleFlagNotDetectorRegion() {
		// A negative status is coatjava's "this is the trigger particle" flag
		// (see DetectorParticleStatus), not a Central-detector marker. A
		// forward-going trigger particle -- typically the electron -- must
		// still get the long forward path length even though its status is
		// negative; getting this backwards is exactly what silently
		// truncated a real forward electron's swim in production.
		RecEventData.Particle forwardTrigger = particleWithStatus(-(FORWARD * REGION));
		assertEquals(SwimRequestPolicy.FORWARD_MAX_PATH_CM, SwimRequestPolicy.maxPathLengthCm(forwardTrigger));

		RecEventData.Particle centralTrigger = particleWithStatus(-(CENTRAL * REGION));
		assertEquals(SwimRequestPolicy.CENTRAL_MAX_PATH_CM, SwimRequestPolicy.maxPathLengthCm(centralTrigger));
	}

	@Test
	void forwardStatusUsesForwardDetectorPathLength() {
		RecEventData.Particle forward = particleWithStatus(FORWARD * REGION);
		assertEquals(SwimRequestPolicy.FORWARD_MAX_PATH_CM, SwimRequestPolicy.maxPathLengthCm(forward));
	}

	@Test
	void combinedForwardAndCentralStatusPrefersTheLongerForwardPathLength() {
		// A track carrying both region bits (e.g. CVT+Forward combined)
		// still reaches all the way to the forward stack, so it needs the
		// longer swim, not the shorter Central-only one.
		RecEventData.Particle combined = particleWithStatus((FORWARD + CENTRAL) * REGION);
		assertEquals(SwimRequestPolicy.FORWARD_MAX_PATH_CM, SwimRequestPolicy.maxPathLengthCm(combined));
	}

	@Test
	void unassignedOrZeroStatusUsesForwardDetectorPathLength() {
		RecEventData.Particle zeroStatus = particleWithStatus(0);
		assertEquals(SwimRequestPolicy.FORWARD_MAX_PATH_CM, SwimRequestPolicy.maxPathLengthCm(zeroStatus));
	}

	@Test
	void nullParticleUsesForwardDetectorPathLength() {
		assertEquals(SwimRequestPolicy.FORWARD_MAX_PATH_CM, SwimRequestPolicy.maxPathLengthCm(null));
	}

	private static RecEventData.Particle particleWithStatus(int status) {
		return new RecEventData.Particle(0, 2212, 1, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, status);
	}
}
