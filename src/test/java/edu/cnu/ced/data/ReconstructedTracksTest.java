package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import cnuphys.lund.LundSupport;
import edu.cnu.ced.event.EventSnapshot;

class ReconstructedTracksTest {

	@Test void dcHitBasedTrackGetsTheHitBasedSyntheticSpeciesByCharge() {
		DataBank hb = dcTrackBank(7, (short) 4000, (byte) 1, 0, 0, 0, 0, 0, 0.4f);
		ReconstructedTracks tracks = ReconstructedTracks.from(
				EventSnapshot.of(event(Map.of("HitBasedTrkg::HBTracks", hb))));

		assertEquals(1, tracks.tracks().size());
		TrackRow row = tracks.tracks().get(0);
		assertEquals(7, row.trackId());
		assertEquals("HitBasedTrkg::HBTracks", row.source());
		assertEquals(LundSupport.getHitbased(1).getId(), row.pid());
		assertTrue(row.isSyntheticPid());
	}

	@Test void dcTimeBasedTrackGetsTheTrackBasedSyntheticSpecies() {
		DataBank tb = dcTrackBank(3, (short) 4000, (byte) -1, 0, 0, 0, 0, 0, 0.4f);
		ReconstructedTracks tracks = ReconstructedTracks.from(
				EventSnapshot.of(event(Map.of("TimeBasedTrkg::TBTracks", tb))));

		assertEquals(LundSupport.getTrackbased(-1).getId(), tracks.tracks().get(0).pid());
	}

	@Test void recParticleGetsItsRealResolvedSpecies() {
		DataBank recon = recBank(2212, 1, 0, 0, 0.5f);
		ReconstructedTracks tracks = ReconstructedTracks.from(
				EventSnapshot.of(event(Map.of(RecEventData.RECON_BANK, recon))));

		TrackRow row = tracks.tracks().get(0);
		assertEquals("p", row.name());
		assertFalse(row.isSyntheticPid());
	}

	@Test void cvtTrackDerivesVertexAndMomentumFromHelixParameters() {
		// phi0 = 0: xo = -d0*sin(0) = 0, yo = d0*cos(0) = d0; px = pt, py = 0
		DataBank cvt = cvtTrackBank(11, (byte) 1, 0.3f, 0f, 2.0f, 5.0f, 0.5f);
		ReconstructedTracks tracks = ReconstructedTracks.from(
				EventSnapshot.of(event(Map.of("CVTRec::Tracks", cvt))));

		TrackRow row = tracks.tracks().get(0);
		assertEquals(11, row.trackId());
		assertEquals(0.0, row.x0(), 1e-6);
		assertEquals(2.0, row.y0(), 1e-6);
		assertEquals(5.0, row.z0(), 1e-6);
		assertEquals(LundSupport.getCVTbased(1).getId(), row.pid());
		// momentum: px=pt=0.3, py=0, pz=pt*tanDip=0.3*0.5=0.15 GeV -> p=sqrt(0.3^2+0.15^2) GeV
		double expectedMomentumMeV = 1000.0 * Math.sqrt(0.3 * 0.3 + 0.15 * 0.15);
		assertEquals(expectedMomentumMeV, row.momentumMeV(), 1e-3);
	}

