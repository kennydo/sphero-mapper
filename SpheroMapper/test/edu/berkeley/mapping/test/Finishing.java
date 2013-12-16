/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.berkeley.mapping.test;

import com.vividsolutions.jts.io.ParseException;
import edu.berkeley.mapping.test.jbox2d.Simulation;

/**
 *
 * @author Gustavo
 */
public class Finishing {
	public static void main(String[] args) throws ParseException {
		Simulation simulation = Simulation.fromWKT(
				"MULTIPOLYGON (((55 52, -60 52, -60 70, 55 70, 55 52)), \n" +
"  ((40 80, 55 80, 55 -69, 40 -69, 40 80)), \n" +
"  ((80 -60, -70 -60, -70 -50, 80 -50, 80 -60)), \n" +
"  ((-30 -80, -50 -80, -50 90, -30 90, -30 -80)))"
		);
		
		simulation.openDefaultFrames();
		
	}
}
