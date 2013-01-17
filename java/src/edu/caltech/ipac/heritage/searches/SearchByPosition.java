package edu.caltech.ipac.heritage.searches;

import com.google.gwt.user.client.rpc.IsSerializable;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.TargetPanel;
import edu.caltech.ipac.heritage.commands.SearchByPositionCmd;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.Serializable;

/**
 * Date: Jun 8, 2009
 *
 * @author loi
 * @version $Id: SearchByPosition.java,v 1.23 2012/09/18 23:08:08 tatianag Exp $
 */
public class SearchByPosition extends HeritageSearch {

    public enum Type {AOR("aorByPosition", DataType.AOR),
        BCD("bcdByPosition", DataType.BCD),
        PBCD("pbcdByPosition", DataType.PBCD),
        IRS_ENHANCED(IRS_ENHANCED_SEARCH_ID, DataType.IRS_ENHANCED),
        SUPERMOSAIC("smByPosition", DataType.SM),
        SOURCE_LIST("SourceListQuery", DataType.SOURCE_LIST);
        String searchId;
        DataType dataType;
        Type(String searchId, DataType dataType) {
            this.searchId = searchId;
            this.dataType = dataType;
        }
    }

    public SearchByPosition(Type type, Request clientReq, boolean isMultiTargets) {
        super(type.dataType, type.dataType.getShortDesc(), chooseSearchReq(isMultiTargets, type, clientReq),
                null, null,false);
    }

    private static Req chooseSearchReq(boolean isMultiTargets, Type type, Request clientReq) {
        if (isMultiTargets) {
            return new MultiTargetReq(type, clientReq);
        } else {
            return new SingleTargetReq(type, clientReq);
        }
    }

    public String getDownloadFilePrefix() {
        TableServerRequest req= this.getSearchRequest();

        String base;
        if (req.containsParam(SearchByPositionCmd.UPLOADED_FILE_PATH)) {
            base ="FileList";
        }
        else if (req.containsParam(TargetPanel.TARGET_NAME_KEY)) {
            base = req.getParam(TargetPanel.TARGET_NAME_KEY);
        }
        else {
            base= "";
        }


        if (!StringUtils.isEmpty(base)) {
            return base.replaceAll("\\s+", "") + "-";
        } else {
            return "tgt-";
        }
    }

    public String getDownloadTitlePrefix() {

        TableServerRequest req= this.getSearchRequest();

        String baseName;
        if (req.containsParam(SearchByPositionCmd.UPLOADED_FILE_PATH)) {
            baseName ="File List: ";
        }
        else if (req.containsParam(TargetPanel.TARGET_NAME_KEY)) {
            baseName = req.getParam(TargetPanel.TARGET_NAME_KEY);
            if (!StringUtils.isEmpty(baseName))  baseName= baseName + ": ";
        }
        else if (req.containsParam(TargetPanel.RA_KEY)) {
            baseName = req.getParam(TargetPanel.RA_KEY) + req.getParam(TargetPanel.DEC_KEY);
            if (!StringUtils.isEmpty(baseName))  baseName= baseName + ": ";
           // baseName= "";
        }
        else {
             baseName= "";
        }
        return baseName;

    }

//====================================================================
//
//====================================================================

    public static abstract class Req extends HeritageRequest implements IsSerializable {
        private static final String RADIUS = SearchByPositionCmd.RADIUS_KEY;
        private static final String MATCH_BY_AOR = SearchByPositionCmd.MATCH_BY_AOR_KEY;

        public Req() {}

        public Req(Type search, Request req) {
            super(search.dataType);
            this.copyFrom(req);
            setRequestId(search.searchId);
        }

        public float getRadius() {
            return getFloatParam(RADIUS);
        }

        public boolean isMatchByAOR() {
            return getBooleanParam(MATCH_BY_AOR);
        }

    }

    public static class SingleTargetReq extends Req implements IsSerializable {

        public SingleTargetReq() {}

        public SingleTargetReq(Type search, Request req) {
            super(search, req);
        }

        public TableServerRequest newInstance() {
            return new SingleTargetReq();
        }


        public WorldPt getPos() {
            return getWorldPtParam(ReqConst.USER_TARGET_WORLD_PT);
        }

        public void setPos(WorldPt wp) {
            if (wp!=null) {
                setParam(ReqConst.USER_TARGET_WORLD_PT,wp.toString());
            }
        }

    }


//====================================================================
//
//====================================================================

    public static class MultiTargetReq extends Req implements Serializable {
        public static final String UPLOAD_FILE_KEY = SearchByPositionCmd.UPLOADED_FILE_PATH;

        public MultiTargetReq() {}

        public TableServerRequest newInstance() {
            return new MultiTargetReq();
        }

        public MultiTargetReq(Type search, Request req) {
            super(search, req);
        }

        public String getUploadedFilePath() {
            return getParam(UPLOAD_FILE_KEY);
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