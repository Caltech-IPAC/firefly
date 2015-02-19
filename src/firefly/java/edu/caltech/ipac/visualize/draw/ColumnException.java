/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

