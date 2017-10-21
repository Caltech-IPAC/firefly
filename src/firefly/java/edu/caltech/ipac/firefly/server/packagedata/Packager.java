/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.JobAttributes;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.servlets.AnyFileDownload;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * User: roby
 * Date: Sep 24, 2008
 * Time: 12:55:53 PM
 */


/**
 * @author tatianag
 * @version $Id: Packager.java,v 1.43 2012/11/16 23:30:31 tlau Exp $
 */
public class Packager {

    public static final long DEFAULT_DATA_BYTES = AppProperties.getLongProperty("download.data.bytesize", 2097152);
    public final static String DOWNLOAD_SERVLET_PATH = "servlet/Download";
    private final String _packageID;
    private final String _dataSource;
    private final BackgroundInfoCacher backgroundInfoCacher;
    private final List<FileGroup> _fgList;
    private final long _maxBundleBytes;
    private final List<PackagedBundle> bundleList= new ArrayList<PackagedBundle>(20);
    private BackgroundStatus _estimateStat = null;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public Packager(String packageID,
                    List<FileGroup> fgList,
                    String dataSource,
                    BackgroundInfoCacher packageInfo,
                    long maxBundleBytes) {

        Assert.argTst(fgList != null && fgList.size() > 0,
                      "fgList cannot be null and must contain at least 1 element");
        Assert.argTst(packageID != null, "packageID cannot be null");
        Assert.argTst(packageInfo != null, "packageInfo cannot be null");

        _packageID = packageID;
        _dataSource = dataSource;
        backgroundInfoCacher = packageInfo;
        _fgList = fgList;
        _maxBundleBytes = maxBundleBytes;
        computeEstimate();
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


//======================================================================
//----------------------- Method from Runnable -------------------------------
//======================================================================

    private static String getUrl(String packageId, int packageIdx, int numPackages, String baseFileName) {
        try {
            String suggestedName;
            try {
                suggestedName = URLEncoder.encode(baseFileName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                suggestedName = "DownloadPackage";
            }
            if (numPackages > 1) suggestedName += "-part" + (packageIdx + 1);

            suggestedName += ".zip";

            File zipFile = getZipFile(packageId, packageIdx);

            return getUrl(zipFile,suggestedName );

        } catch (RuntimeException e) {
            Logger.debug("id= " + packageId + " baseFileName= " + baseFileName);
            throw e;
        }
    }

    private static String getUrl(File f, String suggestedName) {
        String fileStr = ServerContext.replaceWithPrefix(f);
        try {
            fileStr = URLEncoder.encode(fileStr, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // ignore, just proceed
        }
        return ServerContext.getRequestOwner().getBaseUrl() + DOWNLOAD_SERVLET_PATH + "?" +
                AnyFileDownload.FILE_PARAM + "=" + fileStr + "&" +
                AnyFileDownload.RETURN_PARAM + "=" + suggestedName + "&" +
                AnyFileDownload.LOG_PARAM + "=true&" +
                AnyFileDownload.TRACK_PARAM + "=true";
    }

    public static File getZipFile(String packageId, String packageIdx) {
        File stagingDir = ServerContext.getStageWorkDir();
        return new File(stagingDir, packageId + "_" + packageIdx + ".zip");

    }

    private static File getZipFile(String packageId, int packageIdx) {
        return getZipFile(packageId, (new Integer(packageIdx)).toString());
    }

    public void packageAll() {
        BackgroundStatus bgStat = null;
        if (isOneFile()) {
            bgStat= packageOneFile(_fgList.get(0).getFileInfo(0));
        }
        else {
            for (int idx = 0; idx < _estimateStat.getPackageCount(); idx++) {
                if (!backgroundInfoCacher.isCanceled()) {
                    bgStat = packageElement(idx);
                    if (bgStat.getState() != BackgroundState.SUCCESS)
                        break;
                } else {
                    bgStat =  _estimateStat.cloneWithState(BackgroundState.CANCELED);
                    break;
                }
            }
            if (bgStat == null) {
                bgStat =  _estimateStat.cloneWithState(BackgroundState.FAIL);
            }
        }
        backgroundInfoCacher.setStatus(bgStat);
    }

    public boolean isOneFile() {
        return _fgList.size()==1 && _fgList.get(0).getSize()==1;
    }

    private BackgroundStatus packageOneFile(FileInfo fileInfo) {
        BackgroundStatus retval;
        try {
            PackagedBundle bundle= new PackagedBundle(0,0,1,fileInfo.getSizeInBytes());
            backgroundInfoCacher.setStatus(_estimateStat);


            File stagingDir = ServerContext.getStageWorkDir();
            String filename= fileInfo.getInternalFilename();
            File targetFile; // target file should be created after the external name is set
            //------------
            if (filename.contains("://")) {
                URLConnection uc = URLDownload.makeConnection(new URL(filename), fileInfo.getCookies());
                uc.setRequestProperty("Accept", "text/plain");
                if (fileInfo.hasFileNameResolver()) {
                    String suggestedFilename = URLDownload.getSugestedFileName(uc);
                    if (StringUtils.isEmpty(suggestedFilename)) {
                        String path = uc.getURL().getPath();
                        suggestedFilename = path.substring(path.lastIndexOf("/"));
                    }
                    fileInfo.setExternalName(fileInfo.resolveFileName(suggestedFilename));
                    targetFile= makeSingleTargetFile(stagingDir,fileInfo.getExternalName());
                } else {
                    targetFile = makeSingleTargetFile(stagingDir,fileInfo.getExternalName());
                }
                URLDownload.getDataToFile(uc, targetFile, false);
            }
            else {
                targetFile = makeSingleTargetFile(stagingDir,fileInfo.getExternalName());
                FileUtil.copyFile(new File(filename), targetFile);
            }
            //------------
            String url = getUrl(targetFile,
                    fileInfo.getExternalName().replaceAll("\\+\\-","%2D").replaceAll("\\+","%2B"));
            long fSize= targetFile.length();
            bundle.addProcessedBytes(1, fSize, fSize, fSize);
            bundle.finish(url);
            retval= new BackgroundStatus(_packageID, BackgroundState.SUCCESS, BackgroundStatus.BgType.PACKAGE);
            retval.addAttribute(JobAttributes.Zipped);
            retval.setParam(BackgroundStatus.TOTAL_BYTES, fileInfo.getSizeInBytes()+"");
            retval.addPackageProgress(bundle.makePackageProgress());
        } catch (FailedRequestException e) {
            retval = _estimateStat.cloneWithState(BackgroundState.CANCELED);
            retval.addMessage("download file from URL to staging area: " + e.toString());
        } catch (MalformedURLException e) {
            retval = _estimateStat.cloneWithState(BackgroundState.CANCELED);
            retval.addMessage("download file from URL to staging area: " + e.toString());
        } catch (IOException e) {
            retval = _estimateStat.cloneWithState(BackgroundState.CANCELED);
            retval.addMessage("Could not copy file to staging area: " + e.toString());
        }
        return retval;

    }

    private File makeSingleTargetFile(File dir,String externalName) {
        return new File(dir, _packageID + "_" + externalName.replace(File.separator, "_"));
    }

    public BackgroundStatus packageElement(int packageIdx) {
        _estimateStat.setState(BackgroundState.WORKING);
        backgroundInfoCacher.setStatus(_estimateStat);
        PackagedBundle bundle =  bundleList.get(packageIdx);
        File zipFile = getZipFile(_packageID, packageIdx);
        String url = getUrl(_packageID, packageIdx, _estimateStat.getPackageCount(), backgroundInfoCacher.getBaseFileName());
        ZipHandler zipHandler = new ZipHandler(zipFile, url, _fgList, bundle, backgroundInfoCacher, _maxBundleBytes);
        zipHandler.zip();
        addMessages(zipHandler);
        _estimateStat.setPackageCount(bundleList.size());
        for(int i=0; (i<bundleList.size()); i++) {
            _estimateStat.setPartProgress(bundleList.get(i).makePackageProgress(),i);
        }
        if (bundle.getFollowUpBundle() != null) {
            _estimateStat.addPackageProgress(bundle.getFollowUpBundle().makePackageProgress());
            bundleList.add(bundle.getFollowUpBundle());
        }
        return _estimateStat.cloneWithState(bundle.getState());
    }

    private void addMessages(ZipHandler zipHandler) {
        long numAccessDenied = zipHandler.getNumAccessDenied();
        if (numAccessDenied > 0) {
            _estimateStat.addMessage("Access denied to " + numAccessDenied + " files.");
        }
        long numFailed = zipHandler.getNumFailed();
        if (numFailed > 0) {
            _estimateStat.addMessage("Failed to package " + numFailed + " files.");
        }
        if (numAccessDenied > 0 || numFailed > 0) {
            _estimateStat.addMessage("See " + zipHandler.getReadmeName() + " for details.");
        }
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    //int getTotalToZip() { return _estimateStat.getPartCount(); }
    public BackgroundInfoCacher getBackgroundInfoCacher() {
        return backgroundInfoCacher;
    }

    public BackgroundInfo getPackageInfo(){
        return backgroundInfoCacher.getPackageInfo();
    }

    public BackgroundStatus estimate() {
        return _estimateStat;
    }

    public String getID() {
        return _packageID;
    }

    private void computeEstimate() {
        long totalSize = 0;
        int totalFiles = 0;

        // use dynamically created bundles
        for (FileGroup fg : _fgList) {
            if(fg.getSizeInBytes()==0) {
                long size = 0;
                for (FileInfo f : fg) {
                    size = f.getSizeInBytes();
                    if (size == 0) {
                        size = DEFAULT_DATA_BYTES;
                        f.setSizeInBytes(size);
                    }
                    fg.setSizeInBytes(fg.getSizeInBytes() + size);
                }
            }
            totalSize += fg.getSizeInBytes();
            totalFiles +=fg.getSize();
        }
        PackagedBundle bundle= new PackagedBundle(0, 0, totalFiles, totalSize);
        bundleList.add(bundle);
        _estimateStat = new BackgroundStatus( _packageID, BackgroundState.WAITING, BackgroundStatus.BgType.PACKAGE);
        _estimateStat.addAttribute(JobAttributes.Zipped);
        _estimateStat.addAttribute(JobAttributes.DownloadScript);
        _estimateStat.addPackageProgress(bundle.makePackageProgress());
        _estimateStat.setParam(BackgroundStatus.TOTAL_BYTES, totalSize + "");
        _estimateStat.setDataSource(_dataSource);
        backgroundInfoCacher.setStatus(_estimateStat);
    }

// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

}

