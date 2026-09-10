package edu.cnu.ced.view3d;

import java.awt.BorderLayout;
import java.util.EnumSet;

import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.cnu.ced.component.CedDisplayArray;
import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.component.PidLegend;
import edu.cnu.mdi.mdi3D.adapter3D.AlphaSlider;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;

/**
 * Common scene-and-controls base for every CED 3D detector panel.
 *
 * <p>
 * Mirrors legacy CED's {@code cnuphys.ced.ced3d.CedPanel3D}/{@code
 * PlainPanel3D} pair (a "Volume alpha" slider plus a PID legend across the
 * top, and a checkbox array of display toggles on the east side), but built
 * on {@link CedDisplayOption} and {@link CedDisplayArray} -- the same typed
 * toggle vocabulary and component already used by every 2D CED view --
 * rather than legacy's monolithic {@code SHOW_*} string-constant scheme.
 * </p>
 *
 * <p>
 * Subclasses supply the set of {@link CedDisplayOption}s relevant to their
 * detector (e.g. just {@code VOLUMES}/{@code TRUTH} for FTCal) and override
 * {@link Panel3D#createInitialItems()} to populate the scene, exactly like
 * any other {@link Panel3D}.
 * </p>
 */
@SuppressWarnings("serial")
public abstract class CedPanel3D extends Panel3D {

	// addNorth()/addEast() are invoked by the Panel3D superclass constructor,
	// before any of this class's own field initializers have run. These two
	// fields are therefore deliberately declared WITHOUT initializers and are
	// assigned only inside addNorth()/addEast() themselves -- an assignment
	// made that early still sticks, since nothing later overwrites it.
	// Everything that actually needs this constructor's own arguments (the
	// alpha slider, the PID legend, the display-option checkbox array) is
	// built afterwards, in the constructor body below, and dropped into
	// these already-placed placeholders.
	private JPanel northPanel;
	private JPanel eastPanel;

	private AlphaSlider volumeAlphaSlider;
	private PidLegend pidLegend;
	private CedDisplayArray displayArray;

	/**
	 * @param options the display toggles this detector's east panel should
	 *                offer, e.g. {@code EnumSet.of(CedDisplayOption.VOLUMES,
	 *                CedDisplayOption.TRUTH)}
	 */
	protected CedPanel3D(EnumSet<CedDisplayOption> options, float angleX, float angleY, float angleZ,
			float xDist, float yDist, float zDist) {
		super(angleX, angleY, angleZ, xDist, yDist, zDist);

		volumeAlphaSlider = new AlphaSlider(this, "Volume alpha", this::applyVolumeAlphaToItems);
		pidLegend = new PidLegend();
		northPanel.add(pidLegend, BorderLayout.CENTER);
		northPanel.add(volumeAlphaSlider, BorderLayout.EAST);

		displayArray = new CedDisplayArray(options, 3, 4, 4, this::refresh);
		eastPanel.add(displayArray, BorderLayout.CENTER);
	}

	@Override
	protected final JComponent addNorth() {
		northPanel = new JPanel(new BorderLayout(20, 0));
		return northPanel;
	}

	@Override
	protected final JComponent addEast() {
		eastPanel = new JPanel(new BorderLayout());
		return eastPanel;
	}

	@Override
	protected JComponent addWest() {
		return null;
	}

	@Override
	protected JComponent addSouth() {
		return null;
	}

	/** Whether the given display toggle is currently checked. */
	public final boolean isDisplayed(CedDisplayOption option) {
		return displayArray.isSelected(option);
	}

	/** Current "Volume alpha" slider value, 0-255. */
	public final int getVolumeAlpha() {
		return volumeAlphaSlider.getAlpha();
	}

	/**
	 * Pushes a new volume-alpha value to every {@link DetectorItem3D}
	 * currently in the scene, via {@code setFillAlpha(int)}.
	 *
	 * <p>
	 * This exists because {@link Panel3D#display} decides which render
	 * pass (opaque, blending disabled, or transparent, blending enabled)
	 * an item belongs to <em>before</em> that item is asked to draw itself
	 * for the frame -- from the item's own {@code getFillAlpha()}, not
	 * from any color an item's {@code drawShape()} happens to construct.
	 * An item that never calls {@code setFillAlpha} stays classified
	 * opaque forever, so any alpha baked into its draw color is simply
	 * ignored by the (disabled) blend state: it renders fully opaque no
	 * matter what {@link #getVolumeAlpha()} says. Called from the alpha
	 * slider's own change callback, before the {@code refresh()} that
	 * follows it, so the very next frame's classification is already
	 * correct -- not one frame behind. {@link DetectorItem3D}'s own
	 * constructor seeds this same value for items created after this
	 * point (e.g. during {@code createInitialItems()}).
	 * </p>
	 */
	private void applyVolumeAlphaToItems(int alpha) {
		for (Item3D item : _itemList) {
			if (item instanceof DetectorItem3D detectorItem) {
				detectorItem.setFillAlpha(alpha);
			}
		}
	}

	/** The shared PID legend shown in this panel's north strip. */
	public final PidLegend getPidLegend() {
		return pidLegend;
	}
}
