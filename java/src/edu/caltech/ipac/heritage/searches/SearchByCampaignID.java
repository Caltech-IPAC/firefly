package edu.caltech.ipac.heritage.searches;

import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.heritage.commands.SearchByCampaignCmd;
import edu.caltech.ipac.heritage.data.entity.DataType;

import java.io.Serializable;



/**
 * Date: Jun 8, 2009
 *
 * @author loi
 * @version $Id: SearchByCampaignID.java,v 1.11 2012/10/26 14:45:03 tatianag Exp $
 */
public class SearchByCampaignID extends HeritageSearch<SearchByCampaignID.Req> {

    public enum Type {AOR("aorByCampaignID", DataType.AOR),
                      BCD("bcdByCampaignID", DataType.BCD),
                      PBCD("pbcdByCampaignID", DataType.PBCD),
                      IRS_ENHANCED(IRS_ENHANCED_SEARCH_ID, DataType.IRS_ENHANCED);
                        String searchId;
                        DataType dataType;
                        Type(String searchId, DataType dataType) {
                            this.searchId = searchId;
                            this.dataType = dataType;
                        }
                    }


    public SearchByCampaignID(Type type, Request clientReq) {
        super(type.dataType, type.dataType.getShortDesc(), new Req(type, clientReq),
                null, null);
    }


    public String getDownloadFilePrefix() {
        return "campaign"+getSearchRequest().getCampaign().trim()+"-";
    }

    public String getDownloadTitlePrefix() {
            return "Campaign "+getSearchRequest().getCampaign() + ": ";
    }

//====================================================================
//
//====================================================================

    public static class Req extends HeritageRequest implements Serializable {
        public static final String CAMPAIGN = SearchByCampaignCmd.CAMPAIGN_KEY;

        public Req() {}

        public TableServerRequest newInstance() {
            return new Req();
        }

        public Req(Type type, Request req) {
            super(type.dataType);
            this.copyFrom(req);
            setRequestId(type.searchId);
        }

        public String getCampaign() {
            return getParam(CAMPAIGN);
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