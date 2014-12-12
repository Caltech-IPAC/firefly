package edu.caltech.ipac.visualize.draw;

import java.util.Map;
import java.util.HashMap;

/**
 * Date: Mar 28, 2006
 *
 * @author Trey Roby
 * @version $id:$
 */
public class ColumnException extends Exception {

    public enum ColumnType { NAME, RA, DEC};
    private final ColumnType _which;
    private final Map<ColumnType,Integer> _knownColumns=
                                  new HashMap<ColumnType,Integer>(4);
//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================
    public ColumnException(String desc,
                           ColumnType which,
                           Throwable cause,
                           int       targetNameColumn,
                           int       raColumn,
                           int       decColumn) {
        super(desc, cause);
        _which= which;
        _knownColumns.put(ColumnType.NAME, targetNameColumn);
        _knownColumns.put(ColumnType.RA, raColumn);
        _knownColumns.put(ColumnType.DEC, decColumn);
    }

    public ColumnException(String desc,
                           ColumnType which,
                           int       targetNameColumn,
                           int       raColumn,
                           int       decColumn) {
        this(desc, which, null, targetNameColumn, raColumn, decColumn);
    }
//============================================================================
//---------------------------- Public Methods --------------------------------
//============================================================================
    
    public ColumnType getWhichColumn() { return _which; }

    public int getKnownColumn(ColumnType type) {
        return _knownColumns.get(type);
    }

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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