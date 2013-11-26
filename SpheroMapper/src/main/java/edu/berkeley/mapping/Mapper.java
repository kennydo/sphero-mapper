package edu.berkeley.mapping;

/**
 * The class that implements the mapping algorithm.
 */
public class Mapper {
	/**
	 * The commander object for the mapper.
	 */
	Commander commander;

	/**
	 * 
	 * @param commander The commander object for the mapper.
	 */
	public Mapper(Commander commander) {
		this.commander = commander;
	}
	/**
	 * This method must be called by the "robot side" implementation to report
	 * any relevant event to the mapper.
	 * @param event The event reported.
	 */
	public void reportEvent(MappingEvent event){
		
	}
}
