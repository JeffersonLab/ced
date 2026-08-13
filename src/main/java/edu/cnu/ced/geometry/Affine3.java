package edu.cnu.ced.geometry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Transformation3D;

/** Immutable 3-by-4 affine transform. */
public final class Affine3 {
	private final double[][] coefficients;

	public Affine3(double[][] coefficients) { this.coefficients = copy(coefficients); }

	public Point3 apply(Point3 point) {
		double x = point.x(), y = point.y(), z = point.z();
		return new Point3(value(0, x, y, z), value(1, x, y, z), value(2, x, y, z));
	}

	public double[][] coefficients() { return copy(coefficients); }

	public void write(DataOutput output) throws IOException {
		for (double[] row : coefficients) for (double value : row) output.writeDouble(value);
	}

	public static Affine3 read(DataInput input) throws IOException {
		double[][] values = new double[3][4];
		for (int row = 0; row < 3; row++) for (int column = 0; column < 4; column++) values[row][column] = input.readDouble();
		return new Affine3(values);
	}

	public static Affine3 sample(Transformation3D transformation) {
		Point3D origin = transformed(transformation, 0, 0, 0);
		Point3D x = transformed(transformation, 1, 0, 0);
		Point3D y = transformed(transformation, 0, 1, 0);
		Point3D z = transformed(transformation, 0, 0, 1);
		return new Affine3(new double[][] {
			{ x.x() - origin.x(), y.x() - origin.x(), z.x() - origin.x(), origin.x() },
			{ x.y() - origin.y(), y.y() - origin.y(), z.y() - origin.y(), origin.y() },
			{ x.z() - origin.z(), y.z() - origin.z(), z.z() - origin.z(), origin.z() }
		});
	}

	private double value(int row, double x, double y, double z) {
		return coefficients[row][0] * x + coefficients[row][1] * y
				+ coefficients[row][2] * z + coefficients[row][3];
	}
	private static Point3D transformed(Transformation3D transform, double x, double y, double z) {
		Point3D point = new Point3D(x, y, z); transform.apply(point); return point;
	}
	private static double[][] copy(double[][] source) {
		if (source == null || source.length != 3) throw new IllegalArgumentException("Affine transform must have three rows");
		double[][] result = new double[3][4];
		for (int row = 0; row < 3; row++) {
			if (source[row] == null || source[row].length != 4) throw new IllegalArgumentException("Affine rows must have four values");
			System.arraycopy(source[row], 0, result[row], 0, 4);
		}
		return result;
	}
}
