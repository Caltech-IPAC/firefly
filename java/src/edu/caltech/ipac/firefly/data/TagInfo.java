package edu.caltech.ipac.firefly.data;

import java.util.Date;
import java.io.Serializable;

/**
 * @author tatianag
 * $Id: TagInfo.java,v 1.3 2009/10/02 04:20:26 tatianag Exp $
 */
public class TagInfo implements Serializable, Cloneable {

    private int tagID;
    private String tagName;
    private String historyToken;
    private String description;
    private boolean isTag;
    private int numHits;
    private Date timeCreated;
    private Date timeUsed;

    public TagInfo() {
        this(0, null, null, null,
                false, 0, null, null);
    }

    public TagInfo(int tagID, String tagName, String historyToken, String description,
                   boolean isTag, int numHits, Date timeCreated, Date timeUsed) {
        this.tagID = tagID;
        this.tagName = tagName;
        this.historyToken = historyToken;
        this.description = description;
        this.isTag = isTag;
        this.numHits = numHits;
        this.timeCreated = timeCreated;
        this.timeUsed = timeUsed;

    }

    public int getTagID() { return tagID; }
    public String getTagName() { return tagName; }
    public String getHistoryToken() { return historyToken; }
    public String getDescription() { return description; }
    public boolean isTag() { return isTag; }
    public int getNumHits() { return numHits; }
    public Date getTimeCreated() { return timeCreated; }
    public Date getTimeUsed() { return timeUsed; }

    
}
