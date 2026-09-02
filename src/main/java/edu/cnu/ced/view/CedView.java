package edu.cnu.ced.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.JToolBar;

import edu.cnu.ced.component.CedControlPanel;
import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.component.PidLegend;
import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.hover.HoverEvent;
import edu.cnu.mdi.hover.HoverInfoWindow;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.view.BaseView;

/** Common event-aware foundation for all CED detector views. */
@SuppressWarnings("serial")
public abstract class CedView extends BaseView {

	private final EventNavigator navigator;
	private final Consumer<EventNavigationState> eventListener = this::acceptEventState;
	private final Runnable sourceListener = this::acceptSourceChange;
	private CedControlPanel controls;
	private boolean listening;
	private final PidLegend pidLegend = new PidLegend();

	protected CedView(EventNavigator navigator, Object... properties) {
		super(properties);
		this.navigator = navigator;
		installNextButton();
		installPidLegend();
	}

	/**
	 * Install the standard CED side panel and begin event delivery. Subclasses call
	 * this after initializing their own fields, avoiding constructor callbacks into
	 * a partially constructed detector view.
	 */
	protected final void initializeCedView(EnumSet<CedDisplayOption> options,
			List<String> bankPrefixes, ScientificColorMap colorMap, String legendTitle) {
		initializeCedView(options, bankPrefixes, colorMap, legendTitle,
				CedControlPanel.DEFAULT_WIDTH);
	}

	/** Install the standard controls with a detector-specific sidebar width. */
	protected final void initializeCedView(EnumSet<CedDisplayOption> options,
			List<String> bankPrefixes, ScientificColorMap colorMap, String legendTitle,
			int controlWidth) {
		FeedbackPane feedback = initFeedback(Color.CYAN, Color.BLACK, 10);
		controls = new CedControlPanel(options, bankPrefixes, feedback, colorMap,
				legendTitle, this::refresh, controlWidth);
		add(controls, BorderLayout.EAST);
		// BaseView packs its canvas before detector-specific controls are installed.
		// Repack now so an east panel expands the frame instead of stealing canvas.
		pack();
		navigator.addListener(eventListener);
		navigator.addSourceListener(sourceListener);
		listening = true;
		acceptEventState(navigator.state());
	}

	protected final boolean isDisplayed(CedDisplayOption option) {
		return controls != null && controls.isSelected(option);
	}

	/** Add a detector-specific component to the standard display tab. */
	protected final void addDisplayControl(java.awt.Component component) {
		if (controls == null) throw new IllegalStateException("CED view is not initialized");
		controls.addDisplayControl(component);
	}

	/** Add a detector-specific control tab. */
	protected final void addControlTab(String title, java.awt.Component component) {
		if (controls == null) throw new IllegalStateException("CED view is not initialized");
		controls.addTab(title, component);
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
			pidLegend.update(RecEventData.from(state.snapshot()).particles());
			refresh();
		};
		if (SwingUtilities.isEventDispatchThread()) update.run();
		else SwingUtilities.invokeLater(update);
	}

	private void acceptSourceChange() {
		Runnable reset = () -> {
			if (controls != null) controls.showSingleEvent();
			refresh();
		};
		if (SwingUtilities.isEventDispatchThread()) reset.run();
		else SwingUtilities.invokeLater(reset);
	}

	private void installNextButton() {
		if (getToolBar() == null) return;
		JButton next = new JButton("Next");
		next.setToolTipText("Next event");
		next.addActionListener(event -> navigator.next());
		getToolBar().add(next, 0);
		getToolBar().add(new JToolBar.Separator(), 1);
	}

	/**
	 * Adds the shared {@link PidLegend} to this view's toolbar, right after
	 * the standard controls. Every {@code CedView} gets one, kept current on
	 * every event change via {@link #acceptEventState}, regardless of
	 * whether this particular view draws particles itself.
	 */
	private void installPidLegend() {
		if (getToolBar() == null) return;
		getToolBar().add(pidLegend);
	}

	/**
	 * Shows a floating popup near the cursor after it pauses over this
	 * view -- the same information already shown live in the black
	 * feedback pane as the mouse moves, reused here rather than gathered a
	 * second way: this just runs the view's own (possibly overridden)
	 * {@link #getFeedbackStrings} and reformats the result as plain text.
	 * Matches bCNU CED's own delayed hover popup, and {@code MapView2D}'s
	 * identical approach for map feature hover in the {@code mdi}
	 * framework itself.
	 */
	@Override
	public void hoverUpdate(HoverEvent he) {
		IContainer container = getIContainer();
		Point screenPoint = he.getLocation();
		if (container == null || screenPoint == null) return;
		HoverInfoWindow window = container.getHoverWindow();
		if (window == null) return;
		Point2D.Double worldPoint = new Point2D.Double();
		container.localToWorld(screenPoint, worldPoint);
		List<String> hits = new ArrayList<>();
		getFeedbackStrings(container, screenPoint, worldPoint, hits);
		String text = hoverText(hits);
		if (text == null) {
			window.hideMessage();
			return;
		}
		window.showMessage(he, text);
	}

	@Override
	public void hoverClosed(HoverEvent he) {
		IContainer container = getIContainer();
		HoverInfoWindow window = container == null ? null : container.getHoverWindow();
		if (window != null) window.hideMessage();
	}

	/** Package-private (not private) so a test can exercise it directly. */
	static String hoverText(List<String> feedbackLines) {
		StringBuilder text = new StringBuilder();
		for (String line : feedbackLines) {
			String plain = FeedbackPane.stripStyle(line);
			if (plain == null || plain.isEmpty()) continue;
			if (text.length() > 0) text.append('\n');
			text.append(plain);
		}
		return text.length() == 0 ? null : text.toString();
	}

	@Override
	public void dispose() {
		if (listening) {
			navigator.removeListener(eventListener);
			navigator.removeSourceListener(sourceListener);
		}
		super.dispose();
	}
}
