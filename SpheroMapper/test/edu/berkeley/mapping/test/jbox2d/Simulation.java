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
import java.util.Arrays;
import java.util.List;
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
		mapper.getParameters().setContourFinishPointRadiusThreshold((float) (Runner.SQUARE_EDGE * 0.9));
		mapper.getParameters().setCommanderDriveDistance(4);
		mapper.reportEvent(new MappingEvent(MappingEvent.Type.START, runnerBody.getPosition().x, runnerBody.getPosition().y));
		
		getWorld().setContactListener(new SimulationContactListener());
	}

	@Override
	public String getTestName() {
		return "Mapping simulation.";
	}
	
	private static enum RunnerState{
		IDLE,
		/**
		 * When the runner drives backward by half the square edge.
		 */
		SQUARE_FASE1,
		/**
		 * When the runner drives to the square side (left or right) until it reaches the edge length.
		 */
		SQUARE_FASE2,
		/**
		 * When the runner drives forward until it reaches the edge length.
		 */
		SQUARE_FASE3,
		/**
		 * When the runner drives to the opposite direction of the square side (left or right) until it reaches the edge length.
		 */
		SQUARE_FASE4,
		/**
		 * When the runner drives backward by half the square edge to complete the square.
		 */
		SQUARE_FASE5,
		/**
		 * When the runner is drive through a path.
		 */
		PATH_DRIVING
	}
	
	private class Runner implements Commander{
		private float heading = 0;
		private static final float SQUARE_EDGE = 1;
		/**
		 * Tells which direction is the current square run.
		 */
		private boolean isRightSquare = false;
		
		/**
		 * The path for the drive command witch receives a path as parameter.
		 * @see #drive(java.util.List) 
		 */
		private List<Coordinate> path;
		
		private RunnerState state = RunnerState.IDLE;

		@Override
		public void drive(float headingVariation, float distance) {
			distanceObserver.observe(runnerBody.getPosition().x, runnerBody.getPosition().y, distance);
			drive(headingVariation);
		}

		@Override
		public void drive(float headingVariation) {
			//System.out.println("Runner: drive(headingVariation="+headingVariation+")");
			setHeading(heading + headingVariation);
			double rad = (heading/180f)*Math.PI;
			runnerBody.setLinearVelocity(new Vec2((float) Math.cos(rad)*5f, (float) Math.sin(rad)*5f));
		}
		
		/**
		 * It's like the drive command, but set the distance observer to report the event to the runner instead of the mapper.
		 * @param headingVariation
		 * @param distance 
		 * @see #drive(float, float) 
		 */
		private void driveForRunner(float headingVariation, float distance){
			distanceObserver.observe(runnerBody.getPosition().x, runnerBody.getPosition().y, distance, EventTarget.RUNNER);
			drive(headingVariation);
		}

		@Override
		public void stop() {
			runnerBody.setLinearVelocity(new Vec2(0, 0));
		}

		public float getHeading() {
			return heading;
		}

		public void setHeading(float heading) {
			heading = heading%360;
			if(heading < 0) heading = 360+heading;
			this.heading = heading;
		}

		public void makeLeftSquare() {
			isRightSquare = false;
			makeSquare();
		}

		@Override
		public void makeLeftSquare(float collisionAngle) {
			//System.out.println("Runner: makeLeftSquare(collisionAngle=" + collisionAngle + ")");
			setHeading(collisionAngle);
			makeLeftSquare();
		}

		public void makeRightSquare() {
			isRightSquare = true;
			makeSquare();
		}

		@Override
		public void makeRightSquare(float collisionAngle) {
			//System.out.println("Runner: makeRightSquare(collisionAngle=" + collisionAngle + ")");
			setHeading(collisionAngle);
			makeRightSquare();
		}
		
		private void makeSquare(){
			state = RunnerState.SQUARE_FASE1;
			driveForRunner(-180, SQUARE_EDGE/2);
		}
		
		public void onDistanceReached(){
			//System.out.println("Runner: distanceReached(state=" + state + ")");
			switch(state){
				case SQUARE_FASE1:
					state = RunnerState.SQUARE_FASE2;
					driveForRunner(isRightSquare ? 90 : -90, SQUARE_EDGE);
					break;
				case SQUARE_FASE2:
					state = RunnerState.SQUARE_FASE3;
					driveForRunner(isRightSquare ? 90 : -90, SQUARE_EDGE);
					break;
				case SQUARE_FASE3:
					state = RunnerState.SQUARE_FASE4;
					driveForRunner(isRightSquare ? 90 : -90, SQUARE_EDGE);
					break;
				case SQUARE_FASE4:
					state = RunnerState.SQUARE_FASE5;
					driveForRunner(isRightSquare ? 90 : -90, SQUARE_EDGE/2);
					break;
				case SQUARE_FASE5:
					mapper.reportEvent(
						new MappingEvent(
							MappingEvent.Type.SQUARE_COMPLETED,
							runnerBody.getPosition().x,
							runnerBody.getPosition().y,
							heading
						)
					);
					break;
				case PATH_DRIVING:
					drivePath();
					break;
			}
		}

		@Override
		public void drive(List<Coordinate> points) {
			path = points;
			state = RunnerState.PATH_DRIVING;
			drivePath();
		}
		
		private void drivePath(){
			System.out.println("Runner: drivePath(");
			System.out.println(Arrays.toString(path.toArray()));
			System.out.println(")");
			
			if(path.size() > 0){
				mapper.reportEvent(
					new MappingEvent(
						MappingEvent.Type.POINT_REACHED,
						runnerBody.getPosition().x,
						runnerBody.getPosition().y,
						heading
					)
				);
				Coordinate origin = new Coordinate(runnerBody.getPosition().x, runnerBody.getPosition().y);
				Coordinate destiny = path.remove(0);
				double angle = 180f * Math.atan2(destiny.y-origin.y, destiny.x-origin.x) / Math.PI;
				float headingVariation = (float) angle - heading;
				driveForRunner(headingVariation, (float) origin.distance(destiny));
			}else{
				state = RunnerState.IDLE;
				stop();
				mapper.reportEvent(
					new MappingEvent(
						MappingEvent.Type.POINTS_COMPLETED,
						runnerBody.getPosition().x,
						runnerBody.getPosition().y,
						heading
					)
				);
			}
		}
		
		public void cancelPathDriving(){
			path.clear();
		}
		
		public RunnerState getState(){
			return state;
		}

		@Override
		public float getCurrentHeading() {
			return heading;
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
				double angle = Math.atan2(directionVector.y, directionVector.x);
				angle = 180f*angle/Math.PI;
				if(angle < 360) angle += 360;
				//System.out.println("Angle: " + angle);
				//System.out.println("Heading: " + runner.getHeading());
				
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
	
	/**
	 * The target to report the events.
	 */
	private static enum EventTarget{
		RUNNER,
		MAPPER
	}
	
	private class DistanceObserver extends Thread{
		
		private final Coordinate initialPoint = new Coordinate();
		private final Coordinate currentPoint = new Coordinate();
		private final Semaphore startSemaphore = new Semaphore(0);
		
		private double distance;
		private boolean cancel = false;
		private EventTarget eventTarget = EventTarget.MAPPER;
		
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
							if(eventTarget == EventTarget.RUNNER){
								if(runner.getState() == RunnerState.PATH_DRIVING) runner.cancelPathDriving();
							}
							cancel = false;
							startSemaphore.acquire();
						}
					}
					currentPoint.x = runnerBody.getPosition().x;
					currentPoint.y = runnerBody.getPosition().y;
					if(currentPoint.distance(initialPoint) >= distance){
						switch(this.eventTarget){
							case MAPPER:
								mapper.reportEvent(
									new MappingEvent(
										MappingEvent.Type.DISTANCE_REACHED,
										(float) currentPoint.x,
										(float) currentPoint.y,
										runner.getHeading()
									)
								);
								break;
							case RUNNER:
								runner.onDistanceReached();
								break;
						}
						
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
		
		/**
		 * 
		 * @param x0
		 * @param y0
		 * @param distance 
		 */
		public void observe(double x0, double y0, double distance){
			observe(x0, y0, distance, EventTarget.MAPPER);
		}
		
		/**
		 * 
		 * @param x0
		 * @param y0
		 * @param distance
		 * @param eventTarget 
		 */
		public void observe(double x0, double y0, double distance, EventTarget eventTarget){
			this.eventTarget = eventTarget;
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
