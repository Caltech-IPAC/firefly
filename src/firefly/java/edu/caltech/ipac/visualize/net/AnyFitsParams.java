/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.download.BaseNetParams;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.FileUtil;

import java.net.URL;

public class AnyFitsParams extends BaseNetParams {

    public enum Compression { NONE, GZ, GUESS}

    private final URL _url;
    private final Compression _compression;
    private final boolean _allowTables;

    public AnyFitsParams(URL url) {
       this(url,Compression.GUESS,false);
    }

    public AnyFitsParams(URL url,
                         Compression compression) {
        this(url,compression,false);
    }


    public AnyFitsParams(URL url, boolean allowTables) {
        this(url,Compression.GUESS,allowTables);
    }

    public AnyFitsParams(URL url,
                         Compression compression,
                         boolean allowTables) {
        _url= url;
        _allowTables= allowTables;
        _compression= compression;
    }

    public URL getURL() { return _url; }
    public boolean getAllowTables() { return _allowTables; }
    public Compression getCompression() { return _compression; }


    public String getUniqueString() {
       //String fileStr= _url.getFile(); // query field of URL can be very long, so _url.getQuery().hashCode() can shorten the fileStr.
        int urlHashCode= _url.getFile().hashCode();
//        String fileStr = _url.getPath()+"?"+(_url.getQuery()==null?"":Integer.toString(_url.getQuery().hashCode()));
        String fileStr = _url.getPath();
                         //we can determine the file type by getting extension from _url.getPath().
        String ext= FileUtil.getExtension(_url.getPath()==null?fileStr:_url.getPath());
        if (ext==null || ext.length()>10) ext= FileUtil.getExtension(_url.getFile());
        if (ext==null || ext.length()>10) ext= "";

        String queryHashCodeStr= _url.getQuery()==null?"":Integer.toString(_url.getQuery().hashCode());
        if (ComparisonUtil.doCompare(ext,FileUtil.getExtension(_url.getPath()))==0) {
            fileStr = FileUtil.getBase(_url.getPath())+"--"+ queryHashCodeStr;
        } else
            fileStr = _url.getPath()+"--"+ queryHashCodeStr;
        fileStr= fileStr.replace('&','-');
        fileStr= fileStr+"."+ext;

        switch (_compression) {
            case GUESS :
                if ( !ext.equalsIgnoreCase(FileUtil.FIT) &&
                     !ext.equalsIgnoreCase(FileUtil.FITS) &&
                     !ext.equalsIgnoreCase(FileUtil.GZ)) {
                    fileStr= fileStr+ "." + FileUtil.FITS;
                    ext= FileUtil.FITS;
                }
                break;
            case GZ :
                fileStr= fileStr + "." + FileUtil.GZ;
                ext= FileUtil.GZ;
                break;
            case NONE :
                fileStr= fileStr + "." + FileUtil.FITS;
                ext= FileUtil.FITS;
                break;
        }

       // if fileStr length over 150, shorten it again with hashCode() again.
        if (fileStr.length()>150) {
            fileStr= fileStr.substring(0, 130)+urlHashCode + "."+ext;
        }
        String retval=  "AF-" + getShortName(_url.getHost()) +"-"+fileStr;

//       String retval=  "AF-" + _url.getHost() +"-"+
//               (fileStr.length()>150?fileStr.substring(0, 150)+fileStr.hashCode():fileStr);
       return retval.replaceAll("[ :\\[\\]\\/\\\\|\\*\\?<>]", "");
    }

    private static String getShortName(String hostName) {
        if (hostName==null) return "";
        String parts[]= hostName.split("\\.");
        String retval= "";
        if (parts.length>0) {
            retval= (parts.length==1) ? parts[0]  : parts[0]+"."+parts[1];
        }
        return retval;
    }

}
