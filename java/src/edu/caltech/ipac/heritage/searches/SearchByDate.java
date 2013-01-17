package edu.caltech.ipac.heritage.searches;

import com.google.gwt.i18n.client.DateTimeFormat;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.heritage.commands.SearchByDateCmd;
import edu.caltech.ipac.heritage.data.entity.DataType;

import java.io.Serializable;
import java.util.Date;


/**
 * Date: Jun 8, 2009
 *
 * @author loi
 * @version $Id: SearchByDate.java,v 1.7 2010/04/24 01:13:04 loi Exp $
 */
public class SearchByDate extends HeritageSearch<SearchByDate.Req> {

    public enum Type {AOR("aorByDate", DataType.AOR),
                      BCD("bcdByDate", DataType.BCD),
                      PBCD("pbcdByDate", DataType.PBCD);
                        String searchId;
                        DataType dataType;
                        Type(String searchId, DataType dataType) {
                            this.searchId = searchId;
                            this.dataType = dataType;
                        }
                    }


    public SearchByDate(Type type, Request clientReq) {
        super(type.dataType, type.dataType.getShortDesc(), new Req(type, clientReq),
                null, null);
    }


    public String getDownloadFilePrefix() {
        DateTimeFormat fmt2 = DateTimeFormat.getFormat("yyyyMMdd");
        String preFile= "date-search-"+fmt2.format(getSearchRequest().getStartDate())
                    + "-" + fmt2.format(getSearchRequest().getEndDate()) + "-";
        return preFile;
    }

    public String getDownloadTitlePrefix() {
        DateTimeFormat fmt1 = DateTimeFormat.getFormat("yyyy.MM.dd HH:mm:ss");
        String preTitle= "Obs.Date " + fmt1.format(getSearchRequest().getStartDate())
                    + "-" + fmt1.format(getSearchRequest().getStartDate()) + ": ";
        return preTitle;
    }

//====================================================================
//
//====================================================================

    public static class Req extends HeritageRequest implements Serializable {
        private static final String START_DATE = SearchByDateCmd.START_DATE_KEY;
        private static final String END_DATE = SearchByDateCmd.END_DATE_KEY;

        public Req() {}

        public TableServerRequest newInstance() {
            return new Req();
        }
        
        public Req(Type type, Request req) {
            super(type.dataType);
            this.copyFrom(req);
            setRequestId(type.searchId);
        }

        public Date getStartDate() {
            return getDateParam(START_DATE);
        }
        public Date getEndDate() {
            return getDateParam(END_DATE);
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