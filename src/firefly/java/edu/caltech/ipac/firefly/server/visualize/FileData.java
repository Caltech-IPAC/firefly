/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;


import java.io.File;

/**
* @author Trey Roby
*/
public class FileData {
    private final File file;
    private final String desc;
    private final boolean blank;

    public FileData(File file, String desc, boolean blank) {
        this.file= file;
        this.desc= desc;
        this.blank= blank;
    }

    public FileData(File file, String desc) { this(file,desc,false); }

    public File getFile() { return file;}
    public String getDesc() { return desc;}
    public boolean isBlank() { return blank; }

}

