package edu.cnu.ced.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.data.BankColumns;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.EventSnapshot;
import edu.cnu.ced.event.RunConfig;
import edu.cnu.ced.view.currentevent.BankColumnCatalog;
import edu.cnu.ced.view.currentevent.BankColumnEntry;
import edu.cnu.ced.view.currentevent.BankColumnTable;
import edu.cnu.ced.view.currentevent.SeenBankTally;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.BaseView;

/** Navigator and full bank/column browser for the current event. */
@SuppressWarnings("serial")
public final class CurrentEventView extends BaseView {

	private final EventNavigator navigator;
	private final JLabel sourceLabel = new JLabel("No event source open");
	private final JLabel sequenceLabel = new JLabel("Sequence 0 of 0");
	private final JLabel eventLabel = new JLabel("True event —");
	private final JLabel runLabel = new JLabel("Run —");
	private final JButton previousButton = new JButton("Previous");
	private final JButton nextButton = new JButton("Next");
	private final JTextField seqGotoField = new JTextField(6);
	private final JTextField trueGotoField = new JTextField(6);
	private final DefaultListModel<String> valueListModel = new DefaultListModel<>();
	private final BankColumnTable columnTable = new BankColumnTable();
	private final SeenBankTally seenBankTally = new SeenBankTally();
	private final DefaultListModel<String> seenBankModel = new DefaultListModel<>();
	private final Consumer<EventNavigationState> stateListener = this::acceptState;
	private final Runnable sourceListener = this::acceptNewSource;

	private EventSnapshot snapshot = EventSnapshot.empty();

	public CurrentEventView(EventNavigator navigator) {
		super(PropertyUtils.TITLE, "Current Event",
				PropertyUtils.WIDTH, 1100,
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

		JPanel gotoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		gotoPanel.add(new JLabel("goto event   seq:"));
		gotoPanel.add(seqGotoField);
		gotoPanel.add(Box.createHorizontalStrut(6));
		gotoPanel.add(new JLabel("true:"));
		gotoPanel.add(trueGotoField);
		seqGotoField.addKeyListener(gotoKeyListener(seqGotoField, navigator::goToSequence));
		trueGotoField.addKeyListener(gotoKeyListener(trueGotoField, navigator::goToTrueEventNumber));
		header.add(gotoPanel, BorderLayout.CENTER);

		JList<String> valueList = new JList<>(valueListModel);
		valueList.setFont(Fonts.tweenFont);
		JScrollPane valueScroll = new JScrollPane(valueList);
		valueScroll.setBorder(BorderFactory.createTitledBorder("Values"));
		valueScroll.setPreferredSize(new Dimension(180, 400));

		columnTable.getSelectionModel().addListSelectionListener(this::columnSelected);
		JScrollPane tableScroll = new JScrollPane(columnTable);
		tableScroll.setBorder(BorderFactory.createTitledBorder("Present Bank Columns"));

		JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false,
				valueScroll, tableScroll);
		center.setResizeWeight(0.1);

		add(header, BorderLayout.NORTH);
		add(center, BorderLayout.CENTER);
		add(seenBanksPanel(), BorderLayout.WEST);
		navigator.addListener(stateListener);
		navigator.addSourceListener(sourceListener);
		applyState(navigator.state());
	}

	// clears the running tally for the newly-opened source, before its first event arrives
	private void acceptNewSource() {
		seenBankTally.clear();
		if (SwingUtilities.isEventDispatchThread()) {
			refreshSeenBanks();
		} else {
			SwingUtilities.invokeLater(this::refreshSeenBanks);
		}
	}

	// west "Seen Banks" running tally, with a Clear button
	private JPanel seenBanksPanel() {
		JPanel panel = new JPanel(new BorderLayout(4, 4));
		panel.setPreferredSize(new Dimension(170, 300));

		JLabel title = new JLabel("Seen Banks");
		title.setHorizontalAlignment(JLabel.CENTER);
		title.setFont(Fonts.biggerFont);
		title.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		JList<String> seenBankList = new JList<>(seenBankModel);
		seenBankList.setFont(Fonts.smallFont);

		JButton clearButton = new JButton("Clear");
		clearButton.addActionListener(event -> {
			seenBankTally.clear();
			refreshSeenBanks();
		});
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(clearButton);

		panel.add(title, BorderLayout.NORTH);
		panel.add(new JScrollPane(seenBankList), BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		return panel;
	}

	private void refreshSeenBanks() {
		seenBankModel.clear();
		seenBankTally.summaries().forEach(seenBankModel::addElement);
	}

	/** One Enter-key-triggered goto action shared by the seq/true fields. */
	private interface GotoAction {
		boolean goTo(int number);
	}

	private KeyAdapter gotoKeyListener(JTextField field, GotoAction action) {
		return new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent event) {
				if (event.getKeyCode() != KeyEvent.VK_ENTER) {
					return;
				}
				try {
					if (!action.goTo(Integer.parseInt(field.getText().trim()))) {
						field.setText("");
					}
				} catch (NumberFormatException notANumber) {
					field.setText("");
				}
			}
		};
	}

	private void columnSelected(ListSelectionEvent event) {
		if (event.getValueIsAdjusting()) {
			return;
		}
		valueListModel.clear();
		BankColumnEntry entry = columnTable.selectedEntry();
		if (entry == null) {
			return;
		}
		DataBank bank = snapshot.bank(entry.bankName()).orElse(null);
		List<String> values = BankColumns.formattedValues(bank, entry.columnName());
		for (int index = 0; index < values.size(); index++) {
			valueListModel.addElement(String.format("[%02d]  %s", index, values.get(index)));
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
		sourceLabel.setText(state.isOpen() ? state.source() : "No event source open");
		sequenceLabel.setText("Sequence " + state.sequenceNumber() + " of " + state.eventCount());
		RunConfig config = RunConfig.from(state.snapshot()).orElse(null);
		eventLabel.setText(config == null ? "True event —" : "True event " + config.event());
		runLabel.setText(config == null ? "Run —" : "Run " + config.run());
		previousButton.setEnabled(state.canGoPrevious());
		nextButton.setEnabled(state.canGoNext());

		snapshot = state.snapshot();
		valueListModel.clear();
		columnTable.bankColumnModel().setEntries(BankColumnCatalog.build(snapshot));

		seenBankTally.accept(snapshot);
		refreshSeenBanks();
	}

	@Override
	public void dispose() {
		navigator.removeListener(stateListener);
		navigator.removeSourceListener(sourceListener);
		super.dispose();
	}
}
