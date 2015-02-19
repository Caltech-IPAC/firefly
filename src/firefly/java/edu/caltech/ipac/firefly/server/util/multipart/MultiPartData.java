/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.multipart;

import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: Jul 28, 2010
 *
 * @author loi
 * @version $Id: MultiPartData.java,v 1.2 2012/03/23 18:39:34 roby Exp $
 */
public class MultiPartData implements Serializable {
    private Map<String, String> params = new HashMap<String, String>();
    private List<UploadFileInfo> files = new ArrayList<UploadFileInfo>();
    private StringKey cacheKey= null;


    public MultiPartData() { this(null); }

    public MultiPartData(StringKey cacheKey) {
        this.cacheKey= cacheKey;
    }

    public void addParam(String name, String value) {
        params.put(name, value);
    }

    public void addFile(UploadFileInfo fi) {
        files.add(fi);
    }

    public void addFile(String pname, File file, String fileName, String contentType) {
        addFile(new UploadFileInfo(pname, file, fileName, contentType));
    }

    public Map<String, String> getParams() {
        return params;
    }

    public List<UploadFileInfo> getFiles() {
        return files;
    }

    public StringKey getCacheKey() { return cacheKey; }
}
