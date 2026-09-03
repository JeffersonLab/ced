package edu.cnu.ced.view.currentevent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.cnu.ced.event.EventSnapshot;

/**
 * Running per-source tally of how many events (of those already visited)
 * contained each bank at least once -- the west "Seen Banks" list in legacy
 * CED's Current Event window. Unlike {@link BankColumnCatalog}, which is
 * rebuilt fresh from a single snapshot, this accumulates across every
 * snapshot seen since the last {@link #clear()}.
 */
public final class SeenBankTally {

	private final Map<String, Long> counts = new LinkedHashMap<>();

	/** Record one more event's banks; call once per navigated-to event. */
	public void accept(EventSnapshot snapshot) {
		if (snapshot == null || !snapshot.hasEvent()) {
			return;
		}
		for (String bankName : snapshot.bankNames()) {
			counts.merge(bankName, 1L, Long::sum);
		}
	}

	/** Reset the tally, e.g. when a new event source is opened. */
	public void clear() {
		counts.clear();
	}

	/** @return {@code "[bankName]  (count)"} for every seen bank, sorted alphabetically */
	public List<String> summaries() {
		List<String> summaries = new ArrayList<>(counts.size());
		counts.keySet().stream().sorted()
				.forEach(name -> summaries.add("[" + name + "]  (" + counts.get(name) + ")"));
		return summaries;
	}

	/** @return how many visited events have contained {@code bankName} */
	public long count(String bankName) {
		return counts.getOrDefault(bankName, 0L);
	}
}
