package edu.cnu.ced.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import edu.cnu.ced.event.EventSnapshot;

class AlertEventDataTest {

	@Test void decodesTheAdcBanksPackedSuperlayerAndLayerDigits() {
		// layer=32 -> superlayer=3, layer=2 (both 1-based in the bank); sector=1 -> 0;
		// component=15 -> wire 14. Matches legacy CED's AlertDCGeometryNumbering exactly.
		DataBank adc = bank(new String[] { "sector", "layer", "component", "ADC", "time" }, 1,
				Map.of("sector", new byte[] { 1 }, "layer", new byte[] { 32 },
						"component", new short[] { 15 }, "ADC", new int[] { 800 }, "time", new float[] { 5f }));
		AlertEventData data = AlertEventData.from(EventSnapshot.of(event(Map.of(AlertEventData.DC_ADC_BANK, adc))));

		assertEquals(1, data.dcAdcHits().size());
		AlertEventData.DcAdcHit hit = data.dcAdcHits().get(0);
		assertEquals(0, hit.sector());
		assertEquals(2, hit.superlayer());
		assertEquals(1, hit.layer());
		assertEquals(14, hit.wire());
		assertEquals(800, data.maximumDcAdc());
	}

	@Test void skipsNonPositiveAdc() {
		DataBank adc = bank(new String[] { "sector", "layer", "component", "ADC", "time" }, 1,
				Map.of("sector", new byte[] { 1 }, "layer", new byte[] { 11 },
						"component", new short[] { 1 }, "ADC", new int[] { 0 }, "time", new float[] { 5f }));
		AlertEventData data = AlertEventData.from(EventSnapshot.of(event(Map.of(AlertEventData.DC_ADC_BANK, adc))));
		assertTrue(data.dcAdcHits().isEmpty());
	}

	@Test void readsDcHitsFromSeparateSuperlayerAndLayerColumns() {
		DataBank hits = bank(new String[] { "layer", "superlayer", "wire", "time", "doca" }, 1,
				Map.of("layer", new byte[] { 2 }, "superlayer", new byte[] { 4 },
						"wire", new int[] { 10 }, "time", new double[] { 3.5 }, "doca", new double[] { 0.2 }));
		AlertEventData data = AlertEventData.from(EventSnapshot.of(event(Map.of(AlertEventData.DC_HITS_BANK, hits))));

		assertEquals(1, data.dcHits().size());
		AlertEventData.DcHit hit = data.dcHits().get(0);
		assertEquals(3, hit.superlayer());
		assertEquals(1, hit.layer());
		assertEquals(9, hit.wire());
	}

	@Test void convertsDcClustersFromMillimetersToCentimeters() {
		DataBank clusters = bank(new String[] { "x", "y", "z" }, 1,
				Map.of("x", new float[] { 100f }, "y", new float[] { 200f }, "z", new float[] { -50f }));
		AlertEventData data =
				AlertEventData.from(EventSnapshot.of(event(Map.of(AlertEventData.DC_CLUSTERS_BANK, clusters))));

		assertEquals(1, data.dcClusters().size());
		AlertEventData.DcCluster cluster = data.dcClusters().get(0);
		assertEquals(10f, cluster.x(), 1e-6);
		assertEquals(20f, cluster.y(), 1e-6);
		assertEquals(-5f, cluster.z(), 1e-6);
	}

	@Test void convertsTofHitsFromMillimetersToCentimeters() {
		DataBank hits = bank(new String[] { "sector", "layer", "component", "x", "y", "z", "time", "energy" }, 1,
				Map.of("sector", new int[] { 5 }, "layer", new int[] { 1 }, "component", new int[] { 2 },
						"x", new float[] { 770f }, "y", new float[] { -10f }, "z", new float[] { 1398.5f },
						"time", new float[] { 12f }, "energy", new float[] { 3.5f }));
		AlertEventData data = AlertEventData.from(EventSnapshot.of(event(Map.of(AlertEventData.TOF_HITS_BANK, hits))));

		assertEquals(1, data.tofHits().size());
		AlertEventData.TofHit hit = data.tofHits().get(0);
		assertEquals(77f, hit.x(), 1e-4);
		assertEquals(139.85f, hit.z(), 1e-4);
		assertEquals(5, hit.sector());
	}

	@Test void emptyOrMissingBanksYieldNoData() {
		assertTrue(AlertEventData.from(EventSnapshot.empty()).dcAdcHits().isEmpty());
		assertTrue(AlertEventData.from(null).dcAdcHits().isEmpty());
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
					case "getDouble" -> ((double[]) values.get(args[0]))[(int) args[1]];
					default -> null;
				});
	}
}
