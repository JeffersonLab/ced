package edu.cnu.ced.event;

import edu.cnu.ced.data.FTCalAccumulation;
import edu.cnu.ced.data.FTCalEventData;
import edu.cnu.ced.data.PCalAccumulation;
import edu.cnu.ced.data.PCalEventData;

/** Collects detector occupancy only during an explicitly requested accumulation run. */
public final class AccumulationService {

	private final FTCalAccumulation ftcal = new FTCalAccumulation();
	private final PCalAccumulation pcal = new PCalAccumulation();

	public void begin(boolean clearExisting) {
		if (clearExisting) ftcal.clear();
		if (clearExisting) pcal.clear();
	}

	public void accumulate(EventSnapshot snapshot) {
		ftcal.add(FTCalEventData.from(snapshot));
		pcal.add(PCalEventData.from(snapshot));
	}

	public FTCalAccumulation ftcal() { return ftcal; }
	public PCalAccumulation pcal() { return pcal; }
}
