package edu.caltech.ipac.voservices.server;

import edu.caltech.ipac.voservices.server.tablemapper.VoField;
import edu.caltech.ipac.voservices.server.tablemapper.VoServiceParam;

import java.util.Collection;

/**
 * $Id: VOMetadata.java,v 1.1 2010/10/05 22:15:36 tatianag Exp $
 */
public interface VOMetadata {
    String getTableName();
    String getTableDesc();
    Collection<VoField> getVoFields();
    Collection<VoServiceParam> getVoParams();
}
