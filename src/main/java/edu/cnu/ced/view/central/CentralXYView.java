package edu.cnu.ced.view.central;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cnu.ced.component.CedDisplayOption;
import edu.cnu.ced.data.CentralAccumulation;
import edu.cnu.ced.data.CentralEventData.AdcHit;
import edu.cnu.ced.data.CentralEventData.Detector;
import edu.cnu.ced.data.CentralEventData.ReconHit;
import edu.cnu.ced.data.ReconstructedTracks;
import edu.cnu.ced.data.TrackRow;
import edu.cnu.ced.event.EventNavigationState;
import edu.cnu.ced.event.EventNavigator;
import edu.cnu.ced.geometry.BMTGeometry;
import edu.cnu.ced.geometry.BSTGeometry;
import edu.cnu.ced.geometry.CNDGeometry;
import edu.cnu.ced.geometry.CTOFGeometry;
import edu.cnu.ced.geometry.Point3;
import edu.cnu.ced.swim.SwimTrajectoryCache;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Transverse display of the silicon, micromegas, CND, and CTOF detectors --
 * {@link CndCtofXYView} (CND + CTOF, shared with {@code AlertXYView}) plus
 * BST + BMT.
 */
@SuppressWarnings("serial")
public final class CentralXYView extends CndCtofXYView {
	private static final Color EMPTY = new Color(245,248,248);
	private static final Color BST_OUTLINE = new Color(95,175,175);
	private static final Color BMT_C = new Color(220,255,220);
	private static final Color BMT_Z = new Color(242,242,242);
	private static final Color BMT_ADC_PANEL = new Color(210,180,140);
	private static final int MARKER = 5;
	private static final Color BST_LABEL = new Color(25, 115, 45, 155);
	private static final Color BMT_LABEL = new Color(35, 75, 145, 145);
	private static final Color RECON_COLOR = new Color(225, 35, 25);
	private final BSTGeometry bst; private final BMTGeometry bmt;
	private final List<ScreenTrack> screenCvtTracks=new ArrayList<>();
	private volatile List<TrackRow> cvtTracks=List.of();

	public CentralXYView(BSTGeometry bst,BMTGeometry bmt,CNDGeometry cnd,CTOFGeometry ctof,
			EventNavigator navigator,CentralAccumulation accumulation,SwimTrajectoryCache swimCache){
		super(cnd,ctof,navigator,accumulation,swimCache,
				PropertyUtils.TITLE,"Central XY",PropertyUtils.WIDTH,860,PropertyUtils.HEIGHT,760,
				PropertyUtils.WORLDSYSTEM,new Rectangle2D.Double(40,-40,-80,80),PropertyUtils.BACKGROUND,Color.WHITE,
				PropertyUtils.TOOLBARBITS,ToolBits.NAVIGATIONTOOLS,PropertyUtils.WHEELZOOM,true,PropertyUtils.VISIBLE,true);
		this.bst=bst;this.bmt=bmt;
		setAfterDraw(this::draw); initializeCedView(EnumSet.of(CedDisplayOption.SINGLE_EVENT,CedDisplayOption.ACCUMULATION,
				CedDisplayOption.RAW_DATA,CedDisplayOption.RECON_HITS,CedDisplayOption.CLUSTERS,CedDisplayOption.CROSSES,
				CedDisplayOption.CONNECT_CLUSTER_ENDPOINTS,CedDisplayOption.RECON_TRACKS,
				CedDisplayOption.MC_TRACKS,CedDisplayOption.CVT_TRACKS),
				List.of("BST","BMT","CND","CTOF","CVT"),ScientificColorMap.TURBO,"Relative ADC / accumulation");
	}
	@Override protected void eventChanged(EventNavigationState state){super.eventChanged(state);cvtTracks=ReconstructedTracks.cvtTracks(state.snapshot());}

