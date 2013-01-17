package edu.caltech.ipac.hydra.ui.wise;

import edu.caltech.ipac.firefly.ui.creator.eventworker.FormEventWorker;
import edu.caltech.ipac.firefly.ui.creator.eventworker.FormEventWorkerCreator;
import edu.caltech.ipac.util.StringUtils;

import java.util.Map;

/**
 * @author tatianag
 *         $Id: WiseFormEventWorkerCreator.java,v 1.3 2012/02/16 01:32:43 tatianag Exp $
 */
public class WiseFormEventWorkerCreator implements FormEventWorkerCreator {
    public FormEventWorker create(Map<String, String> params) {
        String rule = params.get("rule");
        String fieldName = params.get("fieldName");
        String syncFieldName = params.get("syncFieldName");
        boolean publicRelease = false;
        String opsStr = params.get("ops");
        if (!StringUtils.isEmpty(opsStr)) {
            publicRelease = Boolean.parseBoolean(opsStr);
        }
        boolean validationOnly = false;
        String validationOnlyStr = params.get("validationOnly");
        if (!StringUtils.isEmpty(validationOnlyStr)) {
            validationOnly = Boolean.parseBoolean(validationOnlyStr);
        }

        return new WiseFormEventWorker(rule, fieldName, syncFieldName, publicRelease, validationOnly);
    }
}
