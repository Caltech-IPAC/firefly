/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import edu.caltech.ipac.astro.IpacTableWriter;
import org.json.simple.JSONObject;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Date: Dec 5, 2011
 *
 * @author loi
 * @version $Id: VoTableUtil.java,v 1.4 2013/01/07 22:10:01 tatianag Exp $
 */
public class FitsHDUUtil {

    private enum MetaInfo {
        EXT("Index", "Index", Integer.class, "Extension Index", false),
        NAME("Extension", "Extension", String.class, "Extension name", false),
        TYPE("Type", "Type", String.class, "table type", false);

        String keyName;
        String title;
        Class  metaClass;
        String description;
        boolean bRowInfo;

        MetaInfo(String key, String title, Class c, String des, boolean rowInfo) {
            this.keyName = key;
            this.title = title;
            this.metaClass = c;
            this.description = des;
            this.bRowInfo = rowInfo;
        }

        List<Object> getInfo() {
            return Arrays.asList(keyName, title, metaClass, description);
        }

        String getKey() {
            return keyName;
        }

        String getTitle() {
            return title;
        }

        Class getMetaClass() {
            return metaClass;
        }

        String getDescription() {
            return description;
        }

        boolean isRowInfo() { return bRowInfo; }

    }

    public static DataGroup fitsHeaderToDataGroup(String fitsFile) {
        List<DataType> cols = new ArrayList<DataType>();

        for (MetaInfo meta : MetaInfo.values()) {    // index, name, row, column
            if (meta.isRowInfo()) continue;
            DataType dt = new DataType(meta.getKey(), meta.getTitle(), meta.getMetaClass());
            dt.setShortDesc(meta.getDescription());
            cols.add(dt);
        }

        DataGroup dg = new DataGroup("fits", cols);
        String invalidMsg = "invalid fits file";

        try {
            Fits fits = new Fits(fitsFile); // open fits file
            BasicHDU[] allHDUs = fits.read();

            int index = 0;
            for ( ; index < allHDUs.length; index++) {
                JSONObject extensionInfo = new JSONObject();
                List<JSONObject> rowStats = new ArrayList<>();
                String name = "NoName";
                String type = "IMAGE";

                extensionInfo.put("rowId", index);
                Header hduHeader = allHDUs[index].getHeader();

                for (Cursor citr = hduHeader.iterator(); citr.hasNext(); ) {
                    HeaderCard hc = (HeaderCard) citr.next();

                    if (!hc.isKeyValuePair()) continue;

                    JSONObject oneKeyVal = new JSONObject();
                    String key = hc.getKey();
                    String val = hc.getValue();

                    if (index == 0) {
                        if (key.equals("NAXIS") && val.equals("0")) {
                            type = "";
                        }
                    } else {
                        if (key.equals("XTENSION")) {
                            type = val;
                        } else if (key.equals("NAME") || key.equals("EXTNAME")) {
                            name = val;
                        }
                    }
                    oneKeyVal.put("key", key);
                    oneKeyVal.put("value", val);
                    oneKeyVal.put("comment", hc.getComment());
                    rowStats.add(oneKeyVal);
                }
                extensionInfo.put("rowInfo", rowStats);
                DataObject row = new DataObject(dg);
                row.setDataElement(cols.get(0), index);
                row.setDataElement(cols.get(1), (index == 0) ? "Primary" : name);
                row.setDataElement(cols.get(2), type);
                dg.add(row);
                dg.addAttribute(Integer.toString(index), extensionInfo.toJSONString());
            }

            if (index == 0) {
                throw new FitsException(invalidMsg);
            } else {
                String title = index == 1 ? "a fits with no extension" :
                                            "a fits with " + (index - 1) + ((index > 2) ? " extensions" : " extension");
                dg.setTitle(title);
            }
        } catch (FitsException e) {
            dg.setTitle(invalidMsg);
            e.printStackTrace();
        }

        return dg;
    }
}
