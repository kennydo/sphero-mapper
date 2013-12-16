package edu.berkeley.mapping;

/**
 * Class that defines parameters for the mapping algorithm.
 * @author Group
 */
public class Parameters {
	/**
	 * The distance for the algorithm to set for commander drive function.
	 * @see Commander#drive(float, float) 
	 */
	private float commanderDriveDistance = 10;
	/**
	 * The robot's width, which is used by the mapper to determine the area covered by the runner (e.g. the robot).
	 */
	private float runnerWidth = 4;
	
	private float contourFinishPointRadiusThreshold = 8;
	
	/**
	 * The length of the square the commander makes.
	 */
	private float squareLenght = 10;
	
	/**
	 * @return The distance for the algorithm to set for commander drive function.
	 * @see Commander#drive(float, float) 
	 */
	public float getCommanderDriveDistance() {
		return commanderDriveDistance;
	}

	/**
	 * 
	 * @param commanderDriveDistance The distance for the algorithm to set for commander drive function.
	 * @see Commander#drive(float, float) 
	 */
	public void setCommanderDriveDistance(float commanderDriveDistance) {
		this.commanderDriveDistance = commanderDriveDistance;
	}

	/**
	 * @return The robot's width, which is used by the mapper to determine the area covered by the runner (e.g. the robot).
	 */
	public float getRunnerWidth() {
		return runnerWidth;
	}

	/**
	 * @param runnerWidth The robot's width, which is used by the mapper to determine the area covered by the runner (e.g. the robot).
	 */
	public void setRunnerWidth(float runnerWidth) {
		this.runnerWidth = runnerWidth;
	}

	/**
	 * 
	 * @return 
	 */
	public float getContourFinishPointRadiusThreshold() {
		return contourFinishPointRadiusThreshold;
	}

	/**
	 * 
	 * @param contourFinishPointRadiusThreshold 
	 */
	public void setContourFinishPointRadiusThreshold(float contourFinishPointRadiusThreshold) {
		this.contourFinishPointRadiusThreshold = contourFinishPointRadiusThreshold;
	}

	public float getSquareLenght() {
		return squareLenght;
	}

	public void setSquareLenght(float squareLenght) {
		this.squareLenght = squareLenght;
	}
	
	
}
