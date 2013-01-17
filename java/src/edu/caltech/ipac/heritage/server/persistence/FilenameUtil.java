package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility to change filename from basename to structured
 * (ex. campaign/reqkey/pbcd(bcd)/ch0(1,2,3,4)/currentFilename, where current filename is external file name)
 *
 * Nothing to do with heritage server, can be removed later.
 * @author tatianag
 * @version $Id: FilenameUtil.java,v 1.4 2009/01/23 01:44:14 loi Exp $
 */
public class FilenameUtil {

    static String SQL_BCD_QUERY = "SELECT c.campname, p.reqkey, p.bcdid, p.channum, p.heritagefilename, p.filesize"+
                    " FROM bcdproducts p, campaigninformation c"+
                    " WHERE p.campid=:campid and p.campid=c.campid and p.bcdid > :minid"+
                    " ORDER BY ABS(p.bcdid) ASC";

    static String SQL_BCD_UPDATE = "UPDATE bcdproducts"+
                " SET heritagefilename=? WHERE bcdid=?";

    static String SQL_BCD_ANCIL_QUERY = "SELECT c.campname, p.reqkey, pp.ancilid, p.channum, pp.heritagefilename, pp.filesize"+
                    " FROM bcdproducts p, campaigninformation c, bcdancilproducts pp"+
                    " WHERE p.campid=:campid and p.campid=c.campid and pp.ancilid > :minid and p.bcdid=pp.bcdid"+
                    " ORDER BY ABS(pp.ancilid) ASC";

    static String SQL_BCD_ANCIL_UPDATE = "UPDATE bcdancilproducts"+
                " SET heritagefilename=? WHERE ancilid=?";

    static String SQL_PBCD_QUERY = "SELECT c.campname, p.reqkey, p.pbcdid, p.channum, p.heritagefilename, p.filesize"+
            " FROM postbcdproducts p, campaigninformation c"+
            " WHERE p.campid=:campid and p.campid=c.campid and p.pbcdid >= :minid" +
            " ORDER BY ABS(p.pbcdid) ASC";

    static String SQL_PBCD_UPDATE = "UPDATE postbcdproducts"+
              " SET heritagefilename=? WHERE pbcdid=?";

    static String SQL_PBCD_ANCIL_QUERY = "SELECT c.campname, p.reqkey, pp.ancilid, p.channum, pp.heritagefilename, pp.filesize"+
                    " FROM postbcdproducts p, campaigninformation c, postbcdancilproducts pp"+
                    " WHERE p.campid=:campid and p.campid=c.campid and pp.ancilid > :minid and p.pbcdid=pp.pbcdid"+
                    " ORDER BY ABS(pp.ancilid) ASC";

    static String SQL_PBCD_ANCIL_UPDATE = "UPDATE postbcdancilproducts"+
                " SET heritagefilename=? WHERE ancilid=?";


    public static enum ProdType {
        BCD(SQL_BCD_QUERY, SQL_BCD_UPDATE, "bcd"),
        PBCD(SQL_PBCD_QUERY, SQL_PBCD_UPDATE, "pbcd"),
        BCD_ANCIL(SQL_BCD_ANCIL_QUERY, SQL_BCD_ANCIL_UPDATE, "bcd"),
        PBCD_ANCIL(SQL_PBCD_ANCIL_QUERY, SQL_PBCD_ANCIL_UPDATE, "pbcd");


        final String querySql;
        final String updateSql;
        final String pathid;

        ProdType(String sql1, String sql2, String id) {
            this.querySql = sql1;
            this.updateSql = sql2;
            this.pathid = id;
        }
    }

