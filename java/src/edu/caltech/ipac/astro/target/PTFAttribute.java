package edu.caltech.ipac.astro.target;



/**
 * Date: Dec 20, 2004
 *
 * @author Trey Roby
 * @version $id:$
 */
public class PTFAttribute extends TargetAttribute {

    public static final String PTF= "PTF";

    private PositionJ2000 pos;

//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================

    public PTFAttribute(PositionJ2000 pos) {
        super(PTF);
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
        return new PTFAttribute(pos);
    }

}