package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventSnapshot;

class PCalEventDataTest {
	@Test
	void readsOptionalReconstructedRadius() {
		DataBank recon = bank(new String[] { "sector", "layer", "energy", "time", "x", "y", "z", "radius" }, 1,
				Map.of("sector", new byte[] { 6 }, "layer", new byte[] { 2 },
						"energy", new float[] { 0.75f }, "time", new float[] { 24f },
						"x", new float[] { 11f }, "y", new float[] { 21f },
						"z", new float[] { 31f }, "radius", new float[] { 8.5f }));
		PCalEventData data = PCalEventData.from(EventSnapshot.of(event(Map.of(PCalEventData.RECON_BANK, recon))));
		assertEquals(1, data.reconHits().size());
		assertEquals(8.5f, data.reconHits().get(0).radius());
	}

	@Test
	void defaultsMissingReconstructedRadiusToZero() {
		DataBank recon = bank(new String[] { "sector", "layer", "energy", "time", "x", "y", "z" }, 1,
				Map.of("sector", new byte[] { 6 }, "layer", new byte[] { 2 },
						"energy", new float[] { 0.75f }, "time", new float[] { 24f },
						"x", new float[] { 11f }, "y", new float[] { 21f }, "z", new float[] { 31f }));
		PCalEventData data = PCalEventData.from(EventSnapshot.of(event(Map.of(PCalEventData.RECON_BANK, recon))));
		assertEquals(0f, data.reconHits().get(0).radius());
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
