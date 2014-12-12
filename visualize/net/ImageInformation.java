package edu.caltech.ipac.visualize.net;

import java.awt.Image;
import java.io.Serializable;


/**
 * This class contains various read-only attributes of an image.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		01/25/99
 * @author		J. Jones / 588
**/
public class ImageInformation implements Serializable  {

	private String	fName = null;
	private String  fType = null;
	private int     fWidth = 0;
	private int     fHeight = 0;
	private int     fFileSize = 0;	// KB
	private String	fPreviewName = null;

    public ImageInformation(String name, 
                            String type, 
                            int    width, 
                            int    height, 
                            int    fileSize, 
                            String preview) {
       super();

       fName = name;
       fType = type;
       fWidth = width;
       fHeight = height;
       fFileSize = fileSize;
       fPreviewName = preview;
    }

    public String getName() { return fName; }
    public String getType() { return fType; }
    public int    getWidth()   { return fWidth; }
    public int    getHeight()  { return fHeight; }
    public int    getFileSize(){ return fFileSize; }

    public void setFileSize(int fileSize) {
		fFileSize = fileSize;
    }
	
    public String getPreviewFilename() { return fPreviewName; }
	
    public void setPreviewFilename(String name) { fPreviewName = name; }

    public String toString() {
    	String output = "";

    	output += "\nImage File Information:";
    	output += "\nNAME = " + fName;
    	output += "\nTYPE = " + fType;
    	output += "\nWIDTH = " + fWidth;
    	output += "\nHEIGHT = " + fHeight;

    	return output;
    }
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
