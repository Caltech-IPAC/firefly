/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.searches;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.TargetPanel;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.util.StringUtils;

/**
 * Date: Jun 8, 2009
 *
 * @author Trey
 * @version $Id: DemoSearch2MassPos.java,v 1.3 2010/04/24 01:13:04 loi Exp $
 */
@Deprecated
public class DemoSearch2MassPos extends BaseTableConfig<TableServerRequest>  {


    public DemoSearch2MassPos(TableServerRequest req) {
        super(req, "2 MASS", "Searching 2 MASS", null, null, null);
    }

    public String getDownloadFilePrefix() {
        String tname = this.getSearchRequest().getParam(TargetPanel.TARGET_NAME_KEY);
        if (!StringUtils.isEmpty(tname)) {
            return tname.replaceAll("\\s+", "") + "-";
        } else {
            return "tgt-";
        }
    }

    public String getDownloadTitlePrefix() {
        String tname = this.getSearchRequest().getParam(TargetPanel.TARGET_NAME_KEY);
        if (!StringUtils.isEmpty(tname)) {
            return tname + ": ";
        } else {
            return "";
        }
    }

//====================================================================
//
//====================================================================

//    public static class Demo2MassSingleTargetReq extends TableServerRequest implements IsSerializable {
//
//        private static final String RA = DemoSearch2MassPosCmd.RA_KEY;
//        private static final String DEC = DemoSearch2MassPosCmd.DEC_KEY;
//        private static final String COORDSYS = DemoSearch2MassPosCmd.COORDSYS_KEY;
//        private static final String RADIUS = DemoSearch2MassPosCmd.RADIUS_KEY;
//
//        private transient float ra = Float.NaN;
//        private transient float dec = Float.NaN;
//        private transient CoordinateSys coordsys;
//        private transient WorldPt pos;
//
//        public Demo2MassSingleTargetReq() { this(null); }
//
//        public Demo2MassSingleTargetReq(Request req) {
//            if (req!=null) this.copyFrom(req);
//            setRequestId("DemoSearch2MassPos");
//        }
//
//        public float getRadius() {
//            return getFloatParam(RADIUS);
//        }
//
//        public TableServerRequest newInstance() {
//            return new Demo2MassSingleTargetReq();
//        }
//
//        public float getRa() {
//            if (Float.isNaN(ra)) {
//                CoordinateSys cs = getCoordsys();
//                try {
//                    ra = LonFieldDef.getFloat(getParam(RA), (cs == null || cs.isEquatorial()));
//                } catch (CoordException e) {
//                    ra = Float.MIN_VALUE;
//                }
//            }
//            return ra;
//        }
//
//        public float getDec() {
//            if (Float.isNaN(dec)) {
//                CoordinateSys cs = getCoordsys();
//                try {
//                    dec = LatFieldDef.getFloat(getParam(DEC), (cs == null || cs.isEquatorial()));
//                } catch (CoordException e) {
//                    dec = Float.MIN_VALUE;
//                }
//            }
//            return dec;
//        }
//
//        public CoordinateSys getCoordsys() {
//            if (coordsys == null) {
//                if (getParam(COORDSYS) == null) {
//                    coordsys = null;
//                } else {
//                    coordsys = CoordinateSys.parse(getParam(DemoSearch2MassPosCmd.COORDSYS_KEY));
//                }
//            }
//            return coordsys;
//        }
//
//        public WorldPt getPos() {
//            if (pos == null) {
//                pos = new WorldPt(getRa(), getDec(), getCoordsys());
//            }
//            return pos;
//        }
//
//    }


//====================================================================
//
//====================================================================

//    public static class MultiTargetReq extends Req implements Serializable {
//        public static final String CACHEID = SearchByPositionCmd.TARGETLIST_CACHED_ID;
//
//        public MultiTargetReq() {}
//
//        public TableServerRequest newInstance() {
//            return new MultiTargetReq();
//        }
//
//        public MultiTargetReq(Type search, Request req) {
//            super(search, req);
//        }
//
//        public String getCacheId() {
//            return getParam(CACHEID);
//        }
//
//    }
}
