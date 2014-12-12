package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.client.ClientLog;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.astro.IpacTableWriter;
import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.usVo.xml.voTable.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.FileInputStream;


/**
 * Date: Jan 20, 2006
 *
 * @author Trey Roby
 * @version $id:$
 */
public class FixedObjectGroupUtils {

//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================

//============================================================================
//---------------------------- Public Methods --------------------------------
//============================================================================

    public static FixedObjectGroup makeFixedObjectGroup(
                                  VOTABLEDocument voTableDoc)
                                  throws FailedRequestException {
        return makeFixedObjectGroup(voTableDoc,null);
    }
    public static FixedObjectGroup makeFixedObjectGroup(
                                  VOTABLEDocument voTableDoc,
                                  Map<String,DataType.Enum> overrideFieldTypes)
                                  throws FailedRequestException {
        VOTABLEDocument.VOTABLE voTable = voTableDoc.getVOTABLE();
        checkStatus(voTable); // check for errors from VOTABLE provider
        RESOURCEDocument.RESOURCE[] resources = voTable.getRESOURCEArray();
        if (resources.length > 0) {
            return makeFixedObjectGroup(resources[0], overrideFieldTypes, null, true);
        } else {
            return new FixedObjectGroup();
        }
    }


    public static FixedObjectGroup[] makeMultipleFixedObjectGroup(
                                  VOTABLEDocument voTableDoc,
                                  Map<String,DataType.Enum> overrideFieldTypes)
                                                     throws FailedRequestException {
        return makeMultipleFixedObjectGroup(voTableDoc, overrideFieldTypes, null, true);
    }


    public static FixedObjectGroup[] makeMultipleFixedObjectGroup(
                                       VOTABLEDocument voTableDoc,
                                       Map<String,DataType.Enum> overrideFieldTypes,
                                       List<FixedObjectGroup.ParseGroup> pgList,
                                      boolean useUCDforTitle)
                                  throws FailedRequestException {
        VOTABLEDocument.VOTABLE voTable = voTableDoc.getVOTABLE();
        RESOURCEDocument.RESOURCE[] resources = voTable.getRESOURCEArray();
        List<FixedObjectGroup> retval= new ArrayList<FixedObjectGroup>(resources.length);
        for(RESOURCEDocument.RESOURCE resource : resources) {
            TABLEDocument.TABLE table[] = resource.getTABLEArray();
            if (table != null && table.length > 0) {
                DATADocument.DATA data= table[0].getDATA();
                TABLEDATADocument.TABLEDATA tabledata = data.getTABLEDATA();
                TRDocument.TR rowAry[] = tabledata.getTRArray();
                if (rowAry != null && rowAry.length > 0) {
                    retval.add(makeFixedObjectGroup(resource, overrideFieldTypes,
                                                    pgList, useUCDforTitle));
                }
            }
        }
        if (retval.size()==0) {
            return new FixedObjectGroup[0];
        } else {
         return retval.toArray(new FixedObjectGroup[retval.size()]);
        }

    }

