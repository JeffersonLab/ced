package edu.cnu.ced.view3d;

import java.util.List;

import cnuphys.magfield.FieldProbe;

import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.data.TrackRow;
import edu.cnu.ced.swim.SwimTrajectoryCache;

/**
 * Everything {@link TrackTrajectoryDrawer3D} needs to swim and draw one
 * event's tracks: the seven category lists it reuses from {@code
 * edu.cnu.ced.data} (Monte Carlo truth, DC hit-based/time-based x2 for
 * AI, {@code REC::Particle}, CVT), plus the shared swim cache and field
 * probe every 2D view already uses for the same job.
 *
 * <p>
 * Implemented by any {@link CedPanel3D} that wants track drawing --
 * currently {@code edu.cnu.ced.view.forward.ForwardPanel3D} and {@code
 * edu.cnu.ced.view.alert.AlertPanel3D} -- refreshing its lists once per
 * event (from {@code MonteCarloTracks}/{@code ReconstructedTracks}/
 * {@code RecEventData}) rather than re-parsing banks on every frame.
 * </p>
 */
public interface TrackTrajectorySource {

	List<TrackRow> mcTracks();

	List<TrackRow> hbTracks();

	List<TrackRow> tbTracks();

	List<TrackRow> aiHbTracks();

	List<TrackRow> aiTbTracks();

	List<TrackRow> cvtTracks();

	List<RecEventData.Particle> recParticles();

	SwimTrajectoryCache swimCache();

	FieldProbe fieldProbe();
}
