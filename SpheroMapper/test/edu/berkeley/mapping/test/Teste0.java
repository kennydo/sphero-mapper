package edu.berkeley.mapping.test;


import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import java.awt.Shape;

/**
 *
 * @author Gustavo
 */
public class Teste0 {
	public static void main(String[] args) {
		GeometryFactory f = new GeometryFactory();
		
		Polygon p = f.createPolygon(new Coordinate[]{new Coordinate(1, 1), new Coordinate(2, 4), new Coordinate(4, 0), new Coordinate(1, 1)});
		
		p.apply(new CoordinateFilter() {

			@Override
			public void filter(Coordinate coord) {
				coord.x *= 20;
				coord.y *= 20;
			}
		});
		
		ShapeWriter shapeWriter = new ShapeWriter();
		Shape shape = shapeWriter.toShape(p);
		Renderer renderer = new Renderer(shape);
		renderer.setScale(20);
		renderer.repaint();
	}
}
