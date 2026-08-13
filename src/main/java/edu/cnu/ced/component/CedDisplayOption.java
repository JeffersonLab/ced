package edu.cnu.ced.component;

/** Typed visibility and event-mode options shared by CED detector views. */
public enum CedDisplayOption {
	SINGLE_EVENT("Single", true, "event-mode"),
	ACCUMULATION("Accum.", false, "event-mode"),
	RAW_DATA("Raw Data", true, null),
	RECON_HITS("Recon Hits", true, null),
	RECON_CAL("Recon Cal", true, null),
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
