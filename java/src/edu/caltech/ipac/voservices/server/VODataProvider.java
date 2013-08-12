package edu.caltech.ipac.voservices.server;

import edu.caltech.ipac.util.DataObject;

import java.io.IOException;
import java.util.Collection;

/**
 * $Id: VODataProvider.java,v 1.1 2010/10/05 22:15:36 tatianag Exp $
 */
public interface VODataProvider {
    VOMetadata getVOMetadata();
    Collection<MappedField> getMappedFields() throws NoDataException;
    DataObject getNextRow() throws IOException;
    public void setTestMode(boolean testMode);
    public String getOverflowMessage();
}
