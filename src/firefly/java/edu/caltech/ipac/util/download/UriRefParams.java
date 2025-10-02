/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UriRefParams extends BaseNetParams {

    private final List<UriRef> uriList;
    private UriRef optimalUriRef;
    private String _loginName= null;
    private boolean _checkForNewer= false;
    private String _desc = null;
    private File _dir = null; // if null, the use the default dir
    private long _maxSizeToDownload= 2*FileUtils.ONE_GB;
    private HttpServiceInput addtlInfo;


    public UriRefParams(URL url) { this(Collections.singletonList(UriRef.make(url)),null,null); }

    public UriRefParams(UriRef ref, File downloadDir) {
        this(Collections.singletonList(ref),null,null);
        setDownloadDir(downloadDir);
    }

    public UriRefParams(List<UriRef> uriList, String statusKey, String plotId) {
        super(statusKey,plotId);
        this.uriList = uriList;
    }

    public List<UriRef> getUriList() { return uriList; }
    public UriRef getUriRef() { return optimalUriRef!=null?optimalUriRef:uriList.getFirst(); }

    public void setOptimalUriRef(UriRef optimalUriRef) { this.optimalUriRef = optimalUriRef; }

    public HttpServiceInput getAddtlInfo() { return addtlInfo; }

    public void setAddtlInfo(HttpServiceInput addtlInfo) { this.addtlInfo = addtlInfo; }

    public String getUniqueString() {
        var desc= addtlInfo!=null ? addtlInfo.getDesc() : null;
        return RetrieveUtil.makeCacheFileString(uriList.getFirst(), _loginName, desc);
    }


    public void setLoginName(String name) { _loginName= name; }
    public void setMaxSizeToDownload(long max) {_maxSizeToDownload= max;}
    public long getMaxSizeToDownload() {return _maxSizeToDownload;}
    public void setDownloadDir(File dir) {_dir= dir;}
    public File getDownloadDir() {return _dir;}

    public void setCheckForNewer(boolean check) { _checkForNewer= check; }
    public boolean getCheckForNewer() { return _checkForNewer; }

    public Map<String, String> getAddtlCookies() { return addtlInfo == null ? null : addtlInfo.getCookies(); }
    public Map<String, String> getHeaders() { return addtlInfo == null ? null : addtlInfo.getHeaders(); }

    public void setDesc(String desc) { _desc = desc; }
    public String getDesc() { return _desc; }
}