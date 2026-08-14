package edu.cnu.ced.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.JToolBar;

import edu.cnu.ced.component.CedControlPanel;
import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.view.BaseView;

/** Common event-aware foundation for all CED detector views. */
@SuppressWarnings("serial")
public abstract class CedView extends BaseView {

	private final EventNavigator navigator;
	private final Consumer<EventNavigationState> eventListener = this::acceptEventState;
	private CedControlPanel controls;
	private boolean listening;

	protected CedView(EventNavigator navigator, Object... properties) {
		super(properties);
		this.navigator = navigator;
		installNextButton();
	}

	/**
	 * Install the standard CED side panel and begin event delivery. Subclasses call
	 * this after initializing their own fields, avoiding constructor callbacks into
	 * a partially constructed detector view.
	 */
	protected final void initializeCedView(EnumSet<CedDisplayOption> options,
			List<String> bankPrefixes, ScientificColorMap colorMap, String legendTitle) {
		FeedbackPane feedback = initFeedback(Color.CYAN, Color.BLACK, 10);
		controls = new CedControlPanel(options, bankPrefixes, feedback, colorMap,
				legendTitle, this::refresh);
		add(controls, BorderLayout.EAST);
		// BaseView packs its canvas before detector-specific controls are installed.
		// Repack now so an east panel expands the frame instead of stealing canvas.
		pack();
		navigator.addListener(eventListener);
		listening = true;
		acceptEventState(navigator.state());
	}

	protected final boolean isDisplayed(CedDisplayOption option) {
		return controls != null && controls.isSelected(option);
	}

	protected final EventNavigationState eventState() {
		return navigator.state();
	}

	/** Called on the Swing event-dispatch thread when a complete event is published. */
	protected abstract void eventChanged(EventNavigationState state);

	private void acceptEventState(EventNavigationState state) {
		Runnable update = () -> {
			eventChanged(state);
			if (controls != null) controls.update(state);
			refresh();
		};
		if (SwingUtilities.isEventDispatchThread()) update.run();
		else SwingUtilities.invokeLater(update);
	}

	private void installNextButton() {
		if (getToolBar() == null) return;
		JButton next = new JButton("Next");
		next.setToolTipText("Next event");
		next.addActionListener(event -> navigator.next());
		getToolBar().add(next, 0);
		getToolBar().add(new JToolBar.Separator(), 1);
	}

	@Override
	public void dispose() {
		if (listening) navigator.removeListener(eventListener);
		super.dispose();
	}
}
