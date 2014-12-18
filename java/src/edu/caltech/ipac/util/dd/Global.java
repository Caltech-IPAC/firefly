package edu.caltech.ipac.util.dd;
/**
 * User: roby
 * Date: 2/13/13
 * Time: 10:27 AM
 */


import edu.caltech.ipac.util.HandSerialize;

import java.io.Serializable;

/**
 * @author Trey Roby
 */
public class Global implements Serializable, HandSerialize, RegionFileElement, ContainsOptions {

    private RegionOptions ops;

    public Global(RegionOptions ops) {
        this.ops = ops;
    }

    public RegionOptions getOptions() { return ops; }

    public String toString() {
        return "Global: " + ops.serialize();
    }


    public String serialize() {
        return null;
    }
}

