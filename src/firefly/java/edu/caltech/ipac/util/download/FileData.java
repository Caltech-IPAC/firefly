/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import edu.caltech.ipac.util.cache.FileHolder;

import java.io.File;
import java.io.Serializable;

/**
 * Date: Aug 31, 2005
 *
 * @author Trey Roby
 */
public class FileData implements FileHolder, Serializable, Cloneable {
    private final File file;
    private final String suggestedExternalName;
    private final boolean fileDownloaded;

    public FileData(File file, String suggestedExternalName) {
        this(file,suggestedExternalName, true);
    }
    public FileData(File file,
                    String suggestedExternalName,
                    boolean fileDownloaded) {
        this.file = file;
        this.suggestedExternalName = suggestedExternalName;
        this.fileDownloaded = fileDownloaded;
    }

//============================================================================
//---------------------------- Public Methods --------------------------------
//============================================================================

    public File getFile() {  return file; }
    public String getSuggestedExternalName()  { return suggestedExternalName;}
    public boolean isDownloaded() { return fileDownloaded; }

    public Object clone() { return new FileData(file, suggestedExternalName); }

}

