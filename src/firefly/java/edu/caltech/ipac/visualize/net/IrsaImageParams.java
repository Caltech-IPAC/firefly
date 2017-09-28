/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.Assert;

public class IrsaImageParams extends ImageServiceParams {

    ;

    private String _band= "12";
    private float  _size= 5.0F;

    public IrsaImageParams(ImageSourceTypes type) {super(type);}

    public void   setBand(String b)     { _band= b; }
    public void   setSize(float s)      { _size= s; }
    public String getBand()             { return _band; }
    public float  getSize()             { return _size; }

    public String getUniqueString() {
         String retval= null;
         switch (getType()) {
             case ISSA :
                       retval= "Issa-" + super.toString() + _band + _size;
                       break;
             case TWOMASS :
                       retval= "2mass-" + super.toString() + _band + _size;
                       break;
             case MSX :
                       retval= "msx-" + super.toString() + _band + _size;
                       break;
             case IRIS :
                       retval= "iris-" + super.toString() + _band + _size;
                       break;
             default :
                 Assert.tst(false); break;
         }
         return retval;
    }

    public String toString() { return getUniqueString(); }
}
