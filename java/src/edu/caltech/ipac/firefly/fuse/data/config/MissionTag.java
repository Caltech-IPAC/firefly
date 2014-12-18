package edu.caltech.ipac.firefly.fuse.data.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.io.Serializable;
import java.util.List;

/**
 * Date: Feb 12, 2014
 *
 * @author loi
 * @version $Id: JossoUtil.java,v 1.7 2012/07/16 23:30:10 loi Exp $
 */
@XStreamAlias("Mission")
public class MissionTag implements Serializable {

    private long lastModified;
    @XStreamAsAttribute
    private String name;

    @XStreamAsAttribute
    private String dataTypes;

    @XStreamAlias("Title")
    private String title;

    @XStreamAlias("Desc")
    private String desc;

    @XStreamAlias("SpacialTypes")
    private SpacialTypeTag spacialTypes;
    @XStreamImplicit
    private List<ImageSetTag> imagesetList;
    @XStreamImplicit
    private List<CatalogSetTag> catalogList;
    @XStreamImplicit
    private List<SpectrumTag> spectrumList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(String dataTypes) {
        this.dataTypes = dataTypes;
    }

    public SpacialTypeTag getSpacialTypes() {
        return spacialTypes;
    }

    public void setSpacialTypes(SpacialTypeTag spacialTypes) {
        this.spacialTypes = spacialTypes;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public List<ImageSetTag> getImagesetList() {
        return imagesetList;
    }

    public void setImagesetList(List<ImageSetTag> imagesetList) {
        this.imagesetList = imagesetList;
    }

    public List<CatalogSetTag> getCatalogList() {
        return catalogList;
    }

    public void setCatalogList(List<CatalogSetTag> catalogList) {
        this.catalogList = catalogList;
    }

    public List<SpectrumTag> getSpectrumList() {
        return spectrumList;
    }

    public void setSpectrumList(List<SpectrumTag> spectrumList) {
        this.spectrumList = spectrumList;
    }
}

