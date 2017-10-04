package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;

/**
 * Date: 9/8/17
 *
 * @author loi
 * @version $Id: $
 */
public interface DbAdapter {
    String H2 = "h2";
    String SQLITE = "sqlite";
    String HSQL = "hsql";

    /**
     * @return the name of this database
     */
    String getName();

    /**
     * @param dbFile
     * @return a new DbInstance for the given dbFile
     */
    DbInstance getDbInstance(File dbFile);

    /**
     * @param dataType
     * @return this database's datatype representation of the given java class.
     */
    String getDataType(Class dataType);

    /**
     * @return true if transaction should be used during batch import of the table data.
     */
    boolean useTxnDuringLoad();

    String createDataSql(DataType[] dataDefinitions, String tblName);
    String insertDataSql(DataType[] dataDefinitions, String tblName);

    String createMetaSql(String forTable);
    String insertMetaSql(String forTable);

    String createDDSql(String forTable);
    String insertDDSql(String forTable);

    String getDDSql(String forTable);
    String getMetaSql(String forTable);

    String selectPart(TableServerRequest treq);
    String fromPart(TableServerRequest treq);
    String wherePart(TableServerRequest treq);
    String orderByPart(TableServerRequest treq) ;
    String pagingPart(TableServerRequest treq) ;

    String createTableFromSelect(String tblName, String selectSql);
    String translateSql(String sql);

//====================================================================
//
//====================================================================

    static String DEF_DB_TYPE = AppProperties.getProperty("DbAdapter.type", HSQL);

    static DbAdapter getAdapter(TableServerRequest treq) {
        return getAdapter(treq.getMeta(TBL_FILE_TYPE));
    }

    static DbAdapter getAdapter(String type) {
        type = StringUtils.isEmpty(type) ? DEF_DB_TYPE : type;
        switch (type) {
            case H2:
                return new H2DbAdapter();
            case SQLITE:
                return new SqliteDbAdapter();
            case HSQL:
                return new HsqlDbAdapter();
            default:
                return new HsqlDbAdapter();   // when an unrecognized type is given.
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
