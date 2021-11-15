/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;

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
        })

public class NedSearch extends QueryByConeSearchURL {
    private String NED_OBJECT_NAME = "Object Name";
    private String linkColName = "Details";
    static String url = "https://ned.ipac.caltech.edu/byname?objname=%s";

    @Override
    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        DataGroup resDg = super.fetchDataGroup(req);

        DataType linkNed = new DataType(linkColName, String.class);
        resDg.addDataDefinition(linkNed);

        String colname = NED_OBJECT_NAME;
        for (int r = 0; r < resDg.size(); r++) {
                DataObject row = resDg.get(r);
                String oname = String.valueOf(row.getDataElement(colname));
                String encodedOname = oname;
                try {
                    encodedOname = URLEncoder.encode(oname, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // ignore and use value as is.
                }
                String nedUrl = url.replace("%s", encodedOname);
                String descLink = oname + " details";
                String sval = "<a target=\"_blank\" href=\"" + nedUrl + "\">" + descLink + "</a>";
                row.setDataElement(linkNed, sval);
        }

        return resDg;
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        String url = "http://ned.ipac.caltech.edu/byname?objname=%s&extend=no&list_limit=5&img_stamp=YES";
        String oname = URLEncoder.encode("SSTSL2 J034729.08+240618.6", "UTF-8");
        System.out.println(oname);

        System.out.println(url.replace("%s", oname));
    }
}
