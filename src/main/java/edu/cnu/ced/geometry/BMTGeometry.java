package edu.cnu.ced.geometry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.calib.utils.DatabaseConstantProvider;

import edu.cnu.ced.geometry.cache.CacheableGeometry;

/** Immutable CCDB geometry constants for the six-layer Barrel Micromegas Tracker. */
public final class BMTGeometry implements CacheableGeometry {

	public static final int LAYER_COUNT = 6;
	private static final String ROOT = "/geometry/cvt/mvt/";
	private static final int[] PITCH_LAYERS = {1, 4, 6};

	public record Layer(int number, int region, int axis, int stripCount, double radiusMm,
			double zMinMm, double zMaxMm, double phiMinDeg, double phiMaxDeg,
			double stripPitchMm) { }

	public record PitchGroup(int stripCount, double pitchMm) { }

	private volatile List<Layer> layers = List.of();
	private volatile List<List<PitchGroup>> pitchGroups = List.of();

	@Override
	public String name() {
		return "BMT";
	}

	@Override
	public int formatVersion() {
		return 1;
	}

	@Override
	public void initializeFromSource() {
		initializeFromSource("default");
	}

	@Override
	public void initializeFromSource(String variation) {
		DatabaseConstantProvider provider = new DatabaseConstantProvider(11, variation);
		provider.loadTable(ROOT + "bmt_layer");
		for (int layer = 1; layer <= LAYER_COUNT; layer++) {
			provider.loadTable(ROOT + "bmt_strip_L" + layer);
		}

		ArrayList<Layer> loadedLayers = new ArrayList<>();
		for (int row = 0; row < provider.length(ROOT + "bmt_layer/Layer"); row++) {
			int number = provider.getInteger(ROOT + "bmt_layer/Layer", row);
			loadedLayers.add(new Layer(number, (number + 1) / 2,
					provider.getInteger(ROOT + "bmt_layer/Axis", row),
					provider.getInteger(ROOT + "bmt_layer/Nstrip", row),
					provider.getDouble(ROOT + "bmt_layer/Radius", row),
					provider.getDouble(ROOT + "bmt_layer/Zmin", row),
					provider.getDouble(ROOT + "bmt_layer/Zmax", row),
					provider.getDouble(ROOT + "bmt_layer/Phi_min", row),
					provider.getDouble(ROOT + "bmt_layer/Phi_max", row),
					firstPitch(provider, number)));
		}

		ArrayList<List<PitchGroup>> loadedGroups = new ArrayList<>();
		for (int layer : PITCH_LAYERS) {
			String table = ROOT + "bmt_strip_L" + layer;
			ArrayList<PitchGroup> groups = new ArrayList<>();
			for (int row = 0; row < provider.length(table + "/Group_size"); row++) {
				groups.add(new PitchGroup(provider.getInteger(table + "/Group_size", row),
						provider.getDouble(table + "/Pitch", row)));
			}
			loadedGroups.add(List.copyOf(groups));
		}
		provider.disconnect();
		publish(loadedLayers, loadedGroups);
	}

	public Layer layer(int layer) {
		ensureInitialized();
		if (layer < 1 || layer > LAYER_COUNT) {
			throw new IllegalArgumentException("Invalid BMT layer: " + layer);
		}
		return layers.get(layer - 1);
	}

	/** Variable-pitch groups for regions 1..3 (CCDB layers 1, 4, and 6). */
	public List<PitchGroup> pitchGroups(int region) {
		ensureInitialized();
		if (region < 1 || region > 3) {
			throw new IllegalArgumentException("Invalid BMT region: " + region);
		}
		return pitchGroups.get(region - 1);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		ensureInitialized();
		output.writeInt(layers.size());
		for (Layer layer : layers) {
			output.writeInt(layer.number());
			output.writeInt(layer.region());
			output.writeInt(layer.axis());
			output.writeInt(layer.stripCount());
			output.writeDouble(layer.radiusMm());
			output.writeDouble(layer.zMinMm());
			output.writeDouble(layer.zMaxMm());
			output.writeDouble(layer.phiMinDeg());
			output.writeDouble(layer.phiMaxDeg());
			output.writeDouble(layer.stripPitchMm());
		}
		output.writeInt(pitchGroups.size());
		for (List<PitchGroup> groups : pitchGroups) {
			output.writeInt(groups.size());
			for (PitchGroup group : groups) {
				output.writeInt(group.stripCount());
				output.writeDouble(group.pitchMm());
			}
		}
	}

	@Override
	public void read(DataInput input) throws IOException {
		int layerCount = input.readInt();
		if (layerCount != LAYER_COUNT) {
			throw new IOException("Invalid BMT layer count: " + layerCount);
		}
		ArrayList<Layer> restoredLayers = new ArrayList<>();
		for (int index = 0; index < layerCount; index++) {
			restoredLayers.add(new Layer(input.readInt(), input.readInt(), input.readInt(), input.readInt(),
					input.readDouble(), input.readDouble(), input.readDouble(), input.readDouble(),
					input.readDouble(), input.readDouble()));
		}
		int regionCount = input.readInt();
		if (regionCount != 3) {
			throw new IOException("Invalid BMT pitch-region count: " + regionCount);
		}
		ArrayList<List<PitchGroup>> restoredGroups = new ArrayList<>();
		for (int region = 0; region < regionCount; region++) {
			int count = input.readInt();
			if (count < 0 || count > 10_000) {
				throw new IOException("Invalid BMT pitch-group count: " + count);
			}
			ArrayList<PitchGroup> groups = new ArrayList<>();
			for (int index = 0; index < count; index++) {
				groups.add(new PitchGroup(input.readInt(), input.readDouble()));
			}
			restoredGroups.add(List.copyOf(groups));
		}
		publish(restoredLayers, restoredGroups);
	}

	private static double firstPitch(DatabaseConstantProvider provider, int layer) {
		return provider.getDouble(ROOT + "bmt_strip_L" + layer + "/Pitch", 0);
	}

	private void publish(List<Layer> newLayers, List<List<PitchGroup>> newGroups) {
		if (newLayers.size() != LAYER_COUNT || newGroups.size() != 3) {
			throw new IllegalStateException("Invalid BMT geometry dimensions");
		}
		layers = List.copyOf(newLayers);
		pitchGroups = List.copyOf(newGroups);
	}

	private void ensureInitialized() {
		if (layers.size() != LAYER_COUNT || pitchGroups.size() != 3) {
			throw new IllegalStateException("BMT geometry is not initialized");
		}
	}
}
