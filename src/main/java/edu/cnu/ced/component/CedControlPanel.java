package edu.cnu.ced.component;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.util.EnumSet;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.EventSnapshot;
import edu.cnu.ced.view.currentevent.BankViewerOpener;
import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.ui.colors.ColorScaleBar;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.ui.fonts.Fonts;

/** Reusable side control assembled consistently for CED detector views. */
@SuppressWarnings("serial")
public final class CedControlPanel extends JPanel {

	public static final int DEFAULT_WIDTH = 270;
	private final CedDisplayArray displayArray;
	private final JTextArea eventSummary = new JTextArea("No event source open");
	private final DefaultListModel<String> bankModel = new DefaultListModel<>();
	private final List<String> bankPrefixes;
	private final JPanel displayPanel;
	private final JTabbedPane tabs;
	private final BankViewerOpener bankViewerOpener;
	private EventSnapshot snapshot = EventSnapshot.empty();

	public CedControlPanel(EventNavigator navigator, EnumSet<CedDisplayOption> options,
			List<String> bankPrefixes, FeedbackPane feedback, ScientificColorMap colorMap,
			String legendTitle, Runnable displayChanged) {
		this(navigator, options, bankPrefixes, feedback, colorMap, legendTitle, displayChanged,
				DEFAULT_WIDTH);
	}

	public CedControlPanel(EventNavigator navigator, EnumSet<CedDisplayOption> options,
			List<String> bankPrefixes, FeedbackPane feedback, ScientificColorMap colorMap,
			String legendTitle, Runnable displayChanged, int width) {
		super(new BorderLayout());
		this.bankPrefixes = List.copyOf(bankPrefixes);
		this.bankViewerOpener = BankViewerOpener.sharedFor(navigator);
		int panelWidth = Math.max(DEFAULT_WIDTH, width);
		setPreferredSize(new Dimension(panelWidth, 420));
		displayArray = new CedDisplayArray(options, 3, 4, 3, displayChanged);

		tabs = new JTabbedPane();
		tabs.setFont(Fonts.mediumFont);
		displayPanel = new JPanel();
		displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.Y_AXIS));
		displayPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
		displayArray.setBorder(new CommonBorder("Visibility"));
		displayArray.setAlignmentX(LEFT_ALIGNMENT);
		displayArray.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				displayArray.getPreferredSize().height));
		displayPanel.add(displayArray);
		JPanel eventPanel = new JPanel(new BorderLayout());
		eventPanel.setBorder(new CommonBorder("Current event"));
		eventSummary.setEditable(false);
		eventSummary.setFocusable(false);
		eventSummary.setOpaque(false);
		eventSummary.setFont(Fonts.smallFont);
		eventSummary.setLineWrap(true);
		eventSummary.setWrapStyleWord(false);
		eventPanel.add(eventSummary, BorderLayout.CENTER);
		eventPanel.setAlignmentX(LEFT_ALIGNMENT);
		eventPanel.setPreferredSize(new Dimension(panelWidth - 12, 92));
		eventPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
		displayPanel.add(eventPanel);
		if (colorMap != null) {
			ColorScaleBar legend = new ColorScaleBar(colorMap);
			legend.setLabels("0", "maximum");
			legend.setBorder(new CommonBorder(legendTitle));
			legend.setBarHeight(18);
			legend.setAlignmentX(LEFT_ALIGNMENT);
			Dimension legendSize = new Dimension(panelWidth - 12,
					legend.getPreferredSize().height);
			legend.setPreferredSize(legendSize);
			legend.setMinimumSize(legendSize);
			legend.setMaximumSize(new Dimension(Integer.MAX_VALUE,
					legendSize.height));
			displayPanel.add(legend);
		}
		tabs.addTab("display", displayPanel);

		JList<String> banks = new JList<>(bankModel);
		banks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		banks.getSelectionModel().addListSelectionListener(event -> {
			if (event.getValueIsAdjusting()) return;
			String bankName = banks.getSelectedValue();
			if (bankName != null) bankViewerOpener.open(bankName, snapshot);
		});
		tabs.addTab("banks", new JScrollPane(banks));
		add(tabs, BorderLayout.NORTH);
		add(feedback, BorderLayout.CENTER);
	}

	/** Add a detector-specific control beneath the standard display controls. */
	public void addDisplayControl(Component component) {
		if (component instanceof JComponent swingComponent)
			swingComponent.setAlignmentX(LEFT_ALIGNMENT);
		displayPanel.add(component, 1);
		displayPanel.revalidate();
	}

	/** Add a detector-specific tab beside the standard display and banks tabs. */
	public void addTab(String title, Component component) {
		tabs.addTab(title, component);
	}

	public boolean isSelected(CedDisplayOption option) {
		return displayArray.isSelected(option);
	}

	/** Restore the standard display mode used when an event source changes. */
	public void showSingleEvent() {
		displayArray.setSelected(CedDisplayOption.SINGLE_EVENT, true);
	}

	public void update(EventNavigationState state) {
		if (state.isOpen()) {
			edu.cnu.ced.event.RunConfig config =
					edu.cnu.ced.event.RunConfig.from(state.snapshot()).orElse(null);
			String sourceType = state.source().toLowerCase(java.util.Locale.ROOT)
					.endsWith(".hipo") ? "HIPOFILE" : "EVENT SOURCE";
			eventSummary.setText("Event source: " + sourceType
					+ "\nFile: " + state.source()
					+ "\nSequential number: " + state.sequenceNumber()
					+ (config == null ? "" : "\nTrue number: " + config.event()
							+ "   Run: " + config.run()));
		} else {
			eventSummary.setText("No event source open");
		}
		snapshot = state.snapshot();
		bankModel.clear();
		snapshot.bankNames().stream().filter(this::matches).forEach(bankModel::addElement);
	}

	private boolean matches(String bank) {
		return bankPrefixes.stream().anyMatch(bank::startsWith);
	}
}
