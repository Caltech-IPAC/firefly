package edu.caltech.ipac.hydra.ui.finderchart;

import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorkerCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.FormEventWorker;
import edu.caltech.ipac.firefly.ui.creator.eventworker.FormEventWorkerCreator;
import edu.caltech.ipac.util.StringUtils;

import java.util.Map;

/**
 */
public class FinderChartUICreator {

    public static class FormController implements FormEventWorkerCreator {
        public static final String ID = "FinderChartFormController";

        public FormEventWorker create(Map<String, String> params) {
            FinderChartFormController worker = new FinderChartFormController();
            return worker;
        }
    }

    public static class ResultsController implements EventWorkerCreator {
        public static final String ID = "FinderChartResultsController";

        public EventWorker create(Map<String, String> params) {
            FinderChartResultsController worker = new FinderChartResultsController();
            worker.setQuerySources(StringUtils.asList(params.get(EventWorker.QUERY_SOURCE), ","));
            if (params.containsKey(EventWorker.ID)) worker.setID(params.get(EventWorker.ID));

            return worker;
        }
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
