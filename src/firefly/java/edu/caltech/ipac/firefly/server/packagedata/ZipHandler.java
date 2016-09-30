/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
 * @author tatianag
 * @version $Id: ZipHandler.java,v 1.48 2012/08/08 18:30:49 roby Exp $
 */
public class ZipHandler {

    private static final Logger.LoggerImpl log = Logger.getLogger();
    private static final long UPDATE_SIZE = FileUtil.MEG * 15;

    private final static int COMPRESSION_LEVEL = AppProperties.getIntProperty("download.compression.level", 1);
    private final static String README_SUCCESS_TEXT = AppProperties.getProperty("download.readme.success", "");

    private List<FileGroup> _fgList;
    private PackagedBundle _bundle;
    private BackgroundInfoCacher _backgroundInfoCacher;
    private ZipOutputStream _zout = null;
    private File _zipFile = null;
    private String _url = null;
    private String _readmeName;
    private long _lastTotal = 0;

    private List<FileInfo> _failed;
    private List<FileInfo> _accessDenied;
    private File _baseDir;
    private int _filesPackaged;

    private long _maxBundleBytes;

    /**
     * @param zipFile        the zip file
     * @param url            url string
     * @param fgList         all the files to zip
     * @param bundle         the bundle this zip represents
     * @param backgroundInfoCacher    communication object
     * @param maxBundleBytes maximum uncompressed bytes that should be packaged into one bundle
     * @throws IllegalArgumentException thrown if an access to _packageInfo fails, the packageInfo actually throws the
     *                                  exception
     */
    ZipHandler(File zipFile, String url,
               List<FileGroup> fgList,
               PackagedBundle bundle,
               BackgroundInfoCacher backgroundInfoCacher,
               long maxBundleBytes)
            throws IllegalArgumentException {

        _fgList = fgList;
        _bundle = bundle;
        _backgroundInfoCacher = backgroundInfoCacher;
        _zipFile = zipFile;
        _url = url;
        _failed = null;
        _filesPackaged = 0;
        _accessDenied = null;

        Assert.argTst(_backgroundInfoCacher != null, "Package Info cannot be null");
        Assert.argTst(_bundle != null, "Bundle cannot be null");
        _maxBundleBytes = maxBundleBytes;

        _readmeName = "README";
        int numBundles;
        try {
            numBundles = _backgroundInfoCacher.getStatus().getPackageCount();
        } catch (Exception e) {
            numBundles = 2;
            log.warn(e, "Unable to get number of parts, assuming 2");
        }

        if (numBundles > 1) {
            _readmeName += "-part" + (_bundle.getPackageIdx() + 1);
        }
        _readmeName += ".txt";


    }

    /**
     * @param filename (filename can be url)
     * @param fi       file info pbject
     * @return input stream handle
     * @throws IOException on error
     */
    private static InputStream getInputStream(String filename, FileInfo fi, File baseDir) throws IOException {
        InputStream is;
        if (filename.contains("://")) {
            try {
                URL url = new URL(filename);
                URLConnection uc = URLDownload.makeConnection(url, fi.getCookies());
                uc.setRequestProperty("Accept", "text/plain");

                if (fi.hasFileNameResolver()) {
                    String suggestedFilename = URLDownload.getSugestedFileName(uc);
                    fi.setExternalName(fi.resolveFileName(suggestedFilename));
                }

                is = uc.getInputStream();

            } catch (MalformedURLException e) {
                log.error(e);

                File fileToRead;
                if (baseDir == null) {
                    fileToRead = new File(filename);
                } else {
                    fileToRead = new File(baseDir, filename);
                }

                is = new FileInputStream(fileToRead);
            }

        } else {
            File fileToRead;
            if (baseDir == null) {
                fileToRead = new File(filename);
            } else {
                fileToRead = new File(baseDir, filename);
            }

            is = new FileInputStream(fileToRead);
        }

        return is;
    }

