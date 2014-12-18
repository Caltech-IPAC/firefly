package edu.caltech.ipac.firefly.server.db.spring;

import edu.caltech.ipac.util.CollectionUtil;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Date: Nov 7, 2008
 *
 * @author loi
 * @version $Id: SqlUtil.java,v 1.1 2009/01/23 01:44:14 loi Exp $
 */
public class SqlUtil {

    public static String createTempTable(DataSource dataSource, String tableName, ColumnDef... content) {

        SimpleJdbcTemplate jdbc = new SimpleJdbcTemplate(dataSource);
        String[] tokens = new String[content.length];
        Arrays.fill(tokens, "?");
        String sqlCreateTable = "create temp table " + tableName + " ( " + CollectionUtil.toString(content, ",") + ") with no log";
        String sqlInsert = "insert into tbcdids values (" + CollectionUtil.toString(tokens, ",") + ")";

        jdbc.update(sqlCreateTable);
        jdbc.batchUpdate(sqlInsert, makeListOfObjectArray(content));

        return tableName;
    }

    static private List<Object[]> makeListOfObjectArray(ColumnDef... data) {

        // ensure same length
        int size = -1;
        for(ColumnDef d : data) {
            if (size == -1) {
                size = data.length;
            } else {
                if ( size != data.length ) {
                    throw new RuntimeException("The columns do not contain the same number of values");
                }
            }
        }

        ArrayList<Object[]> retval = new ArrayList<Object[]>();
        for(int i = 0; i < size; i++) {
            Object[] v = new Object[data.length];
            for(int j = 0; j < data.length; j++) {
                v[j] = data[j].getData().get(i);
            }
            retval.add(v);
        }

        return retval;
    }


}
