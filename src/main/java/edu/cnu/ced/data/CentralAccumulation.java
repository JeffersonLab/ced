package edu.cnu.ced.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Per-channel central-detector occupancy accumulated across requested events. */
public final class CentralAccumulation {
	private final Map<Channel,Integer> counts=new HashMap<>(); private int events; private int maximum;
	public synchronized void add(CentralEventData data){if(data==null)return;events++;Set<Channel> seen=new HashSet<>();for(var h:data.adcHits()){Channel k=new Channel(h.detector(),h.sector(),h.layer(),h.component(),h.order());if(seen.add(k))maximum=Math.max(maximum,counts.merge(k,1,Integer::sum));}}
	public synchronized void clear(){counts.clear();events=0;maximum=0;}
	public synchronized int count(CentralEventData.AdcHit h){return counts.getOrDefault(new Channel(h.detector(),h.sector(),h.layer(),h.component(),h.order()),0);}
	public synchronized int count(CentralEventData.Detector d,int sector,int layer,int component,int order){return counts.getOrDefault(new Channel(d,sector,layer,component,order),0);}
	public synchronized Map<Channel,Integer> counts(){return Map.copyOf(counts);}
	public synchronized int eventCount(){return events;} public synchronized int maximumCount(){return maximum;}
	public record Channel(CentralEventData.Detector detector,int sector,int layer,int component,int order){}
}
