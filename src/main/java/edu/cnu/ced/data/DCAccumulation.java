package edu.cnu.ced.data;

import java.util.Arrays;

/** Accumulated raw drift-chamber wire occupancy. */
public final class DCAccumulation {
	private final int[][][][] counts = new int[6][6][6][112];
	private final int[] superlayerMaximum = new int[6];
	private int events;
	private int maximum;

	public synchronized void add(DCEventData data) {
		if (data == null) return;
		events++;
		for (DCEventData.RawHit hit : data.rawHits()) {
			int value = ++counts[hit.sector() - 1][hit.superlayer() - 1]
					[hit.layer() - 1][hit.wire() - 1];
			maximum = Math.max(maximum, value);
			superlayerMaximum[hit.superlayer() - 1] = Math.max(
					superlayerMaximum[hit.superlayer() - 1], value);
		}
	}

	public synchronized void clear() {
		for (int[][][] sector : counts) for (int[][] superlayer : sector)
			for (int[] layer : superlayer) java.util.Arrays.fill(layer, 0);
		events = 0;
		maximum = 0;
		java.util.Arrays.fill(superlayerMaximum, 0);
	}

	public synchronized int count(int sector, int superlayer, int layer, int wire) {
		return counts[sector - 1][superlayer - 1][layer - 1][wire - 1];
	}

	public synchronized int eventCount() { return events; }
	public synchronized int maximumCount() { return maximum; }
	public synchronized int maximumCount(int superlayer) {
		return superlayerMaximum[superlayer - 1];
	}

	/**
	 * Return a robust color-scale ceiling for one superlayer.
	 *
	 * <p>Only occupied wires participate, so zero occupancy remains visually empty.
	 * The nearest-rank percentile prevents a few hot wires from suppressing the
	 * useful color range for the rest of the chamber.</p>
	 *
	 * @param superlayer one-based superlayer number
	 * @param percentile percentile in the inclusive range [0, 1]
	 * @return the count at the requested percentile, or zero when no wire is occupied
	 */
	public synchronized int percentileCount(int superlayer, double percentile) {
		if (superlayer < 1 || superlayer > 6)
			throw new IllegalArgumentException("superlayer must be in [1, 6]");
		if (!Double.isFinite(percentile) || percentile < 0 || percentile > 1)
			throw new IllegalArgumentException("percentile must be in [0, 1]");

		int[] occupied = new int[6 * 6 * 112];
		int size = 0;
		for (int sector = 0; sector < 6; sector++)
			for (int layer = 0; layer < 6; layer++)
				for (int wire = 0; wire < 112; wire++) {
					int count = counts[sector][superlayer - 1][layer][wire];
					if (count > 0) occupied[size++] = count;
				}
		if (size == 0) return 0;
		Arrays.sort(occupied, 0, size);
		int rank = Math.max(1, (int) Math.ceil(percentile * size));
		return occupied[rank - 1];
	}
}
