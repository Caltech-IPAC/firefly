package edu.caltech.ipac.astro;


import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.*;

/**
 * Created by ymei on 1/26/17.
 *
 * DM-8916-JUnitTestIPACTableRead
 *
 * This test is mainly test if IPACTableReader.java reads the IPAC table correctly. The IRSA IPAC table reader requirements are in
 *      http://irsa.ipac.caltech.edu/applications/DDGEN/Doc/ipac_tbl.html
 *
 * An IPAC table is composed by three parts: Attributes which includes keywords and comments; header which includes column names,
 *      data types, data units, and null values; and data rows.
 *
 * The test cases below test if the reader can parse all parts in a table correctly and also if the reader can throw exceptions correctly
 *      when a table violate the requirements.
 *
 */
public class IpacTableReaderTest {

    //Input parameters:
    private static final String catName = "catName";
    String[] onlyColumns = null;
    private boolean useFloatsForDoubles = false;
    private boolean isHeadersOnlyAllow = false;
    private boolean noAttributes = false;
    private int estFileSize = 0;

    //Expected parameters in the data group:
    private String[] attributeKeys = null;
    private String[] attributeValues = null;
    private String[] attributeComments = null;
    private String[] colTitles = null;
    private String[] colKeyNames = null;
    private String[] colDataTypeDesc = null;
    private String[] colDataTypes = null;
    private String[] colDataUnits = null;
    private String[] colNullStrings = null;
    private String[][] dataValues = null;

    //The IPAC table reader store the table content in the dataGroup:
    private DataGroup dataGroup;

