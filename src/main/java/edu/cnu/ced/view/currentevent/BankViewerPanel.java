package edu.cnu.ced.view.currentevent;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.RunConfig;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * Shared content of the per-bank data viewer: a dynamic-column table of that
 * bank's rows, its own prev/next navigation (driven by the same shared
 * {@link EventNavigator} as the rest of CED), and a persisted
 * column-visibility checkbox row. Hosted by both {@link BankFloatingWindow}
 * (a free {@code JFrame}) and {@link BankView} (an MDI-internal view),
 * depending on the user's Options-menu preference -- matching legacy CED's
 * CedDataWindow/CedDataView pair.
 */
@SuppressWarnings("serial")
public final class BankViewerPanel extends JPanel {

	private final EventNavigator navigator;
	private final String bankName;
	private final BankColumnVisibility visibility;
	private final BankRowTable table;
	private final JLabel sequenceLabel = new JLabel("Sequence 0 of 0");
	private final JLabel eventLabel = new JLabel("True event —");
	private final JButton previousButton = new JButton("Previous");
	private final JButton nextButton = new JButton("Next");
	private final Consumer<EventNavigationState> stateListener = this::acceptState;

	public BankViewerPanel(EventNavigator navigator, String bankName, DataBank initialBank,
			BankColumnVisibility visibility) {
		this.navigator = navigator;
		this.bankName = bankName;
		this.visibility = visibility;
		this.table = new BankRowTable(bankName, initialBank);

		setLayout(new BorderLayout());
		add(northPanel(), BorderLayout.NORTH);
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(visibilityPanel(), BorderLayout.SOUTH);
		applyPersistedVisibility();

		navigator.addListener(stateListener);
		acceptState(navigator.state());
	}

	/** Detach from the shared navigator; call when this viewer's window/view closes for good. */
	public void dispose() {
		navigator.removeListener(stateListener);
	}

	private JPanel northPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		previousButton.addActionListener(event -> navigator.previous());
		nextButton.addActionListener(event -> navigator.next());
		panel.add(previousButton);
		panel.add(nextButton);
		panel.add(sequenceLabel);
		panel.add(eventLabel);
		return panel;
	}

	private JPanel visibilityPanel() {
		JPanel panel = new JPanel(new GridLayout(0, 6, 4, 4));
		panel.setBorder(BorderFactory.createTitledBorder("Visibility"));
		for (int column = 1; column < table.getColumnCount(); column++) {
			String columnName = table.rowModel().columnName(column);
			JCheckBox checkbox = new JCheckBox(columnName, visibility.isVisible(bankName, columnName));
			checkbox.setFont(Fonts.tweenFont);
			int fixedColumn = column;
			checkbox.addItemListener(event -> {
				boolean selected = checkbox.isSelected();
				table.setColumnVisible(fixedColumn, selected);
				visibility.setVisible(bankName, columnName, selected);
			});
			panel.add(checkbox);
		}
		return panel;
	}

	private void applyPersistedVisibility() {
		for (int column = 1; column < table.getColumnCount(); column++) {
			String columnName = table.rowModel().columnName(column);
			table.setColumnVisible(column, visibility.isVisible(bankName, columnName));
		}
	}

	private void acceptState(EventNavigationState state) {
		if (SwingUtilities.isEventDispatchThread()) {
			applyState(state);
		} else {
			SwingUtilities.invokeLater(() -> applyState(state));
		}
	}

	private void applyState(EventNavigationState state) {
		sequenceLabel.setText("Sequence " + state.sequenceNumber() + " of " + state.eventCount());
		RunConfig config = RunConfig.from(state.snapshot()).orElse(null);
		eventLabel.setText(config == null ? "True event —" : "True event " + config.event());
		previousButton.setEnabled(state.canGoPrevious());
		nextButton.setEnabled(state.canGoNext());
		table.rowModel().setSnapshot(state.snapshot());
	}
}
