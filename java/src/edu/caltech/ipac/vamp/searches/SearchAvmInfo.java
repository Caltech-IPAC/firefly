package edu.caltech.ipac.vamp.searches;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: May 3, 2010
 * Time: 6:33:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class SearchAvmInfo {

    public static class Req extends TableServerRequest implements Serializable {

        private static final String AVM_ID = "SearchAvmInfo.avm_id";
        private static final String PUBLISHER_ID = "SearchAvmInfo.publisher_id";
        private static final String SQL = "SearchAvmInfo.sql";

        private static String SEARCH_ID = "avmInfo";

        public Req() {}

        public TableServerRequest newInstance() {
            return new Req();
        }

        public Req(String avmId, String publisherId, String sql) {
            setRequestId(SEARCH_ID);
            setAvmId(avmId);
            setPublisherID(publisherId);
            setSql(sql);

            setPageSize(10);
        }

        public void setAvmId(String avmId) {
            setParam(AVM_ID, avmId);
        }

        public void setPublisherID(String publisherId) {
            setParam(PUBLISHER_ID, publisherId);
        }

        public void setSql(String sql) {
            setParam(SQL, sql);
        }

        public String getAvmId() {
            return getParam(AVM_ID);
        }

        public String getPublisherID() {
            return getParam(PUBLISHER_ID);
        }

        public String getSql() {
            return getParam(SQL);
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
