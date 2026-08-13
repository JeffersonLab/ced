package edu.cnu.ced.view;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.List;

import edu.cnu.ced.event.EventNavigator;
import edu.cnu.mdi.container.IContainer;

/** Common world-coordinate behavior for CED laboratory XY views. */
@SuppressWarnings("serial")
public abstract class CedXYView extends CedView {

	protected CedXYView(EventNavigator navigator, Object... properties) {
		super(navigator, properties);
	}

	protected final void addXYFeedback(Point2D.Double worldPoint, String units,
			List<String> feedback) {
		if (worldPoint == null) return;
		feedback.add(String.format("$yellow$(x, y) = (%6.2f, %6.2f) %s",
				worldPoint.x, worldPoint.y, units));
	}

	@Override
	public void getFeedbackStrings(IContainer container, Point screenPoint,
			Point2D.Double worldPoint, List<String> feedback) {
		addXYFeedback(worldPoint, "cm", feedback);
	}
}
