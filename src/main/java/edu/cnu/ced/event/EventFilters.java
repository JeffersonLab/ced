package edu.cnu.ced.event;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The three criteria an event must pass, combined with a logical AND over
 * whichever ones are currently active -- the same composition rule legacy
 * CED's {@code FilterManager} uses (confirmed by its own unit test), just
 * with a fixed, known set of criteria rather than an open-ended registry,
 * since that's what this feature actually needs today.
 */
public final class EventFilters implements EventFilter {

	// one instance per navigator, so every place that needs to read or show
	// filter state -- the Filter dialog, each detector view's CedControlPanel,
	// CurrentEventView -- shares the exact same criteria and "any active"
	// answer, the same reasoning as BankViewerOpener.sharedFor.
	private static final Map<EventNavigator, EventFilters> SHARED = new WeakHashMap<>();

	private final TriggerBitFilter triggerBitFilter;
	private final RequiredBanksFilter requiredBanksFilter;
	private final ParticleIdFilter particleIdFilter;
	private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

	public EventFilters() {
		this(new TriggerBitFilter(), new RequiredBanksFilter(), new ParticleIdFilter());
	}

	public EventFilters(TriggerBitFilter triggerBitFilter, RequiredBanksFilter requiredBanksFilter,
			ParticleIdFilter particleIdFilter) {
		this.triggerBitFilter = triggerBitFilter;
		this.requiredBanksFilter = requiredBanksFilter;
		this.particleIdFilter = particleIdFilter;
	}

	/** @return the single {@link EventFilters} shared by every view driven by {@code navigator} */
	public static synchronized EventFilters sharedFor(EventNavigator navigator) {
		return SHARED.computeIfAbsent(navigator, nav -> new EventFilters());
	}

	public TriggerBitFilter triggerBitFilter() {
		return triggerBitFilter;
	}

	public RequiredBanksFilter requiredBanksFilter() {
		return requiredBanksFilter;
	}

	public ParticleIdFilter particleIdFilter() {
		return particleIdFilter;
	}

	@Override
	public boolean pass(EventSnapshot snapshot) {
		return triggerBitFilter.pass(snapshot) && requiredBanksFilter.pass(snapshot)
				&& particleIdFilter.pass(snapshot);
	}

	/** @return whether any of the three criteria is currently active -- drives the "Filtering Active" indicator */
	public boolean isAnyActive() {
		return triggerBitFilter.isActive() || requiredBanksFilter.isActive() || particleIdFilter.isActive();
	}

	/**
	 * Subscribe to configuration changes (any criterion's active flag,
	 * pattern, bank list, or species set). Deliberately separate from {@link
	 * EventNavigator}'s own listeners: those exist to reprocess and repaint
	 * the current event, which every open detector view does on every call --
	 * expensive, and pointless work to run just because a checkbox in the
	 * Filter dialog changed. A listener here should stay cheap (e.g. toggling
	 * a "Filtering Active" label's visibility).
	 */
	public void addListener(Runnable listener) {
		listeners.addIfAbsent(listener);
	}

	public void removeListener(Runnable listener) {
		listeners.remove(listener);
	}

	/** Notify listeners that a criterion's configuration changed. */
	public void notifyChanged() {
		listeners.forEach(Runnable::run);
	}
}
