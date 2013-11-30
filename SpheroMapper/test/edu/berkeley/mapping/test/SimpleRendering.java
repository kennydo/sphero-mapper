package edu.berkeley.mapping.test;


import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import edu.berkeley.mapping.test.utils.Renderer;
import java.awt.Shape;

/**
 *
 * @author Gustavo
 */
public class SimpleRendering {
	public static void main(String[] args) {
		GeometryFactory f = new GeometryFactory();
		
		Polygon p = f.createPolygon(new Coordinate[]{new Coordinate(10, 10), new Coordinate(20, 40), new Coordinate(40, 0), new Coordinate(10, 10)});
		
		Renderer renderer = new Renderer(p);
	}
}
