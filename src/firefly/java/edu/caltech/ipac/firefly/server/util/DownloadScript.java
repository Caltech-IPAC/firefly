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

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.firefly.core.background.ScriptAttributes.*;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 *
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

//====================================================================
//  Consistent behavior for both curl and wget:
//  - Both curl and wget must follow redirects.
//  - Command exits with non-zero status on failure or error http status code.
//  - If UrlInfo.filePath is provided:
//      * If it ends with '/', treat it as a directory.
//      * Otherwise, treat it as a file path (including possible subdirectories).
//  - When downloading:
//      * Save the file to the specified path.
//      * If no file name is provided, use the remote file name from the URL, or from the Content-Disposition header if available.
//        * curl will overwrite an existing file, whereas wget will create a new file with a numbered suffix to avoid overwriting.
//====================================================================
static final String funcTmpl = """
        download_file() {
          href="$1"; filePath="$2"; fileName="$3"
        
          [ -n "$filePath" ] && { mkdir -p "$filePath" && cd "$filePath" || return 1; }
        
          %s
        
          echo ">> downloading ${href} ..."
          $cmd "$href" || {
            echo ">> ERROR: failed to download ${href}"
            [ -n "$filePath" ] && cd - > /dev/null
            return 1
          }
          [ -n "$filePath" ] && cd - > /dev/null
        }
        """.stripIndent();
    static final String curlFunc = funcTmpl.formatted("""
          cmd="curl -fLJO"
            [ -n "$fileName" ] && cmd="curl -fLo $fileName"
          """);
    static final String wgetFunc = funcTmpl.formatted("""
          cmd="wget --content-disposition"
            [ -n "$fileName" ] && opts="wget -O $fileName"
          """);
//====================================================================



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
                writer.printf("# Date: %s --- Download %s from %s \n", new Date(), dataDesc, FROM_ORG);
                urlInfos.forEach(pi -> writer.println(pi.getHref()));
                return;
            }

            boolean doUnzip = is(Unzip, attribs);

            writer.printf(SCRIPT_HEADER, new Date(), dataDesc, FROM_ORG);
            if (is(Wget, attribs)) {
                writer.println(wgetFunc);
            } else if (is(Curl, attribs)) {
                writer.println(curlFunc);
            }

            for (UrlInfo urlInfo : urlInfos) {
                writer.println("download_file '%s' '%s' '%s'".formatted(urlInfo.getHref(), urlInfo.getPath(), urlInfo.getName()));
                if (doUnzip && urlInfo.getName().endsWith(".zip")) {
                    String dest = isEmpty(urlInfo.getPath()) ? "." : urlInfo.getPath();
                    writer.println(UNZIP_CMD.formatted(dest, urlInfo.getFilePath()));
                }
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
            String fp = filePath == null ? "" : isDir ? filePath.getName() : filePath.getParent();
            return ifNotNull(fp)
                    .then(f -> f.startsWith("/") ? f.substring(1) : f)  // remove absolute path if present
                    .orElse("").get();
        }
        public String getHref() {
            return href;
        }
        public String getFilePath() {
            return isEmpty(getPath()) ? getName() : getPath() + "/" + getName();
        }
    }

}

