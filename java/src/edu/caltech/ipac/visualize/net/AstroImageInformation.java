package edu.caltech.ipac.visualize.net;

import javax.swing.ImageIcon;
import java.io.Serializable;


//import GOV.nasa.gsfc.sea.science.CoordinatesOffset;

/**
 * This class contains various read-only attributes of an astronomical image.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		01/25/99
 * @author		J. Jones / 588
**/
public class AstroImageInformation extends    ImageInformation
                                   implements Serializable  {
	private String		  fTarget;
	//private CoordinatesOffset fPlateSize;
        private double            plateWidth;
        private double            plateHeight;
	private String		  fTelescope;
	private String		  fBand;
	private double		  fResolutionArcsec;
	private String		  fReferenceCode;
        private ImageIcon         previewImage= null;
	
    public AstroImageInformation(
    		String name, 
    		String type, 
    		int    widthPixels, 
    		int    heightPixels,
    		int    fileSize,		// KB
    		String previewName,
    		String target,
    		double plateWidth,	// degrees
    		double plateHeight,	// degrees
    		String telescope,
    		String band,
    		double resolution,
    		String refcode) {

		super(name, type, widthPixels, heightPixels, 
                      fileSize, previewName);
		
		fTarget = target;
		if (fTarget != null) {
			fTarget = fTarget.trim();
		}
		
		//fPlateSize = new CoordinatesOffset(plateWidth, plateHeight);
		
                this.plateWidth=  plateWidth;
                this.plateHeight= plateHeight;
		fBand = band;
		fResolutionArcsec = resolution;
		fReferenceCode = refcode;
		fTelescope = telescope;
    }
    
    public String getTarget() { return fTarget; }
    
   // public CoordinatesOffset getPlateSize() {
   //    return fPlateSize;
   // }
    
    public String getBand() { return fBand; }
    
    public double getResolutionArcsec() { return fResolutionArcsec; }
        
    public String getReferenceCode() { return fReferenceCode; }        
    
    public String getTelescope() { return fTelescope; }
  
    public double getPlateWidth()  { return plateWidth; }
    public double getPlateHeight() { return plateHeight; }

    public void setPreviewImage(ImageIcon image) {
       previewImage= image;
    }
    public ImageIcon getPreviewImage() { return previewImage; }

    //protected void setPlateSize(double width, double height) {
    //   fPlateSize = new CoordinatesOffset(width, height);
    //}
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
