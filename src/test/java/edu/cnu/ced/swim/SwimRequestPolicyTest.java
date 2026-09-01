package edu.cnu.ced.swim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.data.RecEventData;

class SwimRequestPolicyTest {

	@Test
	void negativeStatusUsesCentralDetectorPathLength() {
		RecEventData.Particle central = particleWithStatus(-2000);
		assertEquals(SwimRequestPolicy.CENTRAL_MAX_PATH_CM, SwimRequestPolicy.maxPathLengthCm(central));
	}

	@Test
	void nonNegativeStatusUsesForwardDetectorPathLength() {
		RecEventData.Particle forward = particleWithStatus(2000);
		assertEquals(SwimRequestPolicy.FORWARD_MAX_PATH_CM, SwimRequestPolicy.maxPathLengthCm(forward));

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
