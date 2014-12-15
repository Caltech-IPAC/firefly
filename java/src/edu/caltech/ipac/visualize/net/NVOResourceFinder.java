package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.client.net.*;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.ThrowableUtil;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.visualize.draw.ColumnException;
import edu.caltech.ipac.visualize.draw.FixedObjectGroup;
import edu.caltech.ipac.visualize.draw.FixedObjectGroupUtils;
import org.apache.xmlbeans.XmlOptions;
import org.usVo.xml.voTable.DataType;
import org.usVo.xml.voTable.RESOURCEDocument;
import org.usVo.xml.voTable.VOTABLEDocument;
import org.us_vo.www.RegistryLocator;
import org.us_vo.www.RegistrySoap;
import org.us_vo.www.SimpleResource;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;

/**
 * @author Tatiana Goldina
 * @version $Id: NVOResourceFinder.java,v 1.1 2008/01/31 19:05:00 tatianag Exp $
 */
public class NVOResourceFinder extends ThreadedService {

    private enum NVOSearchType {
        CONE_SEARCH,
        SIAP
    }

    private NetParams _params;
    private NVOSearchType _searchType;
    private static final ClassProperties _prop= new ClassProperties(
                                                  NVOResourceFinder.class);
    private static final long MILLISEC_IN_DAY= 86400000; //1000*60*60*24

    private static final String   DEFAULT_REGISTRY_URL = "http://nvo.stsci.edu/voregistry/registry.asmx";
    private static final String   MIRROR_REGISTRY_URL = "http://voservices.net/registry/registry.asmx";
    private static final String   DEFAULT_NAMESPACE = "http://us-vo.org/xml/VOTable.xsd";
    private static final String[] SUBSTITUTE_NAMESPACES = {"",
            "http://www.ivoa.net/xml/VOTable/v1.0",
            "http://www.ivoa.net/xml/VOTable/v1.1",
            "http://vizier.u-strasbg.fr/xml/VOTable-1.1.xsd"};

    // from Simple Image Access specifications
    // http://www.ivoa.net/Documents/WD/SIA/sia-20040524.html#query-in
    private static final String RA_UCD = "POS_EQ_RA_MAIN";
    private static final String DEC_UCD = "POS_EQ_DEC_MAIN";
    private static final String FORMAT_UCD = "VOX:Image_Format";


    /*
       Required fields from Simple Image Access Specification
       http://www.ivoa.net/Documents/WD/SIA/sia-20040524.html
       <ul>
       <li>Exactly one field MUST have ucd="VOX:Image_Title", with datatype="char", and arraysize="*", containing a short
       (usually one line) description of the image. This should concisely describe the image to a user, typically
       identifying the image source (e.g., survey name), object name or field coordinates, bandpass/filter, etc.
       <li>Exactly one field MUST have ucd="POS_EQ_RA_MAIN", with datatype="double", representing the ICRS
       right-ascension of the center of the image.
       <li>Exactly one field MUST have ucd="POS_EQ_DEC_MAIN", with datatype="double", representing the ICRS
       declination of the center of the image.
       <li>Exactly one field MUST have ucd="VOX:Image_Naxes", with datatype="int", specifying the number of image axes.
       <li>Exactly one field MUST have ucd="VOX:Image_Naxis", with datatype="int", and arraysize="*", with the array
       value giving the length in pixels of each image axis.
       <li>Exactly one field MUST have ucd="VOX:Image_Scale", with datatype="double", and arraysize="*", with
       the array value giving the scale in degrees per pixel of each image axis.
       <li>Exactly one field MUST have ucd="VOX:Image_Format", with datatype="char", and arraysize="*", specifying
       the MIME-type of the object associated with the image acref, e.g., "image/fits", "text/html", and so forth.
       <li>Exactly one field MUST have ucd="VOX:Image_AccessReference", with datatype="char" and arraysize="*",
       specifying the URL to be used to access or retrieve the image. Since the URL will often contain metacharacters
       the URL is normally enclosed in an XML CDATA section (<![CDATA[...]]>) or otherwise encoded to escape
       any embedded metacharacters.
       </ul>
    */
    private final static String [] _siapFields = {
            RA_UCD,
            DEC_UCD,
            "VOX:Image_Title",
            "INST_ID", //optional
            "VOX:Image_Naxes",
            "VOX:Image_Naxis",
            "VOX:Image_Scale",
            "VOX:Image_MJDateObs", //optional - mean modified julian date
            "VOX:Image_AccessReference"
    };

