package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.data.packagedata.PackagedBundle;
import edu.caltech.ipac.firefly.data.packagedata.PackagedReport;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.servlets.AnyFileDownload;
import edu.caltech.ipac.firefly.server.sse.EventData;
import edu.caltech.ipac.firefly.server.sse.ServerEventManager;
import edu.caltech.ipac.firefly.server.sse.ServerSentEvent;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.util.event.ServerSentEventNames;
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
import java.util.HashSet;
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

    private PackagedReport _estimateReport = null;
    private final String _packageID;
    private final String _dataSource;
    private final PackageInfoCacher packageInfoCacher;
    private final List<FileGroup> _fgList;
    private final long _maxBundleBytes;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

//    public Packager(String packageID,
//                    FileGroup fileGroup,
//                    PackageInfo packageInfo,
//                    long maxBundleBytes) {
//        this(packageID, Arrays.asList(fileGroup), packageInfo, maxBundleBytes);
//    }


    public Packager(String packageID,
                    List<FileGroup> fgList,
                    String dataSource,
                    PackageInfoCacher packageInfo,
                    long maxBundleBytes) {

        Assert.argTst(fgList != null && fgList.size() > 0,
                      "fgList cannot be null and must contain at least 1 element");
        Assert.argTst(packageID != null, "packageID cannot be null");
        Assert.argTst(packageInfo != null, "packageInfo cannot be null");

        _packageID = packageID;
        _dataSource = dataSource;
        packageInfoCacher = packageInfo;
        _fgList = fgList;
        _maxBundleBytes = maxBundleBytes;
        resolveUrlData();
        computeEstimate();
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


//======================================================================
//----------------------- Method from Runnable -------------------------------
//======================================================================


    public void packageAll() {
        PackagedReport report = null;
        if (isOneFile()) {
            report= packageOneFile(_fgList.get(0).getFileInfo(0));
        }
        else {
            for (int idx = 0; idx < _estimateReport.getPartCount(); idx++) {
                if (!packageInfoCacher.isCanceled()) {
                    report = packageElement(idx);
                    if (report.getState() != BackgroundState.SUCCESS)
                        break;
                } else {
                    report = (PackagedReport) _estimateReport.cloneWithState(BackgroundState.CANCELED);
                    report.addMessage("Packaging canceled");
                    break;
                }
            }
            if (report == null) {
                // no bundles
                report = (PackagedReport) _estimateReport.cloneWithState(BackgroundState.FAIL);
            }
        }
        try {
            packageInfoCacher.setReport(report);

            ServerSentEvent ev= new ServerSentEvent(ServerSentEventNames.SVR_BACKGROUND_REPORT,
                                                    packageInfoCacher.getEventTarget(),
                                                    new EventData(report));
            ServerEventManager.fireEvent(ev);
        } catch (IllegalPackageStateException e) {
            Logger.warn("could not set report, this should never happen");
        }
    }

    public boolean isOneFile() {
        return _fgList.size()==1 && _fgList.get(0).getSize()==1;
    }


    private PackagedReport packageOneFile(FileInfo fileInfo) {
        PackagedReport retval;
        try {
            PackagedBundle bundle= new PackagedBundle(0,0,1,fileInfo.getSizeInBytes());
            packageInfoCacher.setReport(_estimateReport);


            File stagingDir = ServerContext.getStageWorkDir();
            String filename= fileInfo.getInternalFilename();
//            File targetFile= new File(stagingDir, _packageID + "_" + fileInfo.getExternalName());
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
            bundle.addProcessedBytes(1, fSize,fSize,fSize);
            bundle.finish(url);
            retval= new PackagedReport(_packageID, new PackagedBundle[] {bundle},
                                                      fileInfo.getSizeInBytes(),
                                                      BackgroundState.SUCCESS);
        } catch (FailedRequestException e) {
            retval = (PackagedReport) _estimateReport.cloneWithState(BackgroundState.CANCELED);
            retval.addMessage("download file from URL to staging area: " + e.toString());
        } catch (MalformedURLException e) {
            retval = (PackagedReport) _estimateReport.cloneWithState(BackgroundState.CANCELED);
            retval.addMessage("download file from URL to staging area: " + e.toString());
        } catch (IllegalPackageStateException e) {
            retval = (PackagedReport) _estimateReport.cloneWithState(BackgroundState.CANCELED);
        } catch (IOException e) {
            retval = (PackagedReport) _estimateReport.cloneWithState(BackgroundState.CANCELED);
            retval.addMessage("Could not copy file to staging area: " + e.toString());
        }
        return retval;

    }

    private File makeSingleTargetFile(File dir,String externalName) {
        return new File(dir, _packageID + "_" + externalName.replace(File.separator, "_"));
    }

    public PackagedReport packageElement(int packageIdx) {
        PackagedReport retval;
        try {
            PackagedBundle bundle = (PackagedBundle) _estimateReport.get(packageIdx);
            File zipFile = getZipFile(_packageID, packageIdx);
            String url = getUrl(_packageID, packageIdx, _estimateReport.getPartCount(), packageInfoCacher.getBaseFileName());
            ZipHandler zipHandler = new ZipHandler(zipFile, url, _fgList, bundle, packageInfoCacher, _maxBundleBytes);
            zipHandler.zip();
            long numAccessDenied = zipHandler.getNumAccessDenied();
            if (numAccessDenied > 0) {
                _estimateReport.addMessage("Access denied to " + numAccessDenied + " files.");
            }
            long numFailed = zipHandler.getNumFailed();
            if (numFailed > 0) {
                _estimateReport.addMessage("Failed to package " + numFailed + " files.");
            }
            if (numAccessDenied > 0 || numFailed > 0) {
                _estimateReport.addMessage("See " + zipHandler.getReadmeName() + " for details.");
            }
            if (bundle.getFollowUpBundle() != null) {
                _estimateReport.addBackgroundPart(bundle.getFollowUpBundle());
            }
            packageInfoCacher.setReport(_estimateReport);
            retval = (PackagedReport) _estimateReport.cloneWithState(bundle.getState());
        } catch (IllegalPackageStateException e) {
            retval = (PackagedReport) _estimateReport.cloneWithState(BackgroundState.CANCELED);
            retval.addMessage("Packaging appears to be canceled");
        }
        return retval;
    }

    //int getTotalToZip() { return _estimateReport.getPartCount(); }
    public PackageInfoCacher getPackageInfoCacher() {
        return packageInfoCacher;
    }

    public PackageInfo getPackageInfo(){
        return packageInfoCacher.getPackageInfo();
    }

    public PackagedReport estimate() {
        return _estimateReport;
    }

    public String getID() {
        return _packageID;
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void resolveUrlData() {

        for (FileGroup fg : _fgList) {
            for (FileInfo f : fg) {
                String urlStr = f.getInternalFilename();
                if (urlStr.contains("://")) {
                    long size = f.getSizeInBytes();
                    if (size == 0) {
                        size = DEFAULT_DATA_BYTES;
                        f.setSizeInBytes(size);
                        fg.setSizeInBytes(fg.getSizeInBytes() + size);
                    }
                }
            }
        }
    }

    private void computeEstimate() {
        List<PackagedBundle> bundles = new ArrayList<PackagedBundle>();
        HashSet<String> msg = null;

        long totalSize = 0;
        int totalFiles = 0;

        // use dynamically created bundles
        for (FileGroup fg : _fgList) {
            totalSize += fg.getSizeInBytes();
            for (FileInfo f : fg) {
                totalFiles++;
            }
        }
        bundles.add(new PackagedBundle(0, 0, totalFiles, totalSize));

        _estimateReport = new PackagedReport(
                _packageID,
                bundles.toArray(new PackagedBundle[bundles.size()]),
                totalSize,
                BackgroundState.WAITING);
        _estimateReport.setDataSource(_dataSource);

        if (msg != null) {
            for (String m : msg) _estimateReport.addMessage(m);
        }
        try {
            packageInfoCacher.setReport(_estimateReport);
        } catch (IllegalPackageStateException e) {
            Logger.warn("could not set report, this should never happen");
        }
    }

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
        String fileStr = VisContext.replaceWithPrefix(f);
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

// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

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
