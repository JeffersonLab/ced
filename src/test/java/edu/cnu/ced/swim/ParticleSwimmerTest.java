package edu.cnu.ced.swim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

	// The four surface-stopping swims below all use a straight (zero-field)
	// trajectory along a single axis, so the correct stopping point is
	// exactly computable rather than merely "plausible".

	@Test
	void swimToFixedZStopsAtTheTargetZ() {
		// theta=0: a straight line along +z from the origin.
		SwimmableParticle particle = new SwimmableParticle(2212, 1, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0);

		List<Point3> trajectory = ParticleSwimmer.swimToFixedZ(particle, new ZeroProbe(), 50.0, 0.01, 200.0);

		assertTrue(trajectory.size() >= 2);
		Point3 end = trajectory.get(trajectory.size() - 1);
		assertEquals(50.0, end.z(), 0.1);
	}

	@Test
	void swimToFixedRhoStopsAtTheTargetRadius() {
		// theta=90, phi=0: a straight line along +x from the origin, so rho == x.
		SwimmableParticle particle = new SwimmableParticle(2212, 1, 0.0, 0.0, 0.0, 1.0, 90.0, 0.0, 0);

		List<Point3> trajectory = ParticleSwimmer.swimToFixedRho(particle, new ZeroProbe(), 30.0, 0.01, 200.0);

		assertTrue(trajectory.size() >= 2);
		Point3 end = trajectory.get(trajectory.size() - 1);
		assertEquals(30.0, Math.hypot(end.x(), end.y()), 0.1);
	}

	@Test
	void swimToPlaneStopsAtThePlane() {
		// A plane perpendicular to z at z=80 -- equivalent to swimToFixedZ(80).
		SwimmableParticle particle = new SwimmableParticle(2212, 1, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0);

		List<Point3> trajectory = ParticleSwimmer.swimToPlane(particle, new ZeroProbe(),
				new double[] { 0, 0, 1 }, new double[] { 0, 0, 80 }, 0.01, 200.0);

		assertTrue(trajectory.size() >= 2);
		Point3 end = trajectory.get(trajectory.size() - 1);
		assertEquals(80.0, end.z(), 0.1);
	}

	@Test
	void swimToCylinderStopsAtTheCylinderWall() {
		// A cylinder centered on the z axis, radius 40 -- theta=90, phi=0
		// moves straight along +x, so it should stop at x == 40.
		SwimmableParticle particle = new SwimmableParticle(2212, 1, 0.0, 0.0, 0.0, 1.0, 90.0, 0.0, 0);

		List<Point3> trajectory = ParticleSwimmer.swimToCylinder(particle, new ZeroProbe(),
				new double[] { 0, 0, -100 }, new double[] { 0, 0, 100 }, 40.0, 0.01, 200.0);

		assertTrue(trajectory.size() >= 2);
		Point3 end = trajectory.get(trajectory.size() - 1);
		assertEquals(40.0, Math.hypot(end.x(), end.y()), 0.1);
	}

	// The *Outcome swims below back the swim-test view's own batch tester --
	// unlike their plain counterparts above, they keep the partial
	// trajectory even when the target wasn't reached.

	@Test
	void swimOutcomeSucceedsForANormalSwim() {
		SwimmableParticle particle = new SwimmableParticle(2212, 1, 0.0, 0.0, 0.0, 1.0, 60.0, 30.0, 0);

		ParticleSwimmer.Outcome outcome = ParticleSwimmer.swimOutcome(particle, new ZeroProbe(), 100.0);

		assertTrue(outcome.success());
		assertTrue(outcome.trajectory().size() >= 2);
	}

	@Test
	void swimToFixedZOutcomeSucceedsWhenTheTargetIsReached() {
		SwimmableParticle particle = new SwimmableParticle(2212, 1, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0);

		ParticleSwimmer.Outcome outcome = ParticleSwimmer.swimToFixedZOutcome(particle, new ZeroProbe(), 50.0, 0.01, 200.0);

		assertTrue(outcome.success());
		Point3 end = outcome.trajectory().get(outcome.trajectory().size() - 1);
		assertEquals(50.0, end.z(), 0.1);
	}

	@Test
	void swimToFixedZOutcomeFailsButKeepsThePartialPathWhenUnreachable() {
		// theta=0, straight line along +z, but the max path length (10 cm)
		// is far short of the target z (1000 cm) -- can never get there.
		SwimmableParticle particle = new SwimmableParticle(2212, 1, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0);

		ParticleSwimmer.Outcome outcome = ParticleSwimmer.swimToFixedZOutcome(particle, new ZeroProbe(), 1000.0, 0.01, 10.0);

		assertFalse(outcome.success());
		assertTrue(outcome.trajectory().size() >= 2, "a failed swim should still keep the path it managed");
		Point3 end = outcome.trajectory().get(outcome.trajectory().size() - 1);
		assertTrue(end.z() < 1000.0, "the partial path should fall well short of the unreachable target");
	}

	@Test
	void outcomeMethodsReturnNoneForNullOrZeroMomentum() {
		SwimmableParticle zeroMomentum = new SwimmableParticle(2212, 1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0);
		SwimmableParticle particle = new SwimmableParticle(2212, 1, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0);

		assertEquals(ParticleSwimmer.Outcome.NONE, ParticleSwimmer.swimOutcome(null, new ZeroProbe(), 100.0));
		assertEquals(ParticleSwimmer.Outcome.NONE, ParticleSwimmer.swimOutcome(particle, null, 100.0));
		assertEquals(ParticleSwimmer.Outcome.NONE, ParticleSwimmer.swimOutcome(zeroMomentum, new ZeroProbe(), 100.0));
	}
}