    /**
     * <ul>
     * <li>Exactly one FIELD must have ucd="ID_MAIN", with an array character type (fixed or variable length),
     * representing an ID string for that record of the table. This identifier may not be repeated in the table,
     * and it could be used to retrieve that same record again from that same table.
     * <li>Exactly one FIELD must have ucd="POS_EQ_RA_MAIN", with type double, representing the right-ascension
     * of the source in the ICRS coordinate system.
     * <li>Exactly one FIELD must have ucd="POS_EQ_DEC_MAIN", with type double, representing the declination
     * of the source in the ICRS coordinate system.
     * <li>The VOTable may include an expression of the uncertainty of the positions given in the above mentioned
     * fields to be interpreted either as a positional error of the source positions, or an angular size if
     * the sources are resolved. If this uncertainty is not provided, it should be taken to be zero; otherwise,
     * it may be set for all table entries with a PARAM in the RESOURCE which has a UCD that is set to OBS_ANG-SIZE
     * and has a value which is the angle in decimal degrees. Alternatively, a different value for each row
     * in the table can be given via a FIELD in the table having a UCD set to OBS_ANG-SIZE.
     * </ul>
     *
    private final static String [] _coneSearchFields = {
        "ID_MAIN",
         RA_UCD,
         DEC_UCD,
         "OBS_ANG-SIZE"   
    };
    */

    private static final String   SIAP_OP_DESC= _prop.getName("siap.desc");
    private static final String   CONESEARCH_OP_DESC= _prop.getName("conesearch.desc");
    private static final String   SEARCH_DESC= _prop.getName("searching");



    /**
     * Simple resources by spectral coverage
     */

    private static List<SimpleResource>[] _siapResources = null;
    private static List<SimpleResource>[] _coneResources = null;
    private static int _nServicesSiap = 0;
    private static int _nServicesCone = 0;

    private static boolean TEST = false;


    /**
     * @param w a Window
     * @param params image search parameters
     * @param listener progress listener
     */
    public NVOResourceFinder(SIAPImageParams params, ThreadedServiceListener listener, Window w) {
        super(ThreadedService.BACKGROUND, listener, w);
        setFireListenersInAWTThread(true); // keeps me from having to do invokeLater to update the UI
        setOperationDesc(SIAP_OP_DESC);
        setProcessingDesc(SEARCH_DESC);
        _params = params;
        _searchType = NVOSearchType.SIAP;
    }

    /**
     * @param w a Window
     * @param params cone search parameters
     * @param listener progress listener
     */
    public NVOResourceFinder(ConeSearchParams params, ThreadedServiceListener listener, Window w) {
        super(ThreadedService.BACKGROUND, listener, w);
        setFireListenersInAWTThread(true); // keeps me from having to do invokeLater to update the UI
        setOperationDesc(CONESEARCH_OP_DESC);
        setProcessingDesc(SEARCH_DESC);
        _params = params;
        _searchType = NVOSearchType.CONE_SEARCH;
    }


    public static void getMatchingResources(SIAPImageParams params, ThreadedServiceListener listener, Window w)
        throws FailedRequestException {

        NVOResourceFinder action = new NVOResourceFinder(params, listener, w);
        action.execute(true);
    }

    public static void getMatchingResources(ConeSearchParams params, ThreadedServiceListener listener, Window w)
        throws FailedRequestException {

        NVOResourceFinder action = new NVOResourceFinder(params, listener, w);
        action.execute(true);
    }

    protected void doService() throws Exception {
        lowlevelSearch(_params, _searchType, this);
    }

    private static void lowlevelSearch(NetParams params, NVOSearchType searchType, NVOResourceFinder ts)
            throws FailedRequestException,
            IOException {

        NetCache netCache = null;
        if (!TEST) {
            System.out.println ("Checking cache");
            // check cache, inform listener if results are present
            netCache= NetCache.getInstance();
            List<NVOResource>[] matchingResources = (List<NVOResource>[])netCache.checkCacheForObject(params);
            if (matchingResources != null)  {
                ts.fireUpdate(new ThreadedServiceEvent(ts, matchingResources));
                return;
            }
        }

        // find services that support Simple Image Access Protocol (SIAP from NVO registry
        queryNVORegistry(searchType, ts);

        // find matching resources, threaded service listener is updated with the new list
        // after a resource is confirmed to have matching images
        List<NVOResource>[] resources = findMatches(params, searchType, ts);

        // save results in cache - if completed successfully
        if (!ts.interrupted()) {
            if (resources !=null && netCache != null) {
                Date date= new Date();
                date.setTime(date.getTime() + MILLISEC_IN_DAY);
                netCache.addObjectToCache(params,resources,date);
            }
        }

        ClientLog.message("Done");
    }

