package edu.cnu.ced.data;

/** Per-cell occupancy accumulated for one Cherenkov detector. */
public final class CherenkovAccumulation {
	private final int ringCount;
	private final int[][][] counts;
	private int events;
	private int maximum;

	public CherenkovAccumulation(int ringCount) {
		if (ringCount < 1) throw new IllegalArgumentException("ringCount must be positive");
		this.ringCount = ringCount;
		counts = new int[6][2][ringCount];
	}

	public synchronized void add(CherenkovEventData data) {
		if (data == null) return;
		events++;
		boolean[][][] seen = new boolean[6][2][ringCount];
		for (CherenkovEventData.AdcHit hit : data.adcHits()) {
			int sector = hit.sector() - 1, half = hit.half() - 1, ring = hit.ring() - 1;
			if (sector < 0 || sector >= 6 || half < 0 || half >= 2 || ring < 0
					|| ring >= ringCount || seen[sector][half][ring]) continue;
			seen[sector][half][ring] = true;
			maximum = Math.max(maximum, ++counts[sector][half][ring]);
		}
	}

	public synchronized void clear() {
		for (int[][] sector : counts) for (int[] half : sector) java.util.Arrays.fill(half, 0);
		events = 0;
		maximum = 0;
	}

	public synchronized int count(int sector, int half, int ring) {
		return sector < 1 || sector > 6 || half < 1 || half > 2 || ring < 1 || ring > ringCount
				? 0 : counts[sector - 1][half - 1][ring - 1];
	}
	public synchronized int eventCount() { return events; }
	public synchronized int maximumCount() { return maximum; }
}