    public static void addComplexFilename(ProdType type, int campid, int minid) throws SQLException {

        int count=1;

        DataSource ds = JdbcFactory.getDataSource(DbInstance.archive);
        Connection conn = ds.getConnection();
        Statement stmt = null;
        PreparedStatement pstmt = null;
        try {
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            String query = type.querySql.replace(":minid", (new Integer(minid)).toString()).replace(":campid", (new Integer(campid)).toString());
            System.out.println(query);
            ResultSet rs = stmt.executeQuery(query);

            long cTime = System.currentTimeMillis();
            System.out.println(type.updateSql);
            pstmt = conn.prepareStatement(type.updateSql);
            while ( rs.next() ) {
                FilenameDetail prod = new FilenameDetail(rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getShort(4), rs.getString(5), type.pathid);
                System.out.println("    ["+count+"] "+prod.id+" "+prod.currentFilename+"->"+prod.newFilename);
                if (prod.newFilename != null) {
                    pstmt.setString(1,prod.newFilename);
                    pstmt.setInt(2,prod.id);
                    pstmt.addBatch();
                    count++;
                }

                if ( count % 50 == 0 ) {
                   System.out.println("FLUSH");
                   //flush a batch of updates and release memory: not that we are doing batch update - but in case we will...
                   System.out.println("After "+count+" recs: "+(System.currentTimeMillis()-cTime));
                   int [] updateCounts = pstmt.executeBatch();
                    for (int updateCount : updateCounts) {
                        if (updateCount != 1) {
                            System.out.println("Update failed: i");
                            return;
                        }
                    }
                    pstmt.close();
                    pstmt = conn.prepareStatement(type.updateSql);
                }

                //if (count > 2) break;
            }
            if ( count % 50 != 0) {
                System.out.println("FLUSH");
                //flush a batch of updates and release memory: not that we are doing batch update - but in case we will...
                System.out.println("After "+count+" recs: "+(System.currentTimeMillis()-cTime));
                int [] updateCounts = pstmt.executeBatch();
                for (int updateCount : updateCounts) {
                    if (updateCount != 1) {
                        System.out.println("Update failed: i");
                        return;
                    }
                }
            }
            System.out.println("Full task took: "+(System.currentTimeMillis()-cTime));

       } catch (Exception e) {
            System.out.println("FAILED: "+e.getMessage());
            e.printStackTrace();
        } finally {
            if (stmt != null) stmt.close();
            if (pstmt != null) pstmt.close();
        }

    }


    public static void printFS(ProdType type, int campid, int minid) throws SQLException {

        DataSource ds = JdbcFactory.getDataSource(DbInstance.archive);
        Connection conn = ds.getConnection();
        Statement stmt = null;
        try {
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            String query = type.querySql.replace(":minid", (new Integer(minid)).toString()).replace(":campid", (new Integer(campid)).toString());
            System.out.println(query);
            ResultSet rs = stmt.executeQuery(query);

            while ( rs.next() ) {
                int size = rs.getInt(6);
                //if (size > 2122560) {
                    System.out.println(rs.getString(5)+"\t"+size);
                //}
            }
        } catch (Exception e) {
            System.out.println("FAILED: "+e.getMessage());
            e.printStackTrace();
        } finally {
            if (stmt != null) stmt.close();
        }
    }


    public static class FilenameDetail {
        String cname;
        int reqkey;
        int id;
        short channum;
        String currentFilename;
        String newFilename;
        FilenameDetail(String cname, Number reqkey, Number id, Number channum, String currentFilename, String pathid) {
            this.cname = cname;
            this.reqkey = reqkey.intValue();
            this.id = id.intValue();
            this.channum = channum.shortValue();
            this.currentFilename = currentFilename;
            String pathlet = "/"+pathid+"/";
            this.newFilename = currentFilename.contains(pathlet) ?
                    null : cname+"/"+reqkey+pathlet+"ch"+channum+"/"+currentFilename;

        }

    }


    static final String USAGE = " usage: java FilenameUtil [bcd|pbcd] [campid] [minid]";

    public static void main(String [] argv) {
        boolean printFS = false;
        int minid = 0;
        ProdType [] types = {ProdType.BCD, ProdType.PBCD, ProdType.BCD_ANCIL, ProdType.PBCD_ANCIL};

        if (argv.length < 2 || argv.length > 3) {
            System.out.println(USAGE);
            System.exit(1);
        }
        else if (argv.length == 3) {
            try {
                minid = Integer.parseInt(argv[2]);
            } catch (Exception e) {
                System.out.println("minid must be integer; "+USAGE);
                System.exit(1);
            }
        }

        if (argv[0].equalsIgnoreCase("bcd")) { types = new ProdType[]{ProdType.BCD}; }
        else if (argv[0].equalsIgnoreCase("bcd_ancil")) { types = new ProdType[]{ProdType.BCD_ANCIL}; }
        else if (argv[0].equalsIgnoreCase("pbcd")) { types = new ProdType[]{ProdType.PBCD}; }
        else if (argv[0].equalsIgnoreCase("pbcd_ancil")) { types = new ProdType[]{ProdType.PBCD_ANCIL}; }
        else if (argv[0].equals("print")) {
            printFS= true;
        } else if (!argv[0].equalsIgnoreCase("all")) {
            types = null;
            System.out.println("invalid product types "+argv[0]+"; "+USAGE);
            System.exit(1);
        }

        int campid = Integer.parseInt(argv[1]);

        // loads App Properties
        System.out.println("Working dir: "+ ServerContext.getWorkingDir());

        try {
            for (ProdType type : types) {
                if (printFS)
                    FilenameUtil.printFS(type, campid, minid);
                else
                    FilenameUtil.addComplexFilename(type, campid, minid);
            }
            System.exit(0);
        } catch (Exception e) {
            System.out.println("UPDATE HAS FAILED: "+e.getMessage());
            e.printStackTrace();
            System.exit(1);
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
