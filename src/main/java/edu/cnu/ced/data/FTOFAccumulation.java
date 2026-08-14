package edu.cnu.ced.data;

/** Positive-ADC FTOF paddle occupancy accumulated across requested events. */
public final class FTOFAccumulation {
	private final int[][][] counts = new int[6][3][64];
	private int events;
	private int maximum;

	public synchronized void add(FTOFEventData data) {
		if (data == null) return;
		events++;
		boolean[][][] seen = new boolean[6][3][64];
		for (FTOFEventData.AdcHit hit : data.adcHits()) {
			int sector = hit.sector() - 1;
			int paddle = hit.paddle() - 1;
			if (sector < 0 || sector >= 6 || hit.panel() < 0 || hit.panel() >= 3
					|| paddle < 0 || paddle >= 64 || seen[sector][hit.panel()][paddle]) continue;
			seen[sector][hit.panel()][paddle] = true;
			maximum = Math.max(maximum, ++counts[sector][hit.panel()][paddle]);
		}
	}

	public synchronized void clear() {
		for (int[][] sector : counts) for (int[] panel : sector) java.util.Arrays.fill(panel, 0);
		events = 0;
		maximum = 0;
	}

	public synchronized int count(int sector, int panel, int paddle) {
		return sector < 1 || sector > 6 || panel < 0 || panel > 2 || paddle < 1 || paddle > 64
				? 0 : counts[sector - 1][panel][paddle - 1];
	}
	public synchronized int eventCount() { return events; }
	public synchronized int maximumCount() { return maximum; }
}
