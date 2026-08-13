package edu.cnu.ced.resources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** Discovers and validates the CLAS12 resource tree used by event I/O. */
public final class Clas12ResourceLocator {

	private static final Path BANK_DEFINITIONS = Path.of("etc", "bankdefs", "hipo4");
	private static final Path REQUIRED_SCHEMA = Path.of("header.json");

	private Clas12ResourceLocator() { }

	/** Locate resources using the process configuration and application location. */
	public static Clas12Resources locate() throws IOException {
		return locate(System.getProperty("CLAS12DIR"), System.getenv("CLAS12DIR"),
				Path.of(System.getProperty("user.dir", ".")), codeLocation());
	}

	static Clas12Resources locate(String property, String environment,
			Path workingDirectory, Path applicationLocation) throws IOException {
		Set<Path> candidates = new LinkedHashSet<>();
		addExplicit(candidates, property);
		addExplicit(candidates, environment);
		addSearchCandidates(candidates, workingDirectory);
		addSearchCandidates(candidates, applicationLocation);

		for (Path candidate : candidates) {
			Path root = candidate.toAbsolutePath().normalize();
			Path banks = root.resolve(BANK_DEFINITIONS);
			if (Files.isDirectory(banks) && Files.isRegularFile(banks.resolve(REQUIRED_SCHEMA))) {
				return new Clas12Resources(root.toRealPath(), banks.toRealPath());
			}
		}

		throw new IOException("Could not locate CLAS12 bank definitions. Set CLAS12DIR "
				+ "to a resource root containing " + BANK_DEFINITIONS + ".");
	}

	private static Path codeLocation() {
		try {
			return Path.of(Clas12ResourceLocator.class.getProtectionDomain()
					.getCodeSource().getLocation().toURI());
		} catch (URISyntaxException | NullPointerException exception) {
			return null;
		}
	}

	private static void addExplicit(Set<Path> candidates, String value) {
		if (value != null && !value.isBlank()) {
			candidates.add(Path.of(value.trim()));
		}
	}

	private static void addSearchCandidates(Set<Path> candidates, Path start) {
		if (start == null) {
			return;
		}

		Path current = Files.isDirectory(start) ? start : start.getParent();
		for (int depth = 0; current != null && depth < 10;
				depth++, current = current.getParent()) {
			candidates.add(current);
			candidates.add(current.resolve("coatjava"));
			candidates.add(current.resolve(Path.of("bCNU", "coatjava")));
		}
	}
}
