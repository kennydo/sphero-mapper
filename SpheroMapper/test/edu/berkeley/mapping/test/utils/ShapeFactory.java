package edu.berkeley.mapping.test.utils;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import java.awt.Point;
import java.awt.Shape;

/**
 *
 * @author Gustavo
 */
public class ShapeFactory {
	
	CoordinateScaleFilter coordinateScaleFilter = new CoordinateScaleFilter(1);

	CoordinateTranslationFilter coordinateTranslationFilter = new CoordinateTranslationFilter(new Coordinate(0, 0));
	
	private final ShapeWriter shapeWriter = new ShapeWriter();
		
	public void setScale(double scale) {
		coordinateScaleFilter.setScale(scale);
	}
	
	public void setTranslation(double x, double y){
		coordinateTranslationFilter.setVariation(new Coordinate(x, y));
	}
	
	/**
	 * Converts a JTS Coordinate to a AWT Point.
	 * @param coordinate
	 * @return The AWT Point
	 */
	public Point coordinateToPoint(Coordinate coordinate){
		Coordinate clone = (Coordinate) coordinate.clone();
		coordinateScaleFilter.filter(clone);
		coordinateTranslationFilter.filter(clone);
		return new Point((int) clone.x, (int) clone.y);
	}
	
	public Shape fromGeometry(Geometry geometry){
		Geometry clone = (Geometry) geometry.clone();
		clone.apply(coordinateScaleFilter);
		clone.apply(coordinateTranslationFilter);
		return shapeWriter.toShape(clone);
	}
	
	public CoordinateScaleFilter getCoordinateScaleFilter() {
		return coordinateScaleFilter;
	}
}
