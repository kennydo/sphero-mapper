package edu.berkeley.mapping.test.utils;

import edu.berkeley.mapping.Commander;

/**
 *
 * @author Gustavo
 */
public class EmptyCommander implements Commander{

	@Override
	public void drive(float headingVariation, float distance) {
	}

	@Override
	public void drive(float headingVariation) {
	}

	@Override
	public void makeLeftSquare() {
	}

	@Override
	public void makeRightSquare() {
	}

	@Override
	public void stop() {
	}
	
}
