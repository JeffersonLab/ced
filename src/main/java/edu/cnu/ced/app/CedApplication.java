package edu.cnu.ced.app;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import cnuphys.magfield.MagneticFields;

import edu.cnu.ced.CedVersion;
import edu.cnu.ced.data.MonteCarloTracks;
import edu.cnu.ced.data.ReconstructedTracks;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.AccumulationService;
import edu.cnu.ced.event.EventSource;
import edu.cnu.ced.event.EventStore;
import edu.cnu.ced.event.HipoEventSource;
import edu.cnu.ced.event.RunConfig;
import edu.cnu.ced.event.EventFilters;
import edu.cnu.ced.event.RunTrigger;
import edu.cnu.ced.component.TriggerBitsPanel;
import edu.cnu.ced.dialog.AccumulationDialog;
import edu.cnu.ced.dialog.FilterDialog;
import edu.cnu.ced.geometry.GeometryService;
import edu.cnu.ced.magfield.MagneticFieldService;
import edu.cnu.ced.magfield.RunFieldScaleApplier;
import edu.cnu.ced.resources.Clas12Resources;
import edu.cnu.ced.swim.SwimTrajectoryCache;
import edu.cnu.ced.view.CurrentEventView;
import edu.cnu.ced.view.currentevent.BankViewerDisplayMode;
import edu.cnu.ced.view.alert.AlertXYView;
import edu.cnu.ced.view.fmt.FMTXYView;
import edu.cnu.ced.view.ftcal.FTCalXYView;
import edu.cnu.ced.view.urwt.URWTXYView;
import edu.cnu.ced.view.ftof.FTOFView;
import edu.cnu.ced.view.pcal.PCalView;
import edu.cnu.ced.view.ecal.ECalView;
import edu.cnu.ced.view.central.CentralXYView;
import edu.cnu.ced.view.central.CentralZView;
import edu.cnu.ced.view.dc.AllDCView;
import edu.cnu.ced.view.dc.DCHexView;
import edu.cnu.ced.view.sector.SectorView;
import edu.cnu.ced.view.sector.SectorView.Pair;
import edu.cnu.ced.view.tracks.TrackTableView;
import edu.cnu.mdi.app.BaseMDIApplication;
import edu.cnu.mdi.app.StartupInfo;
import edu.cnu.mdi.app.StartupWindow;
import edu.cnu.mdi.dialog.FileDialogs;
import edu.cnu.mdi.dialog.FileType;
import edu.cnu.mdi.io.RecentFiles;
import edu.cnu.mdi.io.RecentFilesMenu;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.ui.menu.MenuManager;
import edu.cnu.mdi.ui.menu.MenuContribution;
import edu.cnu.mdi.ui.menu.MenuId;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.JsonView;
import edu.cnu.mdi.view.LogView;
import edu.cnu.mdi.view.ViewConfiguration;
import edu.cnu.mdi.view.ViewManager;
import edu.cnu.mdi.view.VirtualView;

/** Initial MDI application shell for CED 2.0. */
@SuppressWarnings("serial")
public final class CedApplication extends BaseMDIApplication {

	/** Stable persistence key for this application. */
	public static final String APPLICATION_ID = "mdi-ced";

	/** Normal CED layout: 12 detector/event columns plus 6 3D columns. */
	public static final int VIRTUAL_DESKTOP_COLUMNS = 18;

	/** Tiled background retained from the existing CED application. */
	public static final String BACKGROUND_RESOURCE = "images/cnu.png";

	private static CedApplication instance;
	private static CedLaunchOptions launchOptions = CedLaunchOptions.parse(null);
	private static CedBootstrapResult bootstrap;
	// Set by main() once bootstrap succeeds, left open rather than closed
	// there; installStartupMarker's readiness callback closes it (and clears
	// this back to null) once the main frame's first paint has actually
	// settled. See main()'s own comment for why.
	private static StartupWindow startupWindow;
	private static final FileType HIPO_FILES = FileType.of("HIPO event files (*.hipo)", "hipo");
	private static final MenuId OPTIONS_MENU_ID = new MenuId("ced.options");
	private static final MenuId EVENTS_MENU_ID = new MenuId("ced.events");
	private static final MenuId FIELD_MENU_ID = new MenuId("ced.field");

