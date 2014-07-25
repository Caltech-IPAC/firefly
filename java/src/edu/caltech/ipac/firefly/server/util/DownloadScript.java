package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.firefly.core.background.ScriptAttributes;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.FileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Nov 12, 2010
 * Time: 5:29:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class DownloadScript {

    private static final  String URL_COMMENT_START= "<!--";
    private static final  String URL_COMMENT_END=  "-->";

    public static void composeDownloadScript(File outFile,
                                             String dataSource,
                                             List<URL> urlList,
                                             List<ScriptAttributes> attributes) {
        BufferedWriter writer = null;

        try {
            //1. Prepare output file with BufferedWriter
            writer = new BufferedWriter(new FileWriter(outFile), IpacTableUtil.FILE_IO_BUFFER_SIZE);

            //2. Using url-only, curl, or wget to download files from URLs
            boolean urlsOnly = attributes.contains(ScriptAttributes.URLsOnly);
            boolean useCurl = attributes.contains(ScriptAttributes.Curl);
            boolean useWget = attributes.contains(ScriptAttributes.Wget);
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
                    filename = findName(url.getQuery());

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

    private static String findName(String query) {
        String retval = null;
        for (String param: query.split("&")) {
            if (param.startsWith("return")) {
                retval = param.split("=")[1].trim();
                break;
            }
        }
        return retval;
    }
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