    public void zip() {
        if (_backgroundInfoCacher.isCanceled()) {
            _bundle.cancel();
            cleanup();
            return;
        }
        int fileIdx = 0;
        int firstFileIdx = _bundle.getFirstFileIdx();
        int lastFileIdx = firstFileIdx + _bundle.getNumFiles();

        try {
            //if (_bundle.isDynamicBundle()) {
            boolean startNewBundle = false;
            for (FileGroup fg : _fgList) {
                long fgSizeInBytes = fg.getSizeInBytes();
                _baseDir = fg.getBaseDir();
                // check if new bundle should be started in case all files in the group should be packaged together
                // and estimated file group size is known
                // packageTogether will not be observed if group size is unknown (0)
                startNewBundle = startNewBundle ||
                        (fg.isPackageTogether() && _bundle.getUncompressedBytes() > 0 && _bundle.getUncompressedBytes() + fgSizeInBytes > _maxBundleBytes);
                if (startNewBundle) {
                    break;
                }
                for (FileInfo fi : fg) {
                    if (fileIdx >= firstFileIdx && fileIdx < lastFileIdx) {
                        if (_backgroundInfoCacher.isCanceled()) {
                            _bundle.cancel();
                            cleanup();
                            return;
                        }
                        if (fi.hasAccess()) {
                            // check if new bundle should be started in case estimated file size is known
                            startNewBundle = startNewBundle || (_bundle.getUncompressedBytes() > 0 && _bundle.getUncompressedBytes() + fi.getSizeInBytes() > _maxBundleBytes);
                            if (startNewBundle) {
                                break;
                            } else {
                                addDataZipEntryFrom(fi);
                                // check if new bundle should be started in case estimated file size is unknown
                                startNewBundle = _bundle.getUncompressedBytes() > 0 && _bundle.getUncompressedBytes() > _maxBundleBytes;
                            }
                        } else {
                            if (_accessDenied == null) _accessDenied = new ArrayList<FileInfo>();
                            _accessDenied.add(fi);
                            updatebundle(1, fi.getSizeInBytes(), 0, 0);
                        }
                    }
                    fileIdx++;
                }
            }

            addReadmeZipEntry();
            finishZip();
            _bundle.finish(_url);
        } catch (Throwable e) {
            _bundle.fail();
            log.warn(e, "Failed packaging bundle " + _bundle.getPackageIdx() + " after packaging " + _bundle.getUncompressedBytes() + " bytes",
                    e.getMessage());
            cleanup();
        }
    }

    public long getNumFailed() {
        if (_failed == null) return 0;
        else {
            return _failed.size();
        }
    }

    public long getNumAccessDenied() {
        if (_accessDenied == null) return 0;
        else {
            return _accessDenied.size();
        }
    }

    public String getReadmeName() {
        return _readmeName;
    }

