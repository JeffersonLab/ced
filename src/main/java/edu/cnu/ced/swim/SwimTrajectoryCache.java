package edu.cnu.ced.swim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cnuphys.magfield.FieldProbe;

import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.event.EventSnapshot;
import edu.cnu.ced.geometry.Point3;

/**
 * Shared, event-scoped cache of swum reconstructed-particle trajectories.
 * <p>
 * A CLAS12 event is usually shown in several views at once (e.g. three
 * {@code SectorView} instances, one per opposite-sector pair, and
 * eventually the Central Detector views too). Each independently receives
 * the same event; without sharing, each would re-swim every particle in the
 * event separately, several times over for the same result. Construct a
 * single {@code SwimTrajectoryCache} once (see {@code CedApplication}) and
 * pass the same instance into every view that draws particle trajectories:
 * the first view to ask for a given particle's trajectory computes it, and
 * every other view -- and every later repaint of the same view, e.g. during
 * pan/zoom -- reads the already-computed result.
 * </p>
 * <p>
 * <b>Thread safety:</b> synchronized, matching the other shared per-event
 * state in this package (e.g. {@code PCalAccumulation}), since a future
 * background accumulation pass could plausibly touch this from off the EDT.
 * </p>
 */
public final class SwimTrajectoryCache {

	private final Map<RecEventData.Particle, List<Point3>> trajectories = new HashMap<>();
	private EventSnapshot currentSnapshot;

	/**
	 * Ensure this cache holds trajectories for {@code snapshot}, discarding
	 * any left over from a previous event.
	 * <p>
	 * A no-op if this cache is already current for {@code snapshot} --
	 * compared by reference, since {@link EventSnapshot} is one immutable
	 * publication per event and every view receives the same instance for a
	 * given event. Safe to call once per view per event without one view's
	 * call wiping out the work another view already did for the same event.
	 * </p>
	 *
	 * @param snapshot the event snapshot views are currently displaying
	 */
	public synchronized void forEvent(EventSnapshot snapshot) {
		if (snapshot == currentSnapshot) return;
		currentSnapshot = snapshot;
		trajectories.clear();
	}

	/**
	 * Get (computing and caching if necessary) the swum trajectory for one
	 * particle, using {@link SwimRequestPolicy} for the maximum path length.
	 *
	 * @param particle the particle to swim
	 * @param probe    the magnetic field probe to swim through
	 * @return the trajectory as lab-frame points, oldest first; empty if
	 *         swimming didn't produce a usable trajectory
	 */
	public synchronized List<Point3> trajectory(RecEventData.Particle particle, FieldProbe probe) {
		if (particle == null) return List.of();
		return trajectories.computeIfAbsent(particle,
				p -> ParticleSwimmer.swim(p, probe, SwimRequestPolicy.maxPathLengthCm(p)));
	}

	/** @return the number of particles currently cached for the current event */
	public synchronized int size() {
		return trajectories.size();
	}
}
