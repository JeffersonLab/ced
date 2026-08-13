package edu.cnu.ced.app;

import java.io.IOException;
import java.nio.file.Path;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import edu.cnu.ced.CedVersion;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.event.EventSource;
import edu.cnu.ced.event.EventStore;
import edu.cnu.ced.event.HipoEventSource;
import edu.cnu.ced.resources.Clas12ResourceLocator;
import edu.cnu.ced.resources.Clas12Resources;
import edu.cnu.ced.view.CurrentEventView;
import edu.cnu.mdi.app.BaseMDIApplication;
import edu.cnu.mdi.dialog.FileDialogs;
import edu.cnu.mdi.dialog.FileType;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.ui.menu.MenuManager;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.JsonView;
import edu.cnu.mdi.view.LogView;
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
	private static final FileType HIPO_FILES = FileType.of("HIPO event files (*.hipo)", "hipo");

	private EventNavigator eventNavigator;
	private LogView logView;
	private JsonView jsonView;
	private CurrentEventView currentEventView;
	private Clas12Resources clas12Resources;

	private CedApplication() {
		super(PropertyUtils.TITLE, CedVersion.title(),
				PropertyUtils.BACKGROUNDIMAGE, BACKGROUND_RESOURCE,
				PropertyUtils.FRACTION, 0.9,
				PropertyUtils.CONSOLELOG, true);
		addCedFileActions();
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
		eventNavigator = new EventNavigator(new EventStore());
		logView = new LogView();

        jsonView = new JsonView();
		currentEventView = new CurrentEventView(eventNavigator);
		initializeClas12Resources();

		Log.getInstance().config("CED MDI application shell initialized with "
				+ VIRTUAL_DESKTOP_COLUMNS + " virtual desktop columns.");
		Log.getInstance().config("CED launch configuration: geometry variation="
				+ launchOptions.geometryVariation() + ", 3D=" + launchOptions.enable3D()
				+ ", experimental=" + launchOptions.experimental());
	}

	private void addCedFileActions() {
		JMenuItem openHipo = new JMenuItem("Open HIPO Event File...");
		openHipo.addActionListener(event -> chooseHipoFile());
		JMenu fileMenu = MenuManager.getInstance().getFileMenu();
		fileMenu.insert(openHipo, 0);
		fileMenu.insertSeparator(1);
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

	private void initializeClas12Resources() {
		try {
			clas12Resources = Clas12ResourceLocator.locate();
			System.setProperty("CLAS12DIR", clas12Resources.root().toString());
			Log.getInstance().config("CLAS12 resources: " + clas12Resources.root());
		} catch (IOException exception) {
			Log.getInstance().warning(exception.getMessage());
		}
	}

	@Override
	protected void defaultViewLayout() {
		virtualViewMove(currentEventView, 0, VirtualView.CENTER);
		virtualViewMove(logView, 17, VirtualView.UPPERLEFT);
		virtualViewMove(jsonView, 17, VirtualView.BOTTOMRIGHT);
	}

	@Override
	protected void prepareForShutdown() {
		eventNavigator.close();
		super.prepareForShutdown();
	}

	/** Launches the MDI application on the Swing event-dispatch thread. */
	public static void main(String[] args) {
		launchOptions = CedLaunchOptions.parse(args);
		BaseMDIApplication.launch(CedApplication::getInstance);
	}
}
