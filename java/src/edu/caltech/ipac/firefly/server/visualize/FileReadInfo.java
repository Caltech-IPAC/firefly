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

