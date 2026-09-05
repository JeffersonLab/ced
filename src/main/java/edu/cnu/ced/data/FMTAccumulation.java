package edu.cnu.ced.data;

/** Positive-ADC FMT strip occupancy accumulated across requested events. */
public final class FMTAccumulation {

	private static final int LAYERS = 6;
	private static final int STRIPS = 1024;

	private final int[][] counts = new int[LAYERS][STRIPS];
	private int events;
	private int maximum;

	public synchronized void add(FMTEventData data) {
		if (data == null) {
			return;
		}
		events++;
		for (FMTEventData.AdcHit hit : data.adcHits()) {
			int layer = hit.layer();
			int strip = hit.strip() - 1;
			if (layer < 0 || layer >= LAYERS || strip < 0 || strip >= STRIPS) {
				continue;
			}
			maximum = Math.max(maximum, ++counts[layer][strip]);
		}
	}

	public synchronized void clear() {
		for (int[] layer : counts) {
			java.util.Arrays.fill(layer, 0);
		}
		events = 0;
		maximum = 0;
	}

	/** @param layer 0-based FMT layer; @param strip 1-based strip number */
	public synchronized int count(int layer, int strip) {
		return layer < 0 || layer >= LAYERS || strip < 1 || strip > STRIPS
				? 0 : counts[layer][strip - 1];
	}

	public synchronized int eventCount() { return events; }
	public synchronized int maximumCount() { return maximum; }
}
