/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.berkeley.mapping.test.utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import edu.berkeley.mapping.Mapper;
import edu.berkeley.mapping.MappingEvent;
import edu.berkeley.mapping.StateChangeListener;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Gustavo
 */
public class MapperRenderer extends JFrame{
	private final Mapper mapper;
	
	private final JFrame frame = this;
	
	private static final Rectangle defaultBounds = new Rectangle(500, 500);
	
	private final ShapeFactory shapeFactory = new ShapeFactory();
	
	private MappingEvent lastEvent;

	public MapperRenderer(Mapper mapper) {
		this.mapper = mapper;
		shapeFactory.getCoordinateScaleFilter().setScaleX(4);
		shapeFactory.getCoordinateScaleFilter().setScaleY(-4);
		setContentPane(new ContentPanel());
		setBounds(defaultBounds);
		setLocation(1000, 0);//TEMP
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
		mapper.addStateChangeListener(new MapperStateChangeListener());
	}
	
	private class ContentPanel extends JPanel{

		private final Color freeMapColor = new Color(0X7FFFFF00, true);
		private final Color objectMapColor = new Color(0X7F0000FF, true);
		
		public ContentPanel() {
			this.setBackground(Color.BLACK);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			Geometry freeGeometry = mapper.getFreeGeometry();
			Geometry objectsGeometry = mapper.getObjectsGeometry();
			/*Define bounds*/
			Rectangle bounds = defaultBounds;
			if(!freeGeometry.isEmpty()){
				Shape freeShape = shapeFactory.fromGeometry(freeGeometry);
				if(freeShape.getBounds().width > bounds.width){
					bounds.setSize(freeShape.getBounds().width, bounds.height);
				}
				if(freeShape.getBounds().height > bounds.height){
					bounds.setSize(bounds.width, freeShape.getBounds().height);
				}
			}
			if(!objectsGeometry.isEmpty()){
				Shape objectsShape = shapeFactory.fromGeometry(objectsGeometry);
				if(objectsShape.getBounds().width > bounds.width){
					bounds.setSize(objectsShape.getBounds().width, bounds.height);
				}
				if(objectsShape.getBounds().height > bounds.height){
					bounds.setSize(bounds.width, objectsShape.getBounds().height);
				}
			}
			frame.setBounds(bounds);
			shapeFactory.setTranslation(bounds.width/2, bounds.height/2);
			
			//Free geometry
			if(!freeGeometry.isEmpty()){
				Shape freeShape = shapeFactory.fromGeometry(freeGeometry);
				
				g2d.setColor(freeMapColor);
				g2d.fill(freeShape);
				
				g2d.setColor(Color.YELLOW);
				g2d.draw(freeShape);
				
			}
			
			//Free geometry
			if(!objectsGeometry.isEmpty()){
				Shape objectsShape = shapeFactory.fromGeometry(objectsGeometry);
				
				g2d.setColor(objectMapColor);
				g2d.fill(objectsShape);
				
				g2d.setColor(Color.BLUE);
				g2d.draw(objectsShape);
			}
			
			if(lastEvent != null){
				Point point = shapeFactory.coordinateToPoint(new Coordinate(lastEvent.getX(), lastEvent.getY()));
				Shape runnerShape = new Ellipse2D.Float(point.x-4, point.y-4, 8, 8);
				g2d.setColor(Color.RED);
				g2d.fill(runnerShape);
				lastEvent = null;
			}
			
		}
	}
	
	private class MapperStateChangeListener implements StateChangeListener{

		@Override
		public void onStateChange(Mapper.State oldState, Mapper.State newState, MappingEvent event) {
			lastEvent = event;
			repaint();
		}
		
	}
}
