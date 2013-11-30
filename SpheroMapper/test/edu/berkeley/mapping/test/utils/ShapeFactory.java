package edu.berkeley.mapping.test.utils;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import java.awt.Shape;

/**
 *
 * @author Gustavo
 */
public class ShapeFactory {
	CoordinateFilter scaleFilter = new CoordinateFilter() {

		@Override
		public void filter(Coordinate coord) {
			coord.x *= scale;
			coord.y *= scale;
		}
	};
	
	private final ShapeWriter shapeWriter = new ShapeWriter();
	
	private double scale = 1;
	
	public void setScale(double scale) {
		this.scale = scale;
	}
	
	public Shape fromGeometry(Geometry geometry){
		Geometry clone = (Geometry) geometry.clone();
		clone.apply(scaleFilter);
		return shapeWriter.toShape(clone);
	}
}
