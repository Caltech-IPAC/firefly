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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