    public static DataGroup getDataGroup(RESOURCEDocument.RESOURCE resource, Map<String,DataType.Enum> overrideFieldTypes, boolean useUCDforID, boolean useUCDforTitle)
        throws FailedRequestException {

        String tableDesc= cleanXML(resource.getDESCRIPTION());
        TABLEDocument.TABLE table[] = resource.getTABLEArray();
        if (table == null || table.length == 0) {
            return new DataGroup("No Data", new ArrayList<edu.caltech.ipac.util.DataType>(0));
        }
        FIELDDocument.FIELD fields[] = table[0].getFIELDArray();
        if (tableDesc==null) tableDesc= cleanXML(table[0].getDESCRIPTION());
        DATADocument.DATA data= table[0].getDATA();
        if (data == null) {
            return new DataGroup("No Data", new ArrayList<edu.caltech.ipac.util.DataType>(0));
        }
        TABLEDATADocument.TABLEDATA tabledata = data.getTABLEDATA();

        edu.caltech.ipac.util.DataType dType;
        List<edu.caltech.ipac.util.DataType> dTypeList=
                  new ArrayList<edu.caltech.ipac.util.DataType>(fields.length);
       String fieldID;
        for(FIELDDocument.FIELD field: fields){
            DataType.Enum sType= field.getDatatype();
            Class cType;

            // determine the field id
            if (useUCDforID && field.getUcd() != null) {
                fieldID = field.getUcd();
            } else if (field.getName() != null) {
                fieldID = (field.getID()!=null && !field.getID().equals(field.getName())) ?
                          field.getID()+field.getName() :
                          field.getName();
            }
            else {
                fieldID = field.getID();
            }

            // determine the field title (what the user sees)
            String title= useUCDforTitle ? field.getUcd() : null;
            if (title==null) {
                title= field.getName();
                if (title==null) title= field.getID();
                title= title.replace('_', ' ');
            }


            // determine the data type: use String for arrays
            if (overrideFieldTypes!=null && overrideFieldTypes.containsKey(fieldID)) {
                sType= overrideFieldTypes.get(fieldID);
                cType= getTranslateDatatype(sType);
            }
            else {
                cType= (field.isSetArraysize() && field.getArraysize().length()>0) ? 
                            String.class : getTranslateDatatype(sType);
            }

            dType=  new edu.caltech.ipac.util.DataType(fieldID, title, cType,
                        edu.caltech.ipac.util.DataType.Importance.HIGH, field.getUnit(), false);

            AnyTEXT descAry[]= field.getDESCRIPTIONArray();
            if (descAry.length>0) {
                String s;
                StringBuffer sb= new StringBuffer(200);
                for(AnyTEXT desc : descAry) {
                    sb.append(" ");
                    s= cleanXML(desc);
                    sb.append(s);
                }
                dType.setShortDesc(sb.toString());
            }

            // set format info, in which width will be saved
            // align character data left, everything else - right
            edu.caltech.ipac.util.DataType.FormatInfo formatInfo = new edu.caltech.ipac.util.DataType.FormatInfo();
            if (cType.equals(String.class)) {
                formatInfo.setHeaderAlign(edu.caltech.ipac.util.DataType.FormatInfo.Align.LEFT);
                formatInfo.setDataAlign(edu.caltech.ipac.util.DataType.FormatInfo.Align.LEFT);
            } else {
                formatInfo.setHeaderAlign(edu.caltech.ipac.util.DataType.FormatInfo.Align.RIGHT);
                formatInfo.setDataAlign(edu.caltech.ipac.util.DataType.FormatInfo.Align.RIGHT);
            }
            dType.setFormatInfo(formatInfo);
            dTypeList.add(dType);

        }
        if (tableDesc == null) tableDesc = "VOTable";
        // populate data
        TRDocument.TR rowAry[] = tabledata.getTRArray();
        if (rowAry == null || rowAry.length == 0) {
            return new DataGroup(tableDesc, dTypeList);  // empty table with valid headers
        }

        DataGroup dataGroup= new DataGroup(tableDesc,dTypeList);
        dataGroup.beginBulkUpdate();
        DataObject dataObject;
        String vStr;
        for(TRDocument.TR row: rowAry){
            TDDocument.TD entryAry[] = row.getTDArray();
            //System.out.printf("New Row: %d\n", cnt++);
            dataObject= new DataObject(dataGroup);
            for(int i=0; (i<entryAry.length); i++) {
                // TODO: internal markup is stripped out
                vStr= parseTD(entryAry[i]);
                if (vStr!=null) {
                    if (vStr.length() > 1 ) {
                        vStr = vStr.replaceAll("\n", ";"); // get rid of newlines
                    }
                    dType = dTypeList.get(i);
                    dataObject.setDataElement(dType,
                                              dType.convertStringToData(vStr) );
                    //System.out.println("   " +vStr);
                    if (vStr.length()>dType.getFormatInfo().getWidth()) {
                        dType.getFormatInfo().setWidth(vStr.length());
                    }
                }
            }
            dataGroup.add(dataObject);
            //System.out.println();
        }

        dataGroup.endBulkUpdate();

        return dataGroup;
    }

