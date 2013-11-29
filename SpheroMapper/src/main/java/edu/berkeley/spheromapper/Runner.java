package edu.berkeley.spheromapper;

import edu.berkeley.mapping.Commander;
import edu.berkeley.mapping.Mapper;
import edu.berkeley.spheromapper.SpheroCommander;
import orbotix.sphero.Sphero;

/**
 * Created by Brandon on 11/27/13.
 */
public class Runner {

    private static Commander commander;
    private static Mapper mapper;

    public void initialize() {
        //Placeholders before getting the actual objects
        Sphero sphero = null;
        mapper = null;

        commander = new SpheroCommander(sphero);
    }

    public static Mapper getMapper() {
        return mapper;
    }

    public static Commander getCommander() {
        return commander;
    }
}
