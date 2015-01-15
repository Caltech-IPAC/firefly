/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.ui.table.EventHub;

import java.util.List;

/**
 * Date: Aug 3, 2010
 *
 * @author loi
 * @version $Id: EventWorker.java,v 1.6 2012/02/27 18:43:20 roby Exp $
 */
public interface EventWorker {

    public static final String QUERY_SOURCE = "QuerySource";  // values separated by ','
    public static final String DEFAULT_ID= "NOID";
    public static final String ID = "Id";


    void bind(EventHub hub);
    List<String> getQuerySources();
    String getType();
    String getDesc();
    String getID();
    int getDelayTime();
    void setDelayTime(int delayTime);
}
