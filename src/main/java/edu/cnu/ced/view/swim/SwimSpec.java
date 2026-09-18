package edu.cnu.ced.view.swim;

/** One hypothetical particle to swim, for {@link SwimTestPanel3D#runBatch}. */
record SwimSpec(int charge, double vx, double vy, double vz, double p, double thetaDeg, double phiDeg) {
}
