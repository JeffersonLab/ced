package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventSnapshot;

class ECalEventDataTest {
	@Test
	void mapsLayersFourThroughNineToPlaneAndView() {
		DataBank adc = bank(new String[] { "sector", "layer", "component", "ADC", "time" }, 3,
				Map.of("sector", new byte[] { 2, 2, 2 }, "layer", new byte[] { 3, 4, 9 },
						"component", new short[] { 1, 17, 36 }, "ADC", new int[] { 99, 250, 300 },
						"time", new float[] { 1f, 12f, 13f }));
		ECalEventData data = ECalEventData.from(EventSnapshot.of(event(Map.of(ECalEventData.ADC_BANK, adc))));
		assertEquals(2, data.adcHits().size());
		assertEquals(0, data.adcHits().get(0).plane());
		assertEquals(0, data.adcHits().get(0).view());
		assertEquals(1, data.adcHits().get(1).plane());
		assertEquals(2, data.adcHits().get(1).view());
		assertEquals(300, data.maximumAdc());
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
