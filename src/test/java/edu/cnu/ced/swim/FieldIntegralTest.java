package edu.cnu.ced.swim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import cnuphys.magfield.FieldProbe;
import cnuphys.magfield.ZeroProbe;

import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.swim.FieldIntegral.Sample;

class FieldIntegralTest {

	private static final double TOLERANCE = 1.0e-6;

	@Test
	void zeroFieldProducesZeroIntegralButRealPathLength() {
		List<Point3> trajectory = List.of(new Point3(0, 0, 0), new Point3(10, 0, 0), new Point3(20, 0, 0));

		List<Sample> samples = FieldIntegral.compute(trajectory, new ZeroProbe());

		assertEquals(3, samples.size());
		assertEquals(0, samples.get(0).pathLengthCm(), TOLERANCE);
		assertEquals(10, samples.get(1).pathLengthCm(), TOLERANCE);
		assertEquals(20, samples.get(2).pathLengthCm(), TOLERANCE);
		for (Sample sample : samples) {
			assertEquals(0, sample.cumulativeIntegralKgCm(), TOLERANCE);
		}
	}

	@Test
	void uniformFieldAlongZGivesAHandCheckableIntegral() {
		// B = (0, 0, 2) kG everywhere, straight-line trajectory along +x --
		// each 10cm segment has dL = (10, 0, 0), so B x dL = (0, 20, 0), a
		// magnitude of exactly 20 kG*cm per segment.
		FieldProbe uniformZ = new FieldProbe(null) {
			@Override public String getName() { return "uniform-z-test-probe"; }
			@Override public void field(float x, float y, float z, float[] result) {
				result[0] = 0f; result[1] = 0f; result[2] = 2f;
			}
			@Override public void gradient(float x, float y, float z, float[] result) {
				result[0] = 0f; result[1] = 0f; result[2] = 0f;
			}
			@Override public float fieldMagnitude(float x, float y, float z) { return 2f; }
			@Override public float getMaxFieldMagnitude() { return 2f; }
			@Override public boolean isZeroField() { return false; }
			@Override public boolean contains(double x, double y, double z) { return true; }
		};
		List<Point3> trajectory = List.of(new Point3(0, 0, 0), new Point3(10, 0, 0), new Point3(20, 0, 0));

		List<Sample> samples = FieldIntegral.compute(trajectory, uniformZ);

		assertEquals(3, samples.size());
		assertEquals(0, samples.get(0).cumulativeIntegralKgCm(), TOLERANCE);
		assertEquals(20, samples.get(1).cumulativeIntegralKgCm(), TOLERANCE);
		assertEquals(40, samples.get(2).cumulativeIntegralKgCm(), TOLERANCE);
	}

	@Test
	void fewerThanTwoPointsProducesNoSamples() {
		assertTrue(FieldIntegral.compute(List.of(), new ZeroProbe()).isEmpty());
		assertTrue(FieldIntegral.compute(List.of(new Point3(0, 0, 0)), new ZeroProbe()).isEmpty());
	}

	@Test
	void nullTrajectoryProducesNoSamples() {
		assertTrue(FieldIntegral.compute(null, new ZeroProbe()).isEmpty());
	}
}