	private void draw(Graphics2D graphics,IContainer container){Graphics2D g=(Graphics2D)graphics.create();try{
		g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
		clearSharedDrawState();screenCvtTracks.clear();
		drawBST(g,container);drawBMT(g,container);drawCTOF(g,container);drawCND(g,container);
		if(!isDisplayed(CedDisplayOption.ACCUMULATION)){if(isDisplayed(CedDisplayOption.RECON_HITS))drawRecon(g,container);if(isDisplayed(CedDisplayOption.CLUSTERS))drawClusters(g,container);if(isDisplayed(CedDisplayOption.CROSSES))drawCrosses(g,container);if(isDisplayed(CedDisplayOption.RECON_TRACKS))drawParticles(g,container);if(isDisplayed(CedDisplayOption.MC_TRACKS))drawMcTracks(g,container);if(isDisplayed(CedDisplayOption.CVT_TRACKS))drawTrackRows(g,container,cvtTracks,screenCvtTracks);}
		drawXYAxes(g,container);
	}finally{g.dispose();}}

	private void drawBST(Graphics2D g,IContainer c){g.setStroke(new BasicStroke(2.2f));for(int layer=0;layer<BSTGeometry.LAYER_COUNT;layer++)for(int sector=0;sector<BSTGeometry.SECTORS_PER_LAYER[layer];sector++){
		Point3 a=bst.midpoint(sector,layer,0),b=bst.midpoint(sector,layer,BSTGeometry.STRIP_COUNT-1);Point p1=screen(c,a.x()/10,a.y()/10),p2=screen(c,b.x()/10,b.y()/10);
		g.setColor(BST_OUTLINE);g.drawLine(p1.x,p1.y,p2.x,p2.y);
	}
		drawBSTSectorLabels(g,c);
		if(isDisplayed(CedDisplayOption.ACCUMULATION)) drawAccumulatedMarkers(g,c,Detector.BST);
		else if(isDisplayed(CedDisplayOption.RAW_DATA)) drawBSTAdc(g,c);
	}
	private void drawBSTAdc(Graphics2D g,IContainer c){Map<PanelAddress,AdcHit> panels=new HashMap<>();for(AdcHit h:data.adcHits())if(h.detector()==Detector.BST&&validBST(h.sector(),h.layer(),h.component()))panels.merge(new PanelAddress(h.sector(),h.layer()),h,(a,b)->a.adc()>=b.adc()?a:b);for(var entry:panels.entrySet()){PanelAddress address=entry.getKey();AdcHit h=entry.getValue();Point3 a=bst.midpoint(address.sector()-1,address.layer()-1,0),b=bst.midpoint(address.sector()-1,address.layer()-1,BSTGeometry.STRIP_COUNT-1);Point p1=screen(c,a.x()/10,a.y()/10),p2=screen(c,b.x()/10,b.y()/10);g.setStroke(new BasicStroke(6f));g.setColor(Color.DARK_GRAY);g.drawLine(p1.x,p1.y,p2.x,p2.y);g.setStroke(new BasicStroke(4f));g.setColor(channelColor(h));g.drawLine(p1.x,p1.y,p2.x,p2.y);}for(AdcHit h:data.adcHits())if(h.detector()==Detector.BST&&validBST(h.sector(),h.layer(),h.component())){Point3 m=bst.midpoint(h.sector()-1,h.layer()-1,h.component()-1);markers.put(h,screen(c,m.x()/10,m.y()/10));}}
	private void drawBSTMarker(Graphics2D g,IContainer c,AdcHit h,boolean recon){if(!validBST(h.sector(),h.layer(),h.component()))return;Point3 m=bst.midpoint(h.sector()-1,h.layer()-1,h.component()-1);Point p=screen(c,m.x()/10,m.y()/10);markers.put(h,p);drawMarker(g,p,recon?Color.RED:channelColor(h),MARKER);}
	private void drawBSTAccumulatedMarker(Graphics2D g,IContainer c,AdcHit h,Color color){if(!validBST(h.sector(),h.layer(),h.component()))return;Point3 m=bst.midpoint(h.sector()-1,h.layer()-1,h.component()-1);drawMarker(g,screen(c,m.x()/10,m.y()/10),color,MARKER);}

