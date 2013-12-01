package edu.berkeley.mapping.test.utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;

/**
 *
 * @author Gustavo
 */
public class CoordinateTranslationFilter implements CoordinateFilter{
	Coordinate variation;

	public CoordinateTranslationFilter(Coordinate variation) {
		this.variation = variation;
	}

	public Coordinate getVariation() {
		return variation;
	}

	public void setVariation(Coordinate variation) {
		this.variation = variation;
	}

	@Override
	public void filter(Coordinate coord) {
		if(variation != null){
			coord.x += variation.x;
			coord.y += variation.y;
			coord.z += variation.z;
		}
	}
	
	
}
