/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.IpacTableDef;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author tatianag
 * $Id: $
 */
@SearchProcessorImpl(id = "NedSearch", params =
        {@ParamDoc(name = "UserTargetWorldPt", desc = "the target point, a serialized WorldPt object"),
                @ParamDoc(name = "radius", desc = "radius in degrees"),
                @ParamDoc(name = "accessUrl", desc = "access URL"),
                @ParamDoc(name = "title", desc = "catalog title"),
                @ParamDoc(name = "use", desc = "catalog_overlay, catalog_primary, or data_primary")
        })

public class NedSearch extends QueryByConeSearchURL {
    private String NED_OBJECT_NAME = "Object Name";
    private String linkColName = "Details";
    static String url = "http://ned.ipac.caltech.edu/cgi-bin/objsearch?objname=%s&extend=no&list_limit=5&img_stamp=YES";

    @Override
    protected String getFilePrefix(TableServerRequest request) {
        return "nedconesearch-";
    }


    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        File dgFile = super.loadDataFile(request);
        try {
            String url = "http://ned.ipac.caltech.edu/cgi-bin/objsearch?objname=%s&extend=no&list_limit=5&img_stamp=YES";
            DataGroup resDg = IpacTableReader.read(dgFile);
//            DataType[] extraDef = new DataType[resDg.getDataDefinitions().length+1];
//
//            for ()
//
//            DataGroup extra = new DataGroup();

            IpacTableDef tableDef = IpacTableUtil.getMetaInfo(dgFile);

            DataType linkNed = new DataType(linkColName, String.class);
            resDg.addDataDefinition(linkNed);

            String colname = NED_OBJECT_NAME;
            for (int r = 0; r < resDg.size(); r++) {
                DataObject row = resDg.get(r);
                String oname = String.valueOf(row.getDataElement(colname));
                String newOname = URLEncoder.encode(oname, "UTF-8");
                String nedUrl = url.replace("%s", newOname);
                String descLink = oname + " details";
                String sval = "<a target=\"_blank\" href=\"" + nedUrl + "\">" + descLink + "</a>";
                row.setDataElement(linkNed, sval);
            }


            for(DataGroup.Attribute att : tableDef.getAttributeList()) {
                if (resDg.getAttribute(att.getKey()) == null) {
                    // add all missing meta
                    resDg.addAttribute(att.getKey(), att.getValue());
                }
            }

            IpacTableWriter.save(dgFile, resDg);
        } catch (IOException e) {
            throw new DataAccessException("Can't add extra column to Ned table", e);
        }
        return dgFile;
    }

    /*
    @Override
    protected File postProcessData(File dgFile, TableServerRequest request) throws Exception {
        String url = "http://ned.ipac.caltech.edu/cgi-bin/objsearch?objname=%s&extend=no&list_limit=5&img_stamp=YES";
        final DataGroup resDg = DataGroupReader.read(dgFile);
        //Check if this is the first time (original table to be postporcess)  for a different usage (Statistic/XYWithWerror, etc.
        if (!StringUtils.isEmpty(resDg.getAttribute("joined"))) {
            return dgFile;
        } else {
            resDg.addAttribute("joined", "true");
        }

        TableDef tableDef = IpacTableUtil.getMetaInfo(dgFile);
        int maxWidth = NED_OBJECT_NAME.length();
        Iterator<DataObject> iterator = resDg.iterator();
        while (iterator.hasNext()) {
            DataObject row = iterator.next();
            String oname = String.valueOf(row.getDataElement(NED_OBJECT_NAME));
            String newOname = URLEncoder.encode(oname, "UTF-8");
            String nedUrl = url.replace("%s", newOname);
            String sval = "<a target=\"_blank\" href=\"" + nedUrl + "\">" + oname + "</a>";
            DataType dtype = row.getDataType(NED_OBJECT_NAME);
            if (sval != null && sval.length() > dtype.getMaxDataWidth()) {
                dtype.getFormatInfo().setWidth(sval.length());
            }
            row.setDataElement(dtype, sval);
            if (oname.length() > maxWidth) {
                maxWidth = oname.length();
            }
        }

        tableDef.setAttribute(DataSetParser.makeAttribKey(DataSetParser.WIDTH_TAG, NED_OBJECT_NAME), maxWidth + "");
        Map<String, DataGroup.Attribute> attribs = resDg.getAttributes();
        if (attribs.size() > 0) {
            tableDef.addAttributes(attribs.values().toArray(new DataGroup.Attribute[attribs.size()]));
        }
        resDg.setAttributes(tableDef.getAllAttributes());
        IpacTableWriter.save(dgFile, resDg);


        return dgFile;
    }
    */


    public static void main(String[] args) throws UnsupportedEncodingException {
        String url = "http://ned.ipac.caltech.edu/cgi-bin/objsearch?objname=%s&extend=no&list_limit=5&img_stamp=YES";
        String oname = URLEncoder.encode("SSTSL2 J034729.08+240618.6", "UTF-8");
        System.out.println(oname);

        System.out.println(url.replace("%s", oname));
    }
}
