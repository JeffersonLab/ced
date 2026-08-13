package edu.cnu.ced.geometry;
import static org.junit.jupiter.api.Assertions.*;import java.io.*;import org.junit.jupiter.api.Test;
class BSTGeometryTest{
 @Test void sourceProvidesAllStripsAndPanels(){BSTGeometry g=new BSTGeometry();g.initializeFromSource("default");assertNotNull(g.strip(9,0,255));assertNotNull(g.strip(17,5,255));assertEquals(10,g.panelLimits(0,0).length);assertEquals(6,g.stripCm(0,0,0).length);assertThrows(IllegalArgumentException.class,()->g.strip(10,0,0));}
 @Test void payloadRoundTrips()throws Exception{BSTGeometry a=new BSTGeometry();a.initializeFromSource("default");ByteArrayOutputStream b=new ByteArrayOutputStream();a.write(new DataOutputStream(b));BSTGeometry c=new BSTGeometry();c.read(new DataInputStream(new ByteArrayInputStream(b.toByteArray())));assertEquals(a.strip(0,0,0),c.strip(0,0,0));assertEquals(a.strip(13,3,127),c.strip(13,3,127));assertEquals(a.strip(17,5,255),c.strip(17,5,255));assertArrayEquals(a.panelLimits(9,1),c.panelLimits(9,1));}
}
