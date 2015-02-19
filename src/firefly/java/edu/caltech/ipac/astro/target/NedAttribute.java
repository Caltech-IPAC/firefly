/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;



/**
 * Date: Dec 20, 2004
 *
 * @author Trey Roby
 * @version $id:$
 */
public class NedAttribute extends TargetAttribute {

    public static final String NED= "NED";

    private PositionJ2000 pos;

//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================

    public NedAttribute(PositionJ2000 pos) {
        super(NED);
        this.pos = pos;
    }

//============================================================================
//---------------------------- Public Methods --------------------------------
//============================================================================

    public PositionJ2000 getPosition() {
        return pos;
    }


//============================================================================
//-------------- Methods from TargetAttribute Interface --------------------
//============================================================================

    public Object clone() {
        return new NedAttribute(pos);
    }

}