    private static void queryNVORegistry(NVOSearchType searchType, NVOResourceFinder ts)
            throws FailedRequestException, RemoteException {

        String protocol;
        String queryString;
        if (searchType.equals(NVOSearchType.SIAP)) {
            if (_siapResources != null) return;
            protocol = "SIAP";
            queryString = "ServiceType like 'SIAP%' and ContentLevel like 'Research%'";
        } else { // cone search
            if (_coneResources != null) return;
            protocol = "Cone Search";
            queryString = "ServiceType like '%CONE%' and ContentLevel like 'Research%'";
        }

        ClientLog.message("Querying resources registered with NVO");
        ts.setOperationDesc("Querying resources registered with NVO");
        RegistrySoap registrySoap;
        try {
            registrySoap = (new RegistryLocator()).getRegistrySoap(new URL(DEFAULT_REGISTRY_URL));
        } catch (Throwable e) {
            ClientLog.message("Problem finding registry at "+DEFAULT_REGISTRY_URL+" "+e.getMessage());
            try {
                registrySoap = (new RegistryLocator()).getRegistrySoap(new URL(MIRROR_REGISTRY_URL));
            } catch (Exception ex) {
                ClientLog.message("Problem finding registry at "+MIRROR_REGISTRY_URL);
                throw new FailedRequestException("Failed to get "+protocol+" resources from NVO registry",
                        "Problem finding registry at "+DEFAULT_REGISTRY_URL+" and "+MIRROR_REGISTRY_URL, ex);
            }
        }

        if (registrySoap == null) {
            ClientLog.message("Unable to access NVO registry");
            return;
        }
        SimpleResource[] services = registrySoap.queryRegistry(queryString);

        int nServices = services.length;
        if (nServices < 1) {
            ClientLog.message("No "+protocol+" resources found");
            throw new FailedRequestException("Failed to get "+protocol+" resources from NVO registry",
                    "No "+protocol+" resources found");
        }

        if (TEST) {
            System.out.println("======================= "+nServices+" services found =================");
            for (SimpleResource resource : services) printResource(resource);
            System.out.println("======================= "+nServices+" records printed ================");
        }

        if (searchType.equals(NVOSearchType.SIAP)) {
            _siapResources = sortByCoverage(services);
            _nServicesSiap = nServices;
        } else {
            _coneResources = sortByCoverage(services);
            _nServicesCone = nServices;
        }


        ClientLog.message("Found "+nServices+" "+protocol+" resources");
    }

    /**
     * Print the list of input resources
     * @param resources  list of services broken by spectal coverage
     */
    public static void printResources(List<SimpleResource>[] resources) {
        int cIdx;
        for (NVOResource.Coverage cv : NVOResource.Coverage.values()) {
            cIdx = cv.getIdx();
            if (resources[cIdx] == null) continue;
            for (SimpleResource s : resources[cIdx]) {
                printResource(s);
            }
        }
    }


    public static void printResource(SimpleResource s) {
        NumberFormat defNF = new DecimalFormat();
        defNF.setMaximumFractionDigits(4);
        StringBuffer sb = new StringBuffer(512);
        sb.append("\n-------------------------------------");
        sb.append("\nTitle: ").append(s.getTitle().trim());
        sb.append("\nShort Name: ").append(s.getShortName());
        sb.append("\nDescription: ").append(s.getDescription());
        sb.append("\nServiceType: ").append(s.getServiceType());
        sb.append("\nType: ").append(s.getServiceType());
        sb.append("\nPublisher: ").append(s.getPublisher());
        sb.append("\nURL: ").append(s.getReferenceURL());
        sb.append("\nContact: ").append(s.getContactName());
        sb.append(" <").append(s.getContactEmail()).append(">");
        sb.append("\nFacility: ").append(s.getFacility());
        sb.append("\nInstruments: ");
        for (String str : s.getInstrument()) {
            sb.append(str).append(" ");
        }
        if (s.getMaxSR() > 0) {
            sb.append("\nMax Search Radius: ").append(defNF.format(s.getMaxSR())).append(" degrees");
        }
        if (s.getEntrySize() > 0) {
            sb.append("\nEntry Size: ").append(defNF.format(s.getEntrySize())).append(" degrees");
        }
        sb.append("\nService URL: ").append(s.getServiceURL()).append("\nCoverageSpectral: ");
        for (String str : s.getCoverageSpectral()) {
            sb.append(str).append("; ");
        }
        sb.append("\nContent Level: ");
        for (String str : s.getContentLevel()) {
            sb.append(str).append("; ");
        }
        System.out.println(sb.toString());
    }