	private void drawBMT(Graphics2D g,IContainer c){for(int layer=1;layer<=BMTGeometry.LAYER_COUNT;layer++){BMTGeometry.Layer info=bmt.layer(layer);for(int sector=0;sector<3;sector++){
			double start=info.phiMinDeg()+120*sector,extent=info.phiMaxDeg()-info.phiMinDeg();drawArc(g,c,info.radiusMm()/10,start,extent,info.axis()==0?BMT_C:BMT_Z);
		}}
		drawBMTSectorLabels(g,c);
		if(isDisplayed(CedDisplayOption.ACCUMULATION)) drawAccumulatedMarkers(g,c,Detector.BMT);
		else if(isDisplayed(CedDisplayOption.RAW_DATA)) drawBMTAdc(g,c);
	}
	private void drawBMTAdc(Graphics2D g,IContainer c){Map<PanelAddress,AdcHit> panels=new HashMap<>();for(AdcHit h:data.adcHits())if(h.detector()==Detector.BMT&&h.layer()>=1&&h.layer()<=BMTGeometry.LAYER_COUNT)panels.putIfAbsent(new PanelAddress(h.sector(),h.layer()),h);for(PanelAddress address:panels.keySet()){BMTGeometry.Layer info=bmt.layer(address.layer());double start=info.phiMinDeg()+120*bmtSector(address.sector()),extent=info.phiMaxDeg()-info.phiMinDeg();drawArcStroke(g,c,info.radiusMm()/10,start,extent,BMT_ADC_PANEL,7f);drawArcStroke(g,c,info.radiusMm()/10,start,extent,Color.RED,1.5f);}for(AdcHit h:data.adcHits())if(h.detector()==Detector.BMT)drawBMTMarker(g,c,h,false);}
	private void drawBMTMarker(Graphics2D g,IContainer c,AdcHit h,boolean recon){if(h.layer()<1||h.layer()>6)return;BMTGeometry.Layer info=bmt.layer(h.layer());if(info.axis()!=1)return;double phi=bmtStripPhi(info,h.sector(),h.component());Point p=screen(c,info.radiusMm()/10*Math.cos(Math.toRadians(phi)),info.radiusMm()/10*Math.sin(Math.toRadians(phi)));markers.put(h,p);drawMarker(g,p,recon?Color.RED:channelColor(h),MARKER);}

	private void drawBSTSectorLabels(Graphics2D g,IContainer c){
		for(int layer=1;layer<BSTGeometry.LAYER_COUNT;layer+=2){
			for(int sector=0;sector<BSTGeometry.SECTORS_PER_LAYER[layer];sector++){
				Point3 a=bst.midpoint(sector,layer,0),b=bst.midpoint(sector,layer,BSTGeometry.STRIP_COUNT-1);
				double x=(a.x()+b.x())/20,y=(a.y()+b.y())/20,r=Math.hypot(x,y);
				if(r>0){x+=1.0*x/r;y+=1.0*y/r;}
				drawCenteredLabel(g,screen(c,x,y),Integer.toString(sector+1),BST_LABEL);
			}
		}
	}
	private void drawBMTSectorLabels(Graphics2D g,IContainer c){
		BMTGeometry.Layer outer=bmt.layer(BMTGeometry.LAYER_COUNT);
		double extent=outer.phiMaxDeg()-outer.phiMinDeg(),radius=outer.radiusMm()/10+1.0;
		for(int sector=1;sector<=3;sector++){
			double phi=outer.phiMinDeg()+120*bmtSector(sector)+extent/2;
			drawCenteredLabel(g,screen(c,radius*Math.cos(Math.toRadians(phi)),radius*Math.sin(Math.toRadians(phi))),Integer.toString(sector),BMT_LABEL);
		}
	}

