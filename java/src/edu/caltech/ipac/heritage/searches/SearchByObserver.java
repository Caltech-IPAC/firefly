package edu.caltech.ipac.heritage.searches;

import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.heritage.commands.SearchByObserverCmd;
import edu.caltech.ipac.heritage.data.entity.DataType;

import java.io.Serializable;



/**
 * Date: Jun 8, 2009
 *
 * @author loi
 * @version $Id: SearchByObserver.java,v 1.9 2012/10/26 14:45:03 tatianag Exp $
 */
public class SearchByObserver extends HeritageSearch<SearchByObserver.Req> {

    public enum Type {AOR("aorByObserver", DataType.AOR),
                      BCD("bcdByObserver", DataType.BCD),
                      PBCD("pbcdByObserver", DataType.PBCD),
                      IRS_ENHANCED(IRS_ENHANCED_SEARCH_ID, DataType.IRS_ENHANCED);
                        String searchId;
                        DataType dataType;
                        Type(String searchId, DataType dataType) {
                            this.searchId = searchId;
                            this.dataType = dataType;
                        }
                    }


    public SearchByObserver(Type type, Request clientReq) {
        super(type.dataType, type.dataType.getShortDesc(), new Req(type, clientReq),
                null, null);
    }

    public String getDownloadFilePrefix() {
        return getSearchRequest().getObserver().replaceAll("\\s+", "")+ "-";
    }

    public String getDownloadTitlePrefix() {
        return "Observer " + getSearchRequest().getObserver() + ": ";
    }



//====================================================================
//
//====================================================================

    public static class Req extends HeritageRequest implements Serializable {
        public static final String OID = SearchByObserverCmd.OBSERVER_KEY;

        public Req() {}

        public TableServerRequest newInstance() {
            return new Req();
        }

        public Req(Type type, Request req) {
            super(type.dataType);
            this.copyFrom(req);
            setRequestId(type.searchId);
        }

        public String getObserver() {
            return getParam(OID);
        }

        public void setObserver(String observer) {
            setParam(OID, observer);
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