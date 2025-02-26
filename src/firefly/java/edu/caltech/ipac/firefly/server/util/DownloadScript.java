/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.firefly.core.background.ScriptAttributes;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private static final  String URL_COMMENT_START= "<!--";
    private static final  String URL_COMMENT_END=  "-->";
    private static int count = 0;


    static final String scriptHeader = """
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


    public static void composeDownloadScript(File outFile,
                                             String dataSource,
                                             List<URL> urlList,
                                             List<ScriptAttributes> attributes) {
        BufferedWriter writer = null;

        try {
            //1. Prepare output file with BufferedWriter
            writer = new BufferedWriter(new FileWriter(outFile), IpacTableUtil.FILE_IO_BUFFER_SIZE);

            //2. Using url-only, curl, or wget to download files from URLs
            boolean urlsOnly = attributes.contains(URLsOnly);
            boolean useCurl = attributes.contains(ScriptAttributes.Curl);
            boolean useWget = attributes.contains(Wget);
            boolean useDitto = attributes.contains(ScriptAttributes.Ditto);
            boolean useUnzip = attributes.contains(ScriptAttributes.Unzip);
            boolean rmZip = attributes.contains(ScriptAttributes.RemoveZip);
            String filename = null, line;
            Date date = new Date();
            String tokenStart = "*";
            String tokenEnd = "";


            if (!urlsOnly) {
                tokenStart = "echo  \'*";
                tokenEnd = "\'";
                writer.write("#! /bin/sh");
                writer.newLine();
            }

            String token= tokenStart+tokenEnd;

            if (urlsOnly) {
                writer.write(URL_COMMENT_START);
                writer.newLine();
            }
            writer.write(tokenStart+"***********************************************"+ tokenEnd);
            writer.newLine();
            writer.write(token);
            writer.newLine();
            writer.write(tokenStart+"    Date: " + date.toString()+ tokenEnd);
            writer.newLine();
            writer.write(token);
            writer.newLine();
            writer.write(tokenStart+"    Download "+getSource(dataSource)+" data from IRSA"+ tokenEnd);
            writer.newLine();
            writer.write(token);
            writer.newLine();
            writer.write(tokenStart+"***********************************************"+ tokenEnd);
            writer.newLine();
            if (urlsOnly) {
                writer.write(URL_COMMENT_END);
                writer.newLine();
            }
            writer.newLine();

            for (URL url: urlList) {
                if (urlsOnly) {
                    writer.write(url.toString());
                    writer.newLine();
                } else if (useCurl || useWget) {
                    filename = findName(url);

                    if (useCurl || useWget) {

                        line = "echo;echo  '>> downloading " + filename + " ...'";
                        writer.write(line);
                        writer.newLine();

                        if (useCurl) {
                            //on Mac, curl "..." -o name.zip will name the downloaded file as name.zip
                            line = "curl \""+url.toString()+"\" -o "+filename;
                        } else {
                            //on Solaris, wget "..." --output-document=name.zip
                            line = "wget \""+url.toString()+"\" --output-document="+filename;
                        }
                        writer.write(line);
                        writer.newLine();

                        if (useDitto || useUnzip) {

                            if (useDitto) {
                                line = "ditto -kx "+filename + " . ";
                            } else {
                                line = "unzip -qq -d . "+filename;
                            }

                            if (rmZip) {
                                line = "("+line+" && rm -f "+filename+") &";
                            } else {
                                line += " &";
                            }
                            writer.write(line);
                            writer.newLine();
                        }
                    }

                }
            }

            if (!urlsOnly) {
                line = "echo; echo; echo \'*** All downloads and extractions (if requested) completed ***\'";
                writer.write(line);
                writer.newLine();
            }
            writer.flush();
        } catch (Exception e) {
            Logger.warn(e, "failed to create download script",
                        "outFile: " + (outFile!=null ? outFile.getPath() : "null"),
                        "dataSource: " + dataSource,
                        "urlList.size():" + (urlList!=null ? urlList.size() : "null"),
                        "ScriptAttributes: "+ Arrays.toString(attributes.toArray()));

        } finally {
            FileUtil.silentClose(writer);
        }
    }

    private static String getSource(String source) {
        String retval = source;

        if (source.startsWith("HYDRA_")) {
            String[] tokens = source.split("_");
            if (tokens.length>=2) {
                retval = tokens[1];
            }
        }
        return retval;
    }

    private static String findName(URL url) {
        String query = url.getQuery();

        // Extract the last part of the path
        String path = url.getPath();
        if (!isEmpty(path)) {
            String lastSegment = path.substring(path.lastIndexOf('/') + 1);
            //check if the last segment has an extension
            if (lastSegment.contains(".") && !lastSegment.startsWith(".")) {
                return sanitizeFileName(lastSegment); //use the last segment as the file name
            }
        }

        // if no valid extension in the path, look for "return" parameter in the query string
        if (query != null && !query.isEmpty()) {
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length > 1 && keyValue[0].trim().equals("return")) {
                    return sanitizeFileName(keyValue[1].trim());
                }
            }
        }

        return "download_file_" + count++;
    }

    private static String sanitizeFileName(String name) {
        //matches \ / : * etc. and other characters not allowed on most OS for file names
        return name.replaceAll("[\\\\/:*?\"<>|]", "_"); // Replace invalid characters
    }



    public static boolean is(ScriptAttributes match, List<ScriptAttributes> attribs) {
        return attribs.contains(match);
    }

    public static void createScript(File outFile,
                                    String dataDesc,
                                    List<FileInfo> fileInfoList,
                                    ScriptAttributes ...attributes) {
        List<UrlInfo> urlInfos = fileInfoList.stream().map(DownloadScript::resolve).collect(Collectors.toList());
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
                writer.printf(scriptHeader, new Date(), dataDesc, FROM_ORG);
            }

            boolean doUnzip = is(Unzip, attribs);
            boolean makeDirs = is(MakeDirs, attribs);
            Function<UrlInfo, String> cmd = is(Wget, attribs) ? (fi) -> doWget(fi, makeDirs) : is(Curl, attribs) ? (fi) -> doCurl(fi, makeDirs) : (fi) -> doUrlOnly(fi);
            for (UrlInfo urlInfo : urlInfos) {
                writer.println(cmd.apply(urlInfo));
                if (doUnzip && urlInfo.getName().endsWith(".zip")) {
                    String dest = isEmpty(urlInfo.getPath()) ? "." : urlInfo.getPath();
                    writer.println("unzip -qq -d %s %s".formatted(dest, urlInfo.getFilePath()));
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

    static private UrlInfo resolve(FileInfo fi) {

        String href = ifNotNull(fi.getInternalFilename()).get(ServerContext::resolveUrl);
        String extName = fi.getExternalName();
        return new UrlInfo(href, extName);
    }

    static private String doWget(UrlInfo urlInfo, boolean makeDirs) {
        String cmd = "wget";
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
        String cmd = "curl";
        if (isEmpty(urlInfo.getName())) {
            cmd += " -J -O \"%s\"".formatted(urlInfo.href);
        } else {
            cmd += " -o %s \"%s\"".formatted(urlInfo.filePath, urlInfo.href);
        }
        String cdir = !isEmpty(urlInfo.getPath()) && makeDirs ? "mkdir -p %s && cd %s".formatted(urlInfo.getPath(), urlInfo.getPath()) : null;
        return cdir == null ? cmd : cdir + " && " + cmd + " && cd -";
    }

    static private String doUrlOnly(UrlInfo pi) {
        return pi.href;
    }





}

