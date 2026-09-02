package edu.cnu.ced.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

import edu.cnu.ced.data.ParticleId;
import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.style.CedDrawingStyle;

/**
 * A compact, self-drawing toolbar legend showing the color and line style
 * used for each distinct particle species actually present in the current
 * event.
 * <p>
 * Mirrors bCNU CED's own {@code PIDLegend} (a toolbar "user component"): a
 * small {@link JComponent} that recomputes its contents from the event's
 * own unique species list on every event change, rather than a fixed
 * reference key -- so a sparse event's legend is short, and a busy one
 * shows only the species that are actually relevant right now. It's driven
 * by {@link CedDrawingStyle}'s {@code cnuphys.lund.LundStyle}-backed
 * colors/strokes, the same source used to draw the particles themselves,
 * so the legend can never drift out of sync with what's on screen.
 * </p>
 * <p>
 * Installed once per {@link edu.cnu.ced.view.CedView}, fed the whole
 * event's particle list regardless of which subset that particular view
 * actually draws -- matching legacy's own event-wide (not per-view)
 * species list, and letting a legend on any view double as a quick census
 * of everything reconstructed in the event.
 * </p>
 */
@SuppressWarnings("serial")
public final class PidLegend extends JComponent {

	private static final Color BACKGROUND = new Color(240, 240, 240);
	private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
	private static final int LINE_LENGTH = 26;
	private static final int GAP_AFTER_LINE = 3;
	private static final int GAP_AFTER_LABEL = 9;
	private static final int LEFT_MARGIN = 4;

	/** Package-private (not private) so a test can inspect the computed list directly. */
	record Species(int pid, int charge) { }

	private List<Species> species = List.of();

	public PidLegend() {
		setBackground(BACKGROUND);
	}

	/** Recomputes the unique species shown, from every particle in the current event. */
	public void update(List<RecEventData.Particle> particles) {
		Set<Species> unique = new LinkedHashSet<>();
		for (RecEventData.Particle particle : particles) {
			unique.add(new Species(particle.pid(), particle.charge()));
		}
		List<Species> sorted = new ArrayList<>(unique);
		sorted.sort(Comparator.comparingInt(Species::pid).thenComparingInt(Species::charge));
		species = sorted;
		revalidate();
		repaint();
	}

	/** @return the currently displayed species, sorted by (pid, charge); for tests */
	List<Species> species() {
		return species;
	}

	@Override
	public Dimension getPreferredSize() {
		FontMetrics metrics = getFontMetrics(LABEL_FONT);
		int height = metrics.getHeight() * 2 + 8;
		int width = LEFT_MARGIN;
		for (Species entry : species) {
			width += entrySpan(metrics, ParticleId.name(entry.pid(), entry.charge()));
		}
		return new Dimension(Math.max(width, 8), height);
	}

	@Override
	public void paintComponent(Graphics g) {
		Rectangle bounds = getBounds();
		g.setColor(getBackground());
		g.fillRect(0, 0, bounds.width, bounds.height);

		if (species.isEmpty()) return;

		FontMetrics metrics = g.getFontMetrics(LABEL_FONT);
		int rowHeight = metrics.getHeight();
		int yCenter = bounds.height / 2;
		int yTopRow = yCenter - rowHeight / 2;
		int yBottomRow = yCenter + rowHeight / 2 + 2;

		int x = LEFT_MARGIN;
		for (int i = 0; i < species.size(); i++) {
			Species entry = species.get(i);
			int y = (i % 2 == 0) ? yTopRow : yBottomRow;
			x += drawEntry(g, x, y, entry);
		}
	}

	private int drawEntry(Graphics g, int x, int y, Species entry) {
		Graphics2D g2 = (Graphics2D) g;
		Color color = CedDrawingStyle.particleColor(entry.pid(), entry.charge());
		Stroke stroke = CedDrawingStyle.particleStroke(entry.pid(), entry.charge());
		String name = ParticleId.name(entry.pid(), entry.charge());

		Stroke previousStroke = g2.getStroke();
		g2.setStroke(stroke);
		g.setColor(color);
		g2.drawLine(x, y, x + LINE_LENGTH, y);
		g2.setStroke(previousStroke);

		int labelX = x + LINE_LENGTH + GAP_AFTER_LINE;
		g.setFont(LABEL_FONT);
		FontMetrics metrics = g.getFontMetrics(LABEL_FONT);
		g.setColor(Color.BLACK);
		g.drawString(name, labelX, y + metrics.getAscent() / 2 - 2);

		return entrySpan(metrics, name);
	}

	private static int entrySpan(FontMetrics metrics, String name) {
		return LINE_LENGTH + GAP_AFTER_LINE + metrics.stringWidth(name) + GAP_AFTER_LABEL;
	}
}
