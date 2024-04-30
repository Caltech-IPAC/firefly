package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.server.query.ObsCoreMetadataQuery.*;
import static edu.caltech.ipac.firefly.server.query.UwsJobProcessor.getTableResult;


@SearchProcessorImpl(id = ID, params = {
        @ParamDoc(name = SVC_URL, desc = "base TAP url endpoint excluding '/sync'"),
        @ParamDoc(name = OBSCORE_TNAME, desc = "obscore schema.table"),
        @ParamDoc(name = COLUMNS, desc = "columns to retrieve metadata for"),
})
public class ObsCoreMetadataQuery extends EmbeddedDbProcessor {
    public static final String ID = "ObsCoreMetadataQuery";
    public static final String SVC_URL = "serviceUrl";
    public static final String OBSCORE_TNAME = "obscoreTable";
    public static final String COLUMNS = "columns";
    public static final String QUERY_FRAGMENT = "/sync?REQUEST=doQuery&LANG=ADQL&QUERY=";
    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();


    public DataGroup fetchDataGroup(TableServerRequest request) throws DataAccessException {
        List<String> columns = StringUtils.asList(request.getParam(COLUMNS), ",");
        List<Future<DataGroup>> futures = new ArrayList<>();
        List<Throwable> exceptions = new ArrayList<>();
        StopWatch timer = StopWatch.getInstance();

        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            timer.start("All queries");
            // Submit queries for each column in parallel
            for (String column : columns) {
                Future<DataGroup> future = executor.submit(() -> {
                    timer.start(column+" query");
                    DataGroup columnMetaData = fetchColumnMetaData(request, column);
                    timer.printLog(column+" query");
                    return columnMetaData;
                });
                futures.add(future);
            }

            // Retrieve DataGroup from each future one by one
            List<DataGroup> columnsMetadata = new ArrayList<>();
            for (Future<DataGroup> future : futures) {
                try {
                    columnsMetadata.add(future.get());  // note: blocking main thread until completion of this future, will still lead to completion of the futures running in other threads
                } catch (InterruptedException | ExecutionException e) {
                    exceptions.add(e);
                    columnsMetadata.add(null);
                }
            }
            timer.printLog("All queries");

            // If all futures failed, throw a combined exception; otherwise log exceptions (if occurred)
            if (exceptions.size() == futures.size()) throw new DataAccessException(mergeErrorMsg(exceptions));
            else if (exceptions.size() > 0) LOGGER.warn(mergeErrorMsg(exceptions));

            return mergeColumnsMetadata(columns, columnsMetadata);
        } finally {
            executor.shutdown();
        }
    }


    private DataGroup mergeColumnsMetadata(List<String> columns, List<DataGroup> columnsMetadata) {
        DataGroup table = new DataGroup("ObsCore Metadata", new DataType[]{
                new DataType("column_name", String.class),
                new DataType("column_options", String.class)
        });

        for (int i=0; i<columns.size(); i++) {
            String colName = columns.get(i);
            DataGroup colMetadata = columnsMetadata.get(i);

            if(colMetadata!=null) {
                for (int j = 0; j < colMetadata.size(); j++) {
                    DataObject row = new DataObject(table);
                    row.setData(new Object[]{colName, colMetadata.getData(colName, j)});
                    table.add(row);
                }
            }
        }
        table.trimToSize();
        return table;
    }

    private DataGroup fetchColumnMetaData(TableServerRequest request, String columnName) throws DataAccessException {
        String serviceUrl = request.getParam(SVC_URL);
        String obscoreTable= request.getParam(OBSCORE_TNAME);
        String query = "SELECT+DISTINCT+" + columnName + "+FROM+" + obscoreTable;
        String url = serviceUrl + QUERY_FRAGMENT + query;
        return getTableResult(url, QueryUtil.getTempDir(request));
    }

    private String mergeErrorMsg(List<Throwable> exceptions) {
        return exceptions.stream().map(Throwable::getMessage).collect(Collectors.joining("\n"));
    }
}
