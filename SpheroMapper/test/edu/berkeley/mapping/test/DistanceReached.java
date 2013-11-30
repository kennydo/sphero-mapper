/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.berkeley.mapping.test;

import com.vividsolutions.jts.geom.Geometry;
import edu.berkeley.mapping.Mapper;
import edu.berkeley.mapping.MappingEvent;
import edu.berkeley.mapping.test.utils.EmptyCommander;
import edu.berkeley.mapping.test.utils.Renderer;

/**
 *
 * @author Gustavo
 */
public class DistanceReached {
	public static void main(String[] args) {
		Mapper mapper = new Mapper(new EmptyCommander());
		mapper.reportEvent(new MappingEvent(MappingEvent.Type.START, 50, 50));
		mapper.reportEvent(new MappingEvent(MappingEvent.Type.DISTANCE_REACHED, 100, 100));
		mapper.reportEvent(new MappingEvent(MappingEvent.Type.DISTANCE_REACHED, 150, 150));
		Geometry freeGeometry = mapper.getFreeGeometry();
		System.out.println(freeGeometry.toText());
		Renderer renderer = new Renderer(freeGeometry);
	}
}
