/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import java.io.Serializable;

/**
 * @author tatianag
 * $Id: ResourcePath.java,v 1.2 2010/03/17 19:41:45 tatianag Exp $
 */
public class ResourcePath implements Serializable {

    /**
     * Ways to get a file: using URL, absolute path, or server request
     */
    public enum PathType { URL, FILE, PROCESSOR }

    private ServerRequest _fileRequest;
    private String _path;
    private PathType _type;

    public ResourcePath(PathType type, String path) {
        assert(!type.equals(PathType.PROCESSOR));
        _path = path;
        _type = type;
        _fileRequest = null;
    }

    public ResourcePath(ServerRequest fileRequest) {
        _type = PathType.PROCESSOR;
        _path = null;
        _fileRequest = fileRequest;
    }

    public ResourcePath() {
        this(null, null);
    }

    public PathType getPathType() { return _type; }
    public String getPath() { return _path; }
    public ServerRequest getFileRequest() { return _fileRequest; }

}
