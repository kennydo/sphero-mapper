/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.berkeley.mapping.test.utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;

/**
 *
 * @author Gustavo
 */
public class CoordinateScaleFilter implements CoordinateFilter {
	double scaleX = 1;
	double scaleY = 1;
	double scaleZ = 1;

	public CoordinateScaleFilter(double scale) {
		this.scaleX = scale;
		this.scaleY = scale;
		this.scaleZ = scale;
	}

	public CoordinateScaleFilter(double scaleX, double scaleY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}
	
	public CoordinateScaleFilter(double scaleX, double scaleY, double scaleZ) {
		this(scaleX, scaleY);
		this.scaleZ = scaleZ;
	}

	public double getScaleX() {
		return scaleX;
	}

	public void setScaleX(double scaleX) {
		this.scaleX = scaleX;
	}

	public double getScaleY() {
		return scaleY;
	}

	public void setScaleY(double scaleY) {
		this.scaleY = scaleY;
	}

	public double getScaleZ() {
		return scaleZ;
	}

	public void setScaleZ(double scaleZ) {
		this.scaleZ = scaleZ;
	}

	@Override
	public void filter(Coordinate coord) {
		coord.x *= scaleX;
		coord.y *= scaleY;
		coord.z *= scaleZ;
	}

	public void setScale(double scale) {
		this.scaleX = scale;
		this.scaleY = scale;
		this.scaleZ = scale;
	}
}
