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
*
* */
