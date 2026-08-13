package edu.cnu.ced.geometry.cache;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** A detector geometry with an explicit, versioned primitive cache payload. */
public interface CacheableGeometry {

	String name();

	int formatVersion();

	void initializeFromSource();

	void read(DataInput input) throws IOException;

	void write(DataOutput output) throws IOException;
}