    @Before
    /**
     *
     */
    public void setUp() throws IpacTableException, ClassNotFoundException, java.io.FileNotFoundException {
        //Read different tables in each test case below.
    }

    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        dataGroup = null;
    }


    @Test
    /**
     * This test calls the methods IpacTableReader.readIpacTable to read all the columns from a valid input IPAC table, IPACTableSample1.tbl,
     * and verify if the produced dataGroup contains the correct information from the table.
     *
     * The input parameters to call IpacTableReader.readIpacTable:
     * File
     * onlyColumns
     * useFloatsForDoubles
     * catName = "catName"
     * isHeadersonlyAllow
     */
    public void testValidIpacTable1() throws IpacTableException, ClassNotFoundException, java.lang.NullPointerException {

        //Read all the columns, other parameters are "false".

        /* IPACTableSample1.tbl:

        \catalog1 = 'Sample Catalog1'
        \catalog2 = 'Sample Catalog2'
        \ Comment1
        \ Comment2
        |   ra      |    dec    |   n_obs  |    V     |   SpType   |
        |   double  |    double |   int    |   real   |   char     |
        |   deg     |    deg    |          |   mag    |            |
        |   null    |    null   |   null   |   null   |   null     |
          165.466279  -34.704730      5       11.27       K6Ve
          123.4       5.67            9       8.9         K6Ve-1

        */

        //Inputs:
        String tableName = "IPACTableSample1.tbl";
        File inFile = new File(FileLoader.getDataPath(IpacTableReaderTest.class) + tableName);
        //onlyColumns = null;

        //Set the expected values:
        setExpectedValues_Input1_full();

        //Read the table and generate the dataGroup:
        dataGroup = IpacTableReader.readIpacTable(inFile, catName);

        //Check the result:
        checkResult(dataGroup);

        //Test result: This IPAC table is read correctly.
        // Passed.
        // Known issue: The data type "real" is currently not supported. It is treated as a String. See the ticket https://jira.lsstcorp.org/browse/DM-9026

    }

    @Test
    /**
     * This test calls the methods IpacTableReader.readIpacTable to read all the columns from a valid input IPAC table, IPACTableSample1.tbl,
     * and verify if the produced dataGroup contains the correct information from the table.
     *         useFloatsForDoubles = true; isHeadersOnlyAllow = true;
     *
     */
    public void testValidIpacTable2() throws IpacTableException, ClassNotFoundException, java.lang.NullPointerException

    {

        //Read all the columns but useFloatsForDoubles = true; isHeadersOnlyAllow = true;

        /* IPACTableSample1.tbl:

        \catalog1 = 'Sample Catalog1'
        \catalog2 = 'Sample Catalog2'
        \ Comment1
        \ Comment2
        |   ra      |    dec    |   n_obs  |    V     |   SpType   |
        |   double  |    double |   int    |   real   |   char     |
        |   deg     |    deg    |          |   mag    |            |
        |   null    |    null   |   null   |   null   |   null     |
          165.466279  -34.704730      5       11.27       K6Ve
          123.4       5.67            9       8.9         K6Ve-1

        */

        //Inputs:
        String tableName = "IPACTableSample1.tbl";
        File inFile = new File(FileLoader.getDataPath(IpacTableReaderTest.class) + tableName);
        //onlyColumns = null;
        useFloatsForDoubles = true;
        isHeadersOnlyAllow = true;

        //Reset the expected values:
        setExpectedValues_Input1_full();

        //Read the table and generate the dataGroup:
        dataGroup = IpacTableReader.readIpacTable(inFile, onlyColumns, useFloatsForDoubles, catName, isHeadersOnlyAllow);

        //Check the result:
        checkResult(dataGroup);

        //Test result: No effects from  useFloatsForDoubles = true; isHeadersOnlyAllow = true;
        // useFloatsForDoubles is not used. isHeadersOnlyAllow = true is not used.
        // Passed.
        //Action: Do we need to use those two parameters and for what?

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read only two columns from a valid input IPAC table, IPACTableSample1.tbl,
     * and verify if the produced dataGroup contains the correct information from the table.
     *
     */
    public void testOnlyColumns() throws IpacTableException, ClassNotFoundException, java.lang.NullPointerException

    {

        /* IPACTableSample1.tbl:

        \catalog1 = 'Sample Catalog1'
        \catalog2 = 'Sample Catalog2'
        \ Comment1
        \ Comment2
        |   ra      |    dec    |   n_obs  |    V     |   SpType   |
        |   double  |    double |   int    |   real   |   char     |
        |   deg     |    deg    |          |   mag    |            |
        |   null    |    null   |   null   |   null   |   null     |
          165.466279  -34.704730      5       11.27       K6Ve
          123.4       5.67            9       8.9         K6Ve-1

        */


        //Inputs:
        String tableName = "IPACTableSample1.tbl";
        File inFile = new File(FileLoader.getDataPath(IpacTableReaderTest.class) + tableName);
        //The same input file but only read two columns:
        onlyColumns = new String[]{"ra", "dec"};
        useFloatsForDoubles = false;
        isHeadersOnlyAllow = false;

        //Reset the expected values:
        setExpectedValues_Input1_radecOnly();

        //Read the table and generate the dataGroup:
        dataGroup = IpacTableReader.readIpacTable(inFile, onlyColumns, useFloatsForDoubles, catName, isHeadersOnlyAllow);

        //Check the result:
        checkResult(dataGroup);

        //Test result: "onlyColumns" works fine. Only those two columns are read.
        // Passed.

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read a valid input IPAC table, IPACTableSample2_NoAttributes.tbl, which has no attributes.
     * More data types are tested.
     * Verify if the produced dataGroup contains the correct information from the table.
     *
     */
    public void testNoAttributes() throws IpacTableException, ClassNotFoundException, java.lang.NullPointerException {

        /* IPACTableSample2_NoAttributes.tbl:

        |id                            |f_x                           |f_y                           |i_x                           |i_y                           |peakValue                     |
        |long                          |float                         |float                         |int                           |int                           |float                         |
        |                              |pixels                        |pixels                        |pixels                        |pixels                        |dn                            |
        |                              |                              |                              |                              |                              |                              |
         1                              1309.0000                      8.0000                         1309                           8                              149.5896
         18422                          389.0000                       4.0000                         389                            4                              11.4204
         18423                          287.0000                       9.0000                         287                            9                              31.1756

        */

        String tableName = "IPACTableSample2_NoAttributes.tbl";
        File inFile = new File(FileLoader.getDataPath(IpacTableReaderTest.class) + tableName);
        noAttributes = true;
        onlyColumns = null;
        setExpectedValues_Input2();
        dataGroup = IpacTableReader.readIpacTable(inFile, onlyColumns, useFloatsForDoubles, catName, isHeadersOnlyAllow);
        checkResult(dataGroup);

        //Test result: It is okay for an IPAC table not having any attributes. The table is read correctly.
        // Passed.
        // Known issues: The trailing zeros to the right of the decimal point is truncated, eg. 1309.0000 to 1309.0.

    }

    @Test
    /** This test calls the method IpacTableReader.readIpacTable to read a IPAC table, IPACTable3_wrongAttributes.tbl,
     * which has some wrong keywords and wrong comments.
     * Need to see how our table reader handle it.
     *
     */
    public void testWrongAttributes1() throws IpacTableException, ClassNotFoundException, java.lang.NullPointerException {

        /* IPACTable3_wrongAttributes.tbl:

        \ catalog1 = 'A space makes this line as a comment'
        \catalog2 = 'This is a valid keyword.'
        \key:3  'missing the equal sign and no leading space, so this line will be ignored'
        \Missing the leading space so this line will be ignored
        \ Comment2
        |   ra      |    dec    |   n_obs  |    V     |   SpType   |
        |   double  |    double |   int    |   real   |   char     |
        |   deg     |    deg    |          |   mag    |            |
        |   null    |    null   |   null   |   null   |   null     |
          165.466279  -34.704730      5       11.27       K6Ve
          123.4       5.67            9       8.9         K6Ve-1

         */


        String tableName = "IPACTable3_wrongAttributes.tbl";
        File inFile = new File(FileLoader.getDataPath(IpacTableReaderTest.class) + tableName);
        noAttributes = false;
        onlyColumns = null;
        setExpectedValues_Input3_wrongAttributes();
        dataGroup = IpacTableReader.readIpacTable(inFile, onlyColumns, useFloatsForDoubles, catName, isHeadersOnlyAllow);
        checkResult(dataGroup);

        /*Test result:

        If a "keyword" has a space after the "\", it will be treated as a comment. Nothing can be done in the table reader about this.
        If a keyword has no "=" , it will be ignored. Nothing can be done in the table reader.
        If a comment has no space after "\", it will be ignored. Nothing can be done in the table reader.

        */

        // Passed.

        // Action: How to improve the way to test those issues?


    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read IPACTable4_wrongAttributes.tbl
     * which some attributes missing the first "\".
     */
    public void testWrongAttributes2() throws IpacTableException, ClassNotFoundException, java.lang.NullPointerException {

        /* IPACTable4_wrongAttributes.tbl:

          Comment1 missing the back slash! This line will be ignored.
          catalog1 = missing the back slash but with a space; this line will be ignored.
         catalog2 = missing the back slash and no space
         \ Comment2 Valid comment
         |   ra      |    dec    |   n_obs  |    V     |   SpType   |
         |   double  |    double |   int    |   real   |   char     |
         |   deg     |    deg    |          |   mag    |            |
         |   null    |    null   |   null   |   null   |   null     |
           165.466279  -34.704730      5       11.27       K6Ve
           123.4       5.67            9       8.9         K6Ve-1

         */

        String tableName = "IPACTable4_wrongAttributes.tbl";
        File inFile = new File(FileLoader.getDataPath(IpacTableReaderTest.class) + tableName);

        try{
            dataGroup = IpacTableReader.readIpacTable(inFile, onlyColumns, useFloatsForDoubles, catName, isHeadersOnlyAllow);
        } catch (IpacTableException e){
            System.out.println("IpacTableException from the table " + tableName + ": " + e.getMessage());
        }

        //Test result:
        //If an attribute starts with a space, it will be ignored.
        //If an attribute has no "\" nor space at beginning, it will trigger IpacTableException: "Data row must start with a space." and no dataGroup is generated.

        //Action item: Anything we can do better in the table reader?


    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read IPACTable5_duplicatedCol.tbl
     * which has duplicate column names.
     */
    public void testDuplicateColumn() throws IpacTableException, ClassNotFoundException, java.lang.NullPointerException {

        /*
        \catalog1 = 'Sample Catalog1'
        \catalog2 = 'Sample Catalog2'
        \ Comment1
        \ Comment2
        |   ra      |    ra     |   n_obs  |    V     |   SpType   |
        |   double  |    double |   int    |   real   |   char     |
        |   deg     |    deg    |          |   mag    |            |
        |   null    |    null   |   null   |   null   |   null     |
          165.466279  -34.704730      5       11.27       K6Ve
          123.4       5.67            9       8.9         K6Ve-1
         */

        //Read a table with duplicated column names
        String tableName = "IPACTable5_duplicatedCol.tbl";
        File inFile = new File(FileLoader.getDataPath(IpacTableReaderTest.class) + tableName);

        try{
            dataGroup = IpacTableReader.readIpacTable(inFile, onlyColumns, useFloatsForDoubles, catName, isHeadersOnlyAllow);
        } catch (IpacTableException e){
            System.out.println("IpacTableException from the table " + tableName + ":" + e.getMessage());
        }

        DataType[] dataTypes = dataGroup.getDataDefinitions();
        List<String> colTitle = new ArrayList<String>();
        for (int i = 0; i < dataTypes.length; i++) {
            colTitle.add(dataTypes[i].getDefaultTitle().toString());
        }

        System.out.println("NO IpacTableException from the table " + tableName + " which has duplicate column names: " + colTitle);

        //Test result:
        //The duplicate columns are not detected. A dataGroup is generated with those duplicate columns!
        //Action item: Issue a ticket!

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read IPACTable6_nodata.tbl which has no data.
     */
    public void testNoData() throws IpacTableException, ClassNotFoundException, java.lang.NullPointerException {

        /*
        \catalog1 = 'Sample Catalog1'
        \catalog2 = 'Sample Catalog2'
        \ Comment1
        \ Comment2
        |   ra      |    dec    |   n_obs  |    V     |   SpType   |
        |   double  |    double |   int    |   real   |   char     |
        |   deg     |    deg    |          |   mag    |            |
        |   null    |    null   |   null   |   null   |   null     |
         */

        //Read a table without data
        String tableName = "IPACTable6_nodata.tbl";
        File inFile = new File(FileLoader.getDataPath(IpacTableReaderTest.class) + tableName);

        try{
            dataGroup = IpacTableReader.readIpacTable(inFile, onlyColumns, useFloatsForDoubles, catName, isHeadersOnlyAllow);
        } catch (IpacTableException e){
            System.out.println("IpacTableException from the table " + tableName + ": "  + e.getMessage());
        }

        //Test result: IpacTableException is thrown.
        //Passed.

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read IPACTable7_dataUnderBar.tbl which has one datum under "|".
     */
    public void testDataUnderBar() throws IpacTableException, ClassNotFoundException, java.lang.NullPointerException {

        /*
        \catalog1 = 'Sample Catalog1'
        \catalog2 = 'Sample Catalog2'
        \ Comment1
        \ Comment2
        |   ra      |    dec    |   n_obs  |    V     |   SpType   |
        |   double  |    double |   int    |   real   |   char     |
        |   deg     |    deg    |          |   mag    |            |
        |   null    |    null   |   null   |   null   |   null     |
           165.466279 -34.704730      5       11.27       K6Ve
          123.4       5.67            9       8.9         K6Ve-1
         */

        // Read a table with some data under "|"
        String tableName = "IPACTable7_dataUnderBar.tbl";
        File inFile = new File(FileLoader.getDataPath(IpacTableReaderTest.class) + tableName);

        //setExpectedValues_Input7_full();

        try{
            dataGroup = IpacTableReader.readIpacTable(inFile, onlyColumns, useFloatsForDoubles, catName, isHeadersOnlyAllow);
        } catch (IpacTableException e){
            System.out.println("IpacTableException from the table " + tableName + ": "  + e.getMessage());
        }

        System.out.println("NO IpacTableException from the table " + tableName + " which has data under \"|\". A dataGroup is generated but the content is wrong.");

        //checkResult(dataGroup);

        //Test result: No exception. A dataGroup is generated but with wrong content! 165.466279 is truncated to 165.46627; -34.704730 is missing.
        //Action item: Issue a ticket.

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read IPACTable8_noHeader.tbl which has no header.
     */
    public void testNoHeader() throws IpacTableException, ClassNotFoundException, java.lang.NullPointerException {

        /* IPACTable8_noHeader.tbl:

        \catalog1 = 'Sample Catalog1'
        \catalog2 = 'Sample Catalog2'
        \ Comment1
        \ Comment2
          165.466279  -34.704730      5       11.27       K6Ve
          123.4       5.67            9       8.9         K6Ve-1
         */

        // Read a table without header.
        String tableName = "IPACTable8_noHeader.tbl";
        File inFile = new File(FileLoader.getDataPath(IpacTableReaderTest.class) + tableName);

        //setExpectedValues_Input7_full();

        try{
            dataGroup = IpacTableReader.readIpacTable(inFile, onlyColumns, useFloatsForDoubles, catName, isHeadersOnlyAllow);
        } catch (IpacTableException e){
            System.out.println("IpacTableException from the table " + tableName + ": "  + e.getMessage());
        }

        DataType[] dataTypes = dataGroup.getDataDefinitions();

        System.out.println("NO IpacTableException from the table " + tableName + " which has no header. The dataGroup is generated but the dataTypes length is " + dataTypes.length);
        //checkResult(dataGroup);

        //Test result: The dataGroup is genearated but no dataTypes.
        //Action item: Issue a ticket: at least the column name is required.

    }


    @Test
    /**
     *
     * This test calls the methods IpacTableReader.readIpacTable(inReader, catName, onlyColumns, useFloatsForDoubles, estFileSize, isHeadersOnlyAllow) which take the input Reader
     * The input parameters:
     * FileReader
     * catName = "catName"
     * onlyColumns
     * useFloatsForDoubles
     * estFileSize
     * isHeaderOnlyAllow
     */
    public void testValidIpacTable_Reader() throws IpacTableException, ClassNotFoundException, FileNotFoundException, java.lang.NullPointerException {


        String tableName = "IPACTableSample1.tbl";
        Reader inReader = new FileReader(FileLoader.getDataPath(IpacTableReaderTest.class) + tableName);
        //onlyColumns = null;
        //useFloatsForDoubles = false;
        //estFileSize = 0;
        //isHeadersOnlyAllow = false;
        //noAttributes = false;

        setExpectedValues_Input1_full();
        dataGroup = IpacTableReader.readIpacTable(inReader, catName, onlyColumns, useFloatsForDoubles, estFileSize, isHeadersOnlyAllow);
        checkResult(dataGroup);

        //Test result: Passed.
    }

    private void setExpectedValues_Input1_full() {

               /*
                \catalog1 = 'Sample Catalog1'
                \catalog2 = 'Sample Catalog2'
                \ Comment1
                \ Comment2
                |   ra      |    dec    |   n_obs  |    V     |   SpType   |
                |   double  |    double |   int    |   real   |   char     |
                |   deg     |    deg    |          |   mag    |            |
                |   null    |    null   |   null   |   null   |   null     |
                  165.466279  -34.704730      5       11.27       K6Ve
                  123.4       5.67            9       8.9         K6Ve-1

                  */

        //Set the attributes:
        attributeKeys = new String[]{"catalog1", "catalog2", "source"};
        attributeValues = new String[]{"\'Sample Catalog1\'", "\'Sample Catalog2\'", "/hydra/cm/firefly_test_data/edu/caltech/ipac/astro/IPACTableSample1.tbl"};
        attributeComments = new String[]{"\\ Comment1", "\\ Comment2"};

        //Set the header:

        colTitles = new String[]{"ra", "dec", "n_obs", "V", "SpType"};
        colKeyNames = new String[]{"ra", "dec", "n_obs", "V", "SpType"};
        colDataTypeDesc = new String[]{"double", "double", "int", "char", "char"}; //The 4th one is "readl" in the table but currently the IPAC table treat it as "char". DM-9026.
        //colDataTypeDesc = new String[]{"double", "double", "int", "char", "char"}; //After DM-9026.
        colDataTypes = new String[]{"class java.lang.Double", "class java.lang.Double",
                "class java.lang.Integer", "class java.lang.String", "class java.lang.String"}; //The 4h one is "real" in the table but currently the IPAC table treats it as String. DM-9026.
        //colDataTypes = new String[]{"class java.lang.Double", "class java.lang.Double",
        //"class java.lang.Integer", "class java.lang.Double", "class java.lang.String"}; //After DM-9026.
        colDataUnits = new String[]{"deg", "deg", "", "mag", ""};
        colNullStrings = new String[]{"null", "null", "null", "null", "null"};

        //Set the data:
        dataValues = new String[][]{{"165.466279", "-34.70473", "5", "11.27", "K6Ve"}, {"123.4", "5.67", "9", "8.9", "K6Ve-1"}};

    }

    private void setExpectedValues_Input1_radecOnly() {

               /*
                \catalog1 = 'Sample Catalog1'
                \catalog2 = 'Sample Catalog2'
                \ Comment1
                \ Comment2
                |   ra      |    dec    |   n_obs  |    V     |   SpType   |
                |   double  |    double |   int    |   real   |   char     |
                |   deg     |    deg    |          |   mag    |            |
                |   null    |    null   |   null   |   null   |   null     |
                  165.466279  -34.704730      5       11.27       K6Ve
                  123.4       5.67            9       8.9         K6Ve-1

                  */

        //Only read two columns: ra and dec.

        //Set the attributes:
        attributeKeys = new String[]{"catalog1", "catalog2", "source"};
        attributeValues = new String[]{"\'Sample Catalog1\'", "\'Sample Catalog2\'", "/hydra/cm/firefly_test_data/edu/caltech/ipac/astro/IPACTableSample1.tbl"};
        attributeComments = new String[]{"\\ Comment1", "\\ Comment2"};

        //Set the header:
        colTitles = new String[]{"ra", "dec"};
        colKeyNames = new String[]{"ra", "dec"};
        colDataTypeDesc = new String[]{"double", "double"};
        colDataTypes = new String[]{"class java.lang.Double", "class java.lang.Double"};
        colDataUnits = new String[]{"deg", "deg"};
        colNullStrings = new String[]{"null", "null"};

        //Set the data:
        dataValues = new String[][]{{"165.466279", "-34.70473"}, {"123.4", "5.67"}};

    }



    private void setExpectedValues_Input2(){

        /*
        |id                            |f_x                           |f_y                           |i_x                           |i_y                           |peakValue                     |
        |long                          |float                         |float                         |int                           |int                           |float                         |
        |                              |pixels                        |pixels                        |pixels                        |pixels                        |dn                            |
        |                              |                              |                              |                              |                              |                              |
        1                              1309.0000                      8.0000                         1309                           8                              149.5896
        18422                          389.0000                       4.0000                         389                            4                              11.4204
        18423                          287.0000                       9.0000                         287                            9                              31.1756
        */


        //Set the attributes:
        attributeKeys = new String[]{"source"};
        attributeValues = new String[]{"/hydra/cm/firefly_test_data/edu/caltech/ipac/astro/IPACTableSample2_NoAttributes.tbl"};
        attributeComments = null;

        //Set the header:
        colTitles = new String[]{"id", "f_x", "f_y", "i_x", "i_y", "peakValue"};
        colKeyNames = new String[]{"id", "f_x", "f_y", "i_x", "i_y", "peakValue"};
        colDataTypeDesc = new String[]{"long", "float", "float", "int", "int", "float"};
        colDataTypes = new String[]{"class java.lang.Long", "class java.lang.Float",
                "class java.lang.Float", "class java.lang.Integer", "class java.lang.Integer", "class java.lang.Float"};
        colDataUnits = new String[]{"", "pixels", "pixels", "pixels", "pixels", "dn"};
        colNullStrings = new String[]{"", "", "", "", "", ""};

        //Set the data:
        dataValues = new String[][]{{"1", "1309.0", "8.0", "1309", "8", "149.5896"},
                                        {"18422", "389.0", "4.0", "389", "4", "11.4204"},
                                        {"18423", "287.0", "9.0", "287", "9", "31.1756"}};
    }

    private void setExpectedValues_Input3_wrongAttributes() {

        /*
        \ catalog1 = 'A space makes this line as a comment'
        \catalog2 = 'This is a valid keyword.'
        \key:3  'missing the equal sign and no leading space, so this line will be ignored'
        \Missing the leading space so this line will be ignored
        \ Comment2
        |   ra      |    dec    |   n_obs  |    V     |   SpType   |
        |   double  |    double |   int    |   real   |   char     |
        |   deg     |    deg    |          |   mag    |            |
        |   null    |    null   |   null   |   null   |   null     |
        165.466279  -34.704730      5       11.27       K6Ve
        123.4       5.67            9       8.9         K6Ve-1

         */

        //Set the attributes:
        attributeKeys = new String[]{"catalog2", "source"};
        attributeValues = new String[]{"\'This is a valid keyword.\'", "/hydra/cm/firefly_test_data/edu/caltech/ipac/astro/IPACTable3_wrongAttributes.tbl"};
        attributeComments = new String[]{"\\ catalog1 = 'A space makes this line as a comment'", "\\ Comment2"};

        //Set the header:
        colTitles = new String[]{"ra", "dec", "n_obs", "V", "SpType"};
        colKeyNames = new String[]{"ra", "dec", "n_obs", "V", "SpType"};
        colDataTypeDesc = new String[]{"double", "double", "int", "char", "char"}; //The 4th one is "readl" in the table but currently the IPAC table treat it as "char". DM-9026.
        //colDataTypeDesc = new String[]{"double", "double", "int", "char", "char"}; //After DM-9026.
        colDataTypes = new String[]{"class java.lang.Double", "class java.lang.Double",
                "class java.lang.Integer", "class java.lang.String", "class java.lang.String"}; //The 4h one is "real" in the table but currently the IPAC table treats it as String. DM-9026.
        //colDataTypes = new String[]{"class java.lang.Double", "class java.lang.Double",
        //"class java.lang.Integer", "class java.lang.Double", "class java.lang.String"}; //After DM-9026.
        colDataUnits = new String[]{"deg", "deg", "", "mag", ""};
        colNullStrings = new String[]{"null", "null", "null", "null", "null"};

        //Set the data:
        dataValues = new String[][]{{"165.466279", "-34.70473", "5", "11.27", "K6Ve"}, {"123.4", "5.67", "9", "8.9", "K6Ve-1"}};

    }

    private void setExpectedValues_Input7_full() {

               /*
                \catalog1 = 'Sample Catalog1'
                \catalog2 = 'Sample Catalog2'
                \ Comment1
                \ Comment2
                |   ra      |    dec    |   n_obs  |    V     |   SpType   |
                |   double  |    double |   int    |   real   |   char     |
                |   deg     |    deg    |          |   mag    |            |
                |   null    |    null   |   null   |   null   |   null     |
                  165.466279  -34.704730      5       11.27       K6Ve
                  123.4       5.67            9       8.9         K6Ve-1

                  */

        //Set the attributes:
        attributeKeys = new String[]{"catalog1", "catalog2", "source"};
        attributeValues = new String[]{"\'Sample Catalog1\'", "\'Sample Catalog2\'", "/hydra/cm/firefly_test_data/edu/caltech/ipac/astro/IPACTable7_dataUnderBar.tbl"};
        attributeComments = new String[]{"\\ Comment1", "\\ Comment2"};

        //Set the header:

        colTitles = new String[]{"ra", "dec", "n_obs", "V", "SpType"};
        colKeyNames = new String[]{"ra", "dec", "n_obs", "V", "SpType"};
        colDataTypeDesc = new String[]{"double", "double", "int", "char", "char"}; //The 4th one is "readl" in the table but currently the IPAC table treat it as "char". DM-9026.
        //colDataTypeDesc = new String[]{"double", "double", "int", "char", "char"}; //After DM-9026.
        colDataTypes = new String[]{"class java.lang.Double", "class java.lang.Double",
                "class java.lang.Integer", "class java.lang.String", "class java.lang.String"}; //The 4h one is "real" in the table but currently the IPAC table treats it as String. DM-9026.
        //colDataTypes = new String[]{"class java.lang.Double", "class java.lang.Double",
        //"class java.lang.Integer", "class java.lang.Double", "class java.lang.String"}; //After DM-9026.
        colDataUnits = new String[]{"deg", "deg", "", "mag", ""};
        colNullStrings = new String[]{"null", "null", "null", "null", "null"};

        //Set the data:
        dataValues = new String[][]{{"165.46627", "-34.70473", "5", "11.27", "K6Ve"}, {"123.4", "5.67", "9", "8.9", "K6Ve-1"}};

    }
    
    /**
     *
     * @param dataGroup
     */

    private void checkResult(DataGroup dataGroup) {

        //Check data definitions:
        DataType[] dataTypes = dataGroup.getDataDefinitions();
        for (int i = 0; i < dataTypes.length; i++) {
            Assert.assertEquals("check column title", dataTypes[i].getDefaultTitle().toString(), colTitles[i]);
            Assert.assertEquals("check column key name", dataTypes[i].getKeyName().toString(), colKeyNames[i]);
            Assert.assertEquals("check column data type short description", dataTypes[i].getTypeDesc().toString(), colDataTypeDesc[i]);
            Assert.assertEquals("check column data type class", dataTypes[i].getDataType().toString(), colDataTypes[i]);
            Assert.assertEquals("check the column unit", dataTypes[i].getDataUnit().toString(), colDataUnits[i]);
            Assert.assertEquals("check column null string", dataTypes[i].getNullString().toString(), colNullStrings[i]);
        }

        //Not test the followings:
        //dt0.getMayBeNull(); //false!!???
        //dt0.getFormatInfo().toString(); //"edu.caltech.ipac.util.DataType$FormatInfo@...."??
        //dt0.getImportance().toString(); //"HIGH" (who assigned this?)
        //dt0.getMaxDataWidth(); //10

        //Check the Attributes (comments):
        List<DataGroup.Attribute> attributes = dataGroup.getKeywords();
        if (attributes.size() == 0){
            //System.out.println("No attributes detected.");
            //Assert.assertTrue("No attributes", noAttributes);
            Assert.assertFalse("No attributes found", !noAttributes);
        }
        else {
            int commentNum = 0;
            for (DataGroup.Attribute att : attributes) {
                if (att.isComment()) {
                    //System.out.println(att.toString());
                    Assert.assertEquals("check the comments:", att.toString(), attributeComments[commentNum]);
                    commentNum++;
                }
            }
        }

        //Check the attributes (key, value):
        Map<String, DataGroup.Attribute> attributeMap = dataGroup.getAttributes();
        //attributeMap.size();
        int j = 0;
        String value;
        for (Map.Entry entry : attributeMap.entrySet()) {
            Assert.assertEquals("check the key", entry.getKey(), attributeKeys[j]);
            value = ((DataGroup.Attribute) entry.getValue()).getValue().toString();
            //System.out.println(value + " " + attributeValues[j] + " " + j);
            Assert.assertEquals("check the value", value, attributeValues[j]);
            j++;
        }

        //Check the data content/values:
        List<DataObject> objList = dataGroup.values(); //
        for (int row = 0; row < objList.size(); row++) {
            for (int col = 0; col < dataTypes.length; col++) {
                String dataExpected = objList.get(row).getDataElement(dataTypes[col]).toString();
                Assert.assertEquals("check the 0th data value in the row", dataExpected, dataValues[row][col]);
            }
        }
    }


    // All the methods:

    //public static DataGroup readIpacTable(File f, String catName)
    //public static DataGroup readIpacTable(File f, String onlyColumns[], boolean useFloatsForDoubles, String catName)
    //public static DataGroup readIpacTable(File f, String onlyColumns[], boolean useFloatsForDoubles, String catName, boolean isHeadersOnlyAllow)

    //public static DataGroup readIpacTable(Reader fr, String catName)
    //public static DataGroup readIpacTable(Reader fr, String catName, long estFileSize)
    //public static DataGroup readIpacTable(Reader fr, String catName, String onlyColumns[],boolean useFloatsForDoubles,long estFileSize)
    //public static DataGroup readIpacTable(Reader fr, String catName, String onlyColumns[],boolean useFloatsForDoubles,long estFileSize, boolean isHeadersOnlyAllow)


}
