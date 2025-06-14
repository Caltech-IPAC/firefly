/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.BaseNetParams;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnyUrlParams extends BaseNetParams {
    public static List<String> DEF_EXT_LIST=
            Arrays.asList(
                    "ul", FileUtil.FITS, FileUtil.GZ, FileUtil.TAR, FileUtil.PDF, FileUtil.GZ, FileUtil.REG,
                    "votable", FileUtil.VOT, FileUtil.XML, FileUtil.TBL, FileUtil.CSV, FileUtil.TSV, FileUtil.TXT,
                    FileUtil.jpeg, FileUtil.jpg, FileUtil.png, FileUtil.bmp, FileUtil.gif, FileUtil.tiff, FileUtil.tif);

    private final static int MAX_LENGTH = 30;
    private final URL _url;
    private final Map<String,String> cookies= new HashMap<>();
    private String _loginName= null;
    private String _securityCookie= null;
    private boolean _checkForNewer= false;
    private final List<String> _localFileExtensions = DEF_EXT_LIST;
    private String _desc = null;
    private File _dir = null; // if null, the use the default dir
    private long _maxSizeToDownload= 0L;
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

        if (cookies.containsKey(_securityCookie))  {
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
        String ext= FileUtil.getExtension(fileStr);
        var fileExt= _localFileExtensions.contains(ext) ? ext : _localFileExtensions.getFirst();
        return retval + "." + fileExt;
    }

    public void setLoginName(String name) { _loginName= name; }
    public void setSecurityCookie(String cookieName) { _securityCookie= cookieName; }
    public void setMaxSizeToDownload(long max) {_maxSizeToDownload= max;}
    public long getMaxSizeToDownload() {return _maxSizeToDownload;}
    public void setFileDir(File dir) {_dir= dir;}
    public File getFileDir() {return _dir;}

    public void setCheckForNewer(boolean check) { _checkForNewer= check; }
    public boolean getCheckForNewer() { return _checkForNewer; }

    public void addCookie(String key, String value) { cookies.put(key,value); }


    public Map<String, String> getHeaders() {
        return addtlInfo == null ? null : addtlInfo.getHeaders();
    }

    public Map<String,String> getCookies() {
        Map<String,String> retval = new HashMap<>(cookies);
        if (addtlInfo != null && addtlInfo.getCookies() != null) retval.putAll(addtlInfo.getCookies());
        return retval;
    }

    public void setDesc(String desc) { _desc = desc; }
    public String getDesc() { return _desc; }
}
