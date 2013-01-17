package edu.caltech.ipac.heritage.searches;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;



/**
 * Date: Jun 8, 2009
 *
 * @author loi
 * @version $Id: SearchAorInfo.java,v 1.3 2010/04/13 16:59:26 roby Exp $
 */
public class SearchAorInfo {
//====================================================================
//
//====================================================================

    public static class Req extends HeritageRequest implements Serializable {
        private static final String REQID = "SearchAorInfo.requestID";
        private static final String INST_MODE = "SearchAorInfo.InstMode";
        private static String SEARCH_ID = "aorInfo";

        public Req() {}

        public TableServerRequest newInstance() {
            return new Req();
        }

        public Req(String instMode, int reqId) {
            setRequestId(SEARCH_ID);
            setReqID(reqId);
            setInstMode(instMode);
            setPageSize(1);
        }

        public void setInstMode(String instMode) {
            setParam(INST_MODE, instMode);
        }

        public String getInstMode() {
            return getParam(INST_MODE);
        }

        public void setReqID(int reqId) {
            setParam(REQID, String.valueOf(reqId));
        }

        public int getReqID() {
            String reqid = getParam(REQID);
            return StringUtils.isEmpty(reqid) ? -1 : Integer.parseInt(reqid);
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
