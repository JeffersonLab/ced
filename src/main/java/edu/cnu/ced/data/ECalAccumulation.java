package edu.cnu.ced.data;

/** Positive-ADC ECAL occupancy accumulated independently for each stack. */
public final class ECalAccumulation {
	private final int[][][][] counts = new int[6][2][3][36];
	private final int[] maximum = new int[2];
	private int events;

	public synchronized void add(ECalEventData data) {
		if (data == null) return;
		events++;
		for (ECalEventData.AdcHit hit : data.adcHits()) {
			int sector = hit.sector() - 1;
			int strip = hit.strip() - 1;
			if (sector < 0 || sector >= 6 || hit.plane() < 0 || hit.plane() >= 2
					|| hit.view() < 0 || hit.view() >= 3 || strip < 0 || strip >= 36) continue;
			maximum[hit.plane()] = Math.max(maximum[hit.plane()],
					++counts[sector][hit.plane()][hit.view()][strip]);
		}
	}

	public synchronized void clear() {
		for (int[][][] sector : counts) for (int[][] plane : sector)
			for (int[] view : plane) java.util.Arrays.fill(view, 0);
		java.util.Arrays.fill(maximum, 0);
		events = 0;
	}

	public synchronized int count(int sector, int plane, int view, int strip) {
		return sector < 1 || sector > 6 || plane < 0 || plane > 1 || view < 0 || view > 2
				|| strip < 1 || strip > 36 ? 0 : counts[sector - 1][plane][view][strip - 1];
	}

	public synchronized int eventCount() { return events; }
	public synchronized int maximumCount(int plane) {
		return plane < 0 || plane > 1 ? 0 : maximum[plane];
	}
}