    public static List<SimpleResource>[] sortByCoverage(SimpleResource[] services) {
        NVOResource.Coverage [] sv = NVOResource.Coverage.values();
        List<SimpleResource>[] sortedResources = new ArrayList[sv.length];
        for (NVOResource.Coverage sc : sv) {
            sortedResources[sc.getIdx()] = new ArrayList<SimpleResource>();
        }
        String [] coverageSpectral;
        Pattern pattern;
        boolean found;
        for (SimpleResource s : services) {
            coverageSpectral = s.getCoverageSpectral();
            if (coverageSpectral == null || coverageSpectral.length < 1) {
                sortedResources[NVOResource.Coverage.OTHER.getIdx()].add(s);
            } else if (coverageSpectral.length > 1) {
                sortedResources[NVOResource.Coverage.MULTI.getIdx()].add(s);
            } else {
                found = false;
                for (NVOResource.Coverage sc : sv) {
                    pattern = sc.getPattern();
                    if (pattern != null && pattern.matcher(coverageSpectral[0]).matches()) {
                        sortedResources[sc.getIdx()].add(s);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    sortedResources[NVOResource.Coverage.OTHER.getIdx()].add(s);
                }
            }
        }
        return sortedResources;
    }

    private static List<NVOResource>[] findMatches(NetParams params, NVOSearchType searchType, NVOResourceFinder ts)
        throws FailedRequestException{

        ts.setOperationDesc("Searching for matches");
        NVOResource.Coverage [] sv = NVOResource.Coverage.values();
        List<NVOResource>[] matchingResources = new ArrayList[sv.length];
        List<SimpleResource>[] allResources;
        int nServices;
        if (searchType.equals(NVOSearchType.SIAP)) {
            allResources = _siapResources;
            nServices = _nServicesSiap;
        } else {
            allResources = _coneResources;
            nServices = _nServicesCone;
        }

        int cIdx;
        int matches = 0, nonmatches = 0;
        int failures = 0;
        String data;
        VOTABLEDocument voTableDoc;
        int nProc = 0;
        ts.setProgressFeedback(0, nServices, nProc, "Checking for matches");
        for (NVOResource.Coverage cv : NVOResource.Coverage.values()) {
            if (ts.interrupted()) break;
            cIdx = cv.getIdx();
            //ClientLog.message("SPECTRAL COVERAGE: "+cv.getDesc()+" -- "+_siapResources[cIdx].size()+" resources");
            matchingResources[cIdx] = new ArrayList<NVOResource>();

            for (SimpleResource s : allResources[cIdx]) {
                if (ts.interrupted()) break;

                URL url;
                try {
                    nProc++;
                    String progressDesc = nProc+"/"+nServices+": "+matches+" matching";
                    ts.incrementProgress(progressDesc);
                    ts.setProcessingDesc(s.getShortName());

                    String serviceURL = s.getServiceURL().trim();

                    if (!serviceURL.endsWith("?") && !serviceURL.endsWith("&")) {
                        serviceURL += "&";
                    }
                    url = new URL(serviceURL+params.toString());

                    URLConnection conn = url.openConnection();  // does not actually connect
                    conn.setConnectTimeout(10000); // 10 sec.
                    conn.setReadTimeout(30000); // 30 sec.
                    String contentType = conn.getContentType(); // connects as a part of this call

                    URLDownload.logHeader(conn);

                    if (contentType != null && contentType.startsWith("text/plain")) {
                        String htmlErr= URLDownload.getStringFromOpenURL(conn,ts);
                        ClientLog.warning("The following error was reported by resource "+
                                s.getTitle()+" when checking for matches: "+
                                htmlErr);
                        failures++;
                        continue;
                    }

                    data = URLDownload.getStringFromOpenURL(conn, null);

                } catch (Exception e) {
                    ClientLog.warning("Unable to check whether NVO resource "+s.getTitle()+
                            " has mathes: "+e.getMessage());
                    failures++;
                    continue;
                }


                XmlOptions xmlOptions = new XmlOptions();
                HashMap<String, String> substituteNamespaceList =
                        new HashMap<String, String>();
                for (String ns: SUBSTITUTE_NAMESPACES) {
                    substituteNamespaceList.put(ns, DEFAULT_NAMESPACE);
                }
                xmlOptions.setLoadSubstituteNamespaces(substituteNamespaceList);
                xmlOptions.setSavePrettyPrint();
                xmlOptions.setSavePrettyPrintIndent(4);

                try {
                    voTableDoc = VOTABLEDocument.Factory.parse(data,xmlOptions);
                    VOTABLEDocument.VOTABLE voTable = voTableDoc.getVOTABLE();
                    FixedObjectGroupUtils.checkStatus(voTable);
                    RESOURCEDocument.RESOURCE[] resources = voTable.getRESOURCEArray();
                    if (resources == null || resources.length < 1) {
                        ClientLog.message("Resource "+s.getTitle().trim()+" returns no matching images: "+url.toString());
                        nonmatches++;
                        continue;
                    }

                    // search type specific settings
                    Map<String,DataType.Enum> overrideFieldTypes = null;
                    boolean useUCDforID = true;
                    boolean useUCDforTitle = true;
                    String [] raNameOptions = {RA_UCD};
                    String [] decNameOptions = {DEC_UCD};

                    if (searchType.equals(NVOSearchType.SIAP)) {
                        // use double type for POS_EQ_RA_MAIN and POS_EQ_DEC_MAIN
                        overrideFieldTypes = new HashMap<String,DataType.Enum>(3);
                        overrideFieldTypes.put(RA_UCD, DataType.DOUBLE);
                        overrideFieldTypes.put(DEC_UCD, DataType.DOUBLE);
                        overrideFieldTypes.put("VOX:Image_Naxes", DataType.INT);
                    }

                    DataGroup fullDataGroup = FixedObjectGroupUtils.getDataGroup(resources[0],
                                overrideFieldTypes, useUCDforID, useUCDforTitle);
                    if (fullDataGroup.size() < 1) {
                        ClientLog.message("Resource "+s.getTitle().trim()+" returns no matches: "+url.toString());
                        nonmatches++;
                        continue;
                    }

                    if (TEST) {
                        // print returned data in IPAC TABLE format
                        try {
                            System.out.println("IPAC TABLE (full): "+fullDataGroup.getTitle());
                            System.out.println(url.toString());
                            IpacTableWriter.save(System.out, fullDataGroup);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    DataGroup dataGroup;
                    if (searchType.equals(NVOSearchType.SIAP)) {
                        // slim data group, leaving only the fields required by SIAP specifications
                        dataGroup = getSIAPSubset(fullDataGroup);
                    } else {
                        dataGroup = fullDataGroup;
                    }
                    dataGroup.setTitle(s.getTitle().trim());

                    String [] targetNameOptions = null;
                    FixedObjectGroup fixGroup = new FixedObjectGroup();
                    if (dataGroup.size()>0) {
                        try {
                               fixGroup= new FixedObjectGroup(dataGroup, targetNameOptions, raNameOptions, decNameOptions);

                        } catch (ColumnException e) {
                            ClientLog.warning("Could not convert parse positions from VOTABLE.",
                                              e.toString(),
                                              "rows: " + dataGroup.size());
                            throw new FailedRequestException("Could not parse positions from VOTABLE.",
                                                             "VOTABLEDocument contains unexpected results.",e);
                        }
                    }
                    //FixedObjectGroup fixGroup = FixedObjectGroupUtils.makeFixedObjectGroup(voTableDoc);
                    if (fixGroup.size() > 0) {
                        // set target name
                        for (int i=0; i<fixGroup.size(); i++) {
                            fixGroup.get(i).setTargetName(s.getShortName().trim()+"-"+(i+1));
                        }
                        //FixedObject fixedObj = fixGroup.get(0);
                        //WorldPt wpt = Plot.convert(fixedObj.getPosition(), CoordinateSys.EQ_J2000);
                        //ClientLog.message("Adding matching resource "+s.getTitle()+" POS="+wpt.toString());
                        NVOResource nvoResource = new NVOResource(s, cv, fixGroup);
                        matchingResources[cIdx].add(nvoResource);
                        matches++;
                        ts.fireUpdate(new ThreadedServiceEvent(ts, nvoResource));
                    } else {
                        ClientLog.message("Resource "+s.getTitle().trim()+" returns no matching images: "+url.toString());
                        nonmatches++;
                    }

                } catch (FailedRequestException fre) {
                    String details = fre.getDetailMessage();
                    if (details == null) details = "";
                    ClientLog.warning(s.getTitle().trim()+": "+fre.getMessage()+" "+details);
                    failures++;
                } catch (Exception ex) {
                    ClientLog.warning(s.getTitle().trim()+": error when parsing VOTable - "+ex.getMessage() +" ***"+ex.getClass().toString());
                    ClientLog.message(ThrowableUtil.getStackTraceAsString(ex));
                    failures++;
                }
            }
        }

        ClientLog.message("Search for resources, matching "+params.toString() +
            " gave "+matches+" matches, "+nonmatches+" nonmatches, "+failures+" failures.");

        if (matches == 0) {
            throw new FailedRequestException("No matching images found");
        }

        return matchingResources;

     }

    /*
        Get DataGroup with only those data columns that are required by SIAP specification
        @param original data group (contains all data VOTable has)
        @return a subset of the original data group that includes only the fields required by specs
     */
    private static DataGroup getSIAPSubset(DataGroup all) {

        List<edu.caltech.ipac.util.DataType> subsetDataTypes = new ArrayList<edu.caltech.ipac.util.DataType>(_siapFields.length);
        edu.caltech.ipac.util.DataType[] allDataTypes = all.getDataDefinitions();
        Map<String, Integer> colMap = makeColMap(all);

        Integer idx;
        List<Integer> subsetIdx = new ArrayList<Integer>(_siapFields.length);
        for(String key: _siapFields) {
            if (!colMap.containsKey(key)) continue;
            idx = colMap.get(key);
            if (idx != null) {
                subsetDataTypes.add(allDataTypes[idx].copyWithNoColumnIdx(0));
                subsetIdx.add(idx);
             }
        }

        DataGroup subset = new DataGroup(all.getTitle(), subsetDataTypes);

        Object [] data;
        DataObject newObj;
        int maxWidth[] = new int[subsetIdx.size()];
        Arrays.fill(maxWidth, 0);

        int width;
        for (DataObject obj : all) {
            if(!obj.getDataElement(FORMAT_UCD).equals("image/fits")) {
                continue;
            }
            newObj = new DataObject(subset);
            data = obj.getData();
            
            int newIdx =0;
            for (int i : subsetIdx) {
                newObj.setDataElement(subsetDataTypes.get(newIdx), data[i]);
                if (data[i] instanceof String) {
                    width = ((String)data[i]).length();
                    if (width > maxWidth[newIdx]) {
                        maxWidth[newIdx] = width;
                    }
                }
                newIdx++;
            }
            subset.add(newObj);
        }

        // set width in data type format
        edu.caltech.ipac.util.DataType.FormatInfo fi;
        edu.caltech.ipac.util.DataType dt;
        for (int i=0; i<subsetDataTypes.size(); i++) {
            dt = subsetDataTypes.get(i);
            width = (maxWidth[i] > dt.getKeyName().length()) ? maxWidth[i] : dt.getKeyName().length();
            fi = subsetDataTypes.get(i).getFormatInfo();
            fi.setWidth(width);
            dt.setFormatInfo(fi);
        }

        return subset;
    }

       /**
     * This method returns a map to look up the column number of a specified key
     * (column name) in the table.
     * @param dataGroup data group
     * @return map with a string key to look up its column number in table
     */
    private static Map<String, Integer> makeColMap(DataGroup dataGroup) {
        edu.caltech.ipac.util.DataType[] dataTypes = dataGroup.getDataDefinitions();
        Map<String, Integer> colMap = new HashMap<String, Integer>(dataTypes.length);
        for (int i=0; i<dataTypes.length; i++) {
            colMap.put(dataTypes[i].getKeyName(), i);
        }
        return colMap;
    }

   /**
    * @param args arguments
    */
    public static void main(String args[]) {

       if (args.length < 1) {
           System.out.println("=================================================");
           System.out.println("NVOResourceFinder [siap|conesearch] ra dec radius\n  all in degrees");
           System.out.println("\nOR\n");
           System.out.println("NVOResourceFinder [siap|conesearch]\n  to run predefined test case.");
           System.out.println("=================================================");
           System.exit(1);
       }
       NVOSearchType searchType = null;
       if (args[0].equals("siap")) {
           searchType = NVOSearchType.SIAP;
       } else if (args[0].equals("conesearch")) {
           searchType = NVOSearchType.CONE_SEARCH;
       } else {
           System.out.println("Invalid search type. Check usage by specifying no arguments.");
           System.exit(1);
       }

       double ra=10.672, dec=41.259, radius=0.05;
       if (args.length > 1) {
           try {
               ra = Double.parseDouble(args[1]);
               dec = Double.parseDouble(args[2]);
               radius = Double.parseDouble(args[3]);
           } catch (Exception e) {
               System.out.println("Invalid ra, dec, or radius. Check usage by specifying no arguments.");
               System.exit(1);
           }
       }
       System.out.println(
               MessageFormat.format("Searching for matching resources using NVO {0} protocol: ra={1}, dec={2}, sr={3}",
                       searchType.toString(), ra, dec, radius));

        try {
            TEST = true;
            JFrame f = new JFrame("Test Dialog");
            if (searchType.equals(NVOSearchType.SIAP)) {
                SIAPImageParams params= new SIAPImageParams(ra, dec, radius);
                getMatchingResources(params, new TstListener(), f);
            } else {
                ConeSearchParams params = new ConeSearchParams(ra, dec, radius);
                getMatchingResources(params, new TstListener(), f);
            }
        }
        catch (Exception e) {
            System.out.println("EXCEPTION WHILE GETTING MATCHING RESOURCES");
            System.out.println(e.getMessage());
            e.printStackTrace();
        } 
    }

    private static class TstListener implements ThreadedServiceListener {

        List<NVOResource>[] _res = null;

        public TstListener() {
            _res = new ArrayList[NVOResource.Coverage.values().length];
            int cIdx;
            for (NVOResource.Coverage cv : NVOResource.Coverage.values()) {
                cIdx = cv.getIdx();
                _res[cIdx] = new ArrayList<NVOResource>();
            }
        }

        /**
         * This method is call when the service all is about
         * to fail for any reason.  The thread may take a few
         * seconds to wind down after preFail is called.
         */
        public void preFail() {
            System.out.println("ThreadedServiceListener: the service is about to fail.");
        }

        /**
         * This method is call when the service has
         * failed for any reason.
         */
        public void failed(FailedRequestException exception) {
            System.out.println("ThreadedServiceListener: the service has failed: "+exception.toString());
            System.exit(1);
        }

        /**
         * This method is called when the service is completly successful.
         */
        public void success() {
            System.out.println("ThreadedServiceListener: the service has completed successfully.");
            int cIdx;
            SimpleResource sr;
            for (NVOResource.Coverage cv : NVOResource.Coverage.values()) {
                cIdx = cv.getIdx();
                if (_res[cIdx] == null) continue;
                for (NVOResource s : _res[cIdx]) {
                    sr = s.getResource();
                    System.out.println(sr.getTitle()+": "+sr.getServiceURL());
                    try {
                        System.out.println("IPAC TABLE (saved): "+s.getGroup().getTitle());
                        IpacTableWriter.save(System.out, s.getGroup().getExtraData());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            System.exit(0);
        }

        /**
         * called after a step is completed
         */
        public void update(ThreadedServiceEvent ev) {
            ThreadedService source = (ThreadedService)ev.getSource();
            System.out.println(source.getProgressDescription());
            System.out.println("UPDATE");
            Object newData = ev.getNewData();
            if (newData instanceof NVOResource) {
                NVOResource newResource = (NVOResource)ev.getNewData();
                synchronized (this) {
                    _res[newResource.getCoverage().getIdx()].add(newResource);
                }
            } else if (newData instanceof List[]) {
                synchronized (this) {
                    _res = (List<NVOResource>[])newData;
                }
            }

            System.out.println();

        }

        /**
         * called before a step is attempted and is vetoable.
         */
        public void vetoableUpdate(ThreadedServiceEvent ev)
                throws FailedRequestException {

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
