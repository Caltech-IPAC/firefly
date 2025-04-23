/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package edu.caltech.ipac.util;

import edu.caltech.ipac.firefly.server.db.DuckDbAdapter;
import edu.caltech.ipac.firefly.server.db.DuckDbReadable;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static edu.caltech.ipac.table.IpacTableUtil.isIpacTable;
import static edu.caltech.ipac.util.FormatUtil.Format.*;

/**
 * A collection of utilities related to File Format
 *
 * @author loi
 * @version : $
 */
public class FormatUtil {
    private static final int SAMPLE_SIZE = (int) (8 * FileUtil.K);
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    public enum Format {
        TSV("tsv", ".tsv"),
        CSV("csv", ".csv"),
        IPACTABLE("ipac", ".tbl"),
        UNKNOWN("null", null),
        TEXT("text", "txt"),
        FIXEDTARGETS("fixed-targets", ".tbl"),
        FITS("fits",".fits"),
        JSON("json", ".json"),
        PDF("pdf", ".pdf"),
        TAR("tar", ".tar"),
        JAR("jar", ".jar"),
        HTML("html", ".html"),
        VO_TABLE("votable", ".xml"),
        VO_TABLE_TABLEDATA("votable-tabledata", ".vot"),
        VO_TABLE_BINARY("votable-binary-inline", ".vot"),
        VO_TABLE_BINARY2("votable-binary2-inline", ".vot"),
        VO_TABLE_FITS("votable-fits-inline",".vot"),
        REGION("reg", ".reg"),
        PNG("png", ".png"),
        JPEG("jpeg", ".jpg"),
        UWS("uws", ".xml"),
        PARQUET(DuckDbReadable.Parquet.NAME, "."+DuckDbReadable.Parquet.NAME),
        ZIP("zip", ".zip"),
        GZIP("gzip", ".gz");

        public final String type;
        final String fileNameExt;
        Format(String type, String ext) {
            this.type = type;
            this.fileNameExt = ext;
        }
        public String getFileNameExt() {
            return fileNameExt;
        }
        public String toString() {return type;}
    }

    /**
     * Determines the MIME type of the given file.
     *
     * @param inFile input file to detect
     * @return A String representing the MIME type of the file, or "application/x-unknown" otherwise
     */
    public static DuckDbAdapter.MimeDesc getMimeType(File inFile) {
        return DuckDbAdapter.getMimeType(inFile);
    }

    /**
     * Detects the format of the given file
     *
     * @param inFile The full path of the file to detect the format for.
     * @return The detected {@code Format} of the file, or {@code UNKNOWN} if the format could not be determined.
     * @throws IOException If an I/O error occurs while accessing the file.
     */
    @Nonnull
    public static Format detect(File inFile) throws IOException {

        Format format = null;
        DuckDbAdapter.MimeDesc mimeDesc = DuckDbAdapter.getMimeType(inFile);
        String mime = mimeDesc.mime();
        format = mapToFormat(mimeDesc.mime(), mimeDesc.desc());
        LOGGER.trace("detectFormat: " + inFile, "mime-type: " + mime, "description: " + mimeDesc.desc());

        if (format != null) {
            LOGGER.debug("Format: %s resolved via mime-type/magic number".formatted(format));
            return format;
        }

        format = guessBySamplingContent(inFile);
        if (format != null) {
            LOGGER.debug("Format: %s resolved via file sampling".formatted(format));
            return format;
        }

        // all failed; fallback to trial and error
        if ( mime.startsWith("text/") ) {     // including "text/xml"
            // if text file, we'll test it against ipactable, region, csv, and tsv
            if (isIpacTable(inFile)) {
                format = IPACTABLE;
            } else if (isRegionFile(inFile)) {
                format = REGION;
            } else {
                DataGroup csv = DuckDbReadable.getInfoOrNull(CSV, inFile.getAbsolutePath());
                DataGroup tsv = DuckDbReadable.getInfoOrNull(TSV, inFile.getAbsolutePath());
                int csvCols = csv == null ? 0 : csv.getDataDefinitions().length - 1;
                int tsvCols = tsv == null ? 0 : tsv.getDataDefinitions().length - 1;
                if (csvCols + tsvCols > 0) {
                    format =  tsvCols > csvCols ? TSV : CSV;
                }
            }
        }

        if (format == null && mimeDesc.mime().equals("text/html"))  format = HTML;      // allow text/html here if all failed.

        if (format != null) {
            LOGGER.debug("Format: %s resolved via trial and error".formatted(format));
            return format;
        } else {
            LOGGER.debug("Failed to detect file: " + inFile,
                    "mime: " + mime,
                    "desc: " + mimeDesc.desc()
            );
            return UNKNOWN;
        }
    }

