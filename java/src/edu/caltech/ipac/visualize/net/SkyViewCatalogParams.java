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
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
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
