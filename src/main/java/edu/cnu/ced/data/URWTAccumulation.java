package edu.cnu.ced.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-(sector, layer, strip) hit occupancy accumulated across requested
 * events.
 * <p>
 * Uses a map rather than a fixed array, unlike this codebase's other
 * accumulation classes (e.g. {@code FMTAccumulation}): {@code URWT::hits}'
 * own bank documentation describes layer as spanning 1-9, wider than
 * {@code URWTGeometry}'s 4 layers per sector, and the actual maximum strip
 * count per (sector, layer) isn't established (see {@link URWTEventData}'s
 * class doc) -- so this accepts whatever addresses actually appear rather
 * than assuming trusted, fixed bounds.
 * </p>
 */
public final class URWTAccumulation {

	private record Address(int sector, int layer, int strip) { }

	private final Map<Address, Integer> counts = new HashMap<>();
	private int events;
	private int maximum;

	public synchronized void add(URWTEventData data) {
		if (data == null) {
			return;
		}
		events++;
		for (URWTEventData.Hit hit : data.hits()) {
			int updated = counts.merge(new Address(hit.sector(), hit.layer(), hit.strip()), 1, Integer::sum);
			maximum = Math.max(maximum, updated);
		}
	}

	public synchronized void clear() {
		counts.clear();
		events = 0;
		maximum = 0;
	}

	public synchronized int count(int sector, int layer, int strip) {
		return counts.getOrDefault(new Address(sector, layer, strip), 0);
	}

	public synchronized int eventCount() { return events; }
	public synchronized int maximumCount() { return maximum; }
}
