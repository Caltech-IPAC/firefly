package edu.caltech.ipac.heritage.searches;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.heritage.commands.SearchIrsEnhancedCmd;
import edu.caltech.ipac.heritage.data.entity.DataType;

import java.io.Serializable;

/**
 * @author tatianag
 *         $Id: SearchIrsEnhanced.java,v 1.1 2011/10/06 21:59:14 tatianag Exp $
 */
public class SearchIrsEnhanced  extends HeritageSearch<SearchIrsEnhanced.Req> {
    public SearchIrsEnhanced(edu.caltech.ipac.firefly.data.Request clientReq) {
        super(DataType.IRS_ENHANCED, DataType.IRS_ENHANCED.getShortDesc(), new Req(clientReq),
                null, null);
    }

    public String getDownloadFilePrefix() {
        return "irs-enhanced";
    }

    public String getDownloadTitlePrefix() {
        return "IRS Enhanced Products";
    }

    public static class Req extends HeritageRequest implements Serializable {
        private static final String SELECTED_COLS = SearchIrsEnhancedCmd.SELECTED_COLS_KEY;
        private static final String CONSTRAINTS = SearchIrsEnhancedCmd.CONSTRAINTS_KEY;



        public Req() {}

        public TableServerRequest newInstance() {
            return new Req();
        }

        public Req(edu.caltech.ipac.firefly.data.Request req) {
            super();
            this.copyFrom(req);
            setRequestId(IRS_ENHANCED_SEARCH_ID);
        }

        public String getSelectedColumns() {return getParam(SELECTED_COLS); }
        public String getConstraints() {return getParam(CONSTRAINTS); }
    }

}
