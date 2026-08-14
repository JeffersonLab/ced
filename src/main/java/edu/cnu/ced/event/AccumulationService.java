package edu.cnu.ced.event;

import edu.cnu.ced.data.FTCalAccumulation;
import edu.cnu.ced.data.FTCalEventData;
import edu.cnu.ced.data.ECalAccumulation;
import edu.cnu.ced.data.ECalEventData;
import edu.cnu.ced.data.PCalAccumulation;
import edu.cnu.ced.data.PCalEventData;
import edu.cnu.ced.data.FTOFAccumulation;
import edu.cnu.ced.data.FTOFEventData;

/** Collects detector occupancy only during an explicitly requested accumulation run. */
public final class AccumulationService {

	private final FTCalAccumulation ftcal = new FTCalAccumulation();
	private final PCalAccumulation pcal = new PCalAccumulation();
	private final ECalAccumulation ecal = new ECalAccumulation();
	private final FTOFAccumulation ftof = new FTOFAccumulation();

	public void begin(boolean clearExisting) {
		if (clearExisting) ftcal.clear();
		if (clearExisting) pcal.clear();
		if (clearExisting) ecal.clear();
		if (clearExisting) ftof.clear();
	}

	public void accumulate(EventSnapshot snapshot) {
		ftcal.add(FTCalEventData.from(snapshot));
		pcal.add(PCalEventData.from(snapshot));
		ecal.add(ECalEventData.from(snapshot));
		ftof.add(FTOFEventData.from(snapshot));
	}

	public FTCalAccumulation ftcal() { return ftcal; }
	public PCalAccumulation pcal() { return pcal; }
	public ECalAccumulation ecal() { return ecal; }
	public FTOFAccumulation ftof() { return ftof; }
}
