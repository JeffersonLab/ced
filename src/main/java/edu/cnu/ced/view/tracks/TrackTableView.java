package edu.cnu.ced.view.tracks;

import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import edu.cnu.ced.data.TrackRow;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.EventSnapshot;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.BaseView;

/**
 * An MDI-internal view showing one of the two small trajectory tables --
 * Monte Carlo Tracks or Reconstructed Tracks -- matching legacy CED's shared
 * {@code ClasIoTrajectoryInfoView} base. Legacy uses inheritance (two thin
 * subclasses); this app's two views differ only in title and row source, so
 * one class parameterized by both, instantiated twice, does the same job
 * without the extra hierarchy.
 */
@SuppressWarnings("serial")
public final class TrackTableView extends BaseView {

	private final EventNavigator navigator;
	private final Function<EventSnapshot, List<TrackRow>> extractor;
	private final TrackTable table = new TrackTable();
	private final Consumer<EventNavigationState> stateListener = this::acceptState;

	public TrackTableView(EventNavigator navigator, String title,
			Function<EventSnapshot, List<TrackRow>> extractor) {
		super(PropertyUtils.TITLE, title,
				PropertyUtils.WIDTH, 1050,
				PropertyUtils.HEIGHT, 350,
				PropertyUtils.USECONTAINER, false);
		this.navigator = navigator;
		this.extractor = extractor;
		add(new JScrollPane(table), BorderLayout.CENTER);
		navigator.addListener(stateListener);
		acceptState(navigator.state());
	}

	private void acceptState(EventNavigationState state) {
		if (SwingUtilities.isEventDispatchThread()) {
			applyState(state);
		} else {
			SwingUtilities.invokeLater(() -> applyState(state));
		}
	}

	private void applyState(EventNavigationState state) {
		table.trackModel().setRows(extractor.apply(state.snapshot()));
	}

	@Override
	public void dispose() {
		navigator.removeListener(stateListener);
		super.dispose();
	}
}
