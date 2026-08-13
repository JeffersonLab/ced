package edu.cnu.ced.event;

import java.util.Objects;

import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataSource;

/** Adapts a coatjava {@link DataSource} to the CED event-source boundary. */
final class CoatDataSourceAdapter implements EventSource {

	private final DataSource source;
	private final String description;
	private int index = -1;

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
		return index;
	}

	@Override
	public boolean hasNext() {
		return source.hasEvent();
	}

	@Override
	public DataEvent next() {
		DataEvent event = source.getNextEvent();
		if (event != null) {
			index++;
		}
		return event;
	}

	@Override
	public DataEvent previous() {
		return goTo(index - 1);
	}

	@Override
	public DataEvent goTo(int zeroBasedIndex) {
		DataEvent event = source.gotoEvent(zeroBasedIndex);
		if (event != null) {
			index = zeroBasedIndex;
		}
		return event;
	}

	@Override
	public void close() {
		source.close();
	}
}
