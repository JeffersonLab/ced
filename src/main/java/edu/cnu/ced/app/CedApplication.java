package edu.cnu.ced.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import edu.cnu.ced.CedVersion;
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
import edu.cnu.ced.view.ftcal.FTCalXYView;
import edu.cnu.ced.view.ftof.FTOFView;
import edu.cnu.ced.view.pcal.PCalView;
import edu.cnu.ced.view.ecal.ECalView;
import edu.cnu.ced.view.central.CentralXYView;
import edu.cnu.ced.view.central.CentralZView;
import edu.cnu.ced.view.dc.AllDCView;
import edu.cnu.ced.view.dc.DCHexView;
import edu.cnu.ced.view.sector.SectorView;
import edu.cnu.ced.view.sector.SectorView.Pair;
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
	private static final FileType HIPO_FILES = FileType.of("HIPO event files (*.hipo)", "hipo");
	private static final MenuId OPTIONS_MENU_ID = new MenuId("ced.options");
	private static final MenuId EVENTS_MENU_ID = new MenuId("ced.events");

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
		JMenuItem filter = new JMenuItem("Filter…");
		filter.addActionListener(event -> showFilterDialog());
		events.add(filter);
		MenuManager.getInstance().addContribution(new MenuContribution(EVENTS_MENU_ID, events, 150));
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
			filterDialog = new FilterDialog(this, EventFilters.sharedFor(eventNavigator),
					eventNavigator::refresh);
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
	 * Diagnostic only: logs when this frame's windowOpened fires (the point
	 * Swing itself considers the main window shown), then posts a further
	 * invokeLater from inside that handler to catch any work Swing queued
	 * alongside opening it (typically the first real paint) that would
	 * otherwise run invisibly between "window shown" and "actually settled".
	 * Isolates whether a slow startup is still CED/EDT work at that point, or
	 * something outside the JVM's control (OS/window-manager) once this fires.
	 */
	private void installStartupMarker() {
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowOpened(java.awt.event.WindowEvent event) {
				Log.getInstance().config("Startup timing - windowOpened, JVM uptime: "
						+ jvmUptimeMillis() + " ms");
				SwingUtilities.invokeLater(() -> Log.getInstance().config(
						"Startup timing - EDT drained after windowOpened, JVM uptime: "
								+ jvmUptimeMillis() + " ms"));
				removeWindowListener(this);
			}
		});
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
			SwingUtilities.invokeAndWait(holder[0]::close);
		} catch (Exception exception) {
			Log.getInstance().exception(exception);
			if (holder[0] != null) {
				try {
					SwingUtilities.invokeAndWait(holder[0]::close);
				} catch (Exception closeException) {
					Log.getInstance().exception(closeException);
				}
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
