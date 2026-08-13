package edu.cnu.ced.geometry;
import static org.junit.jupiter.api.Assertions.*;import java.io.*;import org.junit.jupiter.api.Test;
class ECGeometryTest{
 @Test void sourceProvidesBothStacks(){ECGeometry g=new ECGeometry();g.initializeFromSource();assertEquals(3,g.viewTriangle(6,ECGeometry.OUTER,2).size());assertEquals(8,g.stripVertices(6,ECGeometry.INNER,0,36).size());assertEquals(4,g.stack(ECGeometry.OUTER).edges().get(2).get(35).size());assertTrue(Double.isFinite(g.zFromX(ECGeometry.INNER,0)));}
 @Test void payloadRoundTrips()throws Exception{ECGeometry a=new ECGeometry();a.initializeFromSource();ByteArrayOutputStream b=new ByteArrayOutputStream();a.write(new DataOutputStream(b));ECGeometry c=new ECGeometry();c.read(new DataInputStream(new ByteArrayInputStream(b.toByteArray())));assertEquals(a.stack(0).reference(),c.stack(0).reference());assertEquals(a.viewTriangle(6,1,2),c.viewTriangle(6,1,2));assertEquals(a.stripVertices(4,0,1,18),c.stripVertices(4,0,1,18));assertEquals(a.stack(1).localToSector().apply(new Point3(1,2,3)),c.stack(1).localToSector().apply(new Point3(1,2,3)));}
}