	@Test void combinesEverySourceInDocumentedOrder() {
		Map<String, DataBank> banks = new LinkedHashMap<>();
		banks.put("HitBasedTrkg::HBTracks", dcTrackBank(1, (short) 0, (byte) 1, 0, 0, 0, 0, 0, 0.4f));
		banks.put("TimeBasedTrkg::TBTracks", dcTrackBank(2, (short) 0, (byte) 1, 0, 0, 0, 0, 0, 0.4f));
		banks.put(RecEventData.RECON_BANK, recBank(2212, 1, 0, 0, 0.5f));
		banks.put("HitBasedTrkg::AITracks", dcTrackBank(3, (short) 0, (byte) 1, 0, 0, 0, 0, 0, 0.4f));
		banks.put("TimeBasedTrkg::AITracks", dcTrackBank(4, (short) 0, (byte) 1, 0, 0, 0, 0, 0, 0.4f));
		banks.put("CVTRec::Tracks", cvtTrackBank(5, (byte) 1, 0.3f, 0f, 2.0f, 5.0f, 0.5f));
		banks.put("CVT::Tracks", cvtTrackBank(6, (byte) 1, 0.3f, 0f, 2.0f, 5.0f, 0.5f));

		ReconstructedTracks tracks = ReconstructedTracks.from(EventSnapshot.of(event(banks)));

		assertEquals(
				java.util.List.of("HitBasedTrkg::HBTracks", "TimeBasedTrkg::TBTracks", RecEventData.RECON_BANK,
						"HitBasedTrkg::AITracks", "TimeBasedTrkg::AITracks", "CVTRec::Tracks", "CVT::Tracks"),
				tracks.tracks().stream().map(TrackRow::source).toList());
	}

	@Test void emptyOrMissingBanksYieldNoTracks() {
		assertTrue(ReconstructedTracks.from(EventSnapshot.empty()).tracks().isEmpty());
		assertTrue(ReconstructedTracks.from(null).tracks().isEmpty());
	}

	private static DataBank dcTrackBank(int id, short status, byte q, float vx, float vy, float vz,
			float px, float py, float pz) {
		Map<String, Object> values = Map.ofEntries(
				Map.entry("id", new short[] { (short) id }), Map.entry("status", new short[] { status }),
				Map.entry("q", new byte[] { q }),
				Map.entry("Vtx0_x", new float[] { vx }), Map.entry("Vtx0_y", new float[] { vy }),
				Map.entry("Vtx0_z", new float[] { vz }),
				Map.entry("p0_x", new float[] { px }), Map.entry("p0_y", new float[] { py }),
				Map.entry("p0_z", new float[] { pz }));
		return (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getColumnList" -> values.keySet().toArray(String[]::new);
					case "rows" -> 1;
					case "getShort" -> ((short[]) values.get(args[0]))[(int) args[1]];
					case "getByte" -> ((byte[]) values.get(args[0]))[(int) args[1]];
					case "getFloat" -> ((float[]) values.get(args[0]))[(int) args[1]];
					default -> null;
				});
	}

	private static DataBank cvtTrackBank(int id, byte q, float pt, float phi0, float d0, float z0,
			float tanDip) {
		Map<String, Object> values = Map.of("ID", new short[] { (short) id }, "q", new byte[] { q },
				"pt", new float[] { pt }, "phi0", new float[] { phi0 }, "d0", new float[] { d0 },
				"z0", new float[] { z0 }, "tandip", new float[] { tanDip });
		return (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getColumnList" -> values.keySet().toArray(String[]::new);
					case "rows" -> 1;
					case "getShort" -> ((short[]) values.get(args[0]))[(int) args[1]];
					case "getByte" -> ((byte[]) values.get(args[0]))[(int) args[1]];
					case "getFloat" -> ((float[]) values.get(args[0]))[(int) args[1]];
					default -> null;
				});
	}

	private static DataBank recBank(int pid, int charge, float vx, float vy, float pz) {
		Map<String, Object> values = Map.of("pid", new int[] { pid }, "charge", new byte[] { (byte) charge },
				"px", new float[] { 0f }, "py", new float[] { 0f }, "pz", new float[] { pz },
				"vx", new float[] { vx }, "vy", new float[] { vy }, "vz", new float[] { 0f },
				"status", new short[] { 0 });
		return (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] { DataBank.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getColumnList" -> values.keySet().toArray(String[]::new);
					case "rows" -> 1;
					case "getInt" -> ((int[]) values.get(args[0]))[(int) args[1]];
					case "getByte" -> ((byte[]) values.get(args[0]))[(int) args[1]];
					case "getFloat" -> ((float[]) values.get(args[0]))[(int) args[1]];
					case "getShort" -> ((short[]) values.get(args[0]))[(int) args[1]];
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
