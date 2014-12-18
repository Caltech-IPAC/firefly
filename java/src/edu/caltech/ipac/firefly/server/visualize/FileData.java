package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/10/11
 * Time: 10:01 AM
 */


import java.io.File;

/**
* @author Trey Roby
*/
public class FileData {
    private final File _file;
    private final String _desc;
    private final boolean _blank;

    private FileData(File file, String desc, boolean blank) {
        _file= file;
        _desc= desc;
        _blank= blank;
    }

    public FileData(File file, String desc) { this(file,desc,false); }

    public FileData() { this(null,"BLANK", true); }


    public File getFile() { return _file;}
    public String getDesc() { return _desc;}
    public boolean isBlank() { return _blank; }

}

