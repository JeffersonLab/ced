package edu.cnu.ced.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.junit.jupiter.api.Test;

class PCALGeometryTest {
	@Test void sourceGeometryProvidesAllViewsAndSectors(){PCALGeometry g=new PCALGeometry();g.initializeFromSource();assertEquals(4,g.localFace(0,68).size());assertEquals(4,g.projectionEdges(1,62).size());assertEquals(3,g.viewTriangle(6,2).size());assertEquals(8,g.stripVertices(6,2,62).size());assertTrue(Double.isFinite(g.zFromX(0)));}
	@Test void payloadRestoresRepresentativeGeometryExactly()throws Exception{PCALGeometry source=new PCALGeometry();source.initializeFromSource();ByteArrayOutputStream bytes=new ByteArrayOutputStream();source.write(new DataOutputStream(bytes));PCALGeometry restored=new PCALGeometry();restored.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));assertEquals(source.reference(),restored.reference());assertEquals(source.localFace(0,1),restored.localFace(0,1));assertEquals(source.projectionEdges(2,62),restored.projectionEdges(2,62));assertEquals(source.viewTriangle(6,2),restored.viewTriangle(6,2));assertEquals(source.stripVertices(4,1,31),restored.stripVertices(4,1,31));assertEquals(source.localToSector(new Point3(1,2,3)),restored.localToSector(new Point3(1,2,3)));}
}