    private void addDataZipEntryFrom(FileInfo fi)
            throws Exception {
        // construct and add zip entry
        ZipEntry zipEntry = null;
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            is = getInputStream(fi.getInternalFilename(), fi, _baseDir);
            String zipEntryComment = "(" + fi.getSizeInBytes() + "b) ";
            String filename = fi.getExternalName();

            int inBufSize = 4096;
            if (filename != null && FileUtil.isExtension(filename, FileUtil.GZ)) {
                InputStream decompressedIs = null;
                try {// try to uncompress the data
                    decompressedIs = new GZIPInputStream(is);  // for uncompressed data, throw an exception
                    bis = new BufferedInputStream(decompressedIs);
                } catch (Exception e) {
                    FileUtil.silentClose(decompressedIs);
                    bis = new BufferedInputStream(is, inBufSize);
                }
            } else {
                bis = new BufferedInputStream(is, inBufSize);
            }
            // remove .gz, if exists - filename or url stream are all going to be uncompressed at this point
            if (FileUtil.isExtension(filename, FileUtil.GZ)) {
                filename = filename.substring(0, filename.length() - 3);
            }

            zipEntry = startZipEntry(filename, zipEntryComment);

            int read;
            byte[] buffer = new byte[inBufSize];
            while ((read = bis.read(buffer)) != -1) {
                _zout.write(buffer, 0, read);
            }

            _filesPackaged++;

        } catch (ZipException ze) {
            String zipError = ze.getMessage();

            // ignore duplicate entry errors
            if (zipError.startsWith("duplicate entry:")) {
                Logger.warn("Packaging warning: " + zipError);

            } else {
                Logger.error("Failed packaging " + fi.getExternalName() + " - " + zipError);
                if (_failed == null) _failed = new ArrayList<FileInfo>();
                _failed.add(fi);
                if (zipEntry != null) // if zip
                    throw ze;
            }

        } catch (Exception e) {
            Logger.error("Failed packaging " + fi.getExternalName() + " - " + e.getMessage());
            if (_failed == null) _failed = new ArrayList<FileInfo>();
            _failed.add(fi);
            if (zipEntry != null) // if zip
                throw e;

        } finally {
            long szCompressed = 0;
            long szUncompressed = 0;

            FileUtil.silentClose(bis);

            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    log.warn("Can not close input stream: " + e.getMessage());
                }
            }
            if (zipEntry != null) {
                try {
                    _zout.closeEntry();
                } catch (Exception e) {
                    log.warn("can not close zip entry: " + e.getMessage());
                }
                // record total compressed size of the data
                szCompressed = zipEntry.getCompressedSize();
                szUncompressed = zipEntry.getSize();
                if (szCompressed < 0) szCompressed = 0;
                if (szUncompressed < 0) szUncompressed = 0;
            }
            updatebundle(1, fi.getSizeInBytes(), szUncompressed, szCompressed);
        }
    }

    private void updatebundle(int sizeInFiles, long sizeInByte, long szUncompressed, long szCompressed) {
        _bundle.addProcessedBytes(sizeInFiles, sizeInByte, szUncompressed, szCompressed);
        long total = _bundle.getProcessedBytes();
        if (total > _lastTotal + UPDATE_SIZE) {
            _lastTotal = total;
            BackgroundStatus bgStat=_backgroundInfoCacher.getStatus();
            bgStat.setPartProgress(_bundle.makePackageProgress(),_bundle.getPackageIdx());
            bgStat.setState(_bundle.getState());
            _backgroundInfoCacher.setStatus(bgStat); // should force cache to update
        }

    }

    private void addReadmeZipEntry()
            throws IOException {

        startZipEntry(_readmeName, "Info about package");
        PrintWriter pw = new PrintWriter(_zout);

        try {
            pw.println("\nSuccessfully packaged " + _filesPackaged + " files: " +
                    NumberFormat.getNumberInstance().format(_bundle.getUncompressedBytes()) + " B\n");
            pw.println(README_SUCCESS_TEXT);
            if (_accessDenied != null && _accessDenied.size() > 0) {
                pw.println("\nAccess was denied to " + _accessDenied.size() + " proprietary files: \n");
                for (FileInfo fi : _accessDenied) {
                    pw.println(fi.getExternalName());
                }
            }
            if (_failed != null && _failed.size() > 0) {
                pw.println("\nErrors were encountered when packaging " + _failed.size() +
                        ((_failed.size() > 1) ? " files" : " file") + ": \n");
                for (FileInfo fi : _failed) {
                    pw.println(fi.getExternalName());
                }
            }
            pw.flush();
        } finally {
            try {
                _zout.closeEntry();
            } catch (Exception e) {
                log.warn("can not close readme zip entry: " + e.getMessage());
            }
        }
    }


    private void startZip() throws FileNotFoundException {
        // construct ZipOutputStream object
        _zout = new ZipOutputStream(new FileOutputStream(_zipFile));
        // set the comment for zip file
        _zout.setComment(_bundle.getDesc());
        // set the default compression level and method
        _zout.setMethod(ZipOutputStream.DEFLATED);
        _zout.setLevel(COMPRESSION_LEVEL);
    }

    private ZipEntry startZipEntry(String entryName, String entryComment)
            throws IOException {
        // construct ZipEntry
        ZipEntry zipEntry = new ZipEntry(entryName);
        zipEntry.setComment(entryComment);
        // Zip file should have at least one entry.
        // To avoid errors closing zip with no entries,
        // make sure that at least one entry exists
        // before opening zip file
        if (_zout == null) startZip();
        // put zip entry in the zip archive
        _zout.putNextEntry(zipEntry);
        return zipEntry;
    }

    private void finishZip() {
        if (_zout != null) {
            log.info("FINISHING ZIP: " + _zipFile.getAbsolutePath(),
                    "returning url:" + _url);
            // Will write central directory and the end of central
            // directory to the zip file. If this step fails,
            // zip file might not be possible to unzip.
            try {
                _zout.close();
            } catch (IOException ioe) {
                log.warn(ioe, "Failed to close zip file: " + ioe.getMessage());
            } finally {
                _zout = null;
            }
        }
    }

    private void cleanup() {
        try {
            finishZip();
            if (_zipFile != null && _zipFile.exists()) {
                log.info("DELETING " + _zipFile.getAbsolutePath());
                _zipFile.delete();
            }
        } catch (Throwable th) {
            log.warn(th, "Exception in ZipHandler.cleanup: " + th.getMessage());
        }
    }

}
