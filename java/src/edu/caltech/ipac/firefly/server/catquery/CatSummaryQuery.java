package edu.caltech.ipac.firefly.server.catquery;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author tatianag
 *         $Id: CatSummaryQuery.java,v 1.5 2012/10/23 05:39:52 loi Exp $
 */
@SearchProcessorImpl(id ="catSummary", params =
        {@ParamDoc(name = CatSummaryQuery.RA_DEC_RADIUS_KEY, desc = "comma separated ra, dec, radius in degrees"),
         @ParamDoc(name = CatSummaryQuery.PROJECT_KEY, desc = "project")
        })
public class CatSummaryQuery  extends DynQueryProcessor {

    public static final String PROJECT_KEY = "projshort";
    public static final String RA_DEC_RADIUS_KEY = "ra_dec_radius";


    private static final String DEF_HOST    = AppProperties.getProperty("irsa.catSummary.hostname", "");
    private static final String URL_FORMAT  = "http://%s/%s?%s";


    protected File loadDynDataFile(TableServerRequest req) throws IOException, DataAccessException {


        // get the master table.
        TableServerRequest masterTreq = new TableServerRequest("irsaCatalogMasterTable");
        masterTreq.setPageSize(5000);
        DataGroupPart mtp = new SearchManager().getDataGroup(masterTreq);
        DataGroup masterTable = mtp.getData();


        String proj = req.getParam(PROJECT_KEY);
        if (proj == null) { proj = ""; }

        String raDecRadius = req.getParam(RA_DEC_RADIUS_KEY);
        String constraints = (raDecRadius == null || raDecRadius.length()<5) ?
          "" :  String.format("region=circle(%s)", raDecRadius);
        URL url = new URL(String.format(URL_FORMAT, DEF_HOST, proj, constraints));
        //URL url = new URL("file:///Users/tatianag/sampleJSON.txt");
        
        try {

            URLConnection conn = URLDownload.makeConnection(url);
            conn.addRequestProperty("accept", "*/*");
            ByteArrayOutputStream baos = new  ByteArrayOutputStream();
            DataInputStream in= new DataInputStream(new BufferedInputStream(
                        conn.getInputStream()));
            URLDownload.logHeader(conn);
            URLDownload.netCopy(in, baos, conn, null);
            String jsonContent = baos.toString();
            HashMap<String, Integer> keyValues = parse(jsonContent);
            DataType setType = new DataType("set", String.class);
            setType.getFormatInfo().setWidth(15);
            DataType catType = new DataType("catalog", String.class);
            catType.getFormatInfo().setWidth(30);
            DataType ddlink = new DataType("ddlink", String.class);
            catType.getFormatInfo().setWidth(30);

            DataType countType = new DataType("count", Integer.class);
            DataType[] dt = new DataType[]{setType, catType, ddlink, countType};
            DataGroup dg = new DataGroup("Summary", dt);
            for (String key : keyValues.keySet()) {
                int count = Integer.parseInt(keyValues.get(key).toString());
                if (count > 0) {
                    String [] ff = key.split("/");
                    DataObject obj = new DataObject(dg);

                    String cat = ff[ff.length-1];
                    String catDdlink = getDdlink(masterTable, cat);
                    if (!StringUtils.isEmpty(catDdlink)) {
                        obj.setDataElement(ddlink, catDdlink);
                    }
                    obj.setDataElement(setType, ff[ff.length-2]);
                    obj.setDataElement(catType, cat);
                    obj.setDataElement(countType, keyValues.get(key));
                    dg.add(obj);
                }
            }
            File outf = createFile(req);
            //File outf = new File("/tmp/a.txt");
            dg.shrinkToFitData(true);
            IpacTableWriter.save(outf, dg);            

            return outf;
        } catch (FailedRequestException e) {
            throw new IOException(e);
        }
    }

    private String getDdlink(DataGroup data, String cat) {
        if (StringUtils.isEmpty(cat) || data == null || data.size() == 0) return null;

        DataType catC = data.getDataDefintion("catname");
        for(DataObject row : data) {
            String cname = String.valueOf(row.getDataElement(catC));
            if (cname.equals(cat)) {
                return (String) row.getDataElement("ddlink");
            }
        }
        return null;
    }

    public HashMap<String, Integer> parse(String jsonContent) {
        Pattern p = Pattern.compile("\"(.+)\": (\\d+)");

        HashMap<String, Integer> hm = new LinkedHashMap<String, Integer>();
        String[] flds = jsonContent.split(",");
        for (String f : flds) {
            Matcher m = p.matcher(f);
            if (m.find()) {
                int count = Integer.parseInt(m.group(2));
                // group 0 denotes the entire pattern, it's not included in the count
                if (count > 0) {
                    hm.put(m.group(1), count);
                }
            }
            
        }
        return hm;
    }

    public static void main(String [] args) {
        TableServerRequest req = new TableServerRequest();
        req.setParam(RA_DEC_RADIUS_KEY, "259.5575,-38.973528,0.3");
        req.setParam(PROJECT_KEY, "spitzer");
        try {
            File f = new CatSummaryQuery().loadDynDataFile(req);
            System.out.println(f.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("ERROR: "+e.getMessage());
        }
    }
}
