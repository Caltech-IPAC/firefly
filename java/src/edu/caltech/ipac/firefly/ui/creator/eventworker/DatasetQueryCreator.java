package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.data.table.DataSet;

import java.util.Map;

/**
 * Date: Aug 4, 2010
 *
 * @author loi
 * @version $Id: DatasetQueryCreator.java,v 1.6 2012/02/24 23:55:59 roby Exp $
 */
public class DatasetQueryCreator implements EventWorkerCreator {


    public static final String DATASET_QUERY= "DataSetQuery";

    public EventWorker create(Map<String, String> params) {

        DatasetQueryWorker worker = new DatasetQueryWorker();
        worker.insertCommonArgs(params);
        return worker;
    }


    /**
     * @author Trey Roby
     */
    public static class DatasetQueryWorker extends AbstractDatasetQueryWorker<DataSet> {

        @Override
        public DataSet convertResult(DataSet dataset) { return dataset; }
    }

}
