package edu.caltech.ipac.firefly.fuse.data;
/**
 * User: roby
 * Date: 1/28/14
 * Time: 1:20 PM
 */


import edu.caltech.ipac.firefly.data.ImageIntersectionType;
import edu.caltech.ipac.firefly.data.SpacialType;

import java.io.Serializable;
import java.util.Set;

/**
 * @author Trey Roby
 */
public class MissionInfo implements Serializable {

    public enum DataTypes {CATALOGS, IMAGES, SPECTRUM}

    private String name;
    private String title;
    private String desc;
    private Set<DataTypes> dataTypes;
    private Set<SpacialType> spectrumSpatial;
    private Set<SpacialType> catSpatial;
    private Set<SpacialType> imageSpatial;
    private Set<ImageIntersectionType> imageIntersection;

    public MissionInfo(String name, String title) {
        this(name, title, title);
    }

    public MissionInfo(String name, String title, String desc) {
        this.name = name;
        this.title = title;
        this.desc = desc;
    }

    public void setDataTypes(Set<DataTypes> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public void setSpectrumSpatial(Set<SpacialType> spectrumSpatial) {
        this.spectrumSpatial = spectrumSpatial;
    }

    public void setCatSpatial(Set<SpacialType> catSpatial) {
        this.catSpatial = catSpatial;
    }

    public void setImageSpatial(Set<SpacialType> imageSpatial) {
        this.imageSpatial = imageSpatial;
    }

    public void setImageIntersection(Set<ImageIntersectionType> imageIntersection) {
        this.imageIntersection = imageIntersection;
    }


    public Set<SpacialType> getSpectrumSpatial() {
        return spectrumSpatial;
    }

    public Set<SpacialType> getCatSpatial() {
        return catSpatial;
    }

    public Set<SpacialType> getImageSpatial() {
        return imageSpatial;
    }

    public Set<ImageIntersectionType> getImageIntersection() {
        return imageIntersection;
    }

    public boolean hasCatalogs() {
        return dataTypes != null && dataTypes.contains(DataTypes.CATALOGS);
    }

    public boolean hasImages() {
        return dataTypes != null && dataTypes.contains(DataTypes.IMAGES);
    }

    public boolean hasSpectrum() {
        return dataTypes != null && dataTypes.contains(DataTypes.SPECTRUM);
    }

    public String getDesc() {
        return desc;
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
