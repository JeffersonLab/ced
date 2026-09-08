package edu.cnu.ced.view.swim;

import java.awt.BorderLayout;
import java.awt.Color;
import java.beans.PropertyVetoException;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import edu.cnu.ced.swim.FieldIntegral.Sample;
import edu.cnu.mdi.graphics.style.IStyled;
import edu.cnu.mdi.graphics.style.SymbolType;
import edu.cnu.mdi.splot.fit.CurveDrawingMethod;
import edu.cnu.mdi.splot.pdata.ACurve;
import edu.cnu.mdi.splot.pdata.Curve;
import edu.cnu.mdi.splot.pdata.DataColumn;
import edu.cnu.mdi.splot.pdata.PlotData;
import edu.cnu.mdi.splot.pdata.PlotDataException;
import edu.cnu.mdi.splot.pdata.PlotDataType;
import edu.cnu.mdi.splot.plot.PlotCanvas;
import edu.cnu.mdi.splot.plot.PlotPanel;
import edu.cnu.mdi.ui.colors.X11Colors;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.BaseView;
import edu.cnu.mdi.view.ViewManager;
import edu.cnu.mdi.view.VirtualView;

/**
 * Shared plot of magnetic-field integrals along swum trajectories -- matches
 * legacy CED's own {@code TrajectoryIntegralPlotView}, the companion to
 * {@code SectorView}'s "B magnitude" field-heatmap display. A single shared
 * instance ({@link #getInstance()}), not one per view, matching legacy's own
 * single {@code Ced.getCed().getPlotView()} accessor -- there is only ever
 * one running CED application to plot trajectories from.
 * <p>
 * Created on first use, not pre-registered as a lazy/eager
 * {@code ViewConfiguration} -- the same ad hoc, "cache and reuse one
 * instance" convention {@link edu.cnu.ced.view.currentevent.BankView}
 * already follows, since this window is meant to persist for the
 * application's lifetime once first shown, not be disposed on close.
 * </p>
 */
@SuppressWarnings("serial")
public final class TrajectoryIntegralPlotView extends BaseView {

	private static final Color[] CURVE_COLORS = {
			X11Colors.getX11Color("Dark Red"), X11Colors.getX11Color("Dark Blue"),
			X11Colors.getX11Color("Dark Green"), Color.BLACK, Color.GRAY,
			X11Colors.getX11Color("wheat")
	};

	private static TrajectoryIntegralPlotView instance;

	private final PlotData plotData;
	private final PlotCanvas plotCanvas;

	/** @return the single shared instance, creating it on first call */
	public static synchronized TrajectoryIntegralPlotView getInstance() {
		if (instance == null) {
			instance = new TrajectoryIntegralPlotView();
		}
		return instance;
	}

	private TrajectoryIntegralPlotView() {
		super(PropertyUtils.TITLE, "Magnetic Field Integral",
				PropertyUtils.WIDTH, 700, PropertyUtils.HEIGHT, 700,
				PropertyUtils.USECONTAINER, false, PropertyUtils.VISIBLE, false);
		plotData = createPlotData();
		plotCanvas = new PlotCanvas(plotData, "Magnetic Field Integral", "Path Length (cm)",
				"∫|B × dL| (kG·cm)");
		add(new PlotPanel(plotCanvas), BorderLayout.CENTER);
		installMenu();
	}

	private static PlotData createPlotData() {
		try {
			PlotData data = new PlotData(PlotDataType.XYXY, new String[] { "Trajectory" }, null);
			data.getFirstCurve().setVisible(false);
			return data;
		} catch (PlotDataException exception) {
			throw new IllegalStateException("Could not create the trajectory-integral plot", exception);
		}
	}

	private void installMenu() {
		JMenuItem clear = new JMenuItem("Clear");
		clear.addActionListener(event -> clear());
		JMenu plotMenu = new JMenu("Plot");
		plotMenu.add(clear);
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(plotMenu);
		setJMenuBar(menuBar);
	}

	/**
	 * Adds one trajectory's field-integral curve and brings this view to the
	 * front, creating it if this is the first curve shown this session.
	 *
	 * @param name    curve label, shown in the plot legend
	 * @param samples the trajectory's own field-integral samples (see
	 *                {@link edu.cnu.ced.swim.FieldIntegral#compute})
	 */
	public void addTrajectory(String name, List<Sample> samples) {
		if (samples.isEmpty()) return;
		Curve curve = findAvailableCurve(name);
		double[] path = new double[samples.size()];
		double[] integral = new double[samples.size()];
		for (int i = 0; i < samples.size(); i++) {
			path[i] = samples.get(i).pathLengthCm();
			integral[i] = samples.get(i).cumulativeIntegralKgCm();
		}
		curve.addAll(path, integral);
		plotCanvas.setWorldSystem();
		plotCanvas.repaint();
		showAndActivate();
	}

	private Curve findAvailableCurve(String name) {
		for (int i = 0; i < plotData.size(); i++) {
			Curve curve = (Curve) plotData.getCurve(i);
			if (curve.length() == 0) {
				curve.setName(name);
				curve.setVisible(true);
				configureCurve(curve, i);
				return curve;
			}
		}
		try {
			Curve curve = new Curve(name, new DataColumn(), new DataColumn(), null);
			configureCurve(curve, plotData.size());
			plotData.addCurve(curve);
			return curve;
		} catch (PlotDataException exception) {
			throw new IllegalStateException("Could not add a trajectory-integral curve", exception);
		}
	}

	private static void configureCurve(Curve curve, int index) {
		curve.setCurveDrawingMethod(CurveDrawingMethod.CUBICSPLINE);
		IStyled style = curve.getStyle();
		Color color = CURVE_COLORS[index % CURVE_COLORS.length];
		style.setLineColor(color);
		style.setBorderColor(color);
		style.setFillColor(color);
		style.setSymbolType(SymbolType.X);
		style.setSymbolSize(6);
	}

	private void clear() {
		for (int i = 0; i < plotData.size(); i++) {
			ACurve curve = plotData.getCurve(i);
			curve.clearData();
			curve.setVisible(false);
		}
		plotCanvas.setWorldSystem();
		plotCanvas.repaint();
	}

	/** Show this view and bring it into the virtual desktop's visible column -- same pattern as {@code BankView.showAndActivate}. */
	private void showAndActivate() {
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
}
