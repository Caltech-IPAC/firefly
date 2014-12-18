package edu.caltech.ipac.firefly.fuse.data.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;

/**
 * Date: 7/23/14
 *
 * @author loi
 */
@XStreamAlias("Image")
public class ImageTag implements Serializable {

    @XStreamAlias("ArgCols")
    @XStreamAsAttribute
    protected String argCols;

    @XStreamAlias("ArgHeaders")
    @XStreamAsAttribute
    protected String argHeaders;

    @XStreamAlias("Title")
    @XStreamAsAttribute
    protected String title;

    @XStreamAlias("Zoom")
    @XStreamAsAttribute
    protected String zoom;

    @XStreamAlias("Stretch")
    @XStreamAsAttribute
    protected String stretch;

    @XStreamAlias("CenterOnQueryTarget")
    @XStreamAsAttribute
    protected String centerOnQueryTarget;

    @XStreamAlias("ColorTableId")
    @XStreamAsAttribute
    protected String colorTableId;

    @XStreamAlias("PlotGroup")
    @XStreamAsAttribute
    protected String plotGroup;

    @XStreamAlias("LockRelated")
    @XStreamAsAttribute
    protected String lockRelated;

    @XStreamAlias("RememberPrefs")
    @XStreamAsAttribute
    protected String rememberPrefs;

    @XStreamAlias("MinSize")
    @XStreamAsAttribute
    protected String minSize;

    public String getArgCols() {
        return argCols;
    }

    public void setArgCols(String argCols) {
        this.argCols = argCols;
    }

    public String getArgHeaders() {
        return argHeaders;
    }

    public void setArgHeaders(String argHeaders) {
        this.argHeaders = argHeaders;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getZoom() {
        return zoom;
    }

    public void setZoom(String zoom) {
        this.zoom = zoom;
    }

    public String getStretch() {
        return stretch;
    }

    public void setStretch(String stretch) {
        this.stretch = stretch;
    }

    public String getCenterOnQueryTarget() {
        return centerOnQueryTarget;
    }

    public void setCenterOnQueryTarget(String centerOnQueryTarget) {
        this.centerOnQueryTarget = centerOnQueryTarget;
    }

    public String getColorTableId() {
        return colorTableId;
    }

    public void setColorTableId(String colorTableId) {
        this.colorTableId = colorTableId;
    }

    public String getPlotGroup() {
        return plotGroup;
    }

    public void setPlotGroup(String plotGroup) {
        this.plotGroup = plotGroup;
    }

    public String getLockRelated() {
        return lockRelated;
    }

    public void setLockRelated(String lockRelated) {
        this.lockRelated = lockRelated;
    }

    public String getRememberPrefs() {
        return rememberPrefs;
    }

    public void setRememberPrefs(String rememberPrefs) {
        this.rememberPrefs = rememberPrefs;
    }

    public String getMinSize() {
        return minSize;
    }

    public void setMinSize(String minSize) {
        this.minSize = minSize;
    }
}

