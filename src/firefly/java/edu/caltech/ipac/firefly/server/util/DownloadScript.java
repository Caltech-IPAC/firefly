/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.firefly.core.background.ScriptAttributes;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.util.AppProperties;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.background.ScriptAttributes.*;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Nov 12, 2010
 * Time: 5:29:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class DownloadScript {
    private static final String FROM_ORG = AppProperties.getProperty("mail.from.org", "IRSA");

    static final String SCRIPT_HEADER = """
        #! /bin/sh
        cat <<EOF
        ************************************************
        *
        *    Date: %s
        *
        *    Download %s from %s
        *
        ************************************************
        
        EOF
        """.stripIndent();

    static final String UNZIP_CMD = """
            if command -v unzip &> /dev/null; then
                unzip -qq -d %s %s
            fi
            """.stripIndent();

    public static void createScript(File outFile,
                                    String dataDesc,
                                    List<FileGroup> fileGroups,
                                    ScriptAttributes ...attributes) {
        List<UrlInfo> urlInfos = convertToUrlInfo(fileGroups);
        createScript(outFile, urlInfos, dataDesc, attributes);
    }

    public static void createScript(File outFile,
                                    List<UrlInfo> urlInfos,
                                    String dataDesc,
                                    ScriptAttributes ...attributes) {
        List<ScriptAttributes> attribs = Arrays.asList(attributes);

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outFile), IpacTableUtil.FILE_IO_BUFFER_SIZE))) {

            if (is(URLsOnly, attribs)) {      // most tools do not natively support comments; maybe we should exclude them.
                writer.printf("# Date: %s --- Download %s from %s \n",new Date(), dataDesc, FROM_ORG);
            } else {
                writer.printf(SCRIPT_HEADER, new Date(), dataDesc, FROM_ORG);
            }

            boolean doUnzip = is(Unzip, attribs);
            boolean makeDirs = is(MakeDirs, attribs);
            Function<UrlInfo, String> cmd = is(Wget, attribs) ? (fi) -> doWget(fi, makeDirs) : is(Curl, attribs) ? (fi) -> doCurl(fi, makeDirs) : (fi) -> doUrlOnly(fi);
            for (UrlInfo urlInfo : urlInfos) {
                writer.println(cmd.apply(urlInfo));
                if (doUnzip && urlInfo.getName().endsWith(".zip")) {
                    String dest = isEmpty(urlInfo.getPath()) ? "." : urlInfo.getPath();
                    writer.println(UNZIP_CMD.formatted(dest, urlInfo.getFilePath()));
                }
                writer.println();
            }

        } catch (IOException e) {
            Logger.getLogger().error(e, "failed to create download script",
                    "outFile: " + outFile.getPath(),
                    "dataDesc: " + dataDesc,
                    "files.size():" + (urlInfos!=null ? urlInfos.size() : "null"),
                    "ScriptAttributes: "+ Arrays.toString(attributes));
        }
    }

    static List<UrlInfo> convertToUrlInfo(List<FileGroup> fileGroups) {
        ArrayList<UrlInfo> urlInfos = new ArrayList<>();
        for (FileGroup fg : fileGroups) {
            for (FileInfo fi : fg) {
                urlInfos.add(resolve(fi, fg.getBaseDir()));
            }
        }
        return urlInfos;
    }

    static boolean is(ScriptAttributes match, List<ScriptAttributes> attribs) {
        return attribs.contains(match);
    }

    static UrlInfo resolve(FileInfo fi, File baseDir) {
        String baseDirPath = ifNotNull(baseDir).get(f -> f.getPath().endsWith("/") ? f.getPath() : f.getPath() + "/");
        String href = ifNotNull(fi.getInternalFilename()).get(ServerContext::resolveUrl);
        String extName = ifNotNull(fi.getExternalName()).getOrElse(baseDirPath);
        return new UrlInfo(href, extName);
    }

    static private String doUrlOnly(UrlInfo pi) {
        return pi.href;
    }

//====================================================================
//  Consistent behavior for both curl and wget:
//  - Both curl and wget must follow redirects.
//  - If UrlInfo.filePath is provided:
//      * If it ends with '/', treat it as a directory.
//      * Otherwise, treat it as a file path (including possible subdirectories).
//  - When downloading:
//      * Save the file to the specified path.
//      * If no file name is provided, use the remote file name from the URL
//        (or from the Content-Disposition header if available).
//====================================================================
    static private String doWget(UrlInfo urlInfo, boolean makeDirs) {
        String cmd = "echo  '>> downloading %s ...' \nwget".formatted(urlInfo.getHref());
        if (isEmpty(urlInfo.getName())) {
            cmd += " --content-disposition";
            cmd += isEmpty(urlInfo.getPath()) ? "" : " -P " + urlInfo.getPath();
        } else {
            if (!isEmpty(urlInfo.getPath()) && makeDirs) {
                cmd = "mkdir -p " + urlInfo.getPath() + " && " + cmd;
                cmd += " -O %s".formatted(urlInfo.getFilePath());
            } else {
                cmd += " -O " + urlInfo.getName();
            }
        }
        cmd += " \"%s\"".formatted(urlInfo.getHref());
        return cmd;
    }

    static private String doCurl(UrlInfo urlInfo, boolean makeDirs) {
        String cmd = "echo  '>> downloading %s ...' \ncurl".formatted(urlInfo.getHref());
        if (isEmpty(urlInfo.getName())) {
            cmd += " -LOJ \"%s\"".formatted(urlInfo.getHref());
        } else {
            cmd += " -Lo %s \"%s\"".formatted(urlInfo.getName(), urlInfo.getHref());
        }
        String cdir = !isEmpty(urlInfo.getPath()) && makeDirs ? "mkdir -p %s && cd %s".formatted(urlInfo.getPath(), urlInfo.getPath()) : null;
        return cdir == null ? cmd : cdir + " && " + cmd + " && cd -";
    }

//====================================================================

    public static class UrlInfo implements Serializable {
        private final String href;
        private final boolean isDir;
        private final File filePath;

        public UrlInfo(String href, String filePath) {
            this.href = href;
            this.isDir = String.valueOf(filePath).endsWith("/");
            this.filePath = isEmpty(filePath) ? null : new File(filePath);
        }
        public String getName() {
            return isDir || filePath == null ? "" : filePath.getName();
        }
        public String getPath() {
            return filePath == null ? "" : isDir ? filePath.getName() : filePath.getParent();
        }
        public String getHref() {
            return href;
        }
        public String getFilePath() {
            return isEmpty(getPath()) ? getName() : getPath() + "/" + getName();
        }
    }

}

