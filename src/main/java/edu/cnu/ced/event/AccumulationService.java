package edu.cnu.ced.event;

import edu.cnu.ced.data.FTCalAccumulation;
import edu.cnu.ced.data.FTCalEventData;

/** Collects detector occupancy only during an explicitly requested accumulation run. */
public final class AccumulationService {

	private final FTCalAccumulation ftcal = new FTCalAccumulation();

	public void begin(boolean clearExisting) {
		if (clearExisting) ftcal.clear();
	}

	public void accumulate(EventSnapshot snapshot) {
		ftcal.add(FTCalEventData.from(snapshot));
	}

	public FTCalAccumulation ftcal() { return ftcal; }
}
