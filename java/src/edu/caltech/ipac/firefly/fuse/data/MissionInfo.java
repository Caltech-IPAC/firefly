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

