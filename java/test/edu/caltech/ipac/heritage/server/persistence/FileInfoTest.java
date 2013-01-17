package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/*
 * @author tatianag
 * @version $Id: FileInfoTest.java,v 1.6 2010/12/04 18:51:24 tatianag Exp $
 */
public class FileInfoTest {

    private static int MAX_TEST_ROWS = 2;

    static List<Integer> getTestBCDIds(Connection conn) throws SQLException {
        /*
        Statement stmt = null;
        String sql = "SELECT bcdid FROM bcdproducts";
        try {
            stmt = conn.createStatement();
            stmt.setMaxRows(MAX_TEST_ROWS);
            ResultSet rs = stmt.executeQuery(sql);
            List<Integer> ids = new ArrayList<Integer>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            return ids;
        } finally {
            FileInfoDao.closeStatement(stmt);
        }

        */
        // to test calibration queries
        return FileInfoDao.p2o(new int [] { 198078068, 142071092, 309776353
                
        });
        /**/

    }

    static List<Integer> getTestPBCDIds(Connection conn) throws SQLException {
        /*
        Statement stmt = null;
        String sql = "SELECT pbcdid FROM postbcdproducts";
        try {
            stmt = conn.createStatement();
            stmt.setMaxRows(MAX_TEST_ROWS);
            ResultSet rs = stmt.executeQuery(sql);
            List<Integer> ids = new ArrayList<Integer>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            return ids;
        } finally {
            FileInfoDao.closeStatement(stmt);
        }
        */
        // to test calibration queries
        return FileInfoDao.p2o(new int [] {2605886,2605888,2605890});
        /**/
    }

    static List<Integer> getTestSMIds(Connection conn) throws SQLException {
        return FileInfoDao.p2o(new int [] {6675179, 6842735});
    }


    static List<Integer> getTestAorIds(Connection conn) throws SQLException {
        /*
        Statement stmt = null;
        String sql = "SELECT reqkey from requestinformation";
        try {
            stmt = conn.createStatement();
            stmt.setMaxRows(MAX_TEST_ROWS);
            ResultSet rs = stmt.executeQuery(sql);
            List<Integer> ids = new ArrayList<Integer>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            return ids;
        } finally {
            FileInfoDao.closeStatement(stmt);
        }
        */
        // to test calibration queries
        return FileInfoDao.p2o(new int [] {30614784});
        /**/
    }
    
    
    public static void printFileInfo(Set<FileInfo> result, String tag) {
	System.out.println("------------"+tag+"--------------");
	if (result == null) {
	    System.out.println("No results.");
	    System.exit(1);
	}
	for (Object r : result ) {
	    if (r instanceof FileInfo) {
		FileInfo fir = (FileInfo)r;
		System.out.println("FileInfo: "+fir.getInternalFilename()+" - "+fir.getExternalName()+" - "+fir.getSizeInBytes());
	    } else {
		System.out.println("not FileInfo - "+r.getClass().getName());
	    }
	}
	System.out.println(tag+ " number: "+result.size());
    }



