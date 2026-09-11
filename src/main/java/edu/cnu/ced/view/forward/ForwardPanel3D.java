package edu.cnu.ced.view.forward;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.DCEventData;
import edu.cnu.ced.data.ECalEventData;
import edu.cnu.ced.data.FTOFEventData;
import edu.cnu.ced.data.PCalEventData;
import edu.cnu.ced.geometry.DCGeometry;
import edu.cnu.ced.geometry.ECGeometry;
import edu.cnu.ced.geometry.FTOFGeometry;
import edu.cnu.ced.geometry.PCALGeometry;
import edu.cnu.ced.view3d.CedPanel3D;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;

/**
 * The 3D scene for {@link ForwardView3D}: an axis set, one item per DC
 * superlayer, one item per FTOF sector, and one item per PCAL/ECAL
 * (sector, [stack,] view) plane.
 *
 * <p>
 * Scoped to DC/FTOF/PCAL/ECAL, consistent with every other 3D view so
 * far: reconstructed-cross, trajectory, and recon-cal drawing (legacy's
 * own {@code CrossDrawer3D}/{@code TrajectoryDrawer3D}/{@code
 * RecDrawer3D}) and the magnetic-field-boundary decoration ({@code
 * FieldBoundary}) are left as a follow-up.
 * </p>
 */
final class ForwardPanel3D extends CedPanel3D {

	private static final float XY_MAX = 600f;
	private static final float Z_MIN = -100f;
	private static final float Z_MAX = 600f;

	// Set via setGeometry() by ForwardView3D immediately after this panel
	// is constructed (see make3DPanel()); see FTCalPanel3D's own comment
	// on the identical constructor-ordering reason.
	private DCGeometry dcGeometry;
	private FTOFGeometry ftofGeometry;
	private PCALGeometry pcalGeometry;
	private ECGeometry ecalGeometry;

	private volatile Map<SuperlayerKey, List<LayerWire>> dcHits = Map.of();
	private volatile Map<FtofKey, Integer> ftofAdc = Map.of();
	private volatile Map<PlaneKey, List<StripHit>> pcalHits = Map.of();
	private volatile Map<StackPlaneKey, List<StripHit>> ecalHits = Map.of();
	private volatile int ftofMaxAdc;
	private volatile int pcalMaxAdc;
	private volatile int ecalMaxAdc;

