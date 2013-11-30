/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.berkeley.mapping.test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Shape;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Gustavo
 */
public class Renderer extends JFrame{
	CoordinateFilter scaleFilter = new CoordinateFilter() {

		@Override
		public void filter(Coordinate coord) {
			coord.x *= scale;
			coord.y *= scale;
		}
	};
	
	private double scale = 1;
	
	private ArrayList<RendererShape> shapes = new ArrayList<>();

	public Renderer() throws HeadlessException {
		setContentPane(new ContentPanel());
		setBounds(0, 0, 500, 500);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
	}

	
	
	public Renderer(Shape shape) throws HeadlessException {
		this();
		addShape(shape);
	}
	
	public void addShape(Shape shape){
		shapes.add(new RendererShape(shape));
		repaint();
	}

	public void setScale(double scale) {
		this.scale = scale;
	}
	
	private class ContentPanel extends JPanel{

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			for (RendererShape rendererShape : shapes) {
				g2d.setColor(rendererShape.getStrokeColor());
				g2d.draw(rendererShape.getShape());
				
				g2d.setColor(rendererShape.getFillColor());
				g2d.fill(rendererShape.getShape());
			}
		}
	}
}
