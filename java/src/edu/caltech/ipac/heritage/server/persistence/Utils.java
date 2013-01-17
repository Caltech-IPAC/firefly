package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.target.Fixed;
import edu.caltech.ipac.target.Target;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author tatianag
 * $Id: Utils.java,v 1.14 2012/10/04 23:51:13 tatianag Exp $
 */
public class Utils {

    private static DataGroup.Attribute[] attributes = null;


    public static DataGroup.Attribute[] getBcdAttributes() {
        return getAttributes();
    }

    public static DataGroup.Attribute[] getEnhancedImagesAttributes() {
        return getAttributes();
    }


    public static DataGroup.Attribute[] getPbcdAttributes() {
        return getAttributes();
    }

    private static DataGroup.Attribute[] getAttributes() {
        if (attributes == null) {
            DataGroup.Attribute [] attr = new DataGroup.Attribute[1];

            // To let IRSA know that the file has 4-corner information,
            // use header keyword "datatype" at the start of the file.
            attr[0] = new DataGroup.Attribute("datatype", "fitshdr");

            attributes =  attr;
        }
        return attributes;
    }


    public static String convertToGatorFormat(String uploadedTargetFile) throws DataAccessException, IOException {

        StringKey key = new StringKey(uploadedTargetFile);
        Object cachedPath = UserCache.getInstance().get(key);
        if (cachedPath != null) {
            File f = new File((String)cachedPath);
            if (f.exists()) {
                return VisContext.replaceWithPrefix(f);
            }
        }

        List<Target> targetList = QueryByPosition.getTargetList(uploadedTargetFile);
        edu.caltech.ipac.util.DataType searchTgt = new edu.caltech.ipac.util.DataType("searchTgt", String.class);
        edu.caltech.ipac.util.DataType ra = new edu.caltech.ipac.util.DataType("ra", Double.class);
        ra.setUnits("degrees");
        edu.caltech.ipac.util.DataType dec = new edu.caltech.ipac.util.DataType("dec", Double.class);
        dec.setUnits("degrees");
        DataGroup dg = new DataGroup("targets", new edu.caltech.ipac.util.DataType[]{searchTgt, ra, dec});
        dg.addAttributes(new DataGroup.Attribute("EQUINOX", "\'J2000\'"));
        for (Target tgt : targetList) {
            if (tgt.isFixed()) {
                Fixed ft = (Fixed)tgt;
                DataObject obj = new DataObject(dg);

                obj.setDataElement(searchTgt, ft.getName() != null ? ft.getName() :
                        ft.getPosition().convertLonToString() + " " + ft.getPosition().convertLonToString());
                obj.setDataElement(ra, ft.getPosition().getRa());
                obj.setDataElement(dec, ft.getPosition().getDec());
                dg.add(obj);
            }
        }
        File destDir = ServerContext.getTempWorkDir();
        File uf = File.createTempFile("tgtlist_", "tbl", destDir);
        DataGroupWriter.write(uf, dg, 0);
        UserCache.getInstance().put(key, uf.getAbsolutePath());

        Logger.debug("Converted format from " + uploadedTargetFile, " to " + uf.getAbsolutePath());

        return VisContext.replaceWithPrefix(uf);
    }
    

}
