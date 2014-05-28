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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
