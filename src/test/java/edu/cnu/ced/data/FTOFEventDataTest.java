package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventSnapshot;

class FTOFEventDataTest {

	@Test
	void extractsPositiveAdcHitsReconstructedHitsAndClusters() {
		DataBank adc = bank(new String[] { "sector", "layer", "component", "order", "ADC", "time" }, 3,
				Map.of("sector", new byte[] { 2, 2, 2 }, "layer", new byte[] { 1, 1, 3 },
						"component", new short[] { 7, 7, 4 }, "order", new byte[] { 0, 1, 0 },
						"ADC", new int[] { 0, 4111, 2038 }, "time", new float[] { 0f, 93.2f, 96.4f }));
		DataBank hits = bank(new String[] { "sector", "layer", "component", "id", "energy", "time", "x", "y", "z" }, 1,
				Map.of("sector", new byte[] { 2 }, "layer", new byte[] { 1 }, "component", new short[] { 7 },
						"id", new short[] { 15 }, "energy", new float[] { 1.2f }, "time", new float[] { 107f },
						"x", new float[] { 95f }, "y", new float[] { 128f }, "z", new float[] { 690f }));
		DataBank clusters = bank(new String[] { "sector", "layer", "component", "id", "status", "energy", "time", "x", "y", "z" }, 1,
				Map.of("sector", new byte[] { 2 }, "layer", new byte[] { 3 }, "component", new short[] { 4 },
						"id", new short[] { 9 }, "status", new short[] { 0 }, "energy", new float[] { 2.4f },
						"time", new float[] { 115f }, "x", new float[] { 88f }, "y", new float[] { 121f },
						"z", new float[] { 700f }));

		FTOFEventData data = FTOFEventData.from(EventSnapshot.of(event(Map.of(
				FTOFEventData.ADC_BANK, adc, FTOFEventData.HIT_BANK, hits,
				FTOFEventData.CLUSTER_BANK, clusters))));

		assertEquals(2, data.adcHits().size());
		assertEquals(4111, data.maximumAdc());
		assertEquals(0, data.adcHits().getFirst().panel());
		assertEquals(2, data.adcHits().getLast().panel());
		assertEquals(15, data.reconHits().getFirst().id());
		assertEquals(9, data.clusters().getFirst().id());
	}

	@Test
	void rejectsIncompleteSchemas() {
		DataBank incomplete = bank(new String[] { "sector", "layer", "component", "ADC" }, 1,
				Map.of("sector", new byte[] { 1 }, "layer", new byte[] { 1 },
						"component", new short[] { 1 }, "ADC", new int[] { 42 }));
		FTOFEventData data = FTOFEventData.from(EventSnapshot.of(event(
				Map.of(FTOFEventData.ADC_BANK, incomplete))));
		assertTrue(data.adcHits().isEmpty());
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
