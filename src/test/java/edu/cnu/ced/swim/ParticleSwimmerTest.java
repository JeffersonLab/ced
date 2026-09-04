package edu.cnu.ced.swim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import cnuphys.magfield.ZeroProbe;

import edu.cnu.ced.geometry.Point3;

class ParticleSwimmerTest {

	private static final double POSITION_TOLERANCE = 1.0e-4;

	@Test
	void chargedParticleFollowsStraightLineInZeroField() {
		double theta = 60.0;
		double phi = 30.0;
		double p = 1.0;
		double pathLength = 100.0;

		double thetaRad = Math.toRadians(theta);
		double phiRad = Math.toRadians(phi);
		double sinTheta = Math.sin(thetaRad);

		SwimmableParticle particle = new SwimmableParticle(2212, 1, 1.0, 2.0, 3.0, p, theta, phi, 0);

		List<Point3> trajectory = ParticleSwimmer.swim(particle, new ZeroProbe(), pathLength);

		assertTrue(trajectory.size() >= 2, "expected at least a start and end point");
		Point3 start = trajectory.get(0);
		assertEquals(1.0, start.x(), POSITION_TOLERANCE);
		assertEquals(2.0, start.y(), POSITION_TOLERANCE);
		assertEquals(3.0, start.z(), POSITION_TOLERANCE);

		Point3 end = trajectory.get(trajectory.size() - 1);
		assertEquals(1.0 + pathLength * sinTheta * Math.cos(phiRad), end.x(), POSITION_TOLERANCE);
		assertEquals(2.0 + pathLength * sinTheta * Math.sin(phiRad), end.y(), POSITION_TOLERANCE);
		assertEquals(3.0 + pathLength * Math.cos(thetaRad), end.z(), POSITION_TOLERANCE);
	}

	@Test
	void neutralParticleAlsoProducesAStraightLine() {
		SwimmableParticle particle = new SwimmableParticle(22, 0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0);

		List<Point3> trajectory = ParticleSwimmer.swim(particle, new ZeroProbe(), 50.0);

		assertTrue(trajectory.size() >= 2);
		Point3 end = trajectory.get(trajectory.size() - 1);
		assertEquals(0.0, end.x(), POSITION_TOLERANCE);
		assertEquals(0.0, end.y(), POSITION_TOLERANCE);
		assertEquals(50.0, end.z(), POSITION_TOLERANCE);
	}

	@Test
	void zeroMomentumParticleReturnsEmptyTrajectory() {
		SwimmableParticle particle = new SwimmableParticle(2212, 1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0);

		assertTrue(ParticleSwimmer.swim(particle, new ZeroProbe()).isEmpty());
	}

	@Test
	void nullParticleOrProbeReturnsEmptyTrajectory() {
		SwimmableParticle particle = new SwimmableParticle(2212, 1, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0);

		assertTrue(ParticleSwimmer.swim(null, new ZeroProbe()).isEmpty());
		assertTrue(ParticleSwimmer.swim(particle, null).isEmpty());
	}
}
