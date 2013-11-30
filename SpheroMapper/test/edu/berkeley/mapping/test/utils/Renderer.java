package edu.berkeley.mapping.test.utils;

import com.vividsolutions.jts.geom.Geometry;
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
	
	private ShapeFactory shapeFactory = new ShapeFactory();
	
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
	
	public Renderer(Geometry geometry) throws HeadlessException {
		this();
		addGeometry(geometry);
	}
	
	
	
	public void addShape(Shape shape){
		shapes.add(new RendererShape(shape));
		repaint();
	}
	
	public void addGeometry(Geometry geometry){
		addShape(shapeFactory.fromGeometry(geometry));
	}

	public ShapeFactory getShapeFactory() {
		return shapeFactory;
	}
	
	private class ContentPanel extends JPanel{

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			for (RendererShape rendererShape : shapes) {
				
				System.out.println(rendererShape.getShape().getBounds2D().getCenterX());
				System.out.println(rendererShape.getShape().getBounds2D().getCenterY());
				
				g2d.setColor(rendererShape.getStrokeColor());
				g2d.draw(rendererShape.getShape());
				
				g2d.setColor(rendererShape.getFillColor());
				g2d.fill(rendererShape.getShape());
			}
		}
	}
}
