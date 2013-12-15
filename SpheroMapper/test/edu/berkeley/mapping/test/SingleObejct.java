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
public class SingleObejct {
	public static void main(String[] args) throws ParseException {
		Simulation simulation = Simulation.fromWKT(
				"MULTIPOLYGON (((-120 180, 152 180, 152 165, -120 165, -120 180)), \n" +
				"  ((-130 190, -110 190, -110 -120, -130 -120, -130 190)), \n" +
				"  ((-140 -110, 170 -110, 170 -130, -140 -130, -140 -110)), \n" +
				"  ((176 -155, 140 -155, 140 200, 176 200, 176 -155)), \n" +
				"  ((50 -20, 50 70, 90 60, 80 -10, 50 -20)))"
		);
		
		simulation.openDefaultFrames();
		
	}
}
