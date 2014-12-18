package edu.caltech.ipac.firefly.server.db.spring.mapper;

import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.util.DataGroup;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DataGroupExtractor implements ResultSetExtractor {

    public Object extractData(ResultSet resultSet) throws SQLException, DataAccessException {

        StopWatch.getInstance().start("DataGroupExtractor");
        DataGroup dg = DataGroupUtil.createDataGroup(resultSet);
        if (resultSet.next()) {
            DataGroupUtil.processResults(resultSet, dg);
        }
        StopWatch.getInstance().printLog("DataGroupExtractor");
        return dg;
    }
}
