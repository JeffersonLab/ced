package edu.cnu.ced.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.cnu.ced.data.RecEventData;

class PidLegendTest {

	@Test
	void showsOneEntryPerDistinctSpecies() {
		PidLegend legend = new PidLegend();
		legend.update(List.of(
				particle(11, -1), // e-
				particle(2212, 1), // p
				particle(2212, 1), // another proton -- same species, not a second entry
				particle(22, 0))); // gamma

		assertEquals(3, legend.species().size());
	}

	@Test
	void sortsSpeciesByPidThenCharge() {
		PidLegend legend = new PidLegend();
		legend.update(List.of(particle(2212, 1), particle(11, -1), particle(22, 0)));

		List<PidLegend.Species> species = legend.species();
		assertEquals(11, species.get(0).pid());
		assertEquals(22, species.get(1).pid());
		assertEquals(2212, species.get(2).pid());
	}

	@Test
	void distinguishesSpeciesByChargeToo() {
		// pi+ and pi- are different species (different LundStyle color) even
		// though a naive pid-only key would conflate them if it ignored sign.
		PidLegend legend = new PidLegend();
		legend.update(List.of(particle(211, 1), particle(-211, -1)));

		assertEquals(2, legend.species().size());
	}

	@Test
	void emptyEventShowsNoEntries() {
		PidLegend legend = new PidLegend();
		legend.update(List.of());

		assertTrue(legend.species().isEmpty());
	}

	@Test
	void repeatedIdenticalUpdatesStayIdempotent() {
		PidLegend legend = new PidLegend();
		List<RecEventData.Particle> particles = List.of(particle(11, -1), particle(2212, 1));
		legend.update(particles);
		legend.update(particles);

		assertEquals(2, legend.species().size());
	}

	private static RecEventData.Particle particle(int pid, int charge) {
		return new RecEventData.Particle(0, pid, charge, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0);
	}
}
