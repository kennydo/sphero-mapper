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

    public static void initialize(Sphero sphero) {
        //Placeholders before getting the actual objects
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
