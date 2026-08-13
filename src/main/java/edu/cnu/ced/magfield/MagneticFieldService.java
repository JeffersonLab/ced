package edu.cnu.ced.magfield;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import cnuphys.magfield.MagneticFields;

/** Owns asynchronous initialization of coatjava magnetic-field maps. */
public final class MagneticFieldService {

	@FunctionalInterface
	interface Initializer {
		MagneticFieldStatus initialize();
	}

	private final Initializer initializer;
	private final AtomicReference<CompletableFuture<MagneticFieldStatus>> initialization =
			new AtomicReference<>();

	public MagneticFieldService() {
		this(MagneticFieldService::initializeCoatFields);
	}

	MagneticFieldService(Initializer initializer) {
		this.initializer = Objects.requireNonNull(initializer, "initializer");
	}

	/** Start initialization once and return the shared completion. */
	public CompletableFuture<MagneticFieldStatus> initializeAsync(Executor executor) {
		Objects.requireNonNull(executor, "executor");
		CompletableFuture<MagneticFieldStatus> existing = initialization.get();
		if (existing != null) {
			return existing;
		}
		CompletableFuture<MagneticFieldStatus> created = CompletableFuture
				.supplyAsync(this::initializeSafely, executor);
		return initialization.compareAndSet(null, created) ? created : initialization.get();
	}

	/** Initialize on the calling thread; intended for pre-UI application bootstrap. */
	public MagneticFieldStatus initialize() {
		return initializeSafely();
	}

	private MagneticFieldStatus initializeSafely() {
		try {
			return initializer.initialize();
		} catch (Throwable cause) {
			return MagneticFieldStatus.failed(cause);
		}
	}

	private static MagneticFieldStatus initializeCoatFields() {
		MagneticFields fields = MagneticFields.getInstance();
		fields.initializeMagneticFields();
		boolean initialized = fields.getActiveField() != null;
		return new MagneticFieldStatus(initialized,
				fields.getActiveFieldDescription(), fields.getTorusBaseName(),
				fields.getSolenoidBaseName(), initialized ? "" : "No magnetic-field map was loaded");
	}
}
