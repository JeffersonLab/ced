package edu.cnu.ced.view3d;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.mdi.mdi3D.view3D.PlainView3D;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Common event-aware foundation for every CED 3D detector view.
 *
 * <p>
 * Mirrors legacy CED's {@code cnuphys.ced.ced3d.view.CedView3D}: subscribes
 * to event delivery and drives a "Next event" control, exactly like the 2D
 * {@code edu.cnu.ced.view.CedView} base every 2D CED view already uses --
 * bridging the same {@link EventNavigator}/{@link EventNavigationState}
 * pair rather than legacy's {@code ClasIoEventManager}.
 * </p>
 *
 * <p>
 * 3D MDI views (see {@link PlainView3D}) deliberately have no 2D-style
 * toolbar, so unlike {@code CedView} this puts the "Next" control on the
 * menu bar (alongside the standard view-info button every {@code
 * PlainView3D} already installs there) rather than a toolbar.
 * </p>
 */
@SuppressWarnings("serial")
public abstract class CedView3D extends PlainView3D {

	private final EventNavigator navigator;
	private final Consumer<EventNavigationState> eventListener = this::acceptEventState;
	private boolean listening;

	protected CedView3D(EventNavigator navigator, Object... keyVals) {
		super(prepareKeyVals(keyVals));
		this.navigator = navigator;
		installNextButton();
		navigator.addListener(eventListener);
		listening = true;
		acceptEventState(navigator.state());
	}

	/** Called on the Swing event-dispatch thread when a complete event is published. */
	protected abstract void eventChanged(EventNavigationState state);

	/**
	 * Prepares a subclass's key/value pairs before they reach {@link
	 * PlainView3D}'s constructor -- the last point any code of ours runs
	 * before that constructor's own {@code make3DPanel(...)} call
	 * eventually constructs a {@code Panel3D} on the EDT.
	 *
	 * <p>
	 * First waits for {@link GLWarmup#awaitReady(long)}: without this, the
	 * very first {@code Panel3D} ever constructed triggers JOGL's first GL
	 * context creation synchronously on the EDT, which on macOS can
	 * deadlock against the AppKit main thread (see {@link GLWarmup}'s own
	 * javadoc). {@link GLWarmup#start()} is called once, early in {@code
	 * main()}, well before this could ever run; five seconds is generous
	 * headroom for a warm-up that normally finishes in well under one.
	 * </p>
	 *
	 * <p>
	 * Then appends {@code USECONTAINER=false}: {@link PlainView3D#
	 * resolveContainer} always returns {@code null} (3D views have no 2D
	 * {@code IContainer}), but {@code BaseView}'s own constructor only
	 * skips using that {@code null} when told not to use a container at
	 * all; its "use container" flag otherwise defaults {@code true} and
	 * unconditionally dereferences the result, crashing every 3D CED view
	 * that forgets to pass this. Appended last so it always wins even if a
	 * subclass's own {@code keyVals} happens to set it too.
	 * </p>
	 */
	private static Object[] prepareKeyVals(Object[] keyVals) {
		GLWarmup.awaitReady(5000);
		Object[] combined = Arrays.copyOf(keyVals, keyVals.length + 2);
		combined[keyVals.length] = PropertyUtils.USECONTAINER;
		combined[keyVals.length + 1] = false;
		return combined;
	}

	/** This view's {@link CedPanel3D}, once constructed. */
	protected final CedPanel3D cedPanel3D() {
		return (CedPanel3D) _panel3D;
	}

	private void acceptEventState(EventNavigationState state) {
		Runnable update = () -> {
			eventChanged(state);
			cedPanel3D().getPidLegend().update(RecEventData.from(state.snapshot()).particles());
			refresh();
		};
		if (SwingUtilities.isEventDispatchThread()) {
			update.run();
		} else {
			SwingUtilities.invokeLater(update);
		}
	}

	/**
	 * Adds a "Next" event button as the leftmost item on the menu bar -- the
	 * one chrome region every {@link PlainView3D} builds regardless of
	 * subclass, since 3D views have no 2D-style toolbar to put it on (see
	 * class javadoc). Called from this constructor, after {@code
	 * super(keyVals)} has finished building the menu bar (including the
	 * image menu and the standard view-info button), so the button always
	 * ends up leftmost rather than appended after those.
	 */
	private void installNextButton() {
		JMenuBar menuBar = getJMenuBar();
		if (menuBar == null) {
			return;
		}
		JButton next = new JButton("Next");
		next.setToolTipText("Next event");
		next.addActionListener((ActionEvent e) -> navigator.next());
		menuBar.add(next, 0);
	}

	@Override
	public void dispose() {
		if (listening) {
			navigator.removeListener(eventListener);
		}
		super.dispose();
	}
}
