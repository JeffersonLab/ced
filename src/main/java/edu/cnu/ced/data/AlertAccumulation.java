package edu.cnu.ced.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-wire AHDC ADC occupancy accumulated across requested events.
 * <p>
 * ATOF occupancy isn't tracked here: unlike AHDC's wires (one geometric
 * detector, sector always 0), ATOF's own paddle addressing and its
 * geometry's (sector, superlayer, layer) shape haven't been reconciled with
 * enough confidence to accumulate against (see {@link AlertEventData}'s
 * class doc) -- a future addition, not attempted here.
 * </p>
 */
public final class AlertAccumulation {

	private record Address(int superlayer, int layer, int wire) { }

	private final Map<Address, Integer> counts = new HashMap<>();
	private int events;
	private int maximum;

	public synchronized void add(AlertEventData data) {
		if (data == null) {
			return;
		}
		events++;
		for (AlertEventData.DcAdcHit hit : data.dcAdcHits()) {
			int updated = counts.merge(new Address(hit.superlayer(), hit.layer(), hit.wire()), 1, Integer::sum);
			maximum = Math.max(maximum, updated);
		}
	}

	public synchronized void clear() {
		counts.clear();
		events = 0;
		maximum = 0;
	}

	public synchronized int count(int superlayer, int layer, int wire) {
		return counts.getOrDefault(new Address(superlayer, layer, wire), 0);
	}

	public synchronized int eventCount() { return events; }
	public synchronized int maximumCount() { return maximum; }
}
