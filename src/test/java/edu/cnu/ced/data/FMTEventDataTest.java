package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventSnapshot;

class FMTEventDataTest {

	@Test void readsAdcAboveThresholdAndTracksTheMaximum() {
		DataBank adc = bank(new String[] { "layer", "component", "order", "ADC", "time" }, 2,
				Map.of("layer", new byte[] { 3, 3 }, "component", new short[] { 500, 501 },
						"order", new byte[] { 0, 1 }, "ADC", new int[] { 1200, 400 },
						"time", new float[] { 12.5f, 13.1f }));
		FMTEventData data = FMTEventData.from(EventSnapshot.of(event(Map.of(FMTEventData.ADC_BANK, adc))));

		assertEquals(2, data.adcHits().size());
		assertEquals(1200, data.maximumAdc());
		assertEquals(2, data.adcHits().get(0).layer()); // 1-based layer 3 -> 0-based 2
		assertEquals(500, data.adcHits().get(0).strip());
	}

	@Test void skipsNonPositiveAdcAndOutOfRangeLayers() {
		DataBank adc = bank(new String[] { "layer", "component", "order", "ADC", "time" }, 2,
				Map.of("layer", new byte[] { 0, 3 }, "component", new short[] { 1, 2 },
						"order", new byte[] { 0, 0 }, "ADC", new int[] { 500, 0 },
						"time", new float[] { 1f, 1f }));
		FMTEventData data = FMTEventData.from(EventSnapshot.of(event(Map.of(FMTEventData.ADC_BANK, adc))));
		assertTrue(data.adcHits().isEmpty());
	}

	@Test void readsReconHitsWithOptionalClusterAndTrackIndices() {
		DataBank hits = bank(new String[] { "layer", "strip", "energy", "time", "clusterIndex", "trackIndex" }, 1,
				Map.of("layer", new byte[] { 1 }, "strip", new short[] { 42 },
						"energy", new float[] { 0.03f }, "time", new float[] { 5f },
						"clusterIndex", new short[] { 7 }, "trackIndex", new short[] { 2 }));
		FMTEventData data = FMTEventData.from(EventSnapshot.of(event(Map.of(FMTEventData.HITS_BANK, hits))));

		assertEquals(1, data.reconHits().size());
		FMTEventData.ReconHit hit = data.reconHits().get(0);
		assertEquals(0, hit.layer());
		assertEquals(42, hit.strip());
		assertEquals(7, hit.clusterIndex());
		assertEquals(2, hit.trackIndex());
	}

	@Test void readsClusters() {
		DataBank clusters = bank(new String[] { "layer", "seedStrip", "size", "energy", "time", "trackIndex" }, 1,
				Map.of("layer", new byte[] { 6 }, "seedStrip", new short[] { 900 },
						"size", new short[] { 4 }, "energy", new float[] { 0.12f },
						"time", new float[] { 8f }, "trackIndex", new short[] { 1 }));
		FMTEventData data = FMTEventData.from(EventSnapshot.of(event(Map.of(FMTEventData.CLUSTERS_BANK, clusters))));

		assertEquals(1, data.clusters().size());
		FMTEventData.Cluster cluster = data.clusters().get(0);
		assertEquals(5, cluster.layer());
		assertEquals(900, cluster.seedStrip());
		assertEquals(4, cluster.size());
	}

	@Test void readsCrosses() {
		DataBank crosses = bank(new String[] { "x", "y", "z", "sector", "region", "trkID" }, 1,
				Map.of("x", new float[] { 1.5f }, "y", new float[] { -2.5f }, "z", new float[] { 30.9f },
						"sector", new byte[] { 1 }, "region", new byte[] { 2 }, "trkID", new short[] { 9 }));
		FMTEventData data = FMTEventData.from(EventSnapshot.of(event(Map.of(FMTEventData.CROSSES_BANK, crosses))));

		assertEquals(1, data.crosses().size());
		FMTEventData.Cross cross = data.crosses().get(0);
		assertEquals(1.5f, cross.x());
		assertEquals(-2.5f, cross.y());
		assertEquals(30.9f, cross.z());
		assertEquals(9, cross.trackId());
	}

	@Test void emptyOrMissingBanksYieldNoData() {
		assertTrue(FMTEventData.from(EventSnapshot.empty()).adcHits().isEmpty());
		assertTrue(FMTEventData.from(null).adcHits().isEmpty());
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
