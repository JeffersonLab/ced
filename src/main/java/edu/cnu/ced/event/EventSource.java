package edu.cnu.ced.event;

import org.jlab.io.base.DataEvent;

/** Sequential/random-access source consumed by the CED event navigator. */
public interface EventSource extends AutoCloseable {

	String description();

	int size();

	int index();

	boolean hasNext();

	DataEvent next();

	DataEvent previous();

	DataEvent goTo(int zeroBasedIndex);

	@Override
	void close();
}
