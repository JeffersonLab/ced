package edu.cnu.ced.magfield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class MagneticFieldServiceTest {

	@Test
	void initializesOnlyOnceAndPublishesImmutableStatus() {
		AtomicInteger calls = new AtomicInteger();
		MagneticFieldStatus expected = new MagneticFieldStatus(true, "Composite", "torus.dat",
				"solenoid.dat", "");
		MagneticFieldService service = new MagneticFieldService(() -> {
			calls.incrementAndGet();
			return expected;
		});

		CompletableFuture<MagneticFieldStatus> first = service.initializeAsync(Runnable::run);
		CompletableFuture<MagneticFieldStatus> second = service.initializeAsync(Runnable::run);
		assertSame(first, second);
		assertSame(expected, first.join());
		assertEquals(1, calls.get());
	}

	@Test
	void convertsInitializationFailureToStatus() {
		MagneticFieldService service = new MagneticFieldService(() -> {
			throw new IllegalStateException("missing maps");
		});
		MagneticFieldStatus status = service.initializeAsync(Runnable::run).join();
		assertFalse(status.initialized());
		assertEquals("missing maps", status.error());
		assertTrue(status.description().isEmpty());
	}
}
