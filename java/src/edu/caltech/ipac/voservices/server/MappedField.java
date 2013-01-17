package edu.caltech.ipac.voservices.server;

import edu.caltech.ipac.util.DataObject;

/**
 * $Id: MappedField.java,v 1.1 2010/10/05 22:15:36 tatianag Exp $
 */
public interface MappedField {
    String getMappedValue(DataObject dataObject);
}
