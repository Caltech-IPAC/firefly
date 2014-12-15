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


    public AnyUrlParams(URL url) { this(url,null); }

    public AnyUrlParams(URL url, String statusKey) {
        super(statusKey);
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



    public long getCacheLifespanInSec() { return _cacheLifespan; }


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
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
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
