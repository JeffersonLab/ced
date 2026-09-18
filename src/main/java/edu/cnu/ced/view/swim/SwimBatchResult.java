package edu.cnu.ced.view.swim;

import java.util.List;

import edu.cnu.ced.geometry.Point3;

/**
 * One randomized batch swim's outcome: its trajectory (the partial path
 * reached so far if {@code success} is {@code false} -- see {@link
 * edu.cnu.ced.swim.ParticleSwimmer.Outcome}), the charge it was swum
 * with (for {@link SwimBatchDrawer3D}'s own charge-based coloring), and
 * whether it actually reached its target. Matches legacy CED's own {@code
 * CLAS12SwimResult} in spirit, trimmed to just what {@link
 * SwimBatchDrawer3D}/{@link SwimBatchControlPanel} need.
 */
record SwimBatchResult(List<Point3> trajectory, int charge, boolean success) {
}
