package edu.caltech.ipac.firefly.server.util.multipart;

import java.io.File;
import java.io.Serializable;

/**
 * Date: Jul 27, 2010
 *
 * @author loi
 * @version $Id: UploadFileInfo.java,v 1.1 2010/07/29 00:37:05 loi Exp $
 */
public class UploadFileInfo implements Serializable {
    private String pname;
    private File file;
    private String fileName;
    private String contentType;
    private long size;

    public UploadFileInfo(String pname, File file, String fileName, String contentType) {
        this.pname = pname;
        this.file = file;
        this.fileName = fileName;
        this.contentType = contentType;
        if (file != null && file.exists()) {
            size = file.length();
        }
    }

    public String getPname() {
        return pname;
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "FileEntry{" + fileName + "[" + size + "]}";
    }
}
