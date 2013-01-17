package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.heritage.searches.HeritageRequest;
import edu.caltech.ipac.heritage.ui.MoreOptionsPanel;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author tatianag
 *         $Id: DateTimeFilter.java,v 1.1 2010/04/24 01:02:37 tatianag Exp $
 */
public class DateTimeFilter {


    private static final String START_DATE = MoreOptionsPanel.START_DATE_KEY;
    private static final String END_DATE = MoreOptionsPanel.END_DATE_KEY;

    private boolean hasFilters;
    private boolean hasStartDateFilter;
    private boolean hasEndDateFilter;
    private Date startDate = null;
    private Date endDate = null;

    private HeritageRequest req;
    public DateTimeFilter(HeritageRequest req)  {
        this.req = req;
        init();
    }

    private void init() {

        startDate = req.getDateParam(START_DATE);
        hasStartDateFilter = (startDate != null);
        endDate = req.getDateParam(END_DATE);
        hasEndDateFilter = (endDate != null);

        hasFilters = hasStartDateFilter || hasEndDateFilter;
    }

    public boolean hasFilters() {
        return hasFilters;
    }

    public boolean hasStartDateFilter() {
        return hasStartDateFilter;
    }

    public boolean hasEndDateFilter() {
        return hasEndDateFilter;
    }

    public Date getStartDate() { return startDate; }
    public Date getEndDate() {return endDate; }

    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.S");
        return ("Date Filter: "+
                (hasStartDateFilter ? "start="+sdf.format(startDate) : "")+
                (hasEndDateFilter ? "end="+sdf.format(endDate) : ""));
    }

    public static boolean isDefinedOn(HeritageRequest req) {
        return req.containsParam(START_DATE) || req.containsParam(END_DATE);
    }
}


