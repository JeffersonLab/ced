package edu.cnu.ced.resources;

import java.nio.file.Path;

/** Validated filesystem locations used by coatjava-backed CED services. */
public record Clas12Resources(Path root, Path bankDefinitions) {

	public Clas12Resources {
		root = root.toAbsolutePath().normalize();
		bankDefinitions = bankDefinitions.toAbsolutePath().normalize();
	}
}
