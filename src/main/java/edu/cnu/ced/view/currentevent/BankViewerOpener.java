package edu.cnu.ced.view.currentevent;

import java.util.HashMap;
import java.util.Map;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.EventSnapshot;

/**
 * Opens and caches per-bank data viewers -- the double-click behavior of the
 * present-banks list -- dispatching to a free floating window or an
 * MDI-internal view per {@link BankViewerDisplayMode}, exactly like legacy
 * CED's Ced.isFloatingBankDisplay() switch between CedDataWindow and
 * CedDataView.
 */
public final class BankViewerOpener {

	private final EventNavigator navigator;
	private final BankColumnVisibility visibility;
	private final BankViewerDisplayMode displayMode;
	private final Map<String, BankFloatingWindow> floatingWindows = new HashMap<>();
	private final Map<String, BankView> internalViews = new HashMap<>();

	public BankViewerOpener(EventNavigator navigator, BankColumnVisibility visibility,
			BankViewerDisplayMode displayMode) {
		this.navigator = navigator;
		this.visibility = visibility;
		this.displayMode = displayMode;
	}

	/**
	 * Open (or bring to front) the data viewer for {@code bankName}, reading
	 * its initial column set from {@code snapshot}. Does nothing if that bank
	 * is not actually present in {@code snapshot}.
	 */
	public void open(String bankName, EventSnapshot snapshot) {
		DataBank initialBank = (snapshot == null) ? null : snapshot.bank(bankName).orElse(null);
		if (initialBank == null) {
			return;
		}
		if (displayMode.isFloating()) {
			floatingWindows.computeIfAbsent(bankName, name -> new BankFloatingWindow(navigator, name,
					initialBank, visibility, () -> floatingWindows.remove(name))).showAndFront();
		} else {
			internalViews.computeIfAbsent(bankName,
					name -> new BankView(navigator, name, initialBank, visibility)).showAndActivate();
		}
	}
}
