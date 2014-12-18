package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.table.EventHub;

/**
 * Date: Aug 3, 2010
 *
 * @author loi
 * @version $Id: FormEventWorker.java,v 1.3 2011/09/02 23:06:44 loi Exp $
 */
public interface FormEventWorker {

    /**
     * add a FormHub into this worker so it will be aware of
     * the form(s) it is dealing with.  This method only add
     * the FormHub.
     * @param hub
     */
    void addHub(FormHub hub);

    /**
     * add a FormHub into this worker, and then bind to it..
     * enabling events handling.
     * @param hub
     */
    void bind(FormHub hub);
    void bind(EventHub hub);
}
