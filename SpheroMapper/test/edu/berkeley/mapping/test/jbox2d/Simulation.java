/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.berkeley.mapping.test.jbox2d;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import edu.berkeley.mapping.Commander;
import edu.berkeley.mapping.Mapper;
import edu.berkeley.mapping.MappingEvent;
import edu.berkeley.mapping.test.utils.CoordinateScaleFilter;
import edu.berkeley.mapping.test.utils.MapperRenderer;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.WorldManifold;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.testbed.framework.TestbedController;
import org.jbox2d.testbed.framework.TestbedFrame;
import org.jbox2d.testbed.framework.TestbedModel;
import org.jbox2d.testbed.framework.TestbedPanel;
import org.jbox2d.testbed.framework.TestbedTest;
import org.jbox2d.testbed.framework.j2d.TestPanelJ2D;

/**
 *
 * @author Gustavo
 */
public class Simulation extends TestbedTest{
	
	Geometry environmentGeometry;

	Body runnerBody;
	
	Runner runner = new Runner();
	
	Mapper mapper = new Mapper(runner);
	
	MapperRenderer mapperRenderer = new MapperRenderer(mapper);
	
	DistanceObserver distanceObserver = new DistanceObserver();
	
	public Simulation(Geometry environmentGeometry) {
		this.environmentGeometry = environmentGeometry;
		mapperRenderer.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		distanceObserver.start();
	}
	
	@Override
	public void initTest(boolean deserialized) {
		getWorld().setGravity(new Vec2(0, 0));
		
		int l = environmentGeometry.getNumGeometries();
		for(int i = 0; i<l; i++){
			Geometry geometry = environmentGeometry.getGeometryN(i);
			Coordinate[] coordinates = geometry.getCoordinates();
			
			PolygonShape polygonShape = new PolygonShape();//jbox2d shape
			Vec2[] vertices = new Vec2[coordinates.length];
			for(int j = 0; j<vertices.length; j++ ){
				vertices[j] = new Vec2((float) coordinates[j].x, (float) coordinates[j].y);
			}
			polygonShape.set(vertices, vertices.length);

			BodyDef bodyDef = new BodyDef();
			bodyDef.type = BodyType.STATIC;
			Body body = getWorld().createBody(bodyDef);
			body.createFixture(polygonShape, 5.0f);
		}
		
		//Creating runner
		Shape runnerShape = new CircleShape();
		runnerShape.setRadius(0.6f);
		BodyDef runnerBodyDef = new BodyDef();
		runnerBodyDef.type = BodyType.DYNAMIC;
		Coordinate robotPosition = environmentGeometry.getEnvelopeInternal().centre();
		
		runnerBodyDef.position.set((float) robotPosition.x, (float) robotPosition.y);
		runnerBody = getWorld().createBody(runnerBodyDef);
		runnerBody.createFixture(runnerShape, 5.0f);
		
		mapper.getParameters().setRunnerWidth(runnerShape.getRadius()*2);
		mapper.getParameters().setCommanderDriveDistance(4);
		mapper.reportEvent(new MappingEvent(MappingEvent.Type.START, runnerBody.getPosition().x, runnerBody.getPosition().y));
		
		getWorld().setContactListener(new SimulationContactListener());
	}

	@Override
	public String getTestName() {
		return "Mapping simulation.";
	}
	
	private class Runner implements Commander{
		private float heading = 0;

		@Override
		public void drive(float headingVariation, float distance) {
			distanceObserver.observe(runnerBody.getPosition().x, runnerBody.getPosition().y, distance);
			drive(headingVariation);
		}

		@Override
		public void drive(float headingVariation) {
			System.out.println("Runner: drive(headingVariation="+headingVariation+")");
			heading = (heading + headingVariation)%360;
			if(heading < 0) heading = 360+heading;
			double rad = (heading/180f)*Math.PI;
			runnerBody.setLinearVelocity(new Vec2((float) Math.cos(rad)*5f, (float) Math.sin(rad)*5f));
		}

		@Override
		public void makeLeftSquare() {}

		@Override
		public void makeRightSquare() {}

		@Override
		public void stop() {}

		public float getHeading() {
			return heading;
		}

		public void setHeading(float heading) {
			this.heading = heading;
		}
	}
	
	private class SimulationContactListener implements ContactListener{
		@Override
		public void beginContact(Contact contact) {
			if(runnerBody.getContactList().contact == contact){
				
				distanceObserver.cancel();
				
				WorldManifold worldManifold = new WorldManifold();
				contact.getWorldManifold(worldManifold);
				
				Vec2 directionVector = worldManifold.points[0].sub(runnerBody.getPosition());
				
				runnerBody.setLinearVelocity(new Vec2(0, 0));
				runnerBody.setAngularVelocity(0);

				//TODO confirm angle orientation
				double angle = 180f*Math.atan2(directionVector.y, directionVector.x)/Math.PI;
				mapper.reportEvent(new MappingEvent(MappingEvent.Type.COLLISION, runnerBody.getPosition().x, runnerBody.getPosition().y, (float) angle));
			}
		}

		@Override
		public void endContact(Contact contact) {}

		@Override
		public void preSolve(Contact contact, Manifold oldManifold) {}

		@Override
		public void postSolve(Contact contact, ContactImpulse impulse) {}
	}
	
	private class DistanceObserver extends Thread{
		
		private final Coordinate initialPoint = new Coordinate();
		private final Coordinate currentPoint = new Coordinate();
		private double distance;
		
		private boolean cancel = false;
		
		private final Semaphore startSemaphore = new Semaphore(0);

		@Override
		public void  run() {
			synchronized(startSemaphore){
				try {
					startSemaphore.acquire();
				} catch (InterruptedException ex) {
					Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			while(true){
				try {
					synchronized(startSemaphore){
						if(cancel){
							cancel = false;
							startSemaphore.acquire();
						}
					}
					currentPoint.x = runnerBody.getPosition().x;
					currentPoint.y = runnerBody.getPosition().y;
					if(currentPoint.distance(initialPoint) >= distance){
						mapper.reportEvent(
							new MappingEvent(
								MappingEvent.Type.DISTANCE_REACHED,
								(float) currentPoint.x,
								(float) currentPoint.y,
								runner.getHeading()
							)
						);
						
						synchronized(startSemaphore){
							startSemaphore.acquire();
						}
					}
					DistanceObserver.sleep(10);
				} catch (InterruptedException ex) {
					Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
		
		public void observe(double x0, double y0, double distance){
			initialPoint.x = x0;
			initialPoint.y = y0;
			this.distance = distance;
			startSemaphore.release();
		}
		
		public void cancel(){
			cancel = true;
		}
	}
	
	public void openDefaultFrames(){
		TestbedModel model = new TestbedModel();
		model.addTest(this);
		TestbedPanel panel = new TestPanelJ2D(model);    // create our testbed panel
		JFrame testbed = new TestbedFrame(model, panel, TestbedController.UpdateBehavior.UPDATE_CALLED);
		testbed.setVisible(true);
		testbed.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	/**
	 * 
	 * @param wkt
	 * @return
	 * @throws ParseException 
	 */
	public static Simulation fromWKT(String wkt) throws ParseException{
		WKTReader wKTReader = new WKTReader();
		Geometry environmentGeometry = wKTReader.read(wkt);
		environmentGeometry.apply(new CoordinateScaleFilter(.1));
		return new Simulation(environmentGeometry);
	}
}
