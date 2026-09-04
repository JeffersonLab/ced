package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventSnapshot;

class MonteCarloTracksTest {

	@Test void readsBothParticleAndLundBanksIntoOneList() {
		DataBank particle = bank(2212, 1f, 0f, 0f, 0f, 0f, 0.3f);
		DataBank lund = bank(211, 0f, 0f, 0f, 0.1f, 0f, 0f);

		MonteCarloTracks tracks = MonteCarloTracks.from(EventSnapshot.of(
				event(Map.of(MonteCarloTracks.PARTICLE_BANK, particle, MonteCarloTracks.LUND_BANK, lund))));

		assertEquals(2, tracks.tracks().size());
		assertEquals(MonteCarloTracks.PARTICLE_BANK, tracks.tracks().get(0).source());
		assertEquals("p", tracks.tracks().get(0).name());
		assertEquals(MonteCarloTracks.LUND_BANK, tracks.tracks().get(1).source());
		// cnuphys.lund's own registered name for pid 211, not this app's ASCII
		// ParticleId.name("pi+") -- matches what legacy CED's trajectory table
		// itself shows, since it also reads LundId.getName() directly.
		assertEquals("π⁺", tracks.tracks().get(1).name());
	}

	@Test void anUnregisteredPidStillShowsAsARowRatherThanDisappearing() {
		// 999999 isn't a real PDG/Lund id -- every kinematic field is still
		// known, so the row should show up (with an unresolved placeholder
		// species carrying the real pid), not vanish the way legacy's
		// swimming-oriented view would skip it.
		DataBank particle = bank(999999, 0f, 0f, 0f, 0f, 0f, 0.3f);

		MonteCarloTracks tracks = MonteCarloTracks.from(
				EventSnapshot.of(event(Map.of(MonteCarloTracks.PARTICLE_BANK, particle))));

		assertEquals(1, tracks.tracks().size());
		TrackRow row = tracks.tracks().get(0);
		assertEquals(999999, row.pid());
		assertEquals("?", row.name());
	}

	@Test void emptyOrMissingBanksYieldNoTracks() {
		assertTrue(MonteCarloTracks.from(EventSnapshot.empty()).tracks().isEmpty());
		assertTrue(MonteCarloTracks.from(null).tracks().isEmpty());
	}

	private static DataBank bank(int pid, float vx, float vy, float vz, float px, float py, float pz) {
		Map<String, Object> values = Map.of("pid", new int[] { pid },
				"vx", new float[] { vx }, "vy", new float[] { vy }, "vz", new float[] { vz },
				"px", new float[] { px }, "py", new float[] { py }, "pz", new float[] { pz });
		return (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getColumnList" -> values.keySet().toArray(String[]::new);
					case "rows" -> 1;
					case "getInt" -> ((int[]) values.get(args[0]))[(int) args[1]];
					case "getFloat" -> ((float[]) values.get(args[0]))[(int) args[1]];
					default -> null;
				});
	}

	private static DataEvent event(Map<String, DataBank> banks) {
		return (DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
				new Class<?>[] { DataEvent.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getBankList" -> banks.keySet().toArray(String[]::new);
					case "hasBank" -> banks.containsKey(args[0]);
					case "getBank" -> banks.get(args[0]);
					default -> null;
				});
	}
}