	private Color channelColor(AdcHit h){int max=maximumAdc(h.detector());if(!isDisplayed(CedDisplayOption.RAW_DATA)||max==0)return EMPTY;return ScientificColorMap.TURBO.colorAt((double)h.adc()/max);}
	private void drawAccumulatedMarkers(Graphics2D g,IContainer c,Detector detector){int max=accumulation.maximumCount();if(max==0)return;for(var entry:accumulation.counts().entrySet()){var h=entry.getKey();if(h.detector()!=detector)continue;Color color=ScientificColorMap.TURBO.colorAt((double)entry.getValue()/max);AdcHit proxy=new AdcHit(detector,h.sector(),h.layer(),h.component(),h.order(),1,Float.NaN);if(detector==Detector.BST)drawBSTAccumulatedMarker(g,c,proxy,color);else drawBMTAccumulatedMarker(g,c,proxy,color);}}
	private void drawBMTAccumulatedMarker(Graphics2D g,IContainer c,AdcHit h,Color color){if(h.layer()<1||h.layer()>6)return;BMTGeometry.Layer info=bmt.layer(h.layer());double f=Math.max(0,Math.min(1,(h.component()-.5)/Math.max(1,info.stripCount())));double phi=info.phiMinDeg()+120*bmtSector(h.sector())+f*(info.phiMaxDeg()-info.phiMinDeg());drawMarker(g,screen(c,info.radiusMm()/10*Math.cos(Math.toRadians(phi)),info.radiusMm()/10*Math.sin(Math.toRadians(phi))),color,MARKER);}

	private void drawRecon(Graphics2D g,IContainer c){for(ReconHit h:data.reconHits()){Point p=null;if(h.detector()==Detector.BST&&validBST(h.sector(),h.layer(),h.strip())){Point3 m=bst.midpoint(h.sector()-1,h.layer()-1,h.strip()-1);p=screen(c,m.x()/10,m.y()/10);}else if(h.detector()==Detector.BMT&&h.layer()>=1&&h.layer()<=6){BMTGeometry.Layer info=bmt.layer(h.layer());double f=Math.max(0,Math.min(1,(h.strip()-.5)/Math.max(1,info.stripCount())));double phi=info.phiMinDeg()+120*bmtSector(h.sector())+f*(info.phiMaxDeg()-info.phiMinDeg());p=screen(c,info.radiusMm()/10*Math.cos(Math.toRadians(phi)),info.radiusMm()/10*Math.sin(Math.toRadians(phi)));}if(p!=null){markers.put(h,p);drawReconMarker(g,p);}}}

