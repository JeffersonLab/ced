package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventSnapshot;

class RecEventDataTest {

	@Test
	void readsParticlesWithOptionalColumns() {
		DataBank recon = bank(
				new String[] { "pid", "charge", "px", "py", "pz", "vx", "vy", "vz", "vt", "beta", "chi2pid",
						"status" },
				1,
				Map.ofEntries(
						Map.entry("pid", new int[] { 2212 }), Map.entry("charge", new byte[] { 1 }),
						Map.entry("px", new float[] { 0.3f }), Map.entry("py", new float[] { 0f }),
						Map.entry("pz", new float[] { 0.4f }),
						Map.entry("vx", new float[] { 1f }), Map.entry("vy", new float[] { 2f }),
						Map.entry("vz", new float[] { 3f }),
						Map.entry("vt", new float[] { 5.5f }), Map.entry("beta", new float[] { 0.6f }),
						Map.entry("chi2pid", new float[] { 1.2f }),
						Map.entry("status", new short[] { 2000 })));

		RecEventData data = RecEventData.from(EventSnapshot.of(event(Map.of(RecEventData.RECON_BANK, recon))));

		assertEquals(1, data.particles().size());
		RecEventData.Particle p = data.particles().get(0);
		assertEquals(2212, p.pid());
		assertEquals(1, p.charge());
		assertEquals(0.4f, p.pz());
		assertEquals(3f, p.vz());
		assertEquals(5.5f, p.vt());
		assertEquals(0.6f, p.beta());
		assertEquals(1.2f, p.chi2pid());
		assertEquals(2000, p.status());
		assertEquals(0.5f, p.p(), 1e-6f);
		assertEquals("p", p.displayName());
	}

	@Test
	void defaultsMissingOptionalColumnsToZero() {
		DataBank recon = bank(
				new String[] { "pid", "charge", "px", "py", "pz", "vx", "vy", "vz", "status" },
				1,
				Map.of("pid", new int[] { 211 }, "charge", new byte[] { 1 },
						"px", new float[] { 0.1f }, "py", new float[] { 0.1f }, "pz", new float[] { 0.1f },
						"vx", new float[] { 0f }, "vy", new float[] { 0f }, "vz", new float[] { 0f },
						"status", new short[] { 2000 }));

		RecEventData data = RecEventData.from(EventSnapshot.of(event(Map.of(RecEventData.RECON_BANK, recon))));

		RecEventData.Particle p = data.particles().get(0);
		assertEquals(0f, p.vt());
		assertEquals(0f, p.beta());
		assertEquals(0f, p.chi2pid());
	}

	@Test
	void returnsEmptyWhenRequiredColumnsMissing() {
		DataBank recon = bank(new String[] { "pid", "charge" }, 1,
				Map.of("pid", new int[] { 11 }, "charge", new byte[] { -1 }));

		RecEventData data = RecEventData.from(EventSnapshot.of(event(Map.of(RecEventData.RECON_BANK, recon))));

		assertTrue(data.particles().isEmpty());
	}

	@Test
	void trackSectorPrefersRecTrackDcRowOverMomentumProxy() {
		DataBank recon = bank(
				new String[] { "pid", "charge", "px", "py", "pz", "vx", "vy", "vz", "status" },
				1,
				Map.of("pid", new int[] { 2212 }, "charge", new byte[] { 1 },
						"px", new float[] { 1f }, "py", new float[] { 0f }, "pz", new float[] { 0f },
						"vx", new float[] { 0f }, "vy", new float[] { 0f }, "vz", new float[] { 0f },
						"status", new short[] { 2000 }));
		// Momentum along +x puts this particle's proxy sector at 1 (phi =
		// 0), but its real DC track fit says sector 4 -- standing in for a
		// real event where the momentum phi lands close enough to a
		// 60-degree boundary for the two to disagree.
		DataBank track = bank(
				new String[] { "pindex", "detector", "sector" },
				1,
				Map.of("pindex", new short[] { 0 }, "detector", new byte[] { 6 }, "sector", new byte[] { 4 }));

		RecEventData data = RecEventData.from(EventSnapshot.of(event(
				Map.of(RecEventData.RECON_BANK, recon, RecEventData.TRACK_BANK, track))));

		RecEventData.Particle p = data.particles().get(0);
		assertEquals(1, p.sector());
		assertEquals(4, data.trackSector(p.row()).orElseThrow());
	}

	@Test
	void trackSectorIsEmptyWithoutAMatchingDcTrackRow() {
		DataBank recon = bank(
				new String[] { "pid", "charge", "px", "py", "pz", "vx", "vy", "vz", "status" },
				1,
				Map.of("pid", new int[] { 22 }, "charge", new byte[] { 0 },
						"px", new float[] { 1f }, "py", new float[] { 0f }, "pz", new float[] { 0f },
						"vx", new float[] { 0f }, "vy", new float[] { 0f }, "vz", new float[] { 0f },
						"status", new short[] { 2000 }));

		RecEventData data = RecEventData.from(EventSnapshot.of(event(Map.of(RecEventData.RECON_BANK, recon))));

		assertTrue(data.trackSector(data.particles().get(0).row()).isEmpty());
	}

