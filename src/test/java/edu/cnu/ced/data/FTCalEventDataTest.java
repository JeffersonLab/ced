package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventSnapshot;

class FTCalEventDataTest {

	@Test
	void extractsAdcAndReconstructedHitsFromOneSnapshot() {
		DataBank adc = bank(new String[] { "component", "ADC", "time", "order" }, 2,
				Map.of("component", new short[] { 359, 360 },
						"ADC", new int[] { 0, 4111 },
						"time", new float[] { 142.5f, 93.2f },
						"order", new byte[] { 0, 1 }));
		DataBank hits = bank(new String[] { "hitID", "x", "y", "z" }, 1,
				Map.of("hitID", new short[] { 7 }, "x", new float[] { -5.4f },
						"y", new float[] { 8.3f }, "z", new float[] { 201.2f }));

		FTCalEventData data = FTCalEventData.from(EventSnapshot.of(event(Map.of(
				FTCalEventData.ADC_BANK, adc, FTCalEventData.HIT_BANK, hits))));

		assertEquals(1, data.adcHits().size());
		assertEquals(4111, data.maximumAdc());
		assertEquals(360, data.adcHits().getFirst().component());
		assertEquals(1, data.reconHits().size());
		assertEquals(7, data.reconHits().getFirst().id());
	}

	@Test
	void ignoresBanksThatDoNotProvideTheRequiredSchema() {
		DataBank incomplete = bank(new String[] { "component", "ADC" }, 1,
				Map.of("component", new short[] { 359 }, "ADC", new int[] { 42 }));
		FTCalEventData data = FTCalEventData.from(EventSnapshot.of(event(
				Map.of(FTCalEventData.ADC_BANK, incomplete))));
		assertTrue(data.adcHits().isEmpty());
		assertTrue(data.reconHits().isEmpty());
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
