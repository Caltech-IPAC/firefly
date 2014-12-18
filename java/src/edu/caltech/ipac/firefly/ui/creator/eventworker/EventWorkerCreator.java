package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.ui.creator.UICreator;

import java.util.Map;

/**
 * Date: Aug 4, 2010
 *
 * @author loi
 * @version $Id: EventWorkerCreator.java,v 1.1 2010/08/06 01:15:39 loi Exp $
 */
public interface EventWorkerCreator extends UICreator {
    public EventWorker create(Map<String, String> params);
}
