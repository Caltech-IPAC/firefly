package edu.caltech.ipac.astro;


import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;

import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.IpacTableWriter;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
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
public class IpacTableReaderTest extends ConfigTest{

    //Input parameters:
    String[] onlyColumns = null;
    private boolean noAttributes = false;

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

    @Test
    /**
     * This test calls the methods in IpacTableReader and verify if the produced dataGroup contains the correct information from the table.
            Keep onlyColumns = null to read all the columns.
     */

    public void testValidIpacTable() throws IpacTableException, IOException {

        //Input table:
        String input =
                "\\catalog1 = Sample Catalog1\n" +
                "\\catalog2 = Sample Catalog2\n" +
                "\\ Comment1\n" +
                "\\ Comment2\n" +
                "|   ra      |    dec    |   n_obs  |    V     |   SpType   |\n" +
                "|   double  |    double |   int    |   float  |   char     |\n" +
                "|   deg     |    deg    |          |   mag    |            |\n" +
                "|   null    |    null   |   null   |   null   |   null     |\n" +
                "  165.466279  -34.704730      5       11.27       K6Ve      \n" +
                "  123.4       5.67            9       8.9         K6Ve-1    ";

        //Set the expected values:

        //The attributes:
        attributeKeys = new String[]{"catalog1", "catalog2"};
        attributeValues = new String[]{"Sample Catalog1", "Sample Catalog2"};
        attributeComments = new String[]{"\\ Comment1", "\\ Comment2"};
        //The header:
        colTitles = new String[]{null, null, null, null, null};
        colKeyNames = new String[]{"ra", "dec", "n_obs", "V", "SpType"};
        colDataTypeDesc = new String[]{"double", "double", "int", "float", "char"};
        colDataTypes = new String[]{"class java.lang.Double", "class java.lang.Double", "class java.lang.Integer", "class java.lang.Float", "class java.lang.String"};
        colDataUnits = new String[]{"deg", "deg", "", "mag", ""};
        colNullStrings = new String[]{"null", "null", "null", "null", "null"};
        //The data:
        dataValues = new String[][]{{"165.466279", "-34.70473", "5", "11.27", "K6Ve"}, {"123.4", "5.67", "9", "8.9", "K6Ve-1"}};


        //Test the following methods:
        //public static DataGroup readIpacTable(Reader fr, String catName)
        //public static DataGroup readIpacTable(Reader fr, String catName, String onlyColumns[], boolean isHeadersOnlyAllow)

        DataGroup dataGroup = IpacTableReader.read(asInputStream(input));
        checkResult(dataGroup);

        dataGroup = IpacTableReader.read(asInputStream(input), onlyColumns);
        checkResult(dataGroup); //isHeadersOnlyAllow = true is never use.

        //Test the following methods:
        //public static DataGroup readIpacTable(File f, String catName)
        //public static DataGroup readIpacTable(File f, String onlyColumns[], String catName, boolean isHeadersOnlyAllow)

        File tempFile = new File("./temp.tbl");
        tempFile.deleteOnExit();

        IpacTableWriter.save(tempFile, dataGroup);
        dataGroup = IpacTableReader.read(tempFile);
        checkResult(dataGroup);
        dataGroup = IpacTableReader.read(tempFile, onlyColumns);
        checkResult(dataGroup); //useFloatsForDoubles has never be used!
        dataGroup = IpacTableReader.read(tempFile, onlyColumns);
        checkResult(dataGroup); //isHeadersOnlyAllow=true has never be used!

        //Test result: This IPAC table is read correctly.
        // Passed.
    }


    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable(StringReader fr, String catName) to read only two columns from a valid input IPAC table as a String,
     * and verify if the produced dataGroup contains the correct information from the table.
     */

