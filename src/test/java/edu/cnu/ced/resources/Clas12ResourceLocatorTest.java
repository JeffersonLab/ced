package edu.cnu.ced.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Clas12ResourceLocatorTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void explicitPropertyTakesPrecedence() throws IOException {
		Path propertyRoot = resourceRoot(temporaryDirectory.resolve("property"));
		resourceRoot(temporaryDirectory.resolve(Path.of("bCNU", "coatjava")));

		Clas12Resources found = Clas12ResourceLocator.locate(propertyRoot.toString(),
				null, temporaryDirectory, null);

		assertEquals(propertyRoot.toRealPath(), found.root());
		assertEquals(propertyRoot.resolve(Path.of("etc", "bankdefs", "hipo4")).toRealPath(),
				found.bankDefinitions());
	}

	@Test
	void findsSiblingDevelopmentCheckout() throws IOException {
		Path root = resourceRoot(temporaryDirectory.resolve(Path.of("bCNU", "coatjava")));
		Path project = Files.createDirectories(temporaryDirectory.resolve("mdi_ced"));

		Clas12Resources found = Clas12ResourceLocator.locate(null, null, project, null);

		assertEquals(root.toRealPath(), found.root());
	}

	@Test
	void requiresRecognizableBankDefinitions() throws IOException {
		Path banks = Files.createDirectories(temporaryDirectory.resolve(
				Path.of("coatjava", "etc", "bankdefs", "hipo4")));
		Files.writeString(banks.resolve("unrelated.json"), "{}");

		assertThrows(IOException.class,
				() -> Clas12ResourceLocator.locate(null, null, temporaryDirectory, null));
	}

	private static Path resourceRoot(Path root) throws IOException {
		Path banks = Files.createDirectories(root.resolve(Path.of("etc", "bankdefs", "hipo4")));
		Files.writeString(banks.resolve("header.json"), "{}");
		return root;
	}
}