    private static FixedObjectGroup makeFixedObjectGroup(
                                  RESOURCEDocument.RESOURCE resource,
                                  Map<String,DataType.Enum> overrideFieldTypes,
                                  List<FixedObjectGroup.ParseGroup> pgList,
                                  boolean useUCDforTitle)
                                          throws FailedRequestException {

        DataGroup dataGroup = getDataGroup(resource, overrideFieldTypes, false, useUCDforTitle);

        String raField[]= makeRAFieldAry(pgList);
        String decField[]= makeDecFieldAry(pgList);

        // might be dangerous if pgList is not used
        for (edu.caltech.ipac.util.DataType dType : dataGroup.getDataDefinitions()) {
            Class cType = dType.getDataType();
            if (cType==String.class &&
                    (StringUtil.matchesRegExpList(dType.getKeyName(),raField,true)  ||
                            StringUtil.matchesRegExpList(dType.getKeyName(),decField,true)))  {
                dType.setDataType(null);  // set to guess type later
            }
        }
        guessUndefinedTypes(dataGroup);


        FixedObjectGroup fixedGroup= new FixedObjectGroup();

        if (dataGroup.size()>0) {

            try {
               if (pgList!=null) {
                   fixedGroup= new FixedObjectGroup(dataGroup, null, pgList);
               }
               else {
                   fixedGroup= new FixedObjectGroup(dataGroup);
               }

            } catch (ColumnException e) {
                ClientLog.warning("Could not parse VOTABLEDocument.",
                                  e.toString(),
                                  "rows: " + dataGroup.size());
                throw new FailedRequestException("Could not parse ISO data.",
                                                 "VOTABLEDocument contains unexpected results.",e);
            }

        } else {

        }
        fixedGroup.setTitle(dataGroup.getTitle());

        return fixedGroup;
    }

    private static String[] makeRAFieldAry(List<FixedObjectGroup.ParseGroup> pgList) {

        List<String> retList= new ArrayList<String>(5);
        if (pgList!=null) {
            for(FixedObjectGroup.ParseGroup pg : pgList){
                for(String s : pg.getRaNameOptions()) {
                    retList.add(s);
                }
            }
        }
        if (retList.size()==0) {
            retList.add(".*ra.*");
        }
        return retList.toArray(new String[retList.size()]);
    }


    private static String[] makeDecFieldAry(List<FixedObjectGroup.ParseGroup> pgList) {

        List<String> retList= new ArrayList<String>(5);
        if (pgList!=null) {
            for(FixedObjectGroup.ParseGroup pg : pgList){
                for(String s : pg.getDecNameOptions()) {
                    retList.add(s);
                }
            }
        }
        if (retList.size()==0) {
            retList.add(".*dec.*");
        }
        return retList.toArray(new String[retList.size()]);
    }

    /**
     * Check for errors from VOTABLE producer.
     * @param voTable  VOTABLE object - maps to VOTABLE element
     * @throws edu.caltech.ipac.client.net.FailedRequestException when error is present
     */
    public static void checkStatus(VOTABLEDocument.VOTABLE voTable)
            throws FailedRequestException {
        // Simple Image Access spec says that RESOURCE element must have an INFO child,
        // with attributes name="QUERY_STATUS" and value="OK", or "ERROR", or "OVERFLOW".
        // In practice, INFO element with status might be a sibling of RESOURCE.
        // Cone Search spec says that in the case of error, the service MUST respond
        // with a VOTable that contains a single PARAM element or a single INFO element
        // with name="Error", where the corresponding value attribute should contain
        // some explanation of the nature of the error.
        XmlObject[] infoObjs = voTable.selectPath("declare namespace s='"+voTable.getDomNode().getNamespaceURI()+"' .//s:INFO");
        String valueStr;
        if (infoObjs != null && infoObjs.length > 0) {
            INFODocument.INFO info;
            for (XmlObject infoObj : infoObjs) {
                info = (INFODocument.INFO)infoObj;
                if (info.getName().equalsIgnoreCase("QUERY_STATUS")) {
                    valueStr = info.getValue();
                    if (valueStr.equalsIgnoreCase("ERROR")) {
                        throw new FailedRequestException("Error QUERY_STATUS in VOTABLE.", info.getDomNode().getLastChild().getNodeValue());
                    } else if (valueStr.equalsIgnoreCase("OVERFLOW")) {
                        throw new FailedRequestException("Overflow Error QUERY_STATUS in VOTABLE.", info.getDomNode().getLastChild().getNodeValue());
                    } else if (!valueStr.equalsIgnoreCase("OK")) {
                        ClientLog.warning("Unrecognized QUERY_STATUS in VOTABLE: "+info.getValue());
                    }
                    break;
                } else if (info.getName().equalsIgnoreCase("Error")) {
                    throw new FailedRequestException("Error in VOTable: "+info.getValue());
                }
            }
        }
    }