	ForwardPanel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		super(EnumSet.of(CedDisplayOption.VOLUMES, CedDisplayOption.TRUTH, CedDisplayOption.RAW_DATA,
				CedDisplayOption.SECTOR_1, CedDisplayOption.SECTOR_2, CedDisplayOption.SECTOR_3,
				CedDisplayOption.SECTOR_4, CedDisplayOption.SECTOR_5, CedDisplayOption.SECTOR_6,
				CedDisplayOption.DC, CedDisplayOption.FTOF, CedDisplayOption.PCAL, CedDisplayOption.ECAL),
				angleX, angleY, angleZ, xDist, yDist, zDist);
	}

	void setGeometry(DCGeometry dc, FTOFGeometry ftof, PCALGeometry pcal, ECGeometry ecal) {
		this.dcGeometry = dc;
		this.ftofGeometry = ftof;
		this.pcalGeometry = pcal;
		this.ecalGeometry = ecal;
	}

	@Override
	public void createInitialItems() {
		addItem(new Axes3D(this, -XY_MAX, XY_MAX, -XY_MAX, XY_MAX, Z_MIN, Z_MAX,
				new String[] { "x", "y", "z" }, Color.darkGray, 1f, 7, 7, 8,
				Color.black, new Color(0, 100, 0), new Font("SansSerif", Font.PLAIN, 10), 0));
		for (int sector = 1; sector <= DCGeometry.SECTOR_COUNT; sector++) {
			for (int superlayer = 1; superlayer <= DCGeometry.SUPERLAYER_COUNT; superlayer++) {
				addItem(new DcSuperLayer3D(this, sector, superlayer));
			}
		}
		for (int sector = 1; sector <= FTOFGeometry.SECTOR_COUNT; sector++) {
			addItem(new FtofSector3D(this, sector));
		}
		for (int sector = 1; sector <= PCALGeometry.SECTOR_COUNT; sector++) {
			for (int view = 0; view < PCALGeometry.VIEW_COUNT; view++) {
				addItem(new PcalPlane3D(this, sector, view));
			}
		}
		for (int sector = 1; sector <= ECGeometry.SECTOR_COUNT; sector++) {
			for (int stack = 0; stack < ECGeometry.STACK_COUNT; stack++) {
				for (int view = 0; view < ECGeometry.VIEW_COUNT; view++) {
					addItem(new EcalPlane3D(this, sector, stack, view));
				}
			}
		}
	}

	@Override
	public float getZStep() {
		return (Z_MAX - Z_MIN) / 50f;
	}

	/** Refreshes the current event's hits; called by {@link ForwardView3D}. */
	void setEventData(DCEventData dc, FTOFEventData ftof, PCalEventData pcal, ECalEventData ecal) {
		Map<SuperlayerKey, List<LayerWire>> dcMap = new HashMap<>();
		for (DCEventData.RawHit hit : dc.rawHits()) {
			dcMap.computeIfAbsent(new SuperlayerKey(hit.sector(), hit.superlayer()), k -> new ArrayList<>())
					.add(new LayerWire(hit.layer(), hit.wire()));
		}
		this.dcHits = Map.copyOf(dcMap);

		Map<FtofKey, Integer> ftofMap = new HashMap<>();
		int ftofMax = 0;
		for (FTOFEventData.AdcHit hit : ftof.adcHits()) {
			ftofMap.merge(new FtofKey(hit.sector(), hit.panel(), hit.paddle()), hit.adc(), Math::max);
			ftofMax = Math.max(ftofMax, hit.adc());
		}
		this.ftofAdc = Map.copyOf(ftofMap);
		this.ftofMaxAdc = ftofMax;

		Map<PlaneKey, List<StripHit>> pcalMap = new HashMap<>();
		int pcalMax = 0;
		for (PCalEventData.AdcHit hit : pcal.adcHits()) {
			pcalMap.computeIfAbsent(new PlaneKey(hit.sector(), hit.view()), k -> new ArrayList<>())
					.add(new StripHit(hit.strip(), hit.adc()));
			pcalMax = Math.max(pcalMax, hit.adc());
		}
		this.pcalHits = Map.copyOf(pcalMap);
		this.pcalMaxAdc = pcalMax;

		Map<StackPlaneKey, List<StripHit>> ecalMap = new HashMap<>();
		int ecalMax = 0;
		for (ECalEventData.AdcHit hit : ecal.adcHits()) {
			ecalMap.computeIfAbsent(new StackPlaneKey(hit.sector(), hit.plane(), hit.view()), k -> new ArrayList<>())
					.add(new StripHit(hit.strip(), hit.adc()));
			ecalMax = Math.max(ecalMax, hit.adc());
		}
		this.ecalHits = Map.copyOf(ecalMap);
		this.ecalMaxAdc = ecalMax;
	}

	DCGeometry dcGeometry() {
		return dcGeometry;
	}

	FTOFGeometry ftofGeometry() {
		return ftofGeometry;
	}

	PCALGeometry pcalGeometry() {
		return pcalGeometry;
	}

	ECGeometry ecalGeometry() {
		return ecalGeometry;
	}

	List<LayerWire> dcRawHits(int sector, int superlayer) {
		return dcHits.getOrDefault(new SuperlayerKey(sector, superlayer), List.of());
	}

	Integer ftofAdc(int sector, int panel, int paddle) {
		return ftofAdc.get(new FtofKey(sector, panel, paddle));
	}

	int ftofMaximumAdc() {
		return ftofMaxAdc;
	}

	List<StripHit> pcalHits(int sector, int view) {
		return pcalHits.getOrDefault(new PlaneKey(sector, view), List.of());
	}

	int pcalMaximumAdc() {
		return pcalMaxAdc;
	}

	List<StripHit> ecalHits(int sector, int stack, int view) {
		return ecalHits.getOrDefault(new StackPlaneKey(sector, stack, view), List.of());
	}

	int ecalMaximumAdc() {
		return ecalMaxAdc;
	}

	/** The shared per-sector master display toggle used by every forward detector item. */
	static CedDisplayOption sectorOption(int sector) {
		return switch (sector) {
		case 1 -> CedDisplayOption.SECTOR_1;
		case 2 -> CedDisplayOption.SECTOR_2;
		case 3 -> CedDisplayOption.SECTOR_3;
		case 4 -> CedDisplayOption.SECTOR_4;
		case 5 -> CedDisplayOption.SECTOR_5;
		default -> CedDisplayOption.SECTOR_6;
		};
	}

	/** 1-based (sector, superlayer) address for a DC hit-wire lookup. */
	record SuperlayerKey(int sector, int superlayer) {
	}

	/** 1-based (layer, wire) address of one raw DC hit within a superlayer. */
	record LayerWire(int layer, int wire) {
	}

	/** (1-based sector, 0-based panel, 1-based paddle) address for an FTOF ADC lookup. */
	record FtofKey(int sector, int panel, int paddle) {
	}

	/** (1-based sector, 0-based view) address for a PCAL strip-hit lookup. */
	record PlaneKey(int sector, int view) {
	}

	/** (1-based sector, 0-based stack, 0-based view) address for an ECAL strip-hit lookup. */
	record StackPlaneKey(int sector, int stack, int view) {
	}

	/** One strip's (1-based strip, ADC) for hit-box drawing. */
	record StripHit(int strip, int adc) {
	}
}
