package edu.cnu.ced.event;

import edu.cnu.ced.data.FTCalAccumulation;
import edu.cnu.ced.data.FTCalEventData;
import edu.cnu.ced.data.ECalAccumulation;
import edu.cnu.ced.data.ECalEventData;
import edu.cnu.ced.data.PCalAccumulation;
import edu.cnu.ced.data.PCalEventData;
import edu.cnu.ced.data.FTOFAccumulation;
import edu.cnu.ced.data.FTOFEventData;
import edu.cnu.ced.data.CentralAccumulation;
import edu.cnu.ced.data.CentralEventData;
import edu.cnu.ced.data.DCAccumulation;
import edu.cnu.ced.data.DCEventData;

/** Collects detector occupancy only during an explicitly requested accumulation run. */
public final class AccumulationService {

	private final FTCalAccumulation ftcal = new FTCalAccumulation();
	private final PCalAccumulation pcal = new PCalAccumulation();
	private final ECalAccumulation ecal = new ECalAccumulation();
	private final FTOFAccumulation ftof = new FTOFAccumulation();
	private final CentralAccumulation central = new CentralAccumulation();
	private final DCAccumulation dc = new DCAccumulation();

	public void begin(boolean clearExisting) {
		if (clearExisting) clear();
	}

	/** Discard every detector's accumulated occupancy. */
	public void clear() {
		ftcal.clear();
		pcal.clear();
		ecal.clear();
		ftof.clear();
		central.clear();
		dc.clear();
	}

	public void accumulate(EventSnapshot snapshot) {
		ftcal.add(FTCalEventData.from(snapshot));
		pcal.add(PCalEventData.from(snapshot));
		ecal.add(ECalEventData.from(snapshot));
		ftof.add(FTOFEventData.from(snapshot));
		central.add(CentralEventData.from(snapshot));
		dc.add(DCEventData.from(snapshot));
	}

	public FTCalAccumulation ftcal() { return ftcal; }
	public PCalAccumulation pcal() { return pcal; }
	public ECalAccumulation ecal() { return ecal; }
	public FTOFAccumulation ftof() { return ftof; }
	public CentralAccumulation central() { return central; }
	public DCAccumulation dc() { return dc; }
}
