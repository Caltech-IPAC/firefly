package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.download.NetParams;
import edu.caltech.ipac.util.Assert;

/**
 * This class defines parameters for SkyView catalogs. This class is modeled
 * after IrsaCatalogParams class.
 * @author Trey Roby
 * @see edu.caltech.ipac.util.download.NetParams
 */

public class SkyViewCatalogParams extends BaseIrsaParams implements NetParams  {

    public enum Site {HEASARC, VIZIER}
    private String _catName;
    private String _tname;
    private float  _size = 5.0F;
    private final Site   _site;

    public SkyViewCatalogParams(Site site) {_site= site;}

    public void setCatalogName(String s) { _catName= s; }
    public void setSize(float s) { _size= s; }
    public void setTargetName(String tname) { _tname= tname;}

    public Site getSite() { return _site; }

    public String getCatalogName() { return _catName; }
    public String getTargetName() { return _tname; }
    public float  getSize() { return _size; }

    public String getUniqueString() 
    {
        String header;
        if (_site== SkyViewCatalogParams.Site.HEASARC) {
            header= "Heasarc-";
        }
        else if (_site== SkyViewCatalogParams.Site.VIZIER) {
            header= "Vizier-";
        }
        else {
            header= null;
            Assert.tst(false);
        }
        String retval=  header + _catName + "-" + super.toString() + _size;
        return retval.replaceAll("[ :\\[\\]\\/\\\\|\\*\\?<>]", "");
    }

    public String toString() { return getUniqueString(); }
}
