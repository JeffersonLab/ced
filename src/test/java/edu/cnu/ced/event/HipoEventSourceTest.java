package edu.cnu.ced.event;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HipoEventSourceTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void rejectsMissingAndNonHipoFilesBeforeInvokingCoatjava() throws Exception {
		assertThrows(IllegalArgumentException.class,
				() -> HipoEventSource.open(temporaryDirectory.resolve("missing.hipo")));

		Path text = Files.writeString(temporaryDirectory.resolve("event.txt"), "not hipo");
		assertThrows(IllegalArgumentException.class, () -> HipoEventSource.open(text));
	}
}
