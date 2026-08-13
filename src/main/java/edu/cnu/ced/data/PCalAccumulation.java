package edu.cnu.ced.data;

/** Positive-ADC PCAL strip occupancy accumulated across requested events. */
public final class PCalAccumulation {
	private final int[][][] counts = new int[6][3][68];
	private int events;
	private int maximum;

	public synchronized void add(PCalEventData data) {
		if (data == null) return;
		events++;
		for (PCalEventData.AdcHit hit : data.adcHits()) {
			int sector = hit.sector() - 1;
			int strip = hit.strip() - 1;
			if (sector < 0 || sector >= 6 || hit.view() < 0 || hit.view() >= 3
					|| strip < 0 || strip >= counts[sector][hit.view()].length) continue;
			maximum = Math.max(maximum, ++counts[sector][hit.view()][strip]);
		}
	}

	public synchronized void clear() {
		for (int[][] sector : counts) for (int[] view : sector) java.util.Arrays.fill(view, 0);
		events = 0;
		maximum = 0;
	}

	public synchronized int count(int sector, int view, int strip) {
		return sector < 1 || sector > 6 || view < 0 || view > 2 || strip < 1 || strip > 68
				? 0 : counts[sector - 1][view][strip - 1];
	}
	public synchronized int eventCount() { return events; }
	public synchronized int maximumCount() { return maximum; }
}
