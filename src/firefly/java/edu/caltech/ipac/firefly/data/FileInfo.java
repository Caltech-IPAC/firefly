/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileInfo implements HasAccessInfo, Serializable {

    public static final String INTERNAL_NAME= "internalName";
    public static final String EXTERNAL_NAME= "externalName";
    public static final String RESPONSE_CODE = "responseCode";
    public static final String RESPONSE_CODE_MSG = "responseCodeMsg";
    public static final String FILE_DOWNLOADED= "fileDownloaded";
    public static final String SIZE_IN_BYTES= "sizeInBytes";
    public static final String DESC="desc";
    public static final String BLANK="blank";
    public static final String HAS_ACCESS="hasAccess";

    private final Map<String,String> attributes= new HashMap<>(9);
    private Map<String, String> cookies= null;
    private transient FileNameResolver resolver= null;
    private List<RelatedData> relatedData= null;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    private FileInfo() {}

    public FileInfo(String internalFilename, String externalName, long sizeInBytes) {
        setInternalName(internalFilename);
        setExternalName(externalName);
        setSizeInBytes(sizeInBytes);
    }

    public FileInfo(String internalFilename, FileNameResolver resolver, long sizeInBytes) {
        setInternalName(internalFilename);
        setSizeInBytes(sizeInBytes);
        this.resolver = resolver;
    }

    public FileInfo(File file, String desc) { this(file, null, desc, 200, "OK"); }

    public FileInfo(File file) { this(file, file.getName(), 200, "OK"); }

    public FileInfo(File file, String externalName, int responseCode, String responseCodeMsg) {
        this(file, externalName, externalName, responseCode, responseCodeMsg);
    }

    private FileInfo(File file, String externalName, String desc, int responseCode, String responseCodeMsg) {
        setInternalName( file != null ? file.getAbsolutePath() : null);
        putAttribute(RESPONSE_CODE, responseCode + "");
        putAttribute(RESPONSE_CODE_MSG, responseCodeMsg!=null ? responseCodeMsg : "");
        setSizeInBytes(file != null ? file.length()  : -1 );

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

    public File getFile() {  return new File(attributes.get(INTERNAL_NAME)); }
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

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public String getInternalFilename() { return getAttribute(INTERNAL_NAME); }

    public String getExternalName() { return getAttribute(EXTERNAL_NAME); }

    public void setInternalName(String filename) { putAttribute(INTERNAL_NAME,filename); }
    public void setExternalName(String filename) { putAttribute(EXTERNAL_NAME,filename); }

    public long getSizeInBytes() { return StringUtils.getInt(getAttribute(SIZE_IN_BYTES),0); }

    public void setSizeInBytes(long sizeInBytes) { putAttribute(SIZE_IN_BYTES,sizeInBytes+""); }

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
        if (cookies!=null) {
            fi.cookies= new HashMap<>(cookies);
        }
        if (resolver!=null) fi.resolver= resolver;
        if (relatedData!=null) {
            fi.relatedData= new ArrayList<>(relatedData);
        }
        return fi;
    }

    public FileInfo copyWith(String... valuePairs) {
        FileInfo fi= copy();
        if (valuePairs != null && valuePairs.length > 0) {
            for(int i= 0; (i<valuePairs.length); i+=2) {
                if (valuePairs[i]!=null && valuePairs.length<i+1) {
                    fi.putAttribute(valuePairs[i], valuePairs[i+1]);
                }
            }
        }
        return fi;
    }
    public FileInfo copyWithDesc(String desc) {
        return copyWith(DESC, desc);
    }

    public interface FileNameResolver {
        String getResolvedName(String input);
    }
}

