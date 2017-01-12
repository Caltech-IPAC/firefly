/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.download.BaseNetParams;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.CacheKey;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnyUrlParams extends BaseNetParams {
    private static int MAX_LENGTH = 80;
    private URL _url;
    private Map<String,String> cookies= null;
    private String _loginName= null;
    private String _securityCookie= null;
    private long _cacheLifespan = 0;
    private boolean _checkForNewer= false;
    private List<String> _localFileExtensions = null;
    private String _desc = null;
    private static long _maxSizeToDownload= 0L;


    public AnyUrlParams(URL url) { this(url,null,null); }

    public AnyUrlParams(URL url, String statusKey, String plotId) {
        super(statusKey,plotId);
        _url= url;
    }

    public URL getURL() { return _url; }

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

        String baseKey= loginName+ securCookie +"-"+ fileStr.toString();
        baseKey= baseKey.replaceAll("[ :\\[\\]\\/\\\\|\\*\\?<>]", "");
        int originalHashCode = (_url.getHost()+ baseKey).hashCode();
        if (baseKey.length()>MAX_LENGTH) {
            baseKey= baseKey.substring(0,MAX_LENGTH);
        }

        String retval;
        if (_desc!=null) {
            retval = _desc + "-"+originalHashCode;
        } else
            retval = "URL--"+ _url.getHost() + "-"+originalHashCode+baseKey;
        //note: "=","," signs causes problem in download servlet.
        retval = retval.replaceAll("[ :\\[\\]\\/\\\\|\\*\\?<>\\=\\,]","\\-");
        if (_localFileExtensions!=null && !_localFileExtensions.contains(FileUtil.getExtension(retval))) {
            retval = retval + "." + _localFileExtensions.get(0);
        }
        return retval;
    }

    public void setLoginName(String name) { _loginName= name; }
    public void setSecurityCookie(String cookieName) { _securityCookie= cookieName; }
    public void setCacheLifespanInSec(long lifeSpan) { _cacheLifespan= lifeSpan; }
    public void setMaxSizeToDownload(long max) {_maxSizeToDownload= max;}
    public long getMaxSizeToDownload() {return _maxSizeToDownload;}

    public void setCheckForNewer(boolean check) { _checkForNewer= check; }
    public boolean getCheckForNewer() { return _checkForNewer; }

    public boolean isCompressedFileName() {
        return getUniqueString().toLowerCase().endsWith(FileUtil.GZ);
    }

    public CacheKey getUncompressedKey() {
        if (isCompressedFileName()) {
            return new CacheKey() {
                public String getUniqueString() {
                    return  FileUtil.getBase(AnyUrlParams.this.getUniqueString());
                }
            };
        }
        else {
            return this;
        }

    }

    public void addCookie(String key, String value) {
        if (cookies==null) {
            cookies= new HashMap<String, String>(31);
        }
        cookies.put(key,value);
    }

    public String getCookie(String key) {
        return cookies!=null ? cookies.get(key) : null;
    }

    public Map<String,String> getCookies() {
        Map<String,String> retval;
        if (cookies==null) {
            retval= Collections.emptyMap();
        }
        else {
            retval= cookies;
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
