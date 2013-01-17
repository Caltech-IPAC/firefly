package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

/**
 * Common utilities for temp table creation
 * @author tatianag
 * $Id: TempTable.java,v 1.7 2012/11/13 00:26:10 tatianag Exp $
 */
public class TempTable {

    private static int NUMID_FOR_TEMP_TABLE = AppProperties.getIntProperty("query.selectin.limit", 500);


    public static boolean useTempTable(Collection<Integer> ids) {
        return ids.size() > NUMID_FOR_TEMP_TABLE;
    }


//    public static DataSource getDS(Collection<Integer> ids, DbInstance dbInstance) {
//        if (TempTable.useTempTable(ids)) {
//            return JdbcFactory.getStatefulTemplate(dbInstance).getDataSource();
//        } else {
//            return JdbcFactory.getDataSource(dbInstance);
//        }
//    }

    public static int loadIdsIntoTempTable(Connection conn, Collection<Integer> ids, String tempTableName, String idColName) throws SQLException {

        // make sure prior temp table is dropped
        Statement stmt = conn.createStatement();
        try {
            stmt.executeUpdate("drop table "+tempTableName+";");
        } catch (SQLException e) {
            Logger.briefDebug("No "+tempTableName+" table to drop: "+e.getMessage());
        } finally {
            closeStatement(stmt);
        }

        // create temporary table
        String cmd = "create temp table "+tempTableName+" ("+idColName+" integer) with no log";
        stmt = conn.createStatement();
        try {
            stmt.executeUpdate(cmd);
        } finally {
            closeStatement(stmt);
        }

        // load ids
        cmd =  "insert into "+tempTableName+" values (?)";
        int [] updateCounts;
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(cmd);
            for (Integer id : ids) {
                pstmt.setInt(1, id);
                pstmt.addBatch();
            }
            updateCounts = pstmt.executeBatch();
        }catch(Exception e) {
            e.printStackTrace();
            return 0;
            
        } finally {
            closeStatement(pstmt);
        }

        // check that all rows were inserted successfully
        int updated = 0;
        for (int i=0; i<updateCounts.length; i++) {
            if (updateCounts[i] != 1) {
                Logger.error("Batch insert of "+tempTableName+" failed on "+(i+1)+"th insert.");
                break;
            }
            updated++;
        }

        return updated;
    }

    static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Throwable th) {
                // log and ignore
                Logger.error(th, "Failed to close statement: "+th.getMessage());
            }
        }
    }
}