    private static String cleanXML(AnyTEXT at) {
        String s= null;
        if (at!=null) {
            s = at.getDomNode().getLastChild().getNodeValue();
            /*
            s=  at.toString();
            try {
                s= s.substring(s.indexOf("<xml-fragment"));
                s= s.replace("<xml-fragment","");
                s= s.substring(s.indexOf(">"));
                s= s.replace(">","");
                s= s.replaceAll("</xml-fragment$", "");
            } catch (Exception e) {
                s=  at.toString();
            }
            */
        }
        return s;
    }


    private static Class getTranslateDatatype(DataType.Enum sType) {
        Class retval= null;
        if (sType==DataType.BOOLEAN)  retval= Boolean.class;
        else if (sType== DataType.BIT)  retval= Integer.class;
        else if (sType== DataType.UNSIGNED_BYTE)  retval= Byte.class;
        else if (sType== DataType.SHORT)  retval= Integer.class;
        else if (sType== DataType.INT)  retval= Integer.class;
        else if (sType== DataType.LONG)  retval= Long.class;
        else if (sType== DataType.CHAR)  retval= String.class;
        else if (sType== DataType.UNICODE_CHAR)  retval= Integer.class;
        else if (sType== DataType.FLOAT)  retval= Float.class;
        else if (sType== DataType.DOUBLE)  retval= Double.class;
        else if (sType== DataType.FLOAT_COMPLEX)  retval= Float.class;
        else if (sType== DataType.DOUBLE_COMPLEX)  retval= Double.class;
        else Assert.tst(false);
        return retval;
    }


    private static String parseTD(TDDocument.TD entry) {
        String retval= null;
        try{
            String s= entry.toString();

            // The next who lines are done with reflections to avoid
            // having to have weblogic.jar in the compile
            // line 1: XmlAnySimpleType ast= XmlAnySimpleType.Factory.parse(s);
            // line 2: retval= ast.getStringValue();
            Class fClass= XmlAnySimpleType.Factory.class;
            Method parseCall= fClass.getMethod("parse", String.class);
            Class noParams[]={};
            Object noObj[]={};
            Method getStringValueCall= XmlAnySimpleType.class.getMethod(
                                          "getStringValue", noParams);
            XmlAnySimpleType t= (XmlAnySimpleType)parseCall.invoke( fClass, s);
            retval= (String)getStringValueCall.invoke(t, noObj);
        } catch (Exception e) {
            ClientLog.warning(true,e.toString());
            e.printStackTrace();
        }
        return retval;
    }



    private static  void guessUndefinedTypes(DataGroup dataGroup) {
        edu.caltech.ipac.util.DataType dTypeAry[]=
                                       dataGroup.getDataDefinitions();
        boolean guessed;
        for(edu.caltech.ipac.util.DataType dataType: dTypeAry) {
            if (dataType.getDataType()==null) {
                guessed=  guessDataType(dataGroup,dataType);
                if (guessed) {
                    setAllInColumnToType(dataGroup, dataType);
                }
            }
        }

    }

