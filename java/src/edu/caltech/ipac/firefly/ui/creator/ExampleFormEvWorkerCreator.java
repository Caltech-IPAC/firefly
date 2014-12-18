package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.ui.creator.eventworker.ExampleFormEvWorker;
import edu.caltech.ipac.firefly.ui.creator.eventworker.FormEventWorker;
import edu.caltech.ipac.firefly.ui.creator.eventworker.FormEventWorkerCreator;

import java.util.Map;
/**
 * User: roby
 * Date: Aug 13, 2010
 * Time: 1:42:43 PM
 */


/**
 * @author Trey Roby
 */
public class ExampleFormEvWorkerCreator implements FormEventWorkerCreator {
    public FormEventWorker create(Map<String, String> params) {
        return new ExampleFormEvWorker();
    }
}

