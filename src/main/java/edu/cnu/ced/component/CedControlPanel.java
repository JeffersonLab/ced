package edu.cnu.ced.component;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.EnumSet;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.mdi.component.CommonBorder;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.ui.colors.ColorScaleBar;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.ui.fonts.Fonts;

/** Reusable side control assembled consistently for CED detector views. */
@SuppressWarnings("serial")
public final class CedControlPanel extends JPanel {

	private static final int WIDTH = 270;
	private final CedDisplayArray displayArray;
	private final JLabel eventSummary = new JLabel("No event source open");
	private final DefaultListModel<String> bankModel = new DefaultListModel<>();
	private final List<String> bankPrefixes;

	public CedControlPanel(EnumSet<CedDisplayOption> options, List<String> bankPrefixes,
			FeedbackPane feedback, ScientificColorMap colorMap, String legendTitle,
			Runnable displayChanged) {
		super(new BorderLayout());
		this.bankPrefixes = List.copyOf(bankPrefixes);
		setPreferredSize(new Dimension(WIDTH, 420));
		displayArray = new CedDisplayArray(options, 3, 8, 3, displayChanged);

		JTabbedPane tabs = new JTabbedPane();
		tabs.setFont(Fonts.mediumFont);
		JPanel display = new JPanel();
		display.setLayout(new BoxLayout(display, BoxLayout.Y_AXIS));
		display.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
		displayArray.setBorder(new CommonBorder("Visibility"));
		displayArray.setAlignmentX(LEFT_ALIGNMENT);
		displayArray.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				displayArray.getPreferredSize().height));
		display.add(displayArray);
		JPanel eventPanel = new JPanel(new BorderLayout());
		eventPanel.setBorder(new CommonBorder("Current event"));
		eventPanel.add(eventSummary, BorderLayout.CENTER);
		eventPanel.setAlignmentX(LEFT_ALIGNMENT);
		eventPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
		display.add(eventPanel);
		if (colorMap != null) {
			ColorScaleBar legend = new ColorScaleBar(colorMap);
			legend.setLabels("0", "maximum");
			legend.setBorder(new CommonBorder(legendTitle));
			legend.setBarHeight(18);
			legend.setAlignmentX(LEFT_ALIGNMENT);
			Dimension legendSize = new Dimension(WIDTH - 12,
					legend.getPreferredSize().height);
			legend.setPreferredSize(legendSize);
			legend.setMinimumSize(legendSize);
			legend.setMaximumSize(new Dimension(Integer.MAX_VALUE,
					legendSize.height));
			display.add(legend);
		}
		tabs.addTab("display", display);

		JList<String> banks = new JList<>(bankModel);
		tabs.addTab("banks", new JScrollPane(banks));
		add(tabs, BorderLayout.NORTH);
		add(feedback, BorderLayout.CENTER);
	}

	public boolean isSelected(CedDisplayOption option) {
		return displayArray.isSelected(option);
	}

	public void update(EventNavigationState state) {
		eventSummary.setText(state.isOpen()
				? "Sequence " + state.sequenceNumber() + " of " + state.eventCount()
				: "No event source open");
		bankModel.clear();
		state.snapshot().bankNames().stream().filter(this::matches).forEach(bankModel::addElement);
	}

	private boolean matches(String bank) {
		return bankPrefixes.stream().anyMatch(bank::startsWith);
	}
}
