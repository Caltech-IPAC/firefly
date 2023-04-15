/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.download.ResponseMessage;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.data.HttpResultInfo.CONTENT_TYPE;
import static edu.caltech.ipac.firefly.data.HttpResultInfo.EXTERNAL_NAME;
import static edu.caltech.ipac.firefly.data.HttpResultInfo.RESPONSE_CODE;
import static edu.caltech.ipac.firefly.data.HttpResultInfo.RESPONSE_CODE_MSG;
import static edu.caltech.ipac.firefly.data.HttpResultInfo.SIZE_IN_BYTES;


public class FileInfo implements HasAccessInfo, Serializable, CacheKey {

    public static final String INTERNAL_NAME= "internalName";
    public static final String FILE_DOWNLOADED= "fileDownloaded";
    public static final String DESC="desc";
    public static final String BLANK="blank";
    public static final String HAS_ACCESS="hasAccess";

    private final Map<String,String> attributes= new HashMap<>(9);
    private transient FileNameResolver resolver= null;
    private List<RelatedData> relatedData= null;
    private HttpServiceInput requestInfo= null;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    protected FileInfo() {}

    /**
     * @param internalFilename   this can be a local file path or a url
     * @param externalName       the file path/name to package as
     * @param sizeInBytes        size in bytes of this file.
     */
    public FileInfo(String internalFilename, String externalName, long sizeInBytes) {
        setInternalName(internalFilename);
        setExternalName(externalName);
        setSizeInBytes(sizeInBytes);
    }

    public FileInfo(File file, String desc) { this(file, null, desc, 200, "OK", null); }

    public FileInfo(File file) { this(file, file.getName(), 200, ResponseMessage.getHttpResponseMessage(200)); }


    public FileInfo(File file, String externalName, int responseCode, String responseCodeMsg, String contentType) {
        this(file, externalName, externalName, responseCode, responseCodeMsg, contentType);
    }

    public FileInfo(File file, String externalName, int responseCode, String responseCodeMsg) {
        this(file, externalName, externalName, responseCode, responseCodeMsg, null);
    }


    public FileInfo(int responseCode) {
        this(null,"", responseCode, ResponseMessage.getHttpResponseMessage(responseCode));

    }

    private FileInfo(File file, String externalName, String desc, int responseCode, String responseCodeMsg, String contentType) {
        setInternalName( file != null ? file.getAbsolutePath() : null);
        putAttribute(RESPONSE_CODE, responseCode + "");
        putAttribute(RESPONSE_CODE_MSG, responseCodeMsg!=null ? responseCodeMsg : "");
        setSizeInBytes(file != null ? file.length()  : -1 );
        if (contentType!=null) putAttribute(CONTENT_TYPE, contentType);

        if (externalName != null) {
            setExternalName(externalName);
        } else {
            setExternalName(file != null ? file.getName() : null);
        }

        putAttribute(DESC, desc != null ? desc : file != null ? file.getAbsolutePath() : null);
        putAttribute(HAS_ACCESS, true + "");
    }


    public static FileInfo blankFilePlaceholder() {
        FileInfo fi= new FileInfo();
        fi.putAttribute(BLANK,true+"");
        fi.putAttribute(DESC, "BLANK");
        return fi;
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void putAttribute(String key, String value) { attributes.put(key,value); }
    public String getAttribute(String key) {return attributes.get(key); }
    public Map<String,String> getAttributeMap() {return new HashMap<>(attributes);}



    public File getFile() {
        return attributes.containsKey(INTERNAL_NAME) ? new File(attributes.get(INTERNAL_NAME)) : null;
    }
    public String getDesc() { return attributes.get(DESC); }
    public boolean isBlank() { return StringUtils.getBoolean(attributes.get(BLANK), false); }
    public int getResponseCode() { return StringUtils.getInt(attributes.get(RESPONSE_CODE), 200); }
    public String getResponseCodeMsg() { return attributes.get(RESPONSE_CODE_MSG); }


    public void addRelatedDataList(List<RelatedData> rDataList) {
        if (rDataList==null) return;
        for(RelatedData rd : rDataList) addRelatedData(rd);
    }


    public void addRelatedData(RelatedData rData) {
        if (rData!=null) {
            if (relatedData==null) relatedData= new ArrayList<>();
            relatedData.add(rData);
        }
    }

    public List<RelatedData> getRelatedData() { return relatedData; }

    public HttpServiceInput getRequestInfo() {
        return requestInfo;
    }

    public void setRequestInfo(HttpServiceInput requestInfo) {
        this.requestInfo = requestInfo;
    }

    public String getInternalFilename() { return getAttribute(INTERNAL_NAME); }

    public String getExternalName() { return getAttribute(EXTERNAL_NAME); }

    public String getContentType() { return getAttribute(CONTENT_TYPE); }

    public void setInternalName(String filename) {
        if (filename!=null) putAttribute(INTERNAL_NAME,filename);
    }
    public void setExternalName(String filename) { putAttribute(EXTERNAL_NAME,filename); }

    public long getSizeInBytes() { return StringUtils.getInt(getAttribute(SIZE_IN_BYTES),0); }

    public void setSizeInBytes(long sizeInBytes) { putAttribute(SIZE_IN_BYTES,sizeInBytes+""); }

    public void setDesc(String desc) { putAttribute(DESC,desc); }


    @Override
    public boolean equals(Object o) {
        if (o instanceof FileInfo) {
            FileInfo fi = (FileInfo) o;
            // external name is unique
            return getInternalFilename().equals(fi.getInternalFilename());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        // external name is unique
        return getInternalFilename().hashCode();
    }

    @Override
    public String getUniqueString() { return getInternalFilename(); }

    public void setHasAccess(boolean hasAccess) {
        putAttribute(HAS_ACCESS, hasAccess+"");
    }

    public boolean hasAccess() {
        return StringUtils.getBoolean(attributes.get(HAS_ACCESS), true);
    }

    public boolean hasFileNameResolver() {
        return (resolver != null);
    }

    public String resolveFileName(String name) { return resolver.getResolvedName(name); }


    public FileInfo copy() {
        FileInfo fi= new FileInfo();
        fi.attributes.putAll(this.attributes);
        if (requestInfo!=null) {
            fi.requestInfo= requestInfo.copy();
        }
        if (resolver!=null) fi.resolver= resolver;
        if (relatedData!=null) {
            fi.relatedData= new ArrayList<>(relatedData);
        }
        return fi;
    }

    public interface FileNameResolver {
        String getResolvedName(String input);
    }
}

