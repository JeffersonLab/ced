package edu.cnu.ced.view.currentevent;

import java.awt.BorderLayout;
import java.beans.PropertyVetoException;

import org.jlab.io.base.DataBank;

import edu.cnu.ced.event.EventNavigator;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.BaseView;
import edu.cnu.mdi.view.ViewManager;
import edu.cnu.mdi.view.VirtualView;

/**
 * An MDI-internal "can't get lost" view holding one bank's
 * {@link BankViewerPanel} -- matching legacy CED's CedDataView. Unlike
 * {@link BankFloatingWindow}, this is meant to live for the application's
 * lifetime once created (the same hide-on-close convention every other CED
 * view already follows), so its owner should cache and reuse one instance
 * per bank name rather than disposing it on close.
 */
@SuppressWarnings("serial")
public final class BankView extends BaseView {

	private final BankViewerPanel panel;

	public BankView(EventNavigator navigator, String bankName, DataBank initialBank,
			BankColumnVisibility visibility) {
		super(PropertyUtils.TITLE, bankName,
				PropertyUtils.WIDTH, 700,
				PropertyUtils.HEIGHT, 500,
				PropertyUtils.USECONTAINER, false);
		panel = new BankViewerPanel(navigator, bankName, initialBank, visibility);
		add(panel, BorderLayout.CENTER);
	}

	/** Show this view and bring it into the virtual desktop's visible column. */
	public void showAndActivate() {
		setVisible(true);
		VirtualView virtualView = VirtualView.getInstance();
		if (virtualView != null) {
			virtualView.moveTo(this, virtualView.getCurrentColumn(), VirtualView.CENTER);
		}
		ViewManager.getInstance().makeViewVisibleInVirtualWorld(this);
		try {
			setSelected(true);
		} catch (PropertyVetoException ignored) {
			// another view may veto losing selection focus; not worth failing over
		}
		toFront();
	}

	@Override
	public void dispose() {
		panel.dispose();
		super.dispose();
	}
}
