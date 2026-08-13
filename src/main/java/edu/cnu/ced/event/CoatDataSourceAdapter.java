package edu.cnu.ced.event;

import java.util.Objects;

import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataSource;

/** Adapts a coatjava {@link DataSource} to the CED event-source boundary. */
final class CoatDataSourceAdapter implements EventSource {

	private final DataSource source;
	private final String description;

	CoatDataSourceAdapter(DataSource source, String description) {
		this.source = Objects.requireNonNull(source, "source");
		this.description = Objects.requireNonNull(description, "description");
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public int size() {
		return source.getSize();
	}

	@Override
	public int index() {
		return source.getCurrentIndex();
	}

	@Override
	public boolean hasNext() {
		return source.hasEvent();
	}

	@Override
	public DataEvent next() {
		return source.getNextEvent();
	}

	@Override
	public DataEvent previous() {
		return source.getPreviousEvent();
	}

	@Override
	public DataEvent goTo(int zeroBasedIndex) {
		return source.gotoEvent(zeroBasedIndex);
	}

	@Override
	public void close() {
		source.close();
	}
}
