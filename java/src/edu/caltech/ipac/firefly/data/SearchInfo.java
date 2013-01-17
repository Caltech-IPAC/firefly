package edu.caltech.ipac.firefly.data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author tatianag
 * $Id: SearchInfo.java,v 1.5 2010/04/06 00:30:04 loi Exp $
 */
public class SearchInfo implements Serializable, Cloneable  {
    private int queryid;
    private String loginName;
    private String historytoken;
    private String description;
    private boolean favorite;
    private Date timeadded;

    public SearchInfo() {
        this(0, "unknown", null, null, false, new Date());
    }

    public SearchInfo(int queryid, String loginName, String historytoken, String description,
                      boolean favorite, Date timeadded) {
        this.queryid = queryid;
        this.loginName = loginName;
        this.historytoken = historytoken;
        this.description = description;
        this.favorite = favorite;
        this.timeadded = timeadded;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQueryID() { return queryid; }
    public String getLoginID() { return loginName; }
    public String getHistoryToken() { return historytoken; }
    public String getDescription() { return description; }
    public boolean isFavorite() { return favorite; }
    public Date getTimeAdded() { return timeadded; } 
}
