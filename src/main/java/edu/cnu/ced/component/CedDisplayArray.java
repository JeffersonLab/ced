package edu.cnu.ced.component;

import java.awt.Color;
import java.util.EnumSet;

import edu.cnu.mdi.component.checkboxarray.CheckBoxArray;

/** Standard CED visibility selector built on MDI's checkbox array. */
@SuppressWarnings("serial")
public final class CedDisplayArray extends CheckBoxArray {

	private final Runnable changeListener;

	public CedDisplayArray(EnumSet<CedDisplayOption> options, int columns,
			int horizontalGap, int verticalGap, Runnable changeListener) {
		super(columns, horizontalGap, verticalGap);
		this.changeListener = changeListener == null ? () -> { } : changeListener;
		for (CedDisplayOption option : CedDisplayOption.values()) {
			if (options.contains(option)) {
				add(option.label(), option.initiallySelected(), true, option.group(),
					event -> this.changeListener.run(), Color.DARK_GRAY);
			}
		}
	}

	public boolean isSelected(CedDisplayOption option) {
		return super.isSelected(option.label());
	}

	/** Select an option without synthesizing a user action event. */
	public void setSelected(CedDisplayOption option, boolean selected) {
		super.setSelected(option.label(), selected);
	}
}
