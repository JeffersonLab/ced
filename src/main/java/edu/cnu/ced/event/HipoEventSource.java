package edu.cnu.ced.event;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.jlab.io.hipo.HipoDataSource;

/** Factory for coatjava HIPO file sources. */
public final class HipoEventSource {

	private HipoEventSource() { }

	public static EventSource open(Path path) {
		Path file = path.toAbsolutePath().normalize();
		if (!Files.isRegularFile(file)) {
			throw new IllegalArgumentException("HIPO file does not exist: " + file);
		}
		if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".hipo")) {
			throw new IllegalArgumentException("Not a HIPO file: " + file);
		}

		HipoDataSource source = new HipoDataSource();
		source.open(file.toFile());
		return new CoatDataSourceAdapter(source, file.toString());
	}
}
