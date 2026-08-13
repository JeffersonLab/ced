package edu.cnu.ced.view;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import edu.cnu.ced.event.EventNavigator;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.component.AspectRatioPanel;
import edu.cnu.mdi.ui.fonts.Fonts;

/** Common world-coordinate behavior for CED laboratory XY views. */
@SuppressWarnings("serial")
public abstract class CedXYView extends CedView {

	private static final Color ORIENTATION_FILL = new Color(210, 210, 210, 150);
	private static final Color REVERSED_AXIS_COLOR = new Color(139, 0, 0);
	private static final Color STANDARD_AXIS_COLOR = new Color(0, 0, 139);
	private static final int DIVISIONS = 40;

	protected CedXYView(EventNavigator navigator, Object... properties) {
		super(navigator, properties);
		installAspectRatioCanvas(1.0);
	}

	private void installAspectRatioCanvas(double aspectRatio) {
		IContainer drawingContainer = getIContainer();
		if (drawingContainer == null) return;
		Component canvas = drawingContainer.getComponent();
		Container parent = canvas == null ? null : canvas.getParent();
		if (parent == null || !(parent.getLayout() instanceof BorderLayout)) return;
		parent.remove(canvas);
		parent.add(new AspectRatioPanel(canvas, aspectRatio), BorderLayout.CENTER);
	}

	protected final void addXYFeedback(Point2D.Double worldPoint, String units,
			List<String> feedback) {
		if (worldPoint == null) return;
		feedback.add(String.format("$yellow$(x, y) = (%6.2f, %6.2f) %s",
				worldPoint.x, worldPoint.y, units));
		feedback.add(String.format("$yellow$radius = %6.2f %s", worldPoint.distance(0, 0), units));
		feedback.add(String.format("$yellow$phi = %6.2f°",
				Math.toDegrees(Math.atan2(worldPoint.y, worldPoint.x))));
	}

	/** Draw the standard CED XY border, ticks, labels, and orientation marker. */
	protected final void drawXYAxes(Graphics2D graphics, IContainer container) {
		if (graphics == null || container == null || container.getComponent() == null) return;
		Rectangle screen = new Rectangle(0, 0, container.getComponent().getWidth() - 1,
				container.getComponent().getHeight() - 1);
		if (screen.width < 2 || screen.height < 2) return;

		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setFont(Fonts.mediumFont);
			FontMetrics metrics = g.getFontMetrics();
			Rectangle2D.Double world = new Rectangle2D.Double();
			container.localToWorld(screen, world);
			g.setColor(Color.BLACK);
			g.drawRect(screen.x, screen.y, screen.width, screen.height);
			drawXTicks(g, container, screen, world, metrics);
			drawYTicks(g, container, screen, world, metrics);
			drawOrientation(g, screen, metrics, world.width < 0);
		} finally {
			g.dispose();
		}
	}

	private static void drawXTicks(Graphics2D g, IContainer container, Rectangle screen,
			Rectangle2D.Double world, FontMetrics metrics) {
		double delta = world.width / DIVISIONS;
		int bottom = screen.y + screen.height;
		Point pixel = new Point();
		for (int i = 1; i <= DIVISIONS; i++) {
			double x = world.x + delta * i;
			container.worldToLocal(pixel, x, world.y);
			boolean major = i % 5 == 0;
			g.drawLine(pixel.x, bottom, pixel.x, bottom - (major ? 12 : 5));
			if (major) {
				String label = axisValue(x);
				g.drawString(label, pixel.x - metrics.stringWidth(label) / 2,
						bottom - 14);
			}
		}
	}

	private static void drawYTicks(Graphics2D g, IContainer container, Rectangle screen,
			Rectangle2D.Double world, FontMetrics metrics) {
		double delta = world.height / DIVISIONS;
		Point pixel = new Point();
		for (int i = 0; i <= DIVISIONS; i++) {
			double y = world.y + delta * i;
			container.worldToLocal(pixel, world.x, y);
			boolean major = i % 5 == 0;
			g.drawLine(screen.x, pixel.y, screen.x + (major ? 12 : 5), pixel.y);
			if (major) {
				String label = axisValue(y);
				g.drawString(label, screen.x + 14,
						pixel.y + metrics.getAscent() / 2);
			}
		}
	}

	private static void drawOrientation(Graphics2D g, Rectangle screen,
			FontMetrics metrics, boolean xToLeft) {
		int anchorX = screen.x + 70;
		int bottom = screen.y + screen.height - 46;
		int horizontalEnd = anchorX + (xToLeft ? -50 : 50);
		int top = bottom - 50;
		int boxX = Math.min(anchorX, horizontalEnd) - 22;
		g.setColor(ORIENTATION_FILL);
		g.fillRect(boxX, top - 12, 94, 76);
		g.setColor(xToLeft ? REVERSED_AXIS_COLOR : STANDARD_AXIS_COLOR);
		g.drawLine(anchorX, bottom, horizontalEnd, bottom);
		g.drawLine(anchorX, bottom, anchorX, top);
		g.drawString("x", horizontalEnd + (xToLeft ? -metrics.stringWidth("x") - 3 : 3),
				bottom + metrics.getAscent() / 2);
		g.drawString("y", anchorX + 3, top + metrics.getAscent() / 2);
	}

	private static String axisValue(double value) {
		if (Math.abs(value) < 1.0e-3) return "0";
		if (Math.abs(value) < 1.0) return String.format("%.1f", value);
		return Long.toString(Math.round(value));
	}

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint,
			Point2D.Double worldPoint, List<String> feedback) {
		addXYFeedback(worldPoint, "cm", feedback);
	}
}