	private EventNavigator eventNavigator;
	private EventStore eventStore;
	private MagneticFieldService magneticFieldService;
	private GeometryService geometryService;
	private LogView logView;
	private CurrentEventView currentEventView;
	private Clas12Resources clas12Resources;
	private AccumulationService accumulationService;
	private SwimTrajectoryCache swimCache;
	private RunFieldScaleApplier runFieldScaleApplier;
	private RecentFiles recentEventFiles;
	private RecentFilesMenu recentEventMenuHelper;
	private JMenu recentEventMenu;
	private FilterDialog filterDialog;

	private CedApplication() {
		super(PropertyUtils.TITLE, CedVersion.title(),
				PropertyUtils.BACKGROUNDIMAGE, BACKGROUND_RESOURCE,
				PropertyUtils.FRACTION, 0.9,
				PropertyUtils.CONSOLELOG, true);
		Log.getInstance().config("Startup timing - super() done (BaseMDIApplication ctor incl. "
				+ "addInitialViews), JVM uptime: " + jvmUptimeMillis() + " ms");
		timeStep("addCedFileActions", this::addCedFileActions);
		timeStep("addCedEventActions", this::addCedEventActions);
		timeStep("addCedFieldMenu", this::addCedFieldMenu);
		timeStep("addCedOptions", this::addCedOptions);
		timeStep("addTriggerPanel", this::addTriggerPanel);
	}

	// The main menu bar's trigger-bit status row, matching legacy CED's own
	// main-window trigger display (see TriggerBitsPanel). A fixed strut keeps
	// it visibly separated from the last pull-down menu even in a narrow
	// window; the glue after it parks it toward the right, same as legacy
	// mounts its TriggerMenuPanel directly on the JMenuBar (strut then glue
	// then panel) rather than on a separate toolbar.
	private void addTriggerPanel() {
		TriggerBitsPanel triggerBitsPanel = new TriggerBitsPanel();
		getJMenuBar().add(Box.createHorizontalStrut(20));
		getJMenuBar().add(Box.createHorizontalGlue());
		getJMenuBar().add(triggerBitsPanel);
		eventNavigator.addListener(state -> {
			Runnable apply = () -> triggerBitsPanel.setTrigger(RunTrigger.from(state.snapshot()).orElse(null));
			if (SwingUtilities.isEventDispatchThread()) {
				apply.run();
			} else {
				SwingUtilities.invokeLater(apply);
			}
		});
	}

	private void addCedOptions() {
		JMenu options = new JMenu("Options");

		BankViewerDisplayMode bankViewerDisplayMode = new BankViewerDisplayMode();
		JCheckBoxMenuItem floatingBankViews = new JCheckBoxMenuItem("Bank Views are Free Floating",
				bankViewerDisplayMode.isFloating());
		floatingBankViews.addActionListener(
				event -> bankViewerDisplayMode.setFloating(floatingBankViews.isSelected()));
		options.add(floatingBankViews);
		options.addSeparator();

		JMenuItem deleteCache = new JMenuItem("Delete Geometry Cache…");
		deleteCache.addActionListener(event -> deleteGeometryCache());
		options.add(deleteCache);
		MenuManager.getInstance().addContribution(new MenuContribution(OPTIONS_MENU_ID, options, 200));
	}

