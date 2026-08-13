package edu.cnu.ced.event;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;

/** One immutable publication of a CLAS12 event and its bank membership. */
public final class EventSnapshot {

	private static final EventSnapshot EMPTY = new EventSnapshot(null, List.of());

	private final DataEvent event;
	private final List<String> bankNames;

	private EventSnapshot(DataEvent event, List<String> bankNames) {
		this.event = event;
		this.bankNames = bankNames;
	}

	/** @return the singleton snapshot representing no current event */
	public static EventSnapshot empty() {
		return EMPTY;
	}

	/** Create a consistent snapshot for an event. */
	public static EventSnapshot of(DataEvent event) {
		if (event == null) {
			return EMPTY;
		}

		String[] names = event.getBankList();
		if (names == null || names.length == 0) {
			return new EventSnapshot(event, List.of());
		}

		return new EventSnapshot(event, Arrays.stream(names)
				.filter(name -> name != null && !name.isBlank())
				.distinct()
				.sorted()
				.toList());
	}

	/** @return whether this snapshot contains an event */
	public boolean hasEvent() {
		return event != null;
	}

	/** @return sorted, immutable bank names captured with this event */
	public List<String> bankNames() {
		return bankNames;
	}

	/** Return whether the named bank was present when this snapshot was made. */
	public boolean hasBank(String bankName) {
		return bankName != null && bankNames.contains(bankName);
	}

	/** Resolve a bank only when it belongs to this snapshot. */
	public Optional<DataBank> bank(String bankName) {
		if (!hasBank(bankName) || !event.hasBank(bankName)) {
			return Optional.empty();
		}
		return Optional.ofNullable(event.getBank(bankName));
	}
}
