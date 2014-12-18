package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.download.NetParams;

/**
 * This class defines parameters for Ned catalogs. This class is modeled
 * after IrsaCatalogParams class.
 * @author Michael Nguyen
 * @see edu.caltech.ipac.util.download.NetParams
 */

public class NedCatalogParams extends BaseIrsaParams
                               implements NetParams 
{
    private String _catName;
    private float  _size = 5.0F;

    public NedCatalogParams() {}

    public void setCatalogName(String n) { _catName= n; }
    public void setSize(float s) { _size= s; }

    public String getCatalogName() { return _catName; }
    public float  getSize() { return _size; }

    // maybe needs to replace spaces with some other characters
    public String getUniqueString() 
    {
         return  _catName + "-" + super.toString() + _size;
    }

    public String toString() { return getUniqueString(); }
}