	private void deleteGeometryCache() {
		int answer = JOptionPane.showConfirmDialog(this,
				"Delete the persistent geometry cache?\n"
				+ "CED will reload geometry from its sources the next time it starts.",
				"Delete Geometry Cache", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (answer != JOptionPane.OK_OPTION) {
			return;
		}
		try {
			boolean deleted = GeometryService.deletePersistentCache();
			String message = deleted
					? "Geometry cache deleted. It will be rebuilt on the next launch."
					: "No geometry cache was present.";
			Log.getInstance().info(message);
			JOptionPane.showMessageDialog(this, message, "Geometry Cache",
					JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException exception) {
			String message = "Could not delete the geometry cache: " + exception.getMessage();
			Log.getInstance().error(message);
			JOptionPane.showMessageDialog(this, message, "Geometry Cache",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/** @return the singleton CED application */
	public static CedApplication getInstance() {
		if (instance == null) {
			instance = new CedApplication();
		}
		return instance;
	}

	/** @return immutable options selected for this application launch */
	public static CedLaunchOptions getLaunchOptions() {
		return launchOptions;
	}

	/** @return validated CLAS12 resources, or {@code null} when discovery failed */
	public Clas12Resources getClas12Resources() {
		return clas12Resources;
	}

	@Override
	protected String getApplicationId() {
		return APPLICATION_ID;
	}

	@Override
	protected int getVirtualDesktopColumns() {
		return VIRTUAL_DESKTOP_COLUMNS;
	}

	@Override
	protected void addInitialViews() {
		// BaseMDIApplication invokes this callback from its constructor, before
		// subclass field initializers run. Construct application services here.
		long startupStarted = System.nanoTime();
		installStartupMarker();
		installStartupInputBlock();
		eventStore = new EventStore();
		eventNavigator = new EventNavigator(eventStore);
		eventNavigator.setFilter(EventFilters.sharedFor(eventNavigator));
		accumulationService = new AccumulationService();
		eventNavigator.addSourceListener(accumulationService::clear);
		swimCache = new SwimTrajectoryCache();
		runFieldScaleApplier = new RunFieldScaleApplier();
		eventNavigator.addListener(state -> runFieldScaleApplier.apply(
				RunConfig.from(state.snapshot()).orElse(null)));
		if (bootstrap != null) {
			magneticFieldService = bootstrap.magneticFields();
			geometryService = bootstrap.geometry();
			clas12Resources = bootstrap.resources();
		} else {
			magneticFieldService = new MagneticFieldService();
			geometryService = new GeometryService();
		}
		timeStep("LogView", () -> logView = new LogView());
		timeStep("JSON Viewer (lazy registration)", () -> ViewManager.getInstance().addConfiguration(
				// JsonView's no-arg constructor deliberately starts hidden (its own
				// javadoc: "initially hidden") -- fine for direct construction where
				// callers push content and show it themselves, but wrong for a lazy
				// ViewConfiguration, whose menu item only realizes the view once and
				// otherwise just toggles visibility on an already-realized one. Using
				// the (width, height, visible) constructor with visible=true matches
				// what JsonView's own no-arg constructor passes for width/height.
				ViewConfiguration.lazy("JSON Viewer", () -> new JsonView(900, 600, true),
						17, 0, 0, VirtualView.BOTTOMRIGHT)));
		timeStep("CurrentEventView", () -> currentEventView = new CurrentEventView(eventNavigator));
		timeStep("FTCal XY (lazy registration)", () -> ViewManager.getInstance().addConfiguration(
				ViewConfiguration.lazy("FTCal XY", () -> new FTCalXYView(geometryService.ftcal(),
						eventNavigator, accumulationService.ftcal()),
						8, 0, 0, VirtualView.CENTER)));
		timeStep("FMT XY (lazy registration)", () -> ViewManager.getInstance().addConfiguration(
				ViewConfiguration.lazy("FMT XY", () -> new FMTXYView(geometryService.fmt(),
						eventNavigator, accumulationService.fmt()),
						8, 0, 0, VirtualView.CENTER)));
		timeStep("URWT XY (lazy registration)", () -> ViewManager.getInstance().addConfiguration(
				ViewConfiguration.lazy("URWT XY", () -> new URWTXYView(geometryService.urwt(),
						eventNavigator, accumulationService.urwt(), swimCache),
						8, 0, 0, VirtualView.CENTER)));
		timeStep("ALERT XY (lazy registration)", () -> ViewManager.getInstance().addConfiguration(
				ViewConfiguration.lazy("ALERT XY", () -> new AlertXYView(geometryService.alert(),
						eventNavigator, accumulationService.alert(), swimCache),
						8, 0, 0, VirtualView.CENTER)));
		timeStep("PCAL", () -> ViewManager.getInstance().addConfiguration(ViewConfiguration.eager(
				"PCAL", () -> new PCalView(geometryService.pcal(), eventNavigator,
						accumulationService.pcal()),
				4, 0, 0, VirtualView.CENTERRIGHT)));
		timeStep("ECAL", () -> ViewManager.getInstance().addConfiguration(ViewConfiguration.eager(
				"ECAL", () -> new ECalView(geometryService.ec(), eventNavigator,
						accumulationService.ecal()),
				4, 0, 0, VirtualView.CENTERLEFT)));
		timeStep("FTOF (lazy registration)", () -> ViewManager.getInstance().addConfiguration(
				ViewConfiguration.lazy("FTOF", () -> new FTOFView(geometryService.ftof(),
						eventNavigator, accumulationService.ftof()),
						6, 0, 0, VirtualView.CENTER)));
		timeStep("Central XY", () -> ViewManager.getInstance().addConfiguration(ViewConfiguration.eager(
				"Central XY", () -> new CentralXYView(geometryService.bst(), geometryService.bmt(),
						geometryService.cnd(), geometryService.ctof(), eventNavigator,
						accumulationService.central(), swimCache),
				7, 0, 0, VirtualView.CENTER)));
		timeStep("Central Z", () -> ViewManager.getInstance().addConfiguration(ViewConfiguration.eager(
				"Central Z", () -> new CentralZView(geometryService.bst(), geometryService.bmt(),
						eventNavigator, accumulationService.central(), swimCache),
				8, 0, 0, VirtualView.CENTER)));
		timeStep("All Drift Chambers", () -> ViewManager.getInstance().addConfiguration(
				ViewConfiguration.eager("All Drift Chambers", () -> new AllDCView(geometryService.dc(),
						eventNavigator, accumulationService.dc()),
						3, 0, 0, VirtualView.CENTER)));
		timeStep("DC Hex (lazy registration)", () -> ViewManager.getInstance().addConfiguration(
				ViewConfiguration.lazy("DC Hex", () -> new DCHexView(eventNavigator,
						accumulationService.dc()),
						6, 0, 0, VirtualView.CENTER)));
		timeStep("Sectors 3 and 6", () -> ViewManager.getInstance().addConfiguration(ViewConfiguration.eager(
				"Sectors 3 and 6", () -> new SectorView(Pair.SECTORS_3_6,
						geometryService.dc(), geometryService.ftof(), geometryService.pcal(), geometryService.ec(),
						eventNavigator, accumulationService.dc(), accumulationService.pcal(), accumulationService.ecal(),
						accumulationService.htcc(), accumulationService.ltcc(), swimCache),
				0, 20, 65, VirtualView.UPPERLEFT)));
		timeStep("Sectors 2 and 5", () -> ViewManager.getInstance().addConfiguration(ViewConfiguration.eager(
				"Sectors 2 and 5", () -> new SectorView(Pair.SECTORS_2_5,
						geometryService.dc(), geometryService.ftof(), geometryService.pcal(), geometryService.ec(),
						eventNavigator, accumulationService.dc(), accumulationService.pcal(), accumulationService.ecal(),
						accumulationService.htcc(), accumulationService.ltcc(), swimCache),
				0, 85, 115, VirtualView.UPPERLEFT)));
		timeStep("Sectors 1 and 4", () -> ViewManager.getInstance().addConfiguration(ViewConfiguration.eager(
				"Sectors 1 and 4", () -> new SectorView(Pair.SECTORS_1_4,
						geometryService.dc(), geometryService.ftof(), geometryService.pcal(), geometryService.ec(),
						eventNavigator, accumulationService.dc(), accumulationService.pcal(), accumulationService.ecal(),
						accumulationService.htcc(), accumulationService.ltcc(), swimCache),
				0, 150, 165, VirtualView.UPPERLEFT)));
		timeStep("Monte Carlo Tracks", () -> ViewManager.getInstance().addConfiguration(
				ViewConfiguration.eager("Monte Carlo Tracks", () -> new TrackTableView(eventNavigator,
						"Monte Carlo Tracks", snapshot -> MonteCarloTracks.from(snapshot).tracks()),
						9, 0, 0, VirtualView.CENTERLEFT)));
		timeStep("Reconstructed Tracks", () -> ViewManager.getInstance().addConfiguration(
				ViewConfiguration.eager("Reconstructed Tracks", () -> new TrackTableView(eventNavigator,
						"Reconstructed Tracks", snapshot -> ReconstructedTracks.from(snapshot).tracks()),
						9, 0, 0, VirtualView.CENTERRIGHT)));
		Log.getInstance().config("addInitialViews total: " + elapsedMillis(startupStarted) + " ms");
		Log.getInstance().config("CED MDI application shell initialized with "
				+ VIRTUAL_DESKTOP_COLUMNS + " virtual desktop columns.");
		Log.getInstance().config("CED launch configuration: geometry variation="
				+ launchOptions.geometryVariation() + ", 3D=" + launchOptions.enable3D()
				+ ", experimental=" + launchOptions.experimental());
	}

	/** Run one startup step and log how long it took, to isolate what's slow at launch. */
	private static void timeStep(String label, Runnable step) {
		long started = System.nanoTime();
		step.run();
		Log.getInstance().config("Startup timing - " + label + ": " + elapsedMillis(started) + " ms");
	}

	private static long elapsedMillis(long startedNanos) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
	}

	private void addCedEventActions() {
		JMenu events = new JMenu("Events");
		JMenuItem accumulate = new JMenuItem("Accumulate Events…");
		accumulate.addActionListener(event -> accumulateEvents());
		events.add(accumulate);
		events.addSeparator();

		int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		JMenuItem nextItem = new JMenuItem("Next Event");
		nextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutMask));
		nextItem.addActionListener(event -> eventNavigator.next());
		events.add(nextItem);

		JMenuItem previousItem = new JMenuItem("Previous Event");
		previousItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, shortcutMask));
		previousItem.addActionListener(event -> eventNavigator.previous());
		events.add(previousItem);

		JTextField seqGotoField = new JTextField("1", 8);
		events.add(gotoEventPanel("Go to Sequential Event:", seqGotoField, eventNavigator::goToSequence));

		JTextField trueGotoField = new JTextField("1", 8);
		events.add(gotoEventPanel("Go to True Event:", trueGotoField, eventNavigator::goToTrueEventNumber));

		float[] autoPeriodSeconds = { 2.0f };
		JCheckBox autoNextCheckBox = new JCheckBox("Auto Next-Event Every");
		JTextField autoPeriodField = new JTextField(Float.toString(autoPeriodSeconds[0]), 4);
		Timer autoNextTimer = new Timer((int) (1000 * autoPeriodSeconds[0]), event -> eventNavigator.next());
		autoNextCheckBox.addActionListener(event -> {
			if (autoNextCheckBox.isSelected()) {
				autoNextTimer.setDelay((int) (1000 * autoPeriodSeconds[0]));
				autoNextTimer.restart();
			} else {
				autoNextTimer.stop();
			}
		});
		autoPeriodField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent event) {
				if (event.getKeyCode() != KeyEvent.VK_ENTER) {
					return;
				}
				MenuSelectionManager.defaultManager().clearSelectedPath();
				autoPeriodSeconds[0] = normalizedEventPeriod(autoPeriodField.getText(), autoPeriodSeconds[0]);
				autoPeriodField.setText(Float.toString(autoPeriodSeconds[0]));
				if (autoNextTimer.isRunning()) {
					autoNextTimer.setDelay((int) (1000 * autoPeriodSeconds[0]));
				}
			}
		});
		JPanel autoPanel = transparentFlowPanel();
		autoPanel.add(autoNextCheckBox);
		autoPanel.add(autoPeriodField);
		autoPanel.add(new JLabel("sec"));
		events.add(autoPanel);
		events.addSeparator();

		JLabel eventCountLabel = new JLabel("Event Count: —");
		events.add(eventCountLabel);
		JMenuItem filter = new JMenuItem("Filter…");
		filter.addActionListener(event -> showFilterDialog());
		events.add(filter);

		eventNavigator.addListener(state -> {
			Runnable apply = () -> {
				nextItem.setEnabled(state.canGoNext());
				previousItem.setEnabled(state.canGoPrevious());
				seqGotoField.setEnabled(state.isOpen());
				trueGotoField.setEnabled(state.isOpen());
				autoNextCheckBox.setEnabled(state.canGoNext());
				autoPeriodField.setEnabled(state.canGoNext());
				if (!state.canGoNext()) {
					autoNextCheckBox.setSelected(false);
					autoNextTimer.stop();
				}
				eventCountLabel.setText(state.isOpen() ? "Event Count: " + state.eventCount() : "Event Count: —");
			};
			if (SwingUtilities.isEventDispatchThread()) {
				apply.run();
			} else {
				SwingUtilities.invokeLater(apply);
			}
		});
		MenuManager.getInstance().addContribution(new MenuContribution(EVENTS_MENU_ID, events, 150));
	}

	/** One "label: [field]" row for the Events menu, Enter-triggered, mirroring the seq/true goto fields legacy CED shows inline in its own Events menu. */
	private static JPanel gotoEventPanel(String label, JTextField field, java.util.function.IntPredicate navigation) {
		JPanel panel = transparentFlowPanel();
		panel.add(new JLabel(label));
		panel.add(field);
		field.setEnabled(false);
		field.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent event) {
				if (event.getKeyCode() != KeyEvent.VK_ENTER) {
					return;
				}
				MenuSelectionManager.defaultManager().clearSelectedPath();
				try {
					if (!navigation.test(Integer.parseInt(field.getText().trim()))) {
						field.setText("");
					}
				} catch (NumberFormatException notANumber) {
					field.setText("");
				}
			}
		});
		return panel;
	}

	private static JPanel transparentFlowPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		panel.setOpaque(false);
		return panel;
	}

	/** Clamp a user-entered auto-next-event period to a sane range, falling back on a parse failure -- matches legacy CED's own bounds. */
	static float normalizedEventPeriod(String text, float fallback) {
		try {
			float period = Float.parseFloat(text);
			return Float.isFinite(period) ? Math.max(0.001f, Math.min(60f, period)) : fallback;
		} catch (NumberFormatException notANumber) {
			return fallback;
		}
	}

	/**
	 * Builds CED's "Field" menu directly from coatjava's own {@code
	 * MagneticFields.getMagneticFieldMenu()} -- field-type selection (Torus/
	 * Solenoid/Composite/No Field), scale/shift panels, and "Load a Different
	 * Torus/Solenoid..." are all already wired there (including keeping the
	 * scale/shift text fields themselves in sync when {@link
	 * edu.cnu.ced.magfield.RunFieldScaleApplier} changes a scale factor on a
	 * run change) -- reimplementing that by hand would just be duplicating
	 * coatjava's own menu with extra steps. The one thing removed is the
	 * Interpolate/Nearest Neighbor sampling choice: the user doesn't want
	 * that exposed here.
	 */
	private void addCedFieldMenu() {
		JMenu field = MagneticFields.getInstance().getMagneticFieldMenu();
		removeInterpolationOptions(field);
		MenuManager.getInstance().addContribution(new MenuContribution(FIELD_MENU_ID, field, 160));
	}

	static void removeInterpolationOptions(JMenu menu) {
		int interpolateIndex = indexOfMenuItem(menu, "Interpolate");
		int nearestNeighborIndex = indexOfMenuItem(menu, "Nearest Neighbor");
		if (interpolateIndex < 0 || nearestNeighborIndex != interpolateIndex + 1) {
			// coatjava's own menu layout isn't what this expects -- leave it
			// alone rather than risk removing the wrong components.
			return;
		}
		menu.remove(nearestNeighborIndex);
		menu.remove(interpolateIndex);
		// The separator coatjava placed just before "Interpolate" (between
		// the field-type radios and the interpolation group) would now sit
		// directly before the Scale/Shift panels anyway, so drop it too
		// rather than end up with a doubled-up gap.
		int precedingIndex = interpolateIndex - 1;
		if (precedingIndex >= 0 && menu.getMenuComponent(precedingIndex) instanceof JSeparator) {
			menu.remove(precedingIndex);
		}
	}

	static int indexOfMenuItem(JMenu menu, String text) {
		for (int index = 0; index < menu.getMenuComponentCount(); index++) {
			Component component = menu.getMenuComponent(index);
			if (component instanceof JMenuItem item && text.equals(item.getText())) {
				return index;
			}
		}
		return -1;
	}

	private void accumulateEvents() {
		if (!eventNavigator.state().isOpen() || !eventNavigator.state().canGoNext()) {
			JOptionPane.showMessageDialog(this, "There are no remaining events to accumulate.",
					"Accumulate Events", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		new AccumulationDialog(this, eventNavigator, accumulationService).setVisible(true);
	}

	private void showFilterDialog() {
		if (filterDialog == null) {
			EventFilters eventFilters = EventFilters.sharedFor(eventNavigator);
			filterDialog = new FilterDialog(this, eventFilters, eventFilters::notifyChanged);
		}
		filterDialog.setVisible(true);
		filterDialog.toFront();
	}

	private void addCedFileActions() {
		JMenuItem openHipo = new JMenuItem("Open HIPO Event File...");
		openHipo.addActionListener(event -> chooseHipoFile());
		recentEventFiles = new RecentFiles(Preferences.userNodeForPackage(CedApplication.class)
				.node("recent-event-files"), 12);
		recentEventMenu = new JMenu("Recent Event Files");
		recentEventMenuHelper = new RecentFilesMenu(recentEventFiles,
				file -> openHipoFile(file.toPath()), "event files");
		recentEventMenuHelper.rebuild(recentEventMenu);
		JMenu fileMenu = MenuManager.getInstance().getFileMenu();
		fileMenu.insert(openHipo, 0);
		fileMenu.insert(recentEventMenu, 1);
		fileMenu.insertSeparator(2);
	}

	private void chooseHipoFile() {
		FileDialogs.openFile(this, "ced-hipo-events", "Open HIPO Event File", HIPO_FILES)
				.ifPresent(this::openHipoFile);
	}

	private void openHipoFile(Path path) {
		Log.getInstance().info("Opening HIPO event file: " + path);
		new SwingWorker<EventSource, Void>() {
			@Override
			protected EventSource doInBackground() {
				return HipoEventSource.open(path);
			}

			@Override
			protected void done() {
				try {
					eventNavigator.open(get());
					File opened = path.toAbsolutePath().normalize().toFile();
					recentEventFiles.add(opened);
					recentEventMenuHelper.rebuild(recentEventMenu);
					currentEventView.setVisible(true);
					Log.getInstance().info("Opened " + path + " with "
							+ eventNavigator.state().eventCount() + " events.");
				} catch (Exception exception) {
					Throwable cause = exception.getCause() == null ? exception : exception.getCause();
					String message = "Could not open HIPO event file: " + cause.getMessage();
					Log.getInstance().error(message);
					JOptionPane.showMessageDialog(CedApplication.this, message,
							"Open HIPO File", JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	@Override
	protected void defaultViewLayout() {
		virtualViewMove(currentEventView, 1, VirtualView.CENTER);
		virtualViewMove(logView, 17, VirtualView.UPPERLEFT);
		// JSON Viewer is now lazy (see addInitialViews): its own ViewConfiguration
		// placement (column 17, BOTTOMRIGHT) applies automatically once it's first
		// opened from the Views menu, so there's nothing to position here.

		// Each eager view's own placement (ViewConfiguration.placeViewOnVirtualDesktop)
		// navigates the desktop to ITS column, deferred via invokeLater -- so
		// whichever eager view was registered last in addInitialViews silently
		// decides what column the app opens on. onVirtualDesktopReady (which calls
		// this method) fires after the frame is shown and Swing has stabilized,
		// reliably after every per-view invokeLater placement has already run, so
		// this always wins the race and puts the sector views back on screen at
		// startup regardless of what gets added to addInitialViews later, or in
		// what order.
		VirtualView virtualView = VirtualView.getInstance();
		if (virtualView != null) {
			virtualView.gotoColumn(0);
		}
	}

	@Override
	protected void prepareForShutdown() {
		eventNavigator.close();
		super.prepareForShutdown();
	}

	/** @return milliseconds since the JVM itself started (not since main() began) */
	private static long jvmUptimeMillis() {
		return java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
	}

	/**
	 * Logs when this frame's windowOpened fires (the point Swing itself
	 * considers the main window shown), then posts a further invokeLater
	 * from inside that handler to catch any work Swing queued alongside
	 * opening it (typically the first real paint) that would otherwise run
	 * invisibly between "window shown" and "actually settled". Isolates
	 * whether a slow startup is still CED/EDT work at that point, or
	 * something outside the JVM's control (OS/window-manager) once this
	 * fires -- originally diagnostic-only, but that second point is also
	 * this application's real readiness signal: the EDT is provably idle,
	 * so this is also where {@link #markStartupReady()} lifts the input
	 * block installed by {@link #installStartupInputBlock()} and closes the
	 * startup splash, if {@link #startupWindow} is still open.
	 */
	private void installStartupMarker() {
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowOpened(java.awt.event.WindowEvent event) {
				Log.getInstance().config("Startup timing - windowOpened, JVM uptime: "
						+ jvmUptimeMillis() + " ms");
				SwingUtilities.invokeLater(() -> {
					Log.getInstance().config(
							"Startup timing - EDT drained after windowOpened, JVM uptime: "
									+ jvmUptimeMillis() + " ms");
					markStartupReady();
				});
				removeWindowListener(this);
			}
		});
	}

	/**
	 * Blocks mouse input on this frame from the moment it's shown until
	 * {@link #markStartupReady()} lifts it. Without this, a click landing on
	 * the frame right after {@code setVisible(true)} -- while Swing is still
	 * laying out and painting several fully-eager detector views for the
	 * first time -- is exactly what produces the "spinning color wheel":
	 * clicking into a window whose EDT is still busy with its own first
	 * paint. A glass pane with no listeners of its own would just forward
	 * events to whatever's underneath; adding empty ones is what makes it
	 * actually swallow them.
	 */
	private void installStartupInputBlock() {
		JComponent glassPane = (JComponent) getGlassPane();
		glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		glassPane.addMouseListener(new MouseAdapter() { });
		glassPane.addMouseMotionListener(new MouseMotionAdapter() { });
		glassPane.setVisible(true);
	}

	/**
	 * This application's actual readiness signal (see {@link
	 * #installStartupMarker()}): the main frame's first layout/paint has
	 * settled and the EDT is idle. Lifts {@link #installStartupInputBlock()}'s
	 * block and closes the startup splash, if one is still open.
	 */
	private void markStartupReady() {
		getGlassPane().setVisible(false);
		if (startupWindow != null) {
			startupWindow.close();
			startupWindow = null;
		}
	}

	/** Launches the MDI application on the Swing event-dispatch thread. */
	public static void main(String[] args) {
		Log.getInstance().config("Startup timing - main() entered, JVM uptime: "
				+ jvmUptimeMillis() + " ms");
		// sqlite-jdbc (used by the geometry cache) extracts its native library to
		// java.io.tmpdir by default, under a fresh path most launches -- rewriting
		// and (on macOS) re-scanning that executable every time. Pointing it at a
		// stable, reused directory instead lets it detect the already-extracted,
		// already-verified copy and skip that work. Startup timing showed
		// GeometryCacheCoordinator's cache.open() taking anywhere from 440ms to
		// 3.4s across otherwise-identical runs (every detector a cache hit both
		// times) -- this is that variance's most likely source. Must be set before
		// any class touches org.sqlite.JDBC, so it comes before everything else.
		// The directory must exist first: sqlite-jdbc does not reliably create a
		// missing org.sqlite.tmpdir itself, and failing to load its native library
		// leaves every detector's geometry silently uninitialized (GeometryService
		// swallows the exception into a GeometryStatus), surfacing much later as
		// an IllegalStateException from deep inside painting code.
		try {
			Path sqliteNativeDir = Path.of(System.getProperty("user.home"), ".ced", "sqlite-native");
			Files.createDirectories(sqliteNativeDir);
			System.setProperty("org.sqlite.tmpdir", sqliteNativeDir.toString());
		} catch (IOException exception) {
			// fall back to sqlite-jdbc's own default extraction location
		}
		launchOptions = CedLaunchOptions.parse(args);
		StartupWindow[] holder = new StartupWindow[1];
		boolean bootstrapSucceeded = false;
		try {
			SwingUtilities.invokeAndWait(() -> {
				holder[0] = new StartupWindow(StartupInfo.builder("CED")
						.version(CedVersion.VERSION)
						.organization("Developed at Christopher Newport University")
						.logo(new CedStartupIcon())
						.build());
				holder[0].show();
			});
			bootstrap = CedBootstrap.initialize(launchOptions, holder[0]);
			bootstrapSucceeded = true;
		} catch (Exception exception) {
			Log.getInstance().exception(exception);
		}
		if (bootstrapSucceeded) {
			// Deliberately NOT closed here. Closing as soon as construction
			// finishes just trades one blank gap for another: bootstrap
			// (magnetic fields, geometry) is only ~1s of the real gap between
			// this point and the window actually becoming interactive -- the
			// rest is BaseMDIApplication.launch()'s setVisible(true) laying
			// out and first-painting several fully-eager detector views,
			// which is exactly the work that produces the "spinning color
			// wheel" if the user clicks into the frame while it's still
			// running. Handing the still-open splash to CedApplication (via
			// this static field) lets it stay up -- and keep covering that
			// same not-yet-ready frame, since StartupWindow is always-on-top
			// -- until installStartupMarker's own readiness signal fires and
			// closes it, however long that actually takes on this machine.
			startupWindow = holder[0];
		} else if (holder[0] != null) {
			try {
				SwingUtilities.invokeAndWait(holder[0]::close);
			} catch (Exception closeException) {
				Log.getInstance().exception(closeException);
			}
		}
		Log.getInstance().config("Startup timing - bootstrap done, launching MDI shell, JVM uptime: "
				+ jvmUptimeMillis() + " ms");
		BaseMDIApplication.launch(() -> {
			CedApplication app = CedApplication.getInstance();
			// Splits the bootstrap-done-to-windowOpened gap in two: everything up to
			// here is CedApplication's own constructor (addInitialViews plus its
			// menu-building calls); everything after is BaseMDIApplication's
			// setVisible(true) -- native peer creation, layout, and realizing the
			// window -- which this constructor-scoped timing can't otherwise see.
			Log.getInstance().config("Startup timing - CedApplication constructed (before setVisible), "
					+ "JVM uptime: " + jvmUptimeMillis() + " ms");
			return app;
		});
	}
}
