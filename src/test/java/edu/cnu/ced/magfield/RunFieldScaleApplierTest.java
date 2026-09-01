package edu.cnu.ced.magfield;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.RunConfig;

class RunFieldScaleApplierTest {

	@Test
	void appliesTheFirstConfigItSees() {
		List<double[]> calls = new ArrayList<>();
		RunFieldScaleApplier applier = new RunFieldScaleApplier(
				(torus, solenoid) -> calls.add(new double[] { torus, solenoid }));

		applier.apply(runConfig(19210, 1.0f, -1.0f));

		assertEquals(1, calls.size());
		assertEquals(1.0, calls.get(0)[0]);
		assertEquals(-1.0, calls.get(0)[1]);
	}

	@Test
	void doesNotReapplyForTheSameRun() {
		List<double[]> calls = new ArrayList<>();
		RunFieldScaleApplier applier = new RunFieldScaleApplier(
				(torus, solenoid) -> calls.add(new double[] { torus, solenoid }));

		applier.apply(runConfig(19210, 1.0f, -1.0f));
		applier.apply(runConfig(19210, 1.0f, -1.0f));
		applier.apply(runConfig(19210, 1.0f, -1.0f));

		assertEquals(1, calls.size());
	}

	@Test
	void reappliesWhenTheRunNumberChanges() {
		List<double[]> calls = new ArrayList<>();
		RunFieldScaleApplier applier = new RunFieldScaleApplier(
				(torus, solenoid) -> calls.add(new double[] { torus, solenoid }));

		applier.apply(runConfig(19210, 1.0f, -1.0f));
		applier.apply(runConfig(19211, 1.0f, 1.0f));

		assertEquals(2, calls.size());
		assertEquals(-1.0, calls.get(0)[1]);
		assertEquals(1.0, calls.get(1)[1]);
	}

	@Test
	void ignoresANullConfig() {
		List<double[]> calls = new ArrayList<>();
		RunFieldScaleApplier applier = new RunFieldScaleApplier(
				(torus, solenoid) -> calls.add(new double[] { torus, solenoid }));

		applier.apply(null);

		assertEquals(0, calls.size());
	}

	private static RunConfig runConfig(int run, float torus, float solenoid) {
		return new RunConfig(run, 1, 0L, 0L, (byte) 0, (byte) 0, solenoid, torus);
	}
}
