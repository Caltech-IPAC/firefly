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
