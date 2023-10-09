/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.download.BaseNetParams;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.Locale;


public abstract class ImageServiceParams extends BaseNetParams implements LockingVisNetwork.CanCallService {
    public enum ImageSourceTypes { ISSA, TWOMASS, TWOMASS6, IRIS, MSX, WISE, DSS, SDSS, ZTF, PTF, ATLAS }

    private WorldPt wp;
    private final ImageSourceTypes type;


    public ImageServiceParams(ImageSourceTypes type, String statusKey, String plotId) {
        super(statusKey,plotId);
        this.type= type;
    }
    public void setWorldPt(WorldPt wp) {
        this.wp = wp!=null ? VisUtil.convert(wp, CoordinateSys.EQ_J2000) : null;
    }
    public WorldPt getWorldPt() { return wp; }

    public String getRaJ2000String()     { return wp!=null ? String.format(Locale.US,"%8.6f", wp.getLon()) : ""; }
    public String getDecJ2000String()    { return wp!=null ? String.format(Locale.US,"%8.6f", wp.getLat()) : ""; }

    public String toString() { return String.format(Locale.US,"%8.6f%8.6f",wp.getLon(),wp.getLat()); }

    public ImageSourceTypes getType()          { return type; }
}
