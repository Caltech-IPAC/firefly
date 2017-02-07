package edu.caltech.ipac.astro;


import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
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
    private static final String catName = "catalogName";
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

    @Test
    /**
     * This test calls the methods in IpacTableReader and verify if the produced dataGroup contains the correct information from the table.
            Keep onlyColumns = null to read all the columns.
     */

    public void testValidIpacTable() {

        //Input table:
        String input =
                "\\catalog1 = 'Sample Catalog1'\n" +
                "\\catalog2 = 'Sample Catalog2'\n" +
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
        attributeValues = new String[]{"\'Sample Catalog1\'", "\'Sample Catalog2\'"};
        attributeComments = new String[]{"\\ Comment1", "\\ Comment2"};
        //The header:
        colTitles = new String[]{"ra", "dec", "n_obs", "V", "SpType"};
        colKeyNames = new String[]{"ra", "dec", "n_obs", "V", "SpType"};
        colDataTypeDesc = new String[]{"double", "double", "int", "float", "char"};
        colDataTypes = new String[]{"class java.lang.Double", "class java.lang.Double", "class java.lang.Integer", "class java.lang.Float", "class java.lang.String"};
        colDataUnits = new String[]{"deg", "deg", "", "mag", ""};
        colNullStrings = new String[]{"null", "null", "null", "null", "null"};
        //The data:
        dataValues = new String[][]{{"165.466279", "-34.70473", "5", "11.27", "K6Ve"}, {"123.4", "5.67", "9", "8.9", "K6Ve-1"}};


        try {
            //public static DataGroup readIpacTable(Reader fr, String catName)
            //public static DataGroup readIpacTable(Reader fr, String catName, long estFileSize)
            //public static DataGroup readIpacTable(Reader fr, String catName, String onlyColumns[],boolean useFloatsForDoubles,long estFileSize)
            //public static DataGroup readIpacTable(Reader fr, String catName, String onlyColumns[],boolean useFloatsForDoubles,long estFileSize, boolean isHeadersOnlyAllow)

            DataGroup dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName);
            checkResult(dataGroup);

            estFileSize = 100;
            dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName, estFileSize);
            checkResult(dataGroup); //estFileSize is never used.

            useFloatsForDoubles = true;
            dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName, onlyColumns, useFloatsForDoubles, estFileSize);
            checkResult(dataGroup); //useFloatForDoubles is never used.

            isHeadersOnlyAllow = true;
            dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName, onlyColumns, useFloatsForDoubles, estFileSize, isHeadersOnlyAllow);
            checkResult(dataGroup); //isHeadersOnlyAllow = true is never use.

            //Test the following methods:
            //public static DataGroup readIpacTable(File f, String catName)
            //public static DataGroup readIpacTable(File f, String onlyColumns[], boolean useFloatsForDoubles, String catName)
            //public static DataGroup readIpacTable(File f, String onlyColumns[], boolean useFloatsForDoubles, String catName, boolean isHeadersOnlyAllow)

            File tempFile = new File("./temp.tbl");
            IpacTableWriter.save(tempFile, dataGroup);
            dataGroup = IpacTableReader.readIpacTable(tempFile, catName);
            checkResult(dataGroup);
            useFloatsForDoubles = true;
            dataGroup = IpacTableReader.readIpacTable(tempFile, onlyColumns, useFloatsForDoubles, catName);
            checkResult(dataGroup); //useFloatsForDoubles has never be used!
            isHeadersOnlyAllow = true;
            dataGroup = IpacTableReader.readIpacTable(tempFile, onlyColumns, useFloatsForDoubles, catName, isHeadersOnlyAllow);
            checkResult(dataGroup); //isHeadersOnlyAllow=true has never be used!

            tempFile.delete();

        }catch (Exception e){
            //?
        }

        //Test result: This IPAC table is read correctly.
        // Passed.
    }


    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable(StringReader fr, String catName) to read only two columns from a valid input IPAC table as a String,
     * and verify if the produced dataGroup contains the correct information from the table.
     */

    public void testOnlyColumns() {

        //Input table:
        String input =
                "\\catalog1 = 'Sample Catalog1'\n" +
                "\\catalog2 = 'Sample Catalog2'\n" +
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
        attributeValues = new String[]{"\'Sample Catalog1\'", "\'Sample Catalog2\'"};
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

        try {
            //Read the table and generate the dataGroup:
            DataGroup dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName, onlyColumns, useFloatsForDoubles, estFileSize, isHeadersOnlyAllow);

            //Check the result:
            checkResult(dataGroup);
        }catch(IpacTableException e){

        }

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
    public void testNoAttributes() {


        String input =
                "|   ra      |    dec    |   n_obs  |    V     |   SpType   |\n" +
                "|   double  |    double |   int    |   real   |   char     |\n" +
                "|   deg     |    deg    |          |   mag    |            |\n" +
                "|   null    |    null   |   null   |   null   |   null     |\n" +
                "  165.466279  -34.704730      5       11.27       K6Ve      \n" +
                "  123.4       5.67            9       8.9         K6Ve-1    ";

        noAttributes = true;

        try {
            DataGroup dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName, onlyColumns, useFloatsForDoubles, estFileSize, isHeadersOnlyAllow);

            List<DataGroup.Attribute> comments = dataGroup.getKeywords();
            Assert.assertEquals("There should be 0 comments", 0, comments.size());
            Map<String, DataGroup.Attribute> keywordsMap = dataGroup.getAttributes();
            Assert.assertEquals("There should be 0 keywords", 0, keywordsMap.size());

            Assert.assertEquals("ra for row 1", 165.466279, (Double) dataGroup.get(0).getDataElement("ra"), 0.001);
        }catch(IpacTableException e){

        }


        //Test result: It is okay for an IPAC table not having any attributes. The table is read correctly.
        // Passed.

    }

    @Test
    /** This test calls the method IpacTableReader.readIpacTable to read a table, which has some wrong keywords and wrong comments.
     * Need to see how our table reader handle it.
     *
     */
    public void testWrongAttributes1() {

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

        try {
            DataGroup dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName);

            //Check the Attributes (comments):
            List<DataGroup.Attribute> attributes = dataGroup.getKeywords();
            Assert.assertTrue(attributes.get(0).isComment());
            Assert.assertEquals("The first attribute has a space so it is parsed as a comment.",
                    attributes.get(0).toString(), "\\ catalog1 = 'A space makes this line as a comment'");

            Assert.assertEquals("The last two lines will be ignored as they have no leading space nor '=' ", 1, attributes.size());

        }catch(IpacTableException e){

        }

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
                "\\ Comment2 Valid comment\n" +
                "|   ra      |    dec    |   n_obs  |    V     |   SpType   |\n" +
                "|   double  |    double |   int    |   real   |   char     |\n" +
                "|   deg     |    deg    |          |   mag    |            |\n" +
                "|   null    |    null   |   null   |   null   |   null     |\n" +
                "  165.466279  -34.704730      5       11.27       K6Ve      \n" +
                "  123.4       5.67            9       8.9         K6Ve-1    ";


        try{
            DataGroup dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName);
            Assert.fail("No exception thrown.");
        } catch (Exception e){

        }

        //Test result:
        //If an attribute starts with a space, it will be ignored.
        //If an attribute has no "\" nor space at beginning, it will trigger IpacTableException: "Data row must start with a space." and no dataGroup is generated.

        //Passed

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read a table which has duplicate column names.
     * Test if an exception will be thrown out.
     */
    public void testDuplicateColumn(){

    String input =
            "\\catalog1 = 'Sample Catalog1'\n" +
            "\\catalog2 = 'Sample Catalog2'\n" +
            "\\ Comment1\n" +
            "\\ Comment2\n" +
            "|   ra      |    ra     |   n_obs  |    V     |   SpType   |\n" +
            "|   double  |    double |   int    |   float  |   char     |\n" +
            "|   deg     |    deg    |          |   mag    |            |\n" +
            "|   null    |    null   |   null   |   null   |   null     |\n" +
            "  165.466279  -34.704730      5       11.27       K6Ve      \n" +
            "  123.4       5.67            9       8.9         K6Ve-1    ";



        try{
            DataGroup dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName);
            // After DM-9332 is implemented uncomment this:
            //Assert.fail("No exception thrown out");

        } catch (IpacTableException e){

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
                "\\catalog1 = 'Sample Catalog1'\n" +
                "\\catalog2 = 'Sample Catalog2'\n" +
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

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read a table which has no data.
     * Test if an exception is thrown out.
     */
    public void testNoData() {


        String input =
                "\\catalog1 = 'Sample Catalog1'\n" +
                "\\catalog2 = 'Sample Catalog2'\n" +
                "\\ Comment1\n" +
                "\\ Comment2\n" +
                "|   ra      |    dec    |   n_obs  |    V     |   SpType   |\n" +
                "|   double  |    double |   int    |   real   |   char     |\n" +
                "|   deg     |    deg    |          |   mag    |            |\n" +
                "|   null    |    null   |   null   |   null   |   null     |";


        try{
            DataGroup dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName);
            Assert.fail("No exception thrown out.");
        } catch (IpacTableException e){

        }

        //Test result: IpacTableException is thrown.
        //Passed.

    }

    @Test
    /**
     * This test calls the method IpacTableReader.readIpacTable to read a table which has one datum under "|".
     * Test if an exception is thrown out.
     */
    public void testDataUnderBar() {


        String input =
                "\\catalog1 = 'Sample Catalog1'\n" +
                "\\catalog2 = 'Sample Catalog2'\n" +
                "\\ Comment1\n" +
                "\\ Comment2\n" +
                "|   ra      |    dec    |   n_obs  |    V     |   SpType   |\n" +
                "|   double  |    double |   int    |   float  |   char     |\n" +
                "|   deg     |    deg    |          |   mag    |            |\n" +
                "|   null    |    null   |   null   |   null   |   null     |\n" +
                "   165.466279 -34.704730      5       11.27       K6Ve      \n" +
                "  123.4       5.67            9       8.9         K6Ve-1    ";


        try{
            DataGroup dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName);
            //After https://jira.lsstcorp.org/browse/DM-9333 is implemented, use this:
            //Assert.fail("No exception is thrown out");
        }catch(IpacTableException e){

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
                "\\catalog1 = 'Sample Catalog1'\n" +
                "\\catalog2 = 'Sample Catalog2'\n" +
                "\\ Comment1\n" +
                "\\ Comment2\n" +
                "  165.466279  -34.704730      5       11.27       K6Ve\n" +
                "  123.4       5.67            9       8.9         K6Ve-1";


        try{
            DataGroup dataGroup = IpacTableReader.readIpacTable(new StringReader(input), catName);
            //After https://jira.lsstcorp.org/browse/DM-9335 is implemented, use this:
            //Assert.fail("No exception is thrown out.");
        }catch(IpacTableException e) {

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
            Assert.assertEquals("check column title", dataTypes[i].getDefaultTitle().toString(), colTitles[i]);
            Assert.assertEquals("check column key name", dataTypes[i].getKeyName().toString(), colKeyNames[i]);
            Assert.assertEquals("check column data type short description", dataTypes[i].getTypeDesc().toString(), colDataTypeDesc[i]);
            Assert.assertEquals("check column data type class", dataTypes[i].getDataType().toString(), colDataTypes[i]);
            Assert.assertEquals("check the column unit", dataTypes[i].getDataUnit().toString(), colDataUnits[i]);
            Assert.assertEquals("check column null string", dataTypes[i].getNullString().toString(), colNullStrings[i]);
        }

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
            //Not check the input file source:
            if (j < (attributeMap.size() - 1)) {
                Assert.assertEquals("check the key", entry.getKey(), attributeKeys[j]);
                value = ((DataGroup.Attribute) entry.getValue()).getValue().toString();
                //System.out.println(value + " " + attributeValues[j] + " " + j);
                Assert.assertEquals("check the value", value, attributeValues[j]);
            }
            j++;
        }

        //Check the data content/values:
        List<DataObject> objList = dataGroup.values(); //
        for (int row = 0; row < objList.size(); row++) {
            for (int col = 0; col < dataTypes.length; col++) {
                String dataExpected = objList.get(row).getDataElement(dataTypes[col]).toString();
                Assert.assertEquals("check the data value", dataExpected, dataValues[row][col]);
            }
        }
    }

}
