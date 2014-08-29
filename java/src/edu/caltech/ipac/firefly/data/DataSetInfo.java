package edu.caltech.ipac.firefly.data;
/**
 * User: roby
 * Date: 1/28/14
 * Time: 1:20 PM
 */


import edu.caltech.ipac.firefly.ui.catalog.Proj;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static edu.caltech.ipac.firefly.data.SpacialType.*;

/**
 * @author Trey Roby
 */
public class DataSetInfo {

    private String id;
    private String userDesc;
    private boolean hasCatalogs= false;
    private boolean hasImages= false;
    private boolean hasSpectrum= false;
    private Proj catProjInfo;
    private Set<SpacialType> spectrumSpatial;
    private Set<SpacialType> catSpatial;
    private Set<SpacialType> imageSpatial;
    private Set<ImageIntersectionType> imageIntersection;

    public DataSetInfo(String id, String userDesc) {
        this.id = id;
        this.userDesc = userDesc;
    }


    public String getId() {
        return id;
    }

    public boolean getHasCatalogs() { return hasCatalogs; }
    public boolean getHasImages() { return hasImages; }
    public boolean getHasSpectrum() { return hasSpectrum; }

    public Set<SpacialType> getSpatialSearchType(DataTypes dt) {
        Set<SpacialType> retval= null;
        switch (dt) {
            case CATALOGS: retval= catSpatial; break;
            case IMAGES:   retval= imageSpatial; break;
            case SPECTRUM: retval= spectrumSpatial; break;
        }

        return retval;
    }

    public String getUserDesc() {  return userDesc; }


    public void setCatData(Proj catalogProjInfo) {
        Set set= new HashSet<SpacialType>(Arrays.asList(Cone, Elliptical, Box, Polygon,
                                                        MultiTableUpload, /*MultiPoints, MultiPrevSearch,*/
                                                        AllSky));
        setCatData(catalogProjInfo, set);
    }

    public void setCatData(Proj catProjInfo, Set<SpacialType> catSpatial) {
        this.catProjInfo = catProjInfo;
        hasCatalogs= catProjInfo!=null;
        this.catSpatial = catSpatial;
    }

    public Proj getCatProjInfo() { return catProjInfo; }
    public Proj getCatSpacialSearchTypes() { return catProjInfo; }

    public void setImageProjInfo(Object dummy,
                                 Set<SpacialType> imageSpatial,
                                 Set<ImageIntersectionType> imageIntersection,
                                 Object uiDef) {  // todo, this is only a place holder for image data

        this.imageSpatial= imageSpatial;
        this.imageIntersection= imageIntersection;
        hasImages= dummy!=null;
    }

    public Object getImageUIDef() { return null; }  // todo, this is only a place holder for the xml data

    public void setSpectrumProjInfo(Object dummy) {  // todo, this is only a place holder image data

        hasSpectrum= dummy!=null;
    }

    public enum DataTypes {CATALOGS, IMAGES, SPECTRUM}
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