    private static boolean guessDataType(DataGroup dataGroup,
                               edu.caltech.ipac.util.DataType dataType) {

        boolean       useFloatsForDoubles= false;
        Class         guessClass    = Object.class;
        Class         lastGuessClass;
        String        data;
        Object        o;
        boolean       guessed= false;

        for(DataObject dataObj: dataGroup) {
            lastGuessClass= guessClass;
            o= dataObj.getDataElement(dataType);
            data= o.toString().toLowerCase();
            try {
                if (data.equals("true") || data.equals("false")) {
                    guessClass= Boolean.class;
                }
                else if (data.indexOf(".") > -1) {
                    Double.valueOf(data);
                    guessClass= useFloatsForDoubles ?
                                Float.class : Double.class;
                }
                else {
                    Integer.valueOf(data);
                    guessClass= Integer.class;
                }
            } catch (NumberFormatException e) {
                guessClass= String.class;
            }
            if (guessClass==lastGuessClass ||
                (dataGroup.size() == 1 && guessClass!=null) ) {
                guessed= true;
                dataType.setDataType(guessClass);
                break;
            }
        }
        return guessed;
    }



    private static void setAllInColumnToType(
                                  DataGroup dataGroup,
                                  edu.caltech.ipac.util.DataType dataType) {
        Object        data;
        Assert.tst( dataType.isKnownType());
        for(DataObject dataObj: dataGroup) {
            data= dataObj.getDataElement(dataType);
            dataObj.setDataElement( dataType,
                    dataType.convertStringToData(data.toString()) );
        }
    }

//============================================================================
//---------------------------- Private / Protected Methods -------------------
//============================================================================

//============================================================================
//---------------------------- Factory Methods -------------------------------
//============================================================================

//============================================================================
//---------------------------- Inner Classes ---------------------------------
//============================================================================

//============================================================================
//---------------------------- main - test   ---------------------------------
//============================================================================

    public static void main (String [] args) {
        final String   DEFAULT_NAMESPACE = "http://us-vo.org/xml/VOTable.xsd";
        final String[] SUBSTITUTE_NAMESPACES = {"",
            "http://www.ivoa.net/xml/VOTable/v1.0",
            "http://www.ivoa.net/xml/VOTable/v1.1",
            "http://vizier.u-strasbg.fr/xml/VOTable-1.1.xsd"};

        if (args.length != 1) {
            System.out.println("Please, provide filename with VOTable as an argument");
            System.exit(1);
        }


        try {
            // ** to allow attach with debugger ** System.in.read(new byte[1]);

            // read file into string
            File file = new File(args[0]);
            FileInputStream fi = new FileInputStream(file);
            int size = fi.available();
            byte b[] = new byte[size];
            int numread = fi.read(b);
            if (numread != size) {
                System.out.println("Can not read VOTable.");
                System.exit(1);
            }
            String data = new String(b);

            XmlOptions xmlOptions = new XmlOptions();
            HashMap<String, String> substituteNamespaceList =
                    new HashMap<String, String>();
            for (String ns: SUBSTITUTE_NAMESPACES) {
                substituteNamespaceList.put(ns, DEFAULT_NAMESPACE);
            }
            xmlOptions.setLoadSubstituteNamespaces(substituteNamespaceList);
            xmlOptions.setSavePrettyPrint();
            xmlOptions.setSavePrettyPrintIndent(4);


            VOTABLEDocument voTableDoc = VOTABLEDocument.Factory.parse(data,xmlOptions);
            VOTABLEDocument.VOTABLE voTable = voTableDoc.getVOTABLE();
            FixedObjectGroupUtils.checkStatus(voTable);
            RESOURCEDocument.RESOURCE[] resources = voTable.getRESOURCEArray();
            if (resources == null || resources.length < 1) {
                System.out.println("No resources");
                System.exit(1);
            }

            Map<String,DataType.Enum> overrideFieldTypes = null;



            DataGroup dataGroup = FixedObjectGroupUtils.getDataGroup(resources[0], overrideFieldTypes, true, true);
            if (dataGroup.size() < 1) {
                System.out.println("DataGroup, parsed from VOTable is empty.");
                System.exit(1);
            }

            DataGroup.convertHREFTypes(dataGroup);
            IpacTableWriter.save(System.out, dataGroup);
            System.exit(0);

        } catch (Exception e) {
            System.out.println("Failed with exception: "+e.getMessage());
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