	@Override public void getFeedbackStrings(IContainer c,Point sp,Point2D.Double wp,List<String> feedback){super.getFeedbackStrings(c,sp,wp,feedback);boolean found=addBarrelPolygonFeedback(sp,feedback);if(!found)found=addBSTGeometryFeedback(c,sp,feedback);if(!found)addBMTGeometryFeedback(wp,feedback);addMarkersAndTrackFeedback(sp,feedback);for(ScreenTrack drawn:screenCvtTracks)if(nearAnySegment(drawn.points(),sp,5.0)){addTrackFeedback(drawn.track(),"CVT","dark green",feedback);break;}}
	private boolean addBSTGeometryFeedback(IContainer c,Point sp,List<String> feedback){double best=Double.POSITIVE_INFINITY;PanelAddress closest=null;for(int layer=0;layer<BSTGeometry.LAYER_COUNT;layer++)for(int sector=0;sector<BSTGeometry.SECTORS_PER_LAYER[layer];sector++){Point3 a=bst.midpoint(sector,layer,0),b=bst.midpoint(sector,layer,BSTGeometry.STRIP_COUNT-1);Point p1=screen(c,a.x()/10,a.y()/10),p2=screen(c,b.x()/10,b.y()/10);double d=Line2D.ptSegDist(p1.x,p1.y,p2.x,p2.y,sp.x,sp.y);if(d<best){best=d;closest=new PanelAddress(sector+1,layer+1);}}if(best>7||closest==null)return false;feedback.add("$red$BST layer "+closest.layer());feedback.add("$red$BST region "+((closest.layer()+1)/2));feedback.add("$red$BST sector "+closest.sector());for(AdcHit h:data.adcHits())if(h.detector()==Detector.BST&&h.sector()==closest.sector()&&h.layer()==closest.layer())feedback.add(String.format("$cyan$BST strip %d adc %d time %.3f order %d",h.component(),h.adc(),h.time(),h.order()));return true;}
	private boolean addBMTGeometryFeedback(Point2D.Double wp,List<String> feedback){double radius=Math.hypot(wp.x,wp.y),phi=normalizeDegrees(Math.toDegrees(Math.atan2(wp.y,wp.x)));for(int layer=1;layer<=BMTGeometry.LAYER_COUNT;layer++){BMTGeometry.Layer info=bmt.layer(layer);if(Math.abs(radius-info.radiusMm()/10)>.55)continue;for(int sector=1;sector<=3;sector++){double start=normalizeDegrees(info.phiMinDeg()+120*bmtSector(sector));if(!angleContains(phi,start,info.phiMaxDeg()-info.phiMinDeg()))continue;feedback.add("$green$BMT type "+(info.axis()==1?"Z":"C"));feedback.add("$green$BMT sector "+sector+" layer "+layer);feedback.add("$green$BMT region "+info.region());feedback.add(String.format("$green$radius %.3f cm",info.radiusMm()/10));if(info.axis()==1){double relative=normalizeDegrees(phi-start);int strip=(int)Math.floor(relative/Math.max(.0001,info.phiMaxDeg()-info.phiMinDeg())*info.stripCount())+1;feedback.add("$green$BMT strip "+Math.max(1,Math.min(info.stripCount(),strip)));}return true;}}return false;}

	private static void drawReconMarker(Graphics2D g,Point p){g.setStroke(new BasicStroke(1.5f));g.setColor(RECON_COLOR);g.drawLine(p.x-6,p.y-6,p.x+6,p.y+6);g.drawLine(p.x-6,p.y+6,p.x+6,p.y-6);g.fillRect(p.x-3,p.y-3,7,7);g.setColor(Color.DARK_GRAY);g.drawRect(p.x-3,p.y-3,7,7);}
	private static boolean validBST(int sector,int layer,int strip){return layer>=1&&layer<=6&&sector>=1&&sector<=BSTGeometry.SECTORS_PER_LAYER[layer-1]&&strip>=1&&strip<=256;}
	private static int bmtSector(int dataSector){return dataSector==2?0:dataSector==1?1:2;}
	private static double normalizeDegrees(double degrees){double d=degrees%360;return d<0?d+360:d;}
	private static boolean angleContains(double angle,double start,double extent){double delta=normalizeDegrees(angle-start);return delta<=extent;}
	private static double bmtStripPhi(BMTGeometry.Layer info,int sector,int component){double f=Math.max(0,Math.min(1,(component-.5)/Math.max(1,info.stripCount())));return info.phiMinDeg()+120*bmtSector(sector)+f*(info.phiMaxDeg()-info.phiMinDeg());}
	private static Arc2D arc(IContainer c,double radius,double start,double extent){Point center=screen(c,0,0),edge=screen(c,radius,0);double rp=Math.abs(edge.x-center.x);return new Arc2D.Double(center.x-rp,center.y-rp,2*rp,2*rp,start,extent,Arc2D.OPEN);}
	private static void drawArcStroke(Graphics2D g,IContainer c,double radius,double start,double extent,Color color,float width){g.setColor(color);g.setStroke(new BasicStroke(width));g.draw(arc(c,radius,start,extent));}
	private static void drawArc(Graphics2D g,IContainer c,double radius,double start,double extent,Color color){drawArcStroke(g,c,radius,start,extent,color,4f);drawArcStroke(g,c,radius,start,extent,Color.DARK_GRAY,1f);}
	private record PanelAddress(int sector,int layer){}
}
