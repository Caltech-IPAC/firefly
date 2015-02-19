/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.data.HasAccessInfo;

import java.util.Map;
/**
 * User: roby
 * Date: Sep 26, 2008
 * Time: 12:52:29 PM
 */


/**
 * @author Trey Roby
 */
public class FileInfo implements HasAccessInfo {

    private final String _internalFilename;
    private String _externalName;
    private long _sizeInBytes;
    private boolean hasAccess = true;
    private Object extraData = null;
    private Map<String, String> cookies;

    private FileNameResolver _resolver = null;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public FileInfo(String internalFilename, String externalName, long sizeInBytes) {
        _internalFilename = internalFilename;
        _externalName = externalName;
        _sizeInBytes = sizeInBytes;
    }

    public FileInfo(String internalFilename, FileNameResolver resolver, long sizeInBytes) {
        _internalFilename = internalFilename;
        _externalName = null;
        _resolver = resolver;
        _sizeInBytes = sizeInBytes;
    }

    public FileInfo(String internalFilename, String externalName, Integer sizeInBytes) {
        this(internalFilename, externalName, sizeInBytes.longValue());
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public boolean isUrlBased() {
        return _internalFilename.contains("://");
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public String getInternalFilename() {
        return _internalFilename;
    }

    public String getExternalName() {
        return _externalName;
    }

    public void setExternalName(String filename) {
        _externalName = filename;
    }

    public long getSizeInBytes() {
        return _sizeInBytes;
    }

    public void setSizeInBytes(long sizeInBytes) {
        _sizeInBytes = sizeInBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FileInfo) {
            FileInfo fi = (FileInfo) o;
            // external name is unique
            return getExternalName().equals(fi.getExternalName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        // external name is unique
        return getExternalName().hashCode();
    }

    public void setHasAccess(boolean hasAccess) {
        this.hasAccess = hasAccess;
    }

    public boolean hasAccess() {
        return hasAccess;
    }

    public void setExtraData(Object extraData) {
        this.extraData = extraData;
    }

    public Object getExtraData() {
        return extraData;
    }

    public boolean hasFileNameResolver() {
        return (_resolver != null);
    }

    public String resolveFileName(String name) {
        return _resolver.getResolvedName(name);
    }

    public interface FileNameResolver {
        public String getResolvedName(String input);
    }

}

