/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.berkeley.mapping.test;

import java.awt.Color;
import java.awt.Shape;

/**
 *
 * @author Gustavo
 */
public class RendererShape {
	private Shape shape;
	
	private Color fillColor = Color.BLUE;
	private Color strokeColor = Color.BLUE;

	public RendererShape(Shape shape) {
		this.shape = shape;
	}

	public RendererShape(Shape shape, Color fill, Color stroke) {
		this(shape);
		this.fillColor = fill;
		this.strokeColor = stroke;
	}

	public Color getFillColor() {
		return fillColor;
	}

	public void setFillColor(Color fillColor) {
		this.fillColor = fillColor;
	}

	public Color getStrokeColor() {
		return strokeColor;
	}

	public void setStrokeColor(Color strokeColor) {
		this.strokeColor = strokeColor;
	}

	public Shape getShape() {
		return shape;
	}
	
}
