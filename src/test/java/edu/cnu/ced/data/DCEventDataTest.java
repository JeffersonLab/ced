package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventSnapshot;

class DCEventDataTest {

	@Test
	void extractsRawAndReconstructedAddresses() {
		DataBank raw = bank(new String[] {"sector", "layer", "component", "order", "TDC"}, 2,
				Map.of("sector", new byte[] {2, 2}, "layer", new byte[] {8, 37},
						"component", new short[] {18, 22}, "order", new byte[] {0, 1},
						"TDC", new int[] {145, 200}));
		DataBank hb = bank(new String[] {"sector", "superlayer", "layer", "wire", "id",
				"status", "clusterID", "trkDoca"}, 1,
				Map.of("sector", new byte[] {2}, "superlayer", new byte[] {2},
						"layer", new byte[] {2}, "wire", new short[] {18},
						"id", new short[] {7}, "status", new short[] {0},
						"clusterID", new short[] {3}, "trkDoca", new float[] {.238f}));

		DCEventData data = DCEventData.from(EventSnapshot.of(event(Map.of(
				DCEventData.RAW_BANK, raw, "HitBasedTrkg::Hits", hb))));

		assertEquals(1, data.rawHits().size()); // invalid global layer 37 is omitted
		assertEquals(2, data.rawHits().getFirst().superlayer());
		assertEquals(2, data.rawHits().getFirst().layer());
		assertEquals(1, data.reconHits().size());
		assertEquals(DCEventData.ReconKind.HB, data.reconHits().getFirst().kind());
		assertEquals(.238f, data.reconHits().getFirst().trackDoca());
	}

	@Test
	void fallsBackToFirmwareV2TimeOverThresholdBank() {
		DataBank tot = bank(new String[] {"sector", "layer", "component", "order", "TDC", "ToT"},
				1, Map.of("sector", new byte[] {4}, "layer", new byte[] {31},
						"component", new short[] {112}, "order", new byte[] {2},
						"TDC", new int[] {321}, "ToT", new short[] {19}));

		DCEventData data = DCEventData.from(EventSnapshot.of(event(Map.of(
				DCEventData.TOT_BANK, tot))));

		assertEquals(1, data.rawHits().size());
		assertEquals(4, data.rawHits().getFirst().sector());
		assertEquals(6, data.rawHits().getFirst().superlayer());
		assertEquals(1, data.rawHits().getFirst().layer());
		assertEquals(112, data.rawHits().getFirst().wire());
		assertEquals(321, data.rawHits().getFirst().tdc());
	}

	@Test
	void extractsClusterMembershipAndSegmentEndpoints() {
		DataBank clusters = bank(new String[] {"sector", "superlayer", "id",
				"Hit1_ID", "Hit2_ID", "Hit3_ID"}, 1,
				Map.of("sector", new byte[] {3}, "superlayer", new byte[] {4},
						"id", new short[] {12}, "Hit1_ID", new short[] {21},
						"Hit2_ID", new short[] {22}, "Hit3_ID", new short[] {-1}));
		DataBank segments = bank(new String[] {"sector", "superlayer",
				"SegEndPoint1X", "SegEndPoint1Z", "SegEndPoint2X", "SegEndPoint2Z"}, 1,
				Map.of("sector", new byte[] {3}, "superlayer", new byte[] {4},
						"SegEndPoint1X", new float[] {10.5f},
						"SegEndPoint1Z", new float[] {220.0f},
						"SegEndPoint2X", new float[] {18.5f},
						"SegEndPoint2Z", new float[] {310.0f}));

		DCEventData data = DCEventData.from(EventSnapshot.of(event(Map.of(
				"HitBasedTrkg::HBClusters", clusters,
				"HitBasedTrkg::HBSegments", segments))));

		assertEquals(1, data.clusters().size());
		assertEquals(12, data.clusters().getFirst().id());
		assertEquals(java.util.List.of(21, 22), data.clusters().getFirst().hitIds());
		assertEquals(1, data.segments().size());
		assertEquals(10.5f, data.segments().getFirst().x1());
		assertEquals(310.0f, data.segments().getFirst().z2());
	}

	private static DataEvent event(Map<String, DataBank> banks) {
		return (DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
				new Class<?>[] {DataEvent.class}, (instance, method, args) -> switch (method.getName()) {
					case "getBankList" -> banks.keySet().toArray(String[]::new);
					case "hasBank" -> banks.containsKey(args[0]);
					case "getBank" -> banks.get(args[0]);
					default -> null;
				});
	}

	private static DataBank bank(String[] columns, int rows, Map<String, Object> values) {
		return (DataBank) Proxy.newProxyInstance(DataBank.class.getClassLoader(),
				new Class<?>[] {DataBank.class}, (instance, method, args) -> switch (method.getName()) {
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
