package edu.cnu.ced.component;

/** Typed visibility and event-mode options shared by CED detector views. */
public enum CedDisplayOption {
	SINGLE_EVENT("Single", true, "event-mode"),
	ACCUMULATION("Accum.", false, "event-mode"),
	RAW_DATA("Raw Data", true, null),
	RECON_HITS("Recon Hits", true, null),
	RECON_CAL("Recon Cal", true, null),
	RECON_TRACKS("Recon Tracks", true, null),
	MC_TRACKS("MC Tracks", true, null),
	HB_TRACKS("HB Tracks", true, null),
	TB_TRACKS("TB Tracks", true, null),
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
	W_STRIPS("W Strips", true, null);

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
