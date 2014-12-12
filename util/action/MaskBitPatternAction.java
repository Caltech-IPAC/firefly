package edu.caltech.ipac.util.action;

import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.FieldDefSource;
import edu.caltech.ipac.util.dd.PropDbFieldDefSource;

import java.util.Properties;

/**
 * A class that validates a MaskBitPattern against a set of test.
 *
 * @author Loi Ly
 */
public class MaskBitPatternAction extends IntAction {

    private static final String DOT = ".".intern();

    private int _numBits;

    public MaskBitPatternAction(String propName, Properties alternatePdb) {
        this(new PropDbFieldDefSource(propName,alternatePdb));
    }

    public MaskBitPatternAction(FieldDefSource fds) {
        super(fds, null);
        int numBits = 0;
        if (fds!=null) numBits = StringUtils.getInt(fds.getValue(),0);
        setNumBits(numBits);
    }


    public MaskBitPatternAction(String propName) { this(propName,null); }

    public MaskBitPatternAction(int numBits) {
        super((FieldDefSource)null);
        setNumBits(numBits);
    }

    public int getNumBits() {
        return _numBits;
    }

    public void setNumBits(int numBits) {
        _numBits = numBits;
        if (_numBits <= 0 ) {
            throw new IllegalArgumentException(
                        "Invalid number of bits in Mask Bit Pattern:" + String.valueOf(_numBits));
        }
        super.setMin(0);
        super.setMax((int) Math.pow(2, _numBits) - 1);
        super.setNullAllowed(false);
        super.setValidationType(ActionConst.RANGE_VALIDATION);
    }

    @Override // to limit access
    public void setMax(int max) {
        throw new UnsupportedOperationException("Internally maintained.  Modification is not allowed.");
    }

    @Override // to limit access
    public void setMin(int min) {
        throw new UnsupportedOperationException("Internally maintained.  Modification is not allowed.");
    }

    @Override // to limit access
    public void setNullAllowed(boolean b) {
        throw new UnsupportedOperationException("Internally maintained.  Modification is not allowed.");
    }

    @Override // to limit access
    public void setValidationType(int type) {
        throw new UnsupportedOperationException("Internally maintained.  Modification is not allowed.");
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
