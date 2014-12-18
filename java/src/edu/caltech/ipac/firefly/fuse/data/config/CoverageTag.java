package edu.caltech.ipac.firefly.fuse.data.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;

/**
 * Date: 7/23/14
 *
 * @author loi
 */
@XStreamAlias("RelatedImages")
public class CoverageTag implements Serializable {

    @XStreamAlias("Title")
    @XStreamAsAttribute
    protected String title;

    @XStreamAlias("CenterColumns")
    @XStreamAsAttribute
    protected String CenterColumns;

    @XStreamAlias("CornerColumns")
    @XStreamAsAttribute
    protected String CornerColumns;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCenterColumns() {
        return CenterColumns;
    }

    public void setCenterColumns(String centerColumns) {
        CenterColumns = centerColumns;
    }

    public String getCornerColumns() {
        return CornerColumns;
    }

    public void setCornerColumns(String cornerColumns) {
        CornerColumns = cornerColumns;
    }
}

