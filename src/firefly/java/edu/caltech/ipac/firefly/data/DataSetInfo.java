/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
        Set<SpacialType> set= new HashSet<SpacialType>(Arrays.asList(Cone, Elliptical, Box, Polygon,
                                                        MultiTableUpload,
//                                                        MultiPoints, MultiPrevSearch,
                                                        MultiSupportsOneToOneAttribute,
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

