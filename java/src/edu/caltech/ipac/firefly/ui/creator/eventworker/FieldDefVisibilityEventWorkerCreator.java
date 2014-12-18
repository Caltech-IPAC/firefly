package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.util.StringUtils;

import java.util.Map;

public class FieldDefVisibilityEventWorkerCreator implements FormEventWorkerCreator {
    public FormEventWorker create(Map<String, String> params) {
        String masters = params.get("masters");

        String showItems = params.get("show");
        String hideItems = params.get("hide");

        boolean retainSpace = true;
        String rsStr = params.get("retainSpace");
        if (!StringUtils.isEmpty(rsStr)) {
            retainSpace = Boolean.parseBoolean(rsStr);
        }

        return new FieldDefVisibilityEventWorker(masters, showItems, hideItems, retainSpace);
    }
}

