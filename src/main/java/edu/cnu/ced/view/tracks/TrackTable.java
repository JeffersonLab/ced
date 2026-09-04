package edu.cnu.ced.view.tracks;

import edu.cnu.ced.component.ZebraTable;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * The zebra-striped, bordered-cell table used by both trajectory views (see
 * {@link ZebraTable}), sized to {@link TrackTableModel}'s columns.
 */
@SuppressWarnings("serial")
public final class TrackTable extends ZebraTable {

	public TrackTable() {
		super(new TrackTableModel());
		setFont(Fonts.tweenFont);
		for (int column = 0; column < TrackTableModel.COLUMN_WIDTHS.length; column++) {
			getColumnModel().getColumn(column).setPreferredWidth(TrackTableModel.COLUMN_WIDTHS[column]);
		}
	}

	/** @return this table's model, narrowed from {@link #getModel()} */
	public TrackTableModel trackModel() {
		return (TrackTableModel) getModel();
	}
}