    public void testOnlyColumns() throws IpacTableException, IOException {

        //Input table:
        String input =
                "\\catalog1 = Sample Catalog1\n" +
                "\\catalog2 = Sample Catalog2\n" +
                "\\ Comment1\n" +
                "\\ Comment2\n" +
                "|   ra      |    dec    |   n_obs  |    V     |   SpType   |\n" +
                "|   double  |    double |   int    |   float  |   char     |\n" +
                "|   deg     |    deg    |          |   mag    |            |\n" +
                "|   null    |    null   |   null   |   null   |   null     |\n" +
                "  165.466279  -34.704730      5       11.27       K6Ve      \n" +
                "  123.4       5.67            9       8.9         K6Ve-1    ";

        //Set the expected values:

        onlyColumns = new String[]{"ra", "dec"};

        //Reset the expected values:
        //Set the attributes:
        attributeKeys = new String[]{"catalog1", "catalog2"};
        attributeValues = new String[]{"Sample Catalog1", "Sample Catalog2"};
        attributeComments = new String[]{"\\ Comment1", "\\ Comment2"};
        //Set the header:
        colTitles = new String[]{null, null};
        colKeyNames = new String[]{"ra", "dec"};
        colDataTypeDesc = new String[]{"double", "double"};
        colDataTypes = new String[]{"class java.lang.Double", "class java.lang.Double"};
        colDataUnits = new String[]{"deg", "deg"};
        colNullStrings = new String[]{"null", "null"};
        //Set the data:
        dataValues = new String[][]{{"165.466279", "-34.70473"}, {"123.4", "5.67"}};

        //Read the table and generate the dataGroup:
        DataGroup dataGroup = IpacTableReader.read(asInputStream(input), onlyColumns);

        //Check the result:
        checkResult(dataGroup);

        //Test result: "onlyColumns" works fine. Only those two selected columns are read.
        // Passed.

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read a valid input IPAC table, IPACTableSample2_NoAttributes.tbl, which has no attributes.
     * More data types are tested.
     * Verify if the produced dataGroup contains the correct information from the table.
     *
     */
    public void testNoAttributes() throws IpacTableException, IOException {


        String input =
                "|   ra      |    dec    |   n_obs  |    V     |   SpType   |\n" +
                "|   double  |    double |   int    |   real   |   char     |\n" +
                "|   deg     |    deg    |          |   mag    |            |\n" +
                "|   null    |    null   |   null   |   null   |   null     |\n" +
                "  165.466279  -34.704730      5       11.27       K6Ve      \n" +
                "  123.4       5.67            9       8.9         K6Ve-1    ";

        noAttributes = true;

        DataGroup dataGroup = IpacTableReader.read(new ByteArrayInputStream(input.getBytes()),onlyColumns);


        List<DataGroup.Attribute> comments = dataGroup.getTableMeta().getKeywords();
        Assert.assertEquals("There should be 0 comments", 0, comments.size());
        List<DataGroup.Attribute> keywords = dataGroup.getTableMeta().getKeywords();
        Assert.assertEquals("There should be 0 keywords", 0, keywords.size());

        Assert.assertEquals("ra for row 1", 165.466279, (Double) dataGroup.get(0).getDataElement("ra"), 0.000001);

        //Test result: It is okay for an IPAC table not having any attributes. The table is read correctly.
        // Passed.

    }

    @Test
    /** This test calls the method IpacTableReader.readIpacTable to read a table, which has some wrong keywords and wrong comments.
     * Need to see how our table reader handle it.
     *
     */
    public void testWrongAttributes1() throws IpacTableException, IOException {

        String input =
        "\\ catalog1 = 'A space makes this line as a comment'\n" +
        "\\key:3\n" +
        "\\Missing the leading space so this line will be ignored\n" +
        "|   ra      |    dec    |   n_obs  |    V     |   SpType   |\n" +
        "|   double  |    double |   int    |   real   |   char     |\n" +
        "|   deg     |    deg    |          |   mag    |            |\n" +
        "|   null    |    null   |   null   |   null   |   null     |\n" +
        "  165.466279  -34.704730      5       11.27       K6Ve      \n" +
        "  123.4       5.67            9       8.9         K6Ve-1    ";

        DataGroup dataGroup = IpacTableReader.read(asInputStream(input));


        //Check the Attributes (comments):
        List<DataGroup.Attribute> attributes = dataGroup.getTableMeta().getKeywords();
        Assert.assertTrue(attributes.get(0).isComment());
        Assert.assertEquals("The first attribute has a space so it is parsed as a comment.",
                attributes.get(0).toString(), "\\ catalog1 = 'A space makes this line as a comment'");

        Assert.assertEquals("The last two lines will be ignored as they have no leading space nor '=' ", 1, attributes.size());

        /*Test result:

        If a "keyword" has a space after the "\", it will be treated as a comment. Nothing can be done in the table reader about this.
        If a keyword has no "=" , it will be ignored. Nothing can be done in the table reader.
        If a comment has no space after "\", it will be ignored. Nothing can be done in the table reader.

        */

        // Passed.

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read a table with some wrong attributes missing the first "\".
     * Test if an exception will be thrown out.
     */
    public void testWrongAttributes2() {


        String input =
                "catalog2 = missing the back slash and no space\n" +
                "|   ra      |    dec    |   n_obs  |    V     |   SpType   |\n" +
                "|   double  |    double |   int    |   real   |   char     |\n" +
                "|   deg     |    deg    |          |   mag    |            |\n" +
                "|   null    |    null   |   null   |   null   |   null     |\n" +
                "  165.466279  -34.704730      5       11.27       K6Ve      \n" +
                "  123.4       5.67            9       8.9         K6Ve-1    ";


        try {
            DataGroup dataGroup = IpacTableReader.read(asInputStream(input));
            Assert.fail("No exception thrown.");
        } catch (IOException e) {

        }

        //Test result:
        //If an attribute has no "\" nor space at beginning, it will trigger IpacTableException: "Data row must start with a space." and no dataGroup is generated.

        //Passed

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read a table which has duplicate column names.
     * Test if an exception will be thrown out.
     */
    public void testDuplicateColumn() {

    String input =
            "\\catalog1 = Sample Catalog1\n" +
            "\\catalog2 = Sample Catalog2\n" +
            "\\ Comment1\n" +
            "\\ Comment2\n" +
            "|   ra      |    ra     |   n_obs  |    V     |   SpType   |\n" +
            "|   double  |    double |   int    |   float  |   char     |\n" +
            "|   deg     |    deg    |          |   mag    |            |\n" +
            "|   null    |    null   |   null   |   null   |   null     |\n" +
            "  165.466279  -34.704730      5       11.27       K6Ve      \n" +
            "  123.4       5.67            9       8.9         K6Ve-1    ";



        try{
            DataGroup dataGroup = IpacTableReader.read(asInputStream(input));
            //After DM-9332 is implemented uncomment this:
            //Assert.fail("No exception thrown out");
            //After DM-9332, the LOG.WARN should be deleted:
            LOG.warn("This test should trigger IpacTableException but it doesn't. After DM-9332 fixes it, the test needs to be updated.");

        } catch (IOException e){

        }


        /* How to find the problem:
        DataGroup dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName);
        DataType[] dataTypes = dataGroup.getDataDefinitions();
        List<String> colTitle = new ArrayList<String>();
        for (int i = 0; i < dataTypes.length; i++) {
            colTitle.add(dataTypes[i].getDefaultTitle().toString());
        }
        LOG.warn("The duplicate columns should not be allowed: " + colTitle);
        */

        //Test result:
        //The duplicate columns are not detected. A dataGroup is generated with those duplicate columns!
        //Action item: Issue a ticket: https://jira.lsstcorp.org/browse/DM-9332

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable(StringReader fr, String catName) to read a table with data type as "real".
     * Test if it can be handled correctly.
     */
    public void testDatatype_real() {

        String input =
                "\\catalog1 = Sample Catalog1\n" +
                "\\catalog2 = Sample Catalog2\n" +
                "\\ Comment1\n" +
                "\\ Comment2\n" +
                "|   ra      |    dec    |   n_obs  |    V     |   SpType   |\n" +
                "|   double  |    double |   int    |   real   |   char     |\n" +
                "|   deg     |    deg    |          |   mag    |            |\n" +
                "|   null    |    null   |   null   |   null   |   null     |\n" +
                "  165.466279  -34.704730      5       11.27       K6Ve      \n" +
                "  123.4       5.67            9       8.9         K6Ve-1    ";


        /* After the DM-9026 is implemented, use this:
        //The attributes:
        attributeKeys = new String[]{"catalog1", "catalog2"};
        attributeValues = new String[]{"\'Sample Catalog1\'", "\'Sample Catalog2\'"};
        attributeComments = new String[]{"\\ Comment1", "\\ Comment2"};
        //The header:
        colTitles = new String[]{"ra", "dec", "n_obs", "V", "SpType"};
        colKeyNames = new String[]{"ra", "dec", "n_obs", "V", "SpType"};
        colDataTypeDesc = new String[]{"double", "float", "int", "real", "char"};
        colDataTypes = new String[]{"class java.lang.Double", "class java.lang.Double", "class java.lang.Integer", "class java.lang.Double", "class java.lang.String"}; //real -> Double!
        colDataUnits = new String[]{"deg", "deg", "", "mag", ""};
        colNullStrings = new String[]{"null", "null", "null", "null", "null"};
        //The data:
        dataValues = new String[][]{{"165.466279", "-34.70473", "5", "11.27", "K6Ve"}, {"123.4", "5.67", "9", "8.9", "K6Ve-1"}};

        //Get the dataGroup and check:
        DataGroup dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName);
        checkResult(dataGroup);
        */

        //Test result:
        //The 4th one is "real" in the table but currently the IPAC table treat it as "char". DM-9026.
        LOG.warn("The data type \'real\' is parsed as a char. DM-9026 will parse real as double and then the test needs some updates.");

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read a table which has no data.
     * Test if an exception is thrown out.
     */
    public void testNoData() {


        String input =
                "\\catalog1 = Sample Catalog1\n" +
                "\\catalog2 = Sample Catalog2\n" +
                "\\ Comment1\n" +
                "\\ Comment2\n" +
                "|   ra      |    dec    |   n_obs  |    V     |   SpType   |\n" +
                "|   double  |    double |   int    |   real   |   char     |\n" +
                "|   deg     |    deg    |          |   mag    |            |\n" +
                "|   null    |    null   |   null   |   null   |   null     |";


        try{
            DataGroup dataGroup = IpacTableReader.read(asInputStream(input));
            Assert.assertNotNull(dataGroup);
//            Assert.fail("No exception thrown out.");   ... it's okay as long as the headers are good.
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Test result: IpacTableException is thrown.
        //Passed.

    }

    /**
     * This test IpacTableReader handling of NULL related values
     */
    @Test
    public void testNullValues() {
        String input =  "|ra      |dec    |spec   |SpType   |\n" +
                        "|double  |double |char   |char     |\n" +
                        "|        |       |       |         |\n" +
                        "|null    |       |       |null     |\n" +
                        " 123.4    5.67    null    K6Ve-1    \n" +
                        " null                               \n" +
                        " 123.4    5.67    abc     null      \n";
        try{
            DataGroup dg = IpacTableReader.read(asInputStream(input));

            Assert.assertNull(dg.getData("ra", 1));                         // value is same as NULL_STR, so value should be read in as null
            Assert.assertNull(dg.getData("dec", 1));                        // value is same as NULL_STR, so value should be read in as null

            Assert.assertEquals("null", dg.getData("spec", 0));    // NULL_STR is empty_str, so null should be interpreted as a string
            Assert.assertNull(dg.getData("spec", 1));                       // value is same as NULL_STR, so value should be read in as null

            Assert.assertEquals("", dg.getData("SpType", 1));      // NULL_STR is 'null', so blank should be interpreted as an empty string
            Assert.assertNull(dg.getData("SpType", 2));                     // value is same as NULL_STR, so value should be read in as null
        } catch (IOException e) {
            Assert.fail("Unexpected read error:" + e.getMessage());
        }

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read a table which has one datum under "|".
     * Test if an exception is thrown out.
     */
    public void testDataUnderBar() {


        String input =
                "\\catalog1 = Sample Catalog1\n" +
                "\\catalog2 = Sample Catalog2\n" +
                "\\ Comment1\n" +
                "\\ Comment2\n" +
                "|   ra      |    dec    |   n_obs  |    V     |   SpType   |\n" +
                "|   double  |    double |   int    |   float  |   char     |\n" +
                "|   deg     |    deg    |          |   mag    |            |\n" +
                "|   null    |    null   |   null   |   null   |   null     |\n" +
                "   165.466279 -34.704730      5       11.27       K6Ve      \n" +
                "  123.4       5.67            9       8.9         K6Ve-1    ";


        try{
            DataGroup dataGroup = IpacTableReader.read(asInputStream(input));
            //After https://jira.lsstcorp.org/browse/DM-9333 is implemented, use this Assert.fail and delete the LOG.warn below:
            //Assert.fail("No exception is thrown out");
            LOG.warn("When some data are under \'|\' in an IPAC table, the reader doesn't throw out IpacTableException. DM-9333 will fix it and then this test needs some mods.");
        } catch (IOException e) {
        }

        /* How to find the problem:
        DataGroup dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName);
        Assert.assertNotEquals(165.466279, (Double)dataGroup.get(0).getDataElement("ra"));
        Assert.assertNotEquals(-34.704730, (Double)dataGroup.get(0).getDataElement("dec"));
        */

        //Test result: No exception. A dataGroup is generated but with wrong content! 165.466279 is truncated to 165.46627; -34.704730 is missing.
        //Action item: Issue a ticket https://jira.lsstcorp.org/browse/DM-9333

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read a table which has no header.
     * Test if an exception is thrown out.
     */
    public void testNoHeader() {


        String input =
                "\\catalog1 = Sample Catalog1\n" +
                "\\catalog2 = Sample Catalog2\n" +
                "\\ Comment1\n" +
                "\\ Comment2\n" +
                "  165.466279  -34.704730      5       11.27       K6Ve\n" +
                "  123.4       5.67            9       8.9         K6Ve-1";


        try{
            DataGroup dataGroup = IpacTableReader.read(asInputStream(input));
            //After https://jira.lsstcorp.org/browse/DM-9335 is implemented, use the Assert.fail and delete the LOG.warn below:
            //Assert.fail("No exception is thrown out.");
            LOG.warn("IPAC table reader doesn't catch the no header problem. DM-9335 will fix it and then the test needs some mods.");
        }catch(IOException e) {

        }

        //Test result: The dataGroup is genearated but no dataTypes and data. When try to use the data, java.lang.IllegalArgumentException is thrown.
        //Action item: Issue a ticket: https://jira.lsstcorp.org/browse/DM-9335

    }

    
    /**
     *
     * @param dataGroup
     */

    private void checkResult(DataGroup dataGroup) {

        //Check data definitions:
        DataType[] dataTypes = dataGroup.getDataDefinitions();

        for (int i = 0; i < dataTypes.length; i++) {
            Assert.assertEquals("check column title", dataTypes[i].getLabel(), colTitles[i]);
            Assert.assertEquals("check column key name", dataTypes[i].getKeyName(), colKeyNames[i]);
            Assert.assertEquals("check column data type short description", dataTypes[i].getTypeDesc(), colDataTypeDesc[i]);
            Assert.assertEquals("check column data type class", dataTypes[i].getDataType().toString(), colDataTypes[i]);
            Assert.assertEquals("check the column unit", dataTypes[i].getUnits(), colDataUnits[i]);
            Assert.assertEquals("check column null string", dataTypes[i].getNullString(), colNullStrings[i]);
        }

        //Check the Attributes (comments):
        List<DataGroup.Attribute> attributes = dataGroup.getTableMeta().getKeywords();
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
        List<DataGroup.Attribute> keywords = dataGroup.getTableMeta().getAttributeList();
        for (int j = 0; j < keywords.size() -1; j++) {
            //Not check the input file source:
            if (j < (keywords.size() - 1)) {
                Assert.assertEquals("check the key", keywords.get(j).getKey(), attributeKeys[j]);
                Assert.assertEquals("check the value", keywords.get(j).getValue(), attributeValues[j]);
            }
            j++;
        }

        //Check the data content/values:
        List<DataObject> objList = dataGroup.values(); //
        for (int row = 0; row < objList.size(); row++) {
            for (int col = 0; col < dataTypes.length; col++) {
                String dataExpected = String.valueOf(objList.get(row).getDataElement(dataTypes[col]));
                Assert.assertEquals("check the data value", dataExpected, dataValues[row][col]);
            }
        }
    }

    private static InputStream asInputStream(String input) {
        return new ByteArrayInputStream(input.getBytes());
    }
}