    public static void main(String [] args) {

        // loads App Properties
        System.out.println("Working dir: "+ ServerContext.getWorkingDir());

        try {
            DataSource ds = null;
            try {
                ds = JdbcFactory.getDataSource(DbInstance.archive);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
            Connection conn = null;
            List<Integer> ids;
            Set<FileInfo> result;
            try {
                conn = DataSourceUtils.getConnection(ds);
                ids = getTestBCDIds(conn);
                System.out.println("Number of BCD IDs selected: "+ids.size());

                //result = FileInfoDao.getBcdFileInfo(ids, conn);
                //printFileInfo(result, "BCD Products");
                //result = FileInfoDao.getBcdAncilFileInfo(ids, conn);
                //printFileInfo(result, "Ancil BCD Products");
                //result = FileInfoDao.getBcdRawFileInfo(ids, conn);
                //printFileInfo(result, "RAW BCD Products");
                result = FileInfoDao.getBcdCalFileInfo(ids, conn);
                printFileInfo(result, "CAL BCD Products");
                //result = FileInfoDao.getBcdQAFileInfo(ids, conn);
                //printFileInfo(result, "QA for BCD Products");

                ids = getTestPBCDIds(conn);
                System.out.println("Number of PBCD IDs selected: "+ids.size());

                //result = FileInfoDao.getPbcdFileInfo(ids, conn);
                //printFileInfo(result, "PBCD Products");
                //result = FileInfoDao.getPbcdAncilFileInfo(ids, conn);
                //printFileInfo(result, "Ancil PBCD Products");
                //result = FileInfoDao.getPbcdRawFileInfo(ids, conn);
                //printFileInfo(result, "RAW PBCD Products");
                result = FileInfoDao.getPbcdCalFileInfo(ids, conn);
                printFileInfo(result, "CAL PBCD Products");
                //result = FileInfoDao.getPbcdQAFileInfo(ids, conn);
                //printFileInfo(result, "QA for PBCD Products");


                ids = getTestAorIds(conn);
                System.out.println("Number of AOR IDs selected: "+ids.size());

                //result = FileInfoDao.getAorBcdFileInfo(ids, conn);
                //printFileInfo(result, "AOR BCD Products");
                //result = FileInfoDao.getAorBcdAncilFileInfo(ids, conn);
                //printFileInfo(result, "AOR Ancil BCD Products");
                //result = FileInfoDao.getAorPbcdFileInfo(ids, conn);
                //printFileInfo(result, "AOR PBCD Products");
                //result = FileInfoDao.getAorPbcdAncilFileInfo(ids, conn);
                //printFileInfo(result, "AOR Ancil PBCD Products");
                //result = FileInfoDao.getAorRawFileInfo(ids, conn);
                //printFileInfo(result, "RAW AOR Products");
                result = FileInfoDao.getAorCalFileInfo(ids, conn);
                printFileInfo(result, "CAL AOR Products");
                //result = FileInfoDao.getAorQAFileInfo(ids, conn);
                //printFileInfo(result, "QA for AOR Products");

                //ids = getTestSMIds(conn);
                //System.out.println("Number of SM IDs selected: "+ids.size());
                //result = FileInfoDao.getSmFileInfo(ids, conn);
                //printFileInfo(result, "SM Products");
                //result = FileInfoDao.getSmAncilFileInfo(ids, conn);
                //printFileInfo(result, "SM Ancil Products");
                //result = FileInfoDao.getSmBcdFileInfo(ids, conn);
                //printFileInfo(result, "SM Bcd Products");
                //result = FileInfoDao.getSmBcdAncilFileInfo(ids, conn);
                //printFileInfo(result, "SM Bcd Ancil Products");
            } finally {
                if (conn != null) DataSourceUtils.releaseConnection(conn, ds);
            }

            System.out.println();
            System.out.println("========USING TEMP TABLE============");

            try {
                conn = DataSourceUtils.getConnection(ds);
                ids = getTestBCDIds(conn);
                FileInfoDao.loadBcdIds(conn, ids);

                //result = FileInfoDao.getBcdFileInfo(conn);
                //printFileInfo(result, "BCD Products");
                //result = FileInfoDao.getBcdAncilFileInfo(conn);
                //printFileInfo(result, "Ancil BCD Products");
                //result = FileInfoDao.getBcdRawFileInfo(conn);
                //printFileInfo(result, "RAW BCD Products");
                result =  FileInfoDao.getBcdCalFileInfo(conn);
                printFileInfo(result, "CAL BCD Products");
                //result = FileInfoDao.getBcdQAFileInfo(conn);
                //printFileInfo(result, "QA for BCD Products");

            } finally {
                if (conn != null) DataSourceUtils.releaseConnection(conn, ds);
            }
            try {
                conn = DataSourceUtils.getConnection(ds);
                ids = getTestPBCDIds(conn);
                FileInfoDao.loadPbcdIds(conn, ids);

                //result = FileInfoDao.getPbcdFileInfo(conn);
                //printFileInfo(result, "PBCD Products");
                //result = FileInfoDao.getPbcdAncilFileInfo(conn);
                //printFileInfo(result, "Ancil PBCD Products");
                //result = FileInfoDao.getPbcdRawFileInfo(conn);
                //printFileInfo(result, "RAW PBCD Products");
                result = FileInfoDao.getPbcdCalFileInfo(conn);
                printFileInfo(result, "CAL PBCD Products");
                //result = FileInfoDao.getPbcdQAFileInfo(ids, conn);
                //printFileInfo(result, "QA for PBCD Products");

            } finally {
                if (conn != null) DataSourceUtils.releaseConnection(conn, ds);
                conn = null;
            }
            try {
                conn = DataSourceUtils.getConnection(ds);
                ids = getTestAorIds(conn);
                HashMap<Integer,List<Short>> hm = new HashMap<Integer, List<Short>>(ids.size());
                for (Integer id : ids) hm.put(id, null);
                FileInfoDao.loadAorIds(conn, hm);

                //result = FileInfoDao.getAorBcdFileInfo(conn);
                //printFileInfo(result, "Aor BCD Products");
                //result = FileInfoDao.getAorBcdAncilFileInfo(conn);
                //printFileInfo(result, "Aor Ancil BCD Products");
                //result = FileInfoDao.getAorPbcdFileInfo(conn);
                //printFileInfo(result, "Aor PBCD Products");
                //result = FileInfoDao.getAorPbcdAncilFileInfo(conn);
                //printFileInfo(result, "Aor Ancil PBCD Products");
                //result = FileInfoDao.getAorRawFileInfo(conn);
                //printFileInfo(result, "RAW AOR Products");
                result = FileInfoDao.getAorCalFileInfo(conn);
                printFileInfo(result, "CAL AOR Products");
                //result = FileInfoDao.getAorQAFileInfo(ids, conn);
                //printFileInfo(result, "QA for AOR Products");
            } finally {
                if (conn != null) DataSourceUtils.releaseConnection(conn, ds);
            }
            /**
            try {
                conn = DataSourceUtils.getConnection(ds);

                ids = getTestSMIds(conn);
                FileInfoDao.loadSmpIds(conn, ids);
                System.out.println("Number of SM IDs selected: "+ids.size());
                result = FileInfoDao.getSmFileInfo(conn);
                printFileInfo(result, "SM Products");
                result = FileInfoDao.getSmAncilFileInfo(conn);
                printFileInfo(result, "SM Ancil Products");
                result = FileInfoDao.getSmBcdFileInfo(conn);
                printFileInfo(result, "SM Bcd Products");
                result = FileInfoDao.getSmBcdAncilFileInfo(conn);
                printFileInfo(result, "SM Bcd Ancil Products");
            } finally {
                if (conn != null) DataSourceUtils.releaseConnection(conn, ds);
            }
             */
            System.exit(0);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);

        }
    }



}