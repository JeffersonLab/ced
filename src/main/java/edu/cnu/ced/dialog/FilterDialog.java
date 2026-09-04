package edu.cnu.ced.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import edu.cnu.ced.data.ParticleId;
import edu.cnu.ced.event.EventFilters;
import edu.cnu.ced.event.ParticleIdFilter;
import edu.cnu.ced.event.RequiredBanksFilter;
import edu.cnu.ced.event.TriggerBitFilter;
import edu.cnu.ced.event.TriggerBitFilter.MatchMode;
import edu.cnu.mdi.ui.colors.X11Colors;

/**
 * The "Filter…" configuration dialog (Events menu): one section per {@link
 * EventFilters} criterion -- trigger-bit pattern, required banks, particle
 * species -- each with its own "Active" checkbox, changes applying (and
 * persisting) immediately rather than behind a separate Apply/OK step, the
 * same way the bank viewer's Visibility checkboxes already do in this app.
 * Replaces legacy CED's split arrangement (a dedicated trigger-only dialog
 * reached from the View menu, plus an Event menu submenu that never grew a
 * second filter kind) with one dialog covering all three criteria.
 */
@SuppressWarnings("serial")
public final class FilterDialog extends JDialog {

	private static final int BIT_COUNT = 32;
	private static final Color ON_COLOR = X11Colors.getX11Color("dark blue");
	private static final Color OFF_COLOR = X11Colors.getX11Color("alice blue");
	private static final Font BIT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 9);

	private final EventFilters filters;
	private final Runnable onChange;

	public FilterDialog(Window owner, EventFilters filters, Runnable onChange) {
		super(owner, "Filter Events", Dialog.ModalityType.MODELESS);
		this.filters = filters;
		this.onChange = onChange;
		buildUi();
	}

	private void buildUi() {
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		content.add(triggerSection());
		content.add(Box.createVerticalStrut(6));
		content.add(bankSection());
		content.add(Box.createVerticalStrut(6));
		content.add(pidSection());

		JButton close = new JButton("Close");
		close.addActionListener(event -> dispose());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(close);

		setLayout(new BorderLayout());
		add(content, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(getOwner());
	}

	// ------------------------------------------------------------------
	// Trigger bits
	// ------------------------------------------------------------------

	private JPanel triggerSection() {
		TriggerBitFilter triggerBitFilter = filters.triggerBitFilter();
		JPanel section = new JPanel(new BorderLayout(4, 4));
		section.setBorder(BorderFactory.createTitledBorder("Trigger Bits"));

		JCheckBox active = new JCheckBox("Active", triggerBitFilter.isActive());
		active.addActionListener(event -> {
			triggerBitFilter.setActive(active.isSelected());
			notifyChanged();
		});

		JComboBox<MatchMode> mode = new JComboBox<>(MatchMode.values());
		mode.setSelectedItem(triggerBitFilter.mode());
		mode.addActionListener(event -> {
			triggerBitFilter.setMode((MatchMode) mode.getSelectedItem());
			notifyChanged();
		});

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(active);
		top.add(new JLabel("Matching:"));
		top.add(mode);
		section.add(top, BorderLayout.NORTH);

		JPanel bits = new JPanel(new GridLayout(1, BIT_COUNT, 1, 1));
		for (int index = BIT_COUNT - 1; index >= 0; index--) {
			bits.add(new PatternBitBox(triggerBitFilter, index));
		}
		bits.setMaximumSize(bits.getPreferredSize());
		JPanel bitsRow = new JPanel();
		bitsRow.setLayout(new BoxLayout(bitsRow, BoxLayout.X_AXIS));
		bitsRow.add(new JLabel("Click a box to toggle that bit in the pattern:"));
		bitsRow.add(Box.createHorizontalStrut(6));
		bitsRow.add(bits);
		section.add(bitsRow, BorderLayout.CENTER);
		return section;
	}

	/** One editable bit of the trigger pattern being configured. */
	private final class PatternBitBox extends JComponent {
		private final TriggerBitFilter triggerBitFilter;
		private final int index;

		PatternBitBox(TriggerBitFilter triggerBitFilter, int index) {
			this.triggerBitFilter = triggerBitFilter;
			this.index = index;
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent event) {
					long pattern = triggerBitFilter.pattern();
					triggerBitFilter.setPattern(pattern ^ (1L << PatternBitBox.this.index));
					notifyChanged();
					repaint();
				}
			});
		}

		@Override
		public Dimension getPreferredSize() {
			FontMetrics metrics = getFontMetrics(BIT_FONT);
			int padding = 6;
			return new Dimension(metrics.stringWidth("99") + padding, metrics.getHeight() + padding);
		}

		@Override
		public Dimension getMinimumSize() {
			return getPreferredSize();
		}

		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

		@Override
		protected void paintComponent(Graphics g) {
			int width = getWidth();
			int height = getHeight();
			boolean bitOn = (triggerBitFilter.pattern() & (1L << index)) != 0;

			g.setColor(bitOn ? ON_COLOR : OFF_COLOR);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.black);
			g.drawLine(0, 0, width - 1, 0);
			g.drawLine(0, 0, 0, height - 1);
			g.setColor(Color.white);
			g.drawLine(width - 1, 0, width - 1, height - 1);
			g.drawLine(0, height - 1, width - 1, height - 1);

			g.setFont(BIT_FONT);
			g.setColor(bitOn ? Color.white : Color.black);
			FontMetrics metrics = g.getFontMetrics(BIT_FONT);
			String label = Integer.toString(index);
			int textX = (width - metrics.stringWidth(label)) / 2;
			int textY = (height + metrics.getAscent()) / 2 - 1;
			g.drawString(label, textX, textY);
		}
	}

	// ------------------------------------------------------------------
	// Required banks
	// ------------------------------------------------------------------

	private JPanel bankSection() {
		RequiredBanksFilter requiredBanksFilter = filters.requiredBanksFilter();
		JPanel section = new JPanel(new BorderLayout(4, 4));
		section.setBorder(BorderFactory.createTitledBorder("Required Banks"));

		JCheckBox active = new JCheckBox("Active", requiredBanksFilter.isActive());
		JLabel hint = new JLabel("One bank name per line, e.g. CND::adc -- every listed bank must be present");
		JPanel top = new JPanel(new BorderLayout(10, 0));
		top.add(active, BorderLayout.WEST);
		top.add(hint, BorderLayout.CENTER);
		section.add(top, BorderLayout.NORTH);

		JTextArea banks = new JTextArea(String.join("\n", new TreeSet<>(requiredBanksFilter.requiredBanks())),
				4, 30);
		banks.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent event) {
				applyBanks(requiredBanksFilter, banks);
			}
		});
		active.addActionListener(event -> {
			applyBanks(requiredBanksFilter, banks);
			requiredBanksFilter.setActive(active.isSelected());
			notifyChanged();
		});
		section.add(new JScrollPane(banks), BorderLayout.CENTER);
		return section;
	}

	private void applyBanks(RequiredBanksFilter requiredBanksFilter, JTextArea banks) {
		TreeSet<String> parsed = new TreeSet<>();
		for (String line : banks.getText().split("[\\r\\n,]+")) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty()) {
				parsed.add(trimmed);
			}
		}
		requiredBanksFilter.setRequiredBanks(parsed);
		notifyChanged();
	}

	// ------------------------------------------------------------------
	// Particle species
	// ------------------------------------------------------------------

	private JPanel pidSection() {
		ParticleIdFilter particleIdFilter = filters.particleIdFilter();
		JPanel section = new JPanel(new BorderLayout(4, 4));
		section.setBorder(BorderFactory.createTitledBorder("Particle Species"));

		JCheckBox active = new JCheckBox("Active", particleIdFilter.isActive());
		JLabel hint = new JLabel("At least one reconstructed particle must be one of the checked species");
		JPanel top = new JPanel(new BorderLayout(10, 0));
		top.add(active, BorderLayout.WEST);
		top.add(hint, BorderLayout.CENTER);
		section.add(top, BorderLayout.NORTH);

		JPanel species = new JPanel(new GridLayout(0, 6, 4, 4));
		Map<Integer, String> knownSpecies = ParticleId.knownSpecies();
		for (int pid : new TreeSet<>(knownSpecies.keySet())) {
			JCheckBox pidBox = new JCheckBox(knownSpecies.get(pid), particleIdFilter.pids().contains(pid));
			pidBox.addActionListener(event -> {
				TreeSet<Integer> selected = new TreeSet<>(particleIdFilter.pids());
				if (pidBox.isSelected()) {
					selected.add(pid);
				} else {
					selected.remove(pid);
				}
				particleIdFilter.setPids(selected);
				notifyChanged();
			});
			species.add(pidBox);
		}
		active.addActionListener(event -> {
			particleIdFilter.setActive(active.isSelected());
			notifyChanged();
		});
		section.add(species, BorderLayout.CENTER);
		return section;
	}

	private void notifyChanged() {
		if (onChange != null) {
			onChange.run();
		}
	}
}
