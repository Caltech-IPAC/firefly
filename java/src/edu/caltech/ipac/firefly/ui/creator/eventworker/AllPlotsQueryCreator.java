package edu.caltech.ipac.firefly.ui.creator.eventworker;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jun 13, 2012
 * Time: 2:14:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class AllPlotsQueryCreator implements EventWorkerCreator {
    public static final String ALL_PLOTS_QUERY= "AllPlotsQuery";

    public EventWorker create(Map<String, String> params) {
        AllPlotsQueryWorker worker = new AllPlotsQueryWorker();
        worker.insertCommonArgs(params);
        return worker;
    }
}

