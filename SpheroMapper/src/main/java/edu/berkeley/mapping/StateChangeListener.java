/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.berkeley.mapping;

/**
 * Interface for the listener for a change of state of the algorithm.
 * @author Gustavo
 */
public interface StateChangeListener {
	public void onStateChange(Mapper.State oldState, Mapper.State newState, MappingEvent event);
}