    private static boolean isRegionFile(File inFile) {
        try {
            var res = new RegionParser().processFile(inFile);
            return res != null && !res.regionList().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    private static Format mapToFormat(String mime, String desc) {
        Format format = switch (mime) {
            case "image/fits", "application/fits" -> FITS;
            case "application/x-votable+xml", "text/xml-votable"-> VO_TABLE;
            case "application/vnd.apache.parquet", "application/x-parquet" -> PARQUET;
            case "text/csv", "application/csv" -> CSV;
            case "text/tab-separated-values", "application/tsv" -> TSV;
            case "image/png" -> PNG;
            case "image/jpeg", "image/jpg" -> JPEG;
            case "application/pdf" -> PDF;
            case "application/zip", "application/x-zip-compressed" -> ZIP;
            case "application/gzip", "application/x-gzip" -> GZIP;
            case "application/java-archive" -> JAR;
            case "application/x-tar", "application/tar" -> TAR;
            case "application/html" -> HTML;            // some text file with HTML comments will appear as test/xml.  to avoid false positive, we will not accept text/html.
            case "application/json", "text/json" -> JSON;
            default -> null;
        };
        if (format != null) return format;
        // mime type failed, try desc
        if (desc != null) {
            desc = desc.toLowerCase();
            if (desc.contains("parquet"))   return PARQUET;
            if (desc.contains("csv"))       return CSV;
            if (desc.contains("json"))      return JSON;
            if (desc.contains("tab-separated"))   return TSV;
        }
        return null;
    }

    private static Format guessBySamplingContent(File inf) throws IOException {

        // limit the amount for the guess to SAMPLE_SIZE(32k)
        char[] charAry = new char[SAMPLE_SIZE];
        try (
            BufferedReader sampleData = new BufferedReader(new FileReader(inf), SAMPLE_SIZE);
            BufferedReader reader = new BufferedReader(new CharArrayReader(charAry))
        ) {
            sampleData.read(charAry, 0, charAry.length);  // this ensures sample data is no more than SAMPLE_SIZE

            String line = reader.readLine();
            line = line == null ? "" : line.trim();
            if (line.startsWith("SIMPLE  = ")) {
                return FITS;
            } else if (line.startsWith("{")) {      // not reliable
                return JSON;
            }

            do {
                if (line.startsWith("COORD_SYSTEM: ") || line.startsWith("EQUINOX: ") ||
                        line.startsWith("NAME-RESOLVER: ")) {
                    //NOTE: a fixed targets file contains the following lines at the beginning:
                    //COORD_SYSTEM: xxx
                    //EQUINOX: xxx
                    //NAME-RESOLVER: xxx
                    return FIXEDTARGETS;
                } else if (line.startsWith("<VOTABLE") ||
                        (line.contains("<?xml") && line.contains("<VOTABLE "))) {
                    return VO_TABLE;
                } else if (isUwsEl(line)) {
                    return UWS;
                }
                line = reader.readLine();
            } while (line != null);

        } catch (Exception ignored) {}
        return null;
    }

    private static boolean isUwsEl(String line) {
        line = line.trim().toLowerCase();
        boolean isUws = line.contains("www.ivoa.net/xml/uws");
        return isUws && line.matches("<(.+:)?job .*");
    }

}

