package edu.cnu.ced.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.RunConfig;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.BaseView;

/** Compact navigator and present-bank browser for the current event. */
@SuppressWarnings("serial")
public final class CurrentEventView extends BaseView {

	private final EventNavigator navigator;
	private final JLabel sourceLabel = new JLabel("No event source open");
	private final JLabel sequenceLabel = new JLabel("Sequence 0 of 0");
	private final JLabel eventLabel = new JLabel("True event —");
	private final JLabel runLabel = new JLabel("Run —");
	private final JButton previousButton = new JButton("Previous");
	private final JButton nextButton = new JButton("Next");
	private final DefaultListModel<String> bankModel = new DefaultListModel<>();
	private final Consumer<EventNavigationState> stateListener = this::acceptState;

	public CurrentEventView(EventNavigator navigator) {
		super(PropertyUtils.TITLE, "Current Event",
				PropertyUtils.WIDTH, 700,
				PropertyUtils.HEIGHT, 650,
				PropertyUtils.USECONTAINER, false);
		this.navigator = navigator;

		JPanel header = new JPanel(new BorderLayout(8, 4));
		header.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
		header.add(sourceLabel, BorderLayout.CENTER);

		JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		previousButton.addActionListener(event -> navigator.previous());
		nextButton.addActionListener(event -> navigator.next());
		navigation.add(previousButton);
		navigation.add(nextButton);
		navigation.add(sequenceLabel);
		navigation.add(eventLabel);
		navigation.add(runLabel);
		header.add(navigation, BorderLayout.SOUTH);

		JList<String> banks = new JList<>(bankModel);
		banks.setVisibleRowCount(24);
		JScrollPane scrollPane = new JScrollPane(banks);
		scrollPane.setBorder(BorderFactory.createTitledBorder("Present Banks"));

		add(header, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
		navigator.addListener(stateListener);
		applyState(navigator.state());
	}

	private void acceptState(EventNavigationState state) {
		if (SwingUtilities.isEventDispatchThread()) {
			applyState(state);
		} else {
			SwingUtilities.invokeLater(() -> applyState(state));
		}
	}

	private void applyState(EventNavigationState state) {
		sourceLabel.setText(state.isOpen() ? state.source() : "No event source open");
		sequenceLabel.setText("Sequence " + state.sequenceNumber() + " of " + state.eventCount());
		RunConfig config = RunConfig.from(state.snapshot()).orElse(null);
		eventLabel.setText(config == null ? "True event —" : "True event " + config.event());
		runLabel.setText(config == null ? "Run —" : "Run " + config.run());
		previousButton.setEnabled(state.canGoPrevious());
		nextButton.setEnabled(state.canGoNext());
		bankModel.clear();
		state.snapshot().bankNames().forEach(bankModel::addElement);
	}

	@Override
	public void dispose() {
		navigator.removeListener(stateListener);
		super.dispose();
	}
}