	@Test
	void trackSectorIgnoresNonDcDetectorRows() {
		DataBank recon = bank(
				new String[] { "pid", "charge", "px", "py", "pz", "vx", "vy", "vz", "status" },
				1,
				Map.of("pid", new int[] { 2212 }, "charge", new byte[] { 1 },
						"px", new float[] { 1f }, "py", new float[] { 0f }, "pz", new float[] { 0f },
						"vx", new float[] { 0f }, "vy", new float[] { 0f }, "vz", new float[] { 0f },
						"status", new short[] { 4000 }));
		// A non-DC detector row (Central Detector, e.g.) for this particle
		// must not be mistaken for a forward-detector sector assignment.
		DataBank track = bank(
				new String[] { "pindex", "detector", "sector" },
				1,
				Map.of("pindex", new short[] { 0 }, "detector", new byte[] { 5 }, "sector", new byte[] { 4 }));

		RecEventData data = RecEventData.from(EventSnapshot.of(event(
				Map.of(RecEventData.RECON_BANK, recon, RecEventData.TRACK_BANK, track))));

		assertTrue(data.trackSector(data.particles().get(0).row()).isEmpty());
	}

	@Test
	void unrecognizedPidFallsBackToChargeBasedName() {
		DataBank recon = bank(
				new String[] { "pid", "charge", "px", "py", "pz", "vx", "vy", "vz", "status" },
				1,
				Map.of("pid", new int[] { 0 }, "charge", new byte[] { -1 },
						"px", new float[] { 0f }, "py", new float[] { 0f }, "pz", new float[] { 0f },
						"vx", new float[] { 0f }, "vy", new float[] { 0f }, "vz", new float[] { 0f },
						"status", new short[] { 0 }));

		RecEventData data = RecEventData.from(EventSnapshot.of(event(Map.of(RecEventData.RECON_BANK, recon))));

		assertEquals("unknown-", data.particles().get(0).displayName());
	}

	@Test
	void sectorFollowsClas12SixtyDegreeConvention() {
		// Interior points, one per sector — far enough from every 30/90/150/
		// 210/270/330-degree boundary that float round-trip through
		// cos/sin/atan2 can never cross it.
		assertEquals(1, particleAtPhiDegrees(0.0).sector());
		assertEquals(2, particleAtPhiDegrees(60.0).sector());
		assertEquals(3, particleAtPhiDegrees(120.0).sector());
		assertEquals(4, particleAtPhiDegrees(180.0).sector());
		assertEquals(5, particleAtPhiDegrees(240.0).sector());
		assertEquals(6, particleAtPhiDegrees(300.0).sector());

		// Just off the 1/2 boundary on either side, and wrapped negative —
		// deliberately not testing exactly at 30.0 degrees, since float
		// round-trip through cos/sin/atan2 isn't guaranteed to reproduce a
		// boundary value exactly.
		assertEquals(1, particleAtPhiDegrees(29.9).sector());
		assertEquals(1, particleAtPhiDegrees(-29.9).sector());
		assertEquals(2, particleAtPhiDegrees(30.1).sector());
		assertEquals(1, particleAtPhiDegrees(330.1).sector());
	}

	private static RecEventData.Particle particleAtPhiDegrees(double degrees) {
		double radians = Math.toRadians(degrees);
		float px = (float) Math.cos(radians);
		float py = (float) Math.sin(radians);
		return new RecEventData.Particle(0, 2212, 1, px, py, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0);
	}

	private static DataEvent event(Map<String, DataBank> banks) {
		return (DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
				new Class<?>[] { DataEvent.class }, (instance, method, args) -> switch (method.getName()) {
					case "getBankList" -> banks.keySet().toArray(String[]::new);
					case "hasBank" -> banks.containsKey(args[0]);
					case "getBank" -> banks.get(args[0]);
					default -> null;
				});
	}

	private static DataBank bank(String[] columns, int rows, Map<String, Object> values) {
		return (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (instance, method, args) -> switch (method.getName()) {
					case "getColumnList" -> columns;
					case "rows" -> rows;
					case "getShort" -> ((short[]) values.get(args[0]))[(int) args[1]];
					case "getInt" -> ((int[]) values.get(args[0]))[(int) args[1]];
					case "getFloat" -> ((float[]) values.get(args[0]))[(int) args[1]];
					case "getByte" -> ((byte[]) values.get(args[0]))[(int) args[1]];
					default -> null;
				});
	}
}
