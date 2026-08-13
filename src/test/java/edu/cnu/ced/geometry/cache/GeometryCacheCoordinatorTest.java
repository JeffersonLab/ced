package edu.cnu.ced.geometry.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeometryCacheCoordinatorTest {

	@TempDir Path temp;

	@Test
	void coldLoadWritesAndWarmLoadRestoresExactPayload() throws Exception {
		Path path = temp.resolve("geometry.sqlite");
		GeometryCacheCoordinator coordinator = new GeometryCacheCoordinator(path, "2.0", "default");
		FakeGeometry cold = new FakeGeometry(37);
		assertTrue(coordinator.initialize(List.of(cold)).isEmpty());
		assertEquals(1, cold.sourceLoads);

		FakeGeometry warm = new FakeGeometry(0);
		assertEquals(List.of("test"), coordinator.initialize(List.of(warm)));
		assertEquals(0, warm.sourceLoads);
		assertEquals(37, warm.value);
	}

	@Test
	void variationChangeInvalidatesPayload() throws Exception {
		Path path = temp.resolve("geometry.sqlite");
		new GeometryCacheCoordinator(path, "2.0", "default")
				.initialize(List.of(new FakeGeometry(11)));
		FakeGeometry changed = new FakeGeometry(22);
		assertTrue(new GeometryCacheCoordinator(path, "2.0", "updated")
				.initialize(List.of(changed)).isEmpty());
		assertEquals(1, changed.sourceLoads);
		assertEquals(22, changed.value);
	}

	private static final class FakeGeometry implements CacheableGeometry {
		private int value;
		private int sourceLoads;
		FakeGeometry(int value) { this.value = value; }
		@Override public String name() { return "test"; }
		@Override public int formatVersion() { return 1; }
		@Override public void initializeFromSource() { sourceLoads++; }
		@Override public void read(DataInput input) throws IOException { value = input.readInt(); }
		@Override public void write(DataOutput output) throws IOException { output.writeInt(value); }
	}
}
