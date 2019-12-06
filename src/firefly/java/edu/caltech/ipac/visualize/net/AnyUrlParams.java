/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.BaseNetParams;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnyUrlParams extends BaseNetParams {
    private static int MAX_LENGTH = 30;
    private URL _url;
    private Map<String,String> cookies= null;
    private String _loginName= null;
    private String _securityCookie= null;
    private boolean _checkForNewer= false;
    private List<String> _localFileExtensions = null;
    private String _desc = null;
    private File _dir = null; // if null, the use the default dir
    private static long _maxSizeToDownload= 0L;
    private HttpServiceInput addtlInfo;


    public AnyUrlParams(URL url) { this(url,null,null); }

    public AnyUrlParams(URL url, String statusKey, String plotId) {
        super(statusKey,plotId);
        _url= url;
    }

    public URL getURL() { return _url; }

    public HttpServiceInput getAddtlInfo() {
        return addtlInfo;
    }

    public void setAddtlInfo(HttpServiceInput addtlInfo) {
        this.addtlInfo = addtlInfo;
    }

    public String getUniqueString() {
        String fileStr= _url.getFile();
        fileStr= fileStr.replace('&','-');
        String loginName= "";
        String securCookie= "";
        if (_loginName!=null)  {
            loginName= "-"+ _loginName;
        }

        if (_securityCookie!=null && cookies!=null && cookies.containsKey(_securityCookie))  {
            securCookie= "-"+ cookies.get(_securityCookie);
        }

        // since this string is limited to MAX_LENGTH, having loginName in the baseKey is not ideal
        // loginName can be long.  it'll be used in hashcode calculation instead.
        String baseKey = securCookie +"-"+ fileStr;
        String addtlKeys = addtlInfo == null ? "" : addtlInfo.getDesc();
        int originalHashCode = (_url.getHost() + loginName + baseKey + addtlKeys).hashCode();
        baseKey= baseKey.replaceAll("[ :\\[\\]\\/\\\\|\\*\\?\\+<>]", "");




        if (baseKey.length()>MAX_LENGTH) {
            baseKey= baseKey.substring(0,MAX_LENGTH);
        }

        String retval;
        if (_desc!=null) {
            retval = _desc + "-"+originalHashCode;
        }
        else {
            String host = FileUtil.makeShortHostName(_url.getHost());
            retval = "URL-" + host + "-" + originalHashCode + baseKey;
        }
        //note: "=","," signs causes problem in download servlet.
        retval = retval.replaceAll("[ :\\[\\]\\/\\\\|\\*\\?<>\\=\\,]","\\-");
        if (_localFileExtensions!=null) {
            String ext= FileUtil.getExtension(fileStr);
            if (_localFileExtensions.contains(ext)) {
                retval = retval + "." + ext;
            }
            else {
                retval = retval + "." + _localFileExtensions.get(0);
            }
        }
        return retval;
    }

    public void setLoginName(String name) { _loginName= name; }
    public void setSecurityCookie(String cookieName) { _securityCookie= cookieName; }
    public void setMaxSizeToDownload(long max) {_maxSizeToDownload= max;}
    public long getMaxSizeToDownload() {return _maxSizeToDownload;}
    public void setFileDir(File dir) {_dir= dir;}
    public File getFileDir() {return _dir;}

    public void setCheckForNewer(boolean check) { _checkForNewer= check; }
    public boolean getCheckForNewer() { return _checkForNewer; }

    public boolean isCompressedFileName() {
        return getUniqueString().toLowerCase().endsWith(FileUtil.GZ);
    }

    public void addCookie(String key, String value) {
        if (cookies==null) {
            cookies= new HashMap<String, String>(31);
        }
        cookies.put(key,value);
    }


    public Map<String, String> getHeaders() {
        return addtlInfo == null ? null : addtlInfo.getHeaders();
    }

    public Map<String,String> getCookies() {
        Map<String,String> retval = new HashMap<>();
        if (cookies != null ) {
            retval.putAll(cookies);
        }
        if (addtlInfo != null && addtlInfo.getCookies() != null) {
            retval.putAll(addtlInfo.getCookies());
        }
        return retval;
    }

    public String getAllCookiesAsString() {
        String retval= null;
        if (cookies!=null) {
            StringBuilder sb= new StringBuilder(200);
            for(Map.Entry<String,String> entry : cookies.entrySet()) {
                if (sb.length()>0) sb.append("; ");
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(entry.getValue());
            }
            retval= sb.toString();
        }
        return retval;
    }

    public void setLocalFileExtensions(List<String> extList) { _localFileExtensions = extList; }

    public void setDesc(String desc) { _desc = desc; }
    public String getDesc() { return _desc; }
}
