/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

public class GeomException extends Exception 
{
    private int requested_naxis1;
    private int requested_naxis2;

    public GeomException(String s, int naxis1, int naxis2) 
    { 
	super(s); 
	this.requested_naxis1 = naxis1;
	this.requested_naxis2 = naxis2;
    }

    public int getRequestedNaxis1()
    {
	return(requested_naxis1);
    }
    public int getRequestedNaxis2()
    {
	return(requested_naxis2);
    }
}
