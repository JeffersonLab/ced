package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventSnapshot;

class URWTEventDataTest {

	@Test void readsHitsWithAddressAndOptionalClusterStatus() {
		DataBank hits = bank(new String[] { "sector", "layer", "strip", "energy", "time", "clusterId", "status" }, 1,
				Map.of("sector", new byte[] { 3 }, "layer", new byte[] { 2 }, "strip", new short[] { 44 },
						"energy", new float[] { 120f }, "time", new float[] { 6.5f },
						"clusterId", new short[] { 5 }, "status", new short[] { 0 }));
		URWTEventData data = URWTEventData.from(EventSnapshot.of(event(Map.of(URWTEventData.HITS_BANK, hits))));

		assertEquals(1, data.hits().size());
		URWTEventData.Hit hit = data.hits().get(0);
		assertEquals(3, hit.sector());
		assertEquals(2, hit.layer());
		assertEquals(44, hit.strip());
		assertEquals(5, hit.clusterId());
	}

	@Test void readsClustersWithExplicitWorldEndpoints() {
		DataBank clusters = bank(
				new String[] { "sector", "layer", "xo", "yo", "zo", "xe", "ye", "ze", "energy", "time", "size" }, 1,
				Map.ofEntries(
						Map.entry("sector", new byte[] { 1 }), Map.entry("layer", new byte[] { 1 }),
						Map.entry("xo", new float[] { 10f }), Map.entry("yo", new float[] { 20f }),
						Map.entry("zo", new float[] { 170f }), Map.entry("xe", new float[] { 11f }),
						Map.entry("ye", new float[] { 21f }), Map.entry("ze", new float[] { 171f }),
						Map.entry("energy", new float[] { 300f }), Map.entry("time", new float[] { 4f }),
						Map.entry("size", new short[] { 3 })));
		URWTEventData data = URWTEventData.from(
				EventSnapshot.of(event(Map.of(URWTEventData.CLUSTERS_BANK, clusters))));

		assertEquals(1, data.clusters().size());
		URWTEventData.Cluster cluster = data.clusters().get(0);
		assertEquals(10f, cluster.xo());
		assertEquals(21f, cluster.ye());
		assertEquals(3, cluster.size());
	}

	@Test void readsCrosses() {
		DataBank crosses = bank(new String[] { "sector", "region", "x", "y", "z", "energy", "time" }, 1,
				Map.of("sector", new byte[] { 2 }, "region", new byte[] { 1 },
						"x", new float[] { 100f }, "y", new float[] { -50f }, "z", new float[] { 180f },
						"energy", new float[] { 50f }, "time", new float[] { 2f }));
		URWTEventData data = URWTEventData.from(EventSnapshot.of(event(Map.of(URWTEventData.CROSSES_BANK, crosses))));

		assertEquals(1, data.crosses().size());
		URWTEventData.Cross cross = data.crosses().get(0);
		assertEquals(100f, cross.x());
		assertEquals(-50f, cross.y());
		assertEquals(180f, cross.z());
	}

	@Test void emptyOrMissingBanksYieldNoData() {
		assertTrue(URWTEventData.from(EventSnapshot.empty()).hits().isEmpty());
		assertTrue(URWTEventData.from(null).hits().isEmpty());
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
