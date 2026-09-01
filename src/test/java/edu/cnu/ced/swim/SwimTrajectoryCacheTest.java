package edu.cnu.ced.swim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;

import org.jlab.io.base.DataEvent;
import org.junit.jupiter.api.Test;

import cnuphys.magfield.ZeroProbe;

import edu.cnu.ced.data.RecEventData;
import edu.cnu.ced.event.EventSnapshot;
import edu.cnu.ced.geometry.Point3;

class SwimTrajectoryCacheTest {

	@Test
	void computesAndCachesATrajectory() {
		SwimTrajectoryCache cache = new SwimTrajectoryCache();
		cache.forEvent(snapshot());
		RecEventData.Particle particle = particle();

		List<Point3> first = cache.trajectory(particle, new ZeroProbe());
		assertTrue(first.size() >= 2);
		assertEquals(1, cache.size());

		List<Point3> second = cache.trajectory(particle, new ZeroProbe());
		assertSame(first, second, "second lookup for the same particle should return the cached list");
		assertEquals(1, cache.size());
	}

	@Test
	void nullParticleReturnsEmptyWithoutCaching() {
		SwimTrajectoryCache cache = new SwimTrajectoryCache();
		cache.forEvent(snapshot());

		assertTrue(cache.trajectory(null, new ZeroProbe()).isEmpty());
		assertEquals(0, cache.size());
	}

	@Test
	void sameSnapshotDoesNotClearAlreadyCachedWork() {
		SwimTrajectoryCache cache = new SwimTrajectoryCache();
		EventSnapshot snapshot = snapshot();
		cache.forEvent(snapshot);
		cache.trajectory(particle(), new ZeroProbe());
		assertEquals(1, cache.size());

		// A second view receiving the same event snapshot must not wipe out
		// the first view's already-computed trajectory.
		cache.forEvent(snapshot);
		assertEquals(1, cache.size());
	}

	@Test
	void differentSnapshotClearsPreviousEventsTrajectories() {
		SwimTrajectoryCache cache = new SwimTrajectoryCache();
		cache.forEvent(snapshot());
		cache.trajectory(particle(), new ZeroProbe());
		assertEquals(1, cache.size());

		cache.forEvent(snapshot());
		assertEquals(0, cache.size());
	}

	private static RecEventData.Particle particle() {
		return new RecEventData.Particle(0, 2212, 1, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0);
	}

	private static EventSnapshot snapshot() {
		DataEvent event = (DataEvent) Proxy.newProxyInstance(DataEvent.class.getClassLoader(),
				new Class<?>[] { DataEvent.class }, (instance, method, args) -> switch (method.getName()) {
					case "getBankList" -> new String[0];
					default -> null;
				});
		return EventSnapshot.of(event);
	}
}
