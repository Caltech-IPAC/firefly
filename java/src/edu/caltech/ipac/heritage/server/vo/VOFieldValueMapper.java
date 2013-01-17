package edu.caltech.ipac.heritage.server.vo;

import edu.caltech.ipac.util.DataObject;

/**
 * @author tatianag
 *         $Id: VOFieldValueMapper.java,v 1.1 2009/09/23 22:23:29 tatianag Exp $
 */
public interface VOFieldValueMapper {
    public String getMappedValue(DataObject row);
    public int getMappedWidth();
}
