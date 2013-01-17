package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.data.HasAccessInfo;
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

    private FileNameResolver _resolver = null;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public FileInfo(String internalFilename, String externalName, long sizeInBytes) {
        _internalFilename = internalFilename;
        _externalName = externalName;
        _sizeInBytes= sizeInBytes;
    }

    public FileInfo(String internalFilename, FileNameResolver resolver, long sizeInBytes) {
        _internalFilename = internalFilename;
        _externalName = null;
        _resolver = resolver;
        _sizeInBytes= sizeInBytes;
    }

    public FileInfo(String internalFilename, String externalName, Integer sizeInBytes) {
        this(internalFilename, externalName, sizeInBytes.longValue());
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public String getInternalFilename() { return _internalFilename; }
    public String getExternalName() { return _externalName; }
    public void setExternalName(String filename) {_externalName = filename; }
    public long getSizeInBytes() { return _sizeInBytes; }
    public void setSizeInBytes(long sizeInBytes) { _sizeInBytes = sizeInBytes; }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FileInfo) {
            FileInfo fi = (FileInfo)o;
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
