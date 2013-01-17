package edu.caltech.ipac.firefly.server.db.spring;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.mapper.DataGroupMapper;
import edu.caltech.ipac.firefly.server.db.spring.mapper.IpacTableExtractor;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataGroup;
import org.springframework.jdbc.core.simple.ParameterizedSingleColumnRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import javax.sql.DataSource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Date: Oct 8, 2008
 *
 * @author loi
 * @version $Id: SpringTest.java,v 1.7 2010/03/08 22:40:04 loi Exp $
 */
public class SpringTest {
    SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(DbInstance.archive);


    static {
        AppProperties.loadClassPropertiesFromFileToPdb(new File("/has/server/hosted_config/heritage/db.prop"), null);
    }

    /**
     * retrieve a list of BCD IDs from the bcdproducts table.
     * @param count  the number of IDs to retrieve
     * @return
     */
    public List<Integer> getBcdIds(int count) {
        String sql = "select first " + count + " bcdid from bcdproducts";
        return jdbc.query(sql, new ParameterizedSingleColumnRowMapper<Integer>());
    }

    /**
     * retrieve the BCD file info for the given list of BCD IDs
     * this method uses the IN clause to select the data.
     * @param bcdids a list of BCD IDs
     * @return
     */
    public DataGroup getFileInfoUsingInClause(List<Integer> bcdids) {
        String sql = "select archivefilename, filesize from bcdproducts where bcdid in (" +
                    CollectionUtil.toString(bcdids, ",") + ")";

        return jdbc.queryForObject(sql, new DataGroupMapper());
    }

    /**
     * retrieve the BCD file info for the given list of BCD IDs
     * this method uses the IN clause to select the data.
     * @param bcdids a list of BCD IDs
     */
    public void getFileInfoToFile(List<Integer> bcdids, File outf) {
        String sql = "select * from bcdproducts where bcdid in (" +
                    CollectionUtil.toString(bcdids, ",") + ")";

        DataSource ds = JdbcFactory.getDataSource(DbInstance.archive);
        IpacTableExtractor.query(ds, outf, sql);
    }

    /**
     * retrieve the BCD file information for the given list of BCD IDs
     * this method uses a temporary table.  load the temp table with the given
     * IDs, then join with the bcdproducts.
     * @param bcdids a list of BCD IDs
     * @return
     */
    public DataGroup getFileInfoUsingTempTable(List<Integer> bcdids) {

        long cTime = System.currentTimeMillis();
        SimpleJdbcTemplate openedJdbc = JdbcFactory.getStatefulSimpleTemplate(DbInstance.archive);
        System.out.println("get connection: " + (System.currentTimeMillis() - cTime));

        String sqlCreateTable = "create temp table tbcdids (id integer) with no log";
        String sqlInsert = "insert into tbcdids values (?)";

        cTime = System.currentTimeMillis();
        openedJdbc.update(sqlCreateTable);
        System.out.println("create temp table: " + (System.currentTimeMillis() - cTime));

        cTime = System.currentTimeMillis();
        openedJdbc.batchUpdate(sqlInsert, makeListOfObjectArray(bcdids));
        System.out.println("batch insert: " + (System.currentTimeMillis() - cTime));

        String sql = "select bp.archivefilename, bp.filesize from bcdproducts bp, tbcdids tt where bp.bcdid = tt.id";

        cTime = System.currentTimeMillis();
        DataGroup dg =  openedJdbc.queryForObject(sql, new DataGroupMapper());
        System.out.println("query for DG: " + (System.currentTimeMillis() - cTime));
        return dg;
    }

    private List<Object[]> makeListOfObjectArray(List<Integer> l) {
        List<Object[]> ids = new ArrayList<Object[]>(l.size());
        for(Integer v : l) {
            ids.add(new Integer[]{v});
        }
        return ids;
    }
    
    public static void main(String[] args) {

        try {
            SpringTest test = new SpringTest();
            List<Integer> ids = test.getBcdIds(Integer.parseInt(args[0]));

            long cTime = System.currentTimeMillis();

            // getFileInfo using IN clause
            test.getFileInfoToFile(ids, new File("/has/wip/test.tbl"));
            System.out.println("Extract result to file with " + args[0] + " params: " + (System.currentTimeMillis() - cTime) );

//            cTime = System.currentTimeMillis();
//            DataGroup dg = IpacTableReader.readIpacTable(new File("/has/wip/big_test.tbl"), "big test");
//            System.out.println("Reading:" + (System.currentTimeMillis() - cTime));
//            cTime = System.currentTimeMillis();
//            DataGroupQuery dgq = new DataGroupQuery();
//            dgq.setOrderBy("bcdid");
//            dgq.setSortDir(DataGroupQuery.SortDir.ASC);
//            DataGroup sdg = dgq.doQuery(dg);
//            System.out.println("Sorting:" + (System.currentTimeMillis() - cTime));
//            cTime = System.currentTimeMillis();
//            IpacTableWriter.save(new File("/has/wip/sorted_big_test.tbl"), sdg);
//            System.out.println("Writing:" + (System.currentTimeMillis() - cTime));
//

            cTime = System.currentTimeMillis();
            DataGroupPart dataPart = IpacTableParser.getData(new File("/has/wip/test.tbl"), 5, 25);
            System.out.println("start index:" + dataPart.getStartRow() + " rowRetrieve:" + dataPart.getRowCount());
            System.out.println("Read selected rows from file:" + (System.currentTimeMillis() - cTime) + " total rows:" + dataPart.getRowCount());

            cTime = System.currentTimeMillis();
            IpacTableWriter.save(new File("/has/wip/test_out.tbl"), dataPart.getData());
            System.out.println("Write selected to file:" + (System.currentTimeMillis() - cTime));

            if(true) return;

            cTime = System.currentTimeMillis();
            // getFileInfo using IN clause
            DataGroup dgIn = test.getFileInfoUsingInClause(ids);
            System.out.println("Using IN with " + args[0] + " params: " + (System.currentTimeMillis() - cTime));
            IpacTableWriter.save(new File("file_info_in.tbl"), dgIn);


            cTime = System.currentTimeMillis();
            // getFileInfo using temp table
            DataGroup dgTemp = test.getFileInfoUsingTempTable(ids);
            System.out.println("Using Temp Table with " + args[0] + " params: " + (System.currentTimeMillis() - cTime));
            IpacTableWriter.save(new File("file_info_temp.tbl"), dgTemp);



        }catch(Exception e) {
            e.printStackTrace();
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
