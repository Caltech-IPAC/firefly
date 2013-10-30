package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/16/11
 * Time: 3:53 PM
 */


import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.visualize.plot.FitsRead;

import java.io.File;

/**
* @author Trey Roby
*/
class FileReadInfo {

    private final Band band;
    private final File originalFile; // the original file name
    private final File workingFile;  //  the version of the original file without any .zip on it, used as base name for _workingFile
    private final int originalImageIdx;
    private final FitsRead fr;
    private final String dataDesc;
    private final ModFileWriter modFileWriter;
    private final String uploadedName;

    FileReadInfo(File originalFile,
                 FitsRead fr,
                 Band band,
                 int imageIdx,
                 String dataDesc,
                 String uploadedName,
                 ModFileWriter modFileWriter) {
        this.originalFile= originalFile;
        this.workingFile = originalFile;
        this.band= band;
        this.originalImageIdx= imageIdx;
        this.fr= fr;
        this.modFileWriter= modFileWriter;
        this.dataDesc= dataDesc;
        this.uploadedName= uploadedName;
    }

    public Band getBand() { return band; }
    public File getOriginalFile() { return originalFile; }
    public File getWorkingFile() { return workingFile; }
    public int getOriginalImageIdx() { return originalImageIdx; }
    public FitsRead getFitsRead() { return fr; }
    public String getDataDesc() { return dataDesc; }
    public ModFileWriter getModFileWriter() { return modFileWriter; }
    public String getUploadedName() {return uploadedName;}
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
