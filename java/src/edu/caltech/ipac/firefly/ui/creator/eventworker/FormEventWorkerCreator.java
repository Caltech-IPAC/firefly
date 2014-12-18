package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.ui.creator.UICreator;

import java.util.Map;

/**
 * Date: Aug 4, 2010
 *
 * @author loi
 * @version $Id: FormEventWorkerCreator.java,v 1.1 2010/08/13 22:14:47 roby Exp $
 */
public interface FormEventWorkerCreator extends UICreator {
    public FormEventWorker create(Map<String, String> params);
}
