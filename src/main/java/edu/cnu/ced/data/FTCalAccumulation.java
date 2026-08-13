package edu.cnu.ced.data;

import java.util.HashMap;
import java.util.Map;

/** Positive-ADC FTCAL occupancy accumulated across visited events. */
public final class FTCalAccumulation {
	private final Map<Integer, Integer> counts = new HashMap<>();
	private int events;
	private int maximum;

	public synchronized void add(FTCalEventData data) {
		if (data == null) return;
		events++;
		for (FTCalEventData.AdcHit hit : data.adcHits()) {
			maximum = Math.max(maximum, counts.merge(hit.component(), 1, Integer::sum));
		}
	}

	public synchronized void clear() {
		counts.clear();
		events = 0;
		maximum = 0;
	}

	public synchronized int count(int component) { return counts.getOrDefault(component, 0); }
	public synchronized int eventCount() { return events; }
	public synchronized int maximumCount() { return maximum; }
}
