package edu.cnu.ced.component;

/** Typed visibility and event-mode options shared by CED detector views. */
public enum CedDisplayOption {
	SINGLE_EVENT("Single", true, "event-mode"),
	ACCUMULATION("Accum.", false, "event-mode"),
	// Added automatically to every CedView (see CedView.initializeCedView),
	// not detector-specific like the rest -- gates only the delayed popup
	// hover window (CedView.hoverUpdate); the continuous feedback-pane text
	// is never suppressed by this.
	HOVER_POPUP("Hover Popup", true, null),
	RAW_DATA("Raw Data", true, null),
	RECON_HITS("Recon Hits", true, null),
	RECON_CAL("Recon Cal", true, null),
	RECON_TRACKS("Recon Tracks", true, null),
	MC_TRACKS("MC Tracks", true, null),
	HB_TRACKS("HB Tracks", true, null),
	TB_TRACKS("TB Tracks", true, null),
	AI_HB_TRACKS("AI HB Tracks", false, null),
	AI_TB_TRACKS("AI TB Tracks", false, null),
	CVT_TRACKS("CVT Tracks", true, null),
	CLUSTERS("Clusters", true, null),
	CROSSES("Crosses", true, null),
	HB_HITS("HB Hits", true, null),
	TB_HITS("TB Hits", true, null),
	AI_HB_HITS("AI HB Hits", false, null),
	AI_TB_HITS("AI TB Hits", false, null),
	HB_SEGMENTS("HB Segments", true, null),
	TB_SEGMENTS("TB Segments", true, null),
	AI_HB_SEGMENTS("AI HB Segments", false, null),
	AI_TB_SEGMENTS("AI TB Segments", false, null),
	CONNECT_CLUSTER_ENDPOINTS("Connect ends", false, null),
	PANEL_1A("Panel 1A", true, "ftof-panel"),
	PANEL_1B("Panel 1B", false, "ftof-panel"),
	PANEL_2("Panel 2", false, "ftof-panel"),
	INNER_PLANE("Inner", true, "ec-plane"),
	OUTER_PLANE("Outer", false, "ec-plane"),
	U_STRIPS("U Strips", true, null),
	V_STRIPS("V Strips", true, null),
	W_STRIPS("W Strips", true, null),
	// 3D-view-only toggles (see edu.cnu.ced.view3d). Kept in this shared
	// enum rather than a 3D-specific one so a single vocabulary of display
	// toggles covers both 2D and 3D CED views.
	VOLUMES("Volumes", true, null),
	TRUTH("Truth", true, null),
	// Central 3D View (edu.cnu.ced.view.central): independent per-detector
	// master toggles, plus per-layer toggles for CND (three physical
	// layers; CTOF is a single layer so needs no sub-toggle of its own).
	CND("CND", true, null),
	CND_LAYER_1("CND Layer 1", true, null),
	CND_LAYER_2("CND Layer 2", true, null),
	CND_LAYER_3("CND Layer 3", true, null),
	CTOF("CTOF", true, null),
	// BST/BMT (barrel silicon/micromegas trackers): six physical layers
	// each, per edu.cnu.ced.geometry.BSTGeometry/BMTGeometry.
	BST("BST", true, null),
	BST_LAYER_1("BST Layer 1", true, null),
	BST_LAYER_2("BST Layer 2", true, null),
	BST_LAYER_3("BST Layer 3", true, null),
	BST_LAYER_4("BST Layer 4", true, null),
	BST_LAYER_5("BST Layer 5", true, null),
	BST_LAYER_6("BST Layer 6", true, null),
	BMT("BMT", true, null),
	BMT_LAYER_1("BMT Layer 1", true, null),
	BMT_LAYER_2("BMT Layer 2", true, null),
	BMT_LAYER_3("BMT Layer 3", true, null),
	BMT_LAYER_4("BMT Layer 4", true, null),
	BMT_LAYER_5("BMT Layer 5", true, null),
	BMT_LAYER_6("BMT Layer 6", true, null),
	// FMT 3D View (edu.cnu.ced.view.fmt): six physical layers and four
	// disconnected strip regions per edu.cnu.ced.geometry.FMTGeometry.
	// Hit/cluster highlighting reuses the existing generic RECON_HITS/
	// CLUSTERS toggles rather than adding FMT-specific duplicates.
	FMT("FMT", true, null),
	FMT_LAYER_1("FMT Layer 1", true, null),
	FMT_LAYER_2("FMT Layer 2", true, null),
	FMT_LAYER_3("FMT Layer 3", true, null),
	FMT_LAYER_4("FMT Layer 4", true, null),
	FMT_LAYER_5("FMT Layer 5", true, null),
	FMT_LAYER_6("FMT Layer 6", true, null),
	FMT_REGION_1("FMT Region 1", true, null),
	FMT_REGION_2("FMT Region 2", true, null),
	FMT_REGION_3("FMT Region 3", true, null),
	FMT_REGION_4("FMT Region 4", true, null),
	// Forward 3D View (edu.cnu.ced.view.forward): per-sector master gate
	// shared by every forward detector (DC/FTOF/PCAL/ECAL, matching legacy
	// CED's own showSector(sector) convention), plus one master toggle per
	// detector. Hit highlighting reuses the existing generic RAW_DATA
	// toggle rather than adding per-detector duplicates.
	SECTOR_1("Sector 1", true, null),
	SECTOR_2("Sector 2", true, null),
	SECTOR_3("Sector 3", true, null),
	SECTOR_4("Sector 4", true, null),
	SECTOR_5("Sector 5", true, null),
	SECTOR_6("Sector 6", true, null),
	DC("DC", true, null),
	FTOF("FTOF", true, null),
	PCAL("PCAL", true, null),
	ECAL("ECAL", true, null),
	// ALERT 3D View (edu.cnu.ced.view.alert): master toggles for the two
	// ALERT subsystems (AHDC drift chamber, ATOF time-of-flight), plus
	// per-sector gates for ATOF's 15 sectors -- AHDC has no real sector
	// division (edu.cnu.ced.geometry.AlertGeometry's sole DC sector is
	// always 0), so DC visibility is controlled by ALERT_DC alone.
	ALERT_DC("ALERT DC", true, null),
	ALERT_TOF("ALERT TOF", true, null),
	ALERT_SECTOR_1("ALERT Sector 1", true, null),
	ALERT_SECTOR_2("ALERT Sector 2", true, null),
	ALERT_SECTOR_3("ALERT Sector 3", true, null),
	ALERT_SECTOR_4("ALERT Sector 4", true, null),
	ALERT_SECTOR_5("ALERT Sector 5", true, null),
	ALERT_SECTOR_6("ALERT Sector 6", true, null),
	ALERT_SECTOR_7("ALERT Sector 7", true, null),
	ALERT_SECTOR_8("ALERT Sector 8", true, null),
	ALERT_SECTOR_9("ALERT Sector 9", true, null),
	ALERT_SECTOR_10("ALERT Sector 10", true, null),
	ALERT_SECTOR_11("ALERT Sector 11", true, null),
	ALERT_SECTOR_12("ALERT Sector 12", true, null),
	ALERT_SECTOR_13("ALERT Sector 13", true, null),
	ALERT_SECTOR_14("ALERT Sector 14", true, null),
	ALERT_SECTOR_15("ALERT Sector 15", true, null);

	private final String label;
	private final boolean initiallySelected;
	private final String group;

	CedDisplayOption(String label, boolean initiallySelected, String group) {
		this.label = label;
		this.initiallySelected = initiallySelected;
		this.group = group;
	}

	public String label() { return label; }
	public boolean initiallySelected() { return initiallySelected; }
	public String group() { return group; }
}
