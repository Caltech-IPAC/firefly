package edu.caltech.ipac.heritage.searches;

import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.heritage.commands.AbstractSearchCmd;

import java.io.Serializable;

/**
 * Date: Jun 8, 2009
 *
 * @author loi
 * @version $Id: AbstractSearch.java,v 1.7 2010/04/24 01:13:04 loi Exp $
 */
public class AbstractSearch extends HeritageSearch<AbstractSearch.Req> {
    private static final String SEARCH_TYPE = "abstractSearch";

    public AbstractSearch(Request clientReq) {
        super(null, "Abstract Search", new Req(clientReq), null, null);
   }

    public String getDownloadFilePrefix() {
        return null;
    }

    public String getDownloadTitlePrefix() {
        return null;
    }

//====================================================================
//
//====================================================================

    public static class Req extends HeritageRequest implements Serializable {
        private static final String queryString = AbstractSearchCmd.SEARCH_FIELD_PROP;

        public Req() {}

        public TableServerRequest newInstance() {
            return new Req();
        }

        public Req(Request req) {
            this.copyFrom(req);
            setRequestId(SEARCH_TYPE);
        }

        public void setQueryString(int cid) {
            setParam(queryString, String.valueOf(cid));
        }

        public String getQueryString() {
            return getParam(queryString);
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
