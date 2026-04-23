/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package edu.caltech.ipac.util;

import edu.caltech.ipac.firefly.server.db.DuckDbReadable;
import edu.caltech.ipac.firefly.server.util.Logger;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Set;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
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

    public record MimeDesc(String mime, String desc) {}

    public enum Format {
        TSV          ("tsv",                    ".tsv",  "text/tab-separated-values"),
        CSV          ("csv",                    ".csv",  "text/csv"),
        IPACTABLE    ("ipac",                   ".tbl",  "application/ipac-table"),
        UNKNOWN      ("null",                   null,    "application/x-unknown"),
        TEXT         ("text",                   ".txt",  "text/plain"),
        FIXEDTARGETS ("fixed-targets",          ".tbl",  "application/fixed-targets"),
        FITS         ("fits",                   ".fits", "image/fits"),
        ASDF         ("asdf",                   ".asdf", "application/asdf"),
        JSON         ("json",                   ".json", "application/json"),
        PDF          ("pdf",                    ".pdf",  "application/pdf"),
        TAR          ("tar",                    ".tar",  "application/x-tar"),
        JAR          ("jar",                    ".jar",  "application/java-archive"),
        HTML         ("html",                   ".html", "text/html"),
        VO_TABLE     ("votable",                ".xml",  "application/x-votable+xml"),
        VO_TABLE_TABLEDATA  ("votable-tabledata",        ".vot",  "application/x-votable+xml"),
        VO_TABLE_BINARY     ("votable-binary-inline",    ".vot",  "application/x-votable+xml"),
        VO_TABLE_BINARY2    ("votable-binary2-inline",   ".vot",  "application/x-votable+xml"),
        VO_TABLE_FITS       ("votable-fits-inline",      ".vot",  "application/x-votable+xml"),
        REGION       ("reg",                    ".reg",  "application/region-file"),
        PNG          ("png",                    ".png",  "image/png"),
        JPEG         ("jpeg",                   ".jpg",  "image/jpeg"),
        UWS          ("uws",                    ".xml",  "application/xml+uws"),
        PARQUET      (DuckDbReadable.Parquet.NAME, "."+DuckDbReadable.Parquet.NAME, "application/vnd.apache.parquet"),
        ZIP          ("zip",                    ".zip",  "application/zip"),
        GZIP         ("gzip",                   ".gz",   "application/gzip"),
        BZIP2        ("bzip2",                  ".bz2",  "application/x-bzip2"),
        OCTET_STREAM ("octet-stream",           null,    "application/octet-stream");

        public final String type;
        private final String fileNameExt;
        private final String mime;

        Format(String type, String ext, String mime) {
            this.type = type;
            this.fileNameExt = ext;
            this.mime = mime;
        }

        public String fileExt() { return fileNameExt; }
        public String mime() { return mime; }
        public String toString() { return type; }

        private static final Set<Format> LOADABLE = EnumSet.of(TSV, CSV, IPACTABLE, FITS, ASDF, JSON, VO_TABLE, VO_TABLE_TABLEDATA, VO_TABLE_BINARY, VO_TABLE_BINARY2, VO_TABLE_FITS, REGION, PARQUET);
        public static boolean isLoadable(Format f) { return LOADABLE.contains(f); }

        /** Returns the primary Format for a given mime type string, or UNKNOWN. */
        public static Format fromMime(String mime) {
            if (mime == null) return UNKNOWN;
            for (Format f : values()) {
                if (f.mime.equalsIgnoreCase(mime)) return f;
            }
            // common aliases
            return switch (mime.toLowerCase()) {
                case "application/fits"             -> FITS;
                case "text/xml-votable"             -> VO_TABLE;
                case "application/x-parquet"        -> PARQUET;
                case "application/csv"              -> CSV;
                case "application/tsv"              -> TSV;
                case "image/jpg"                    -> JPEG;
                case "application/x-zip-compressed" -> ZIP;
                case "application/x-gzip"           -> GZIP;
                case "application/tar"              -> TAR;
                case "application/html"             -> HTML;
                case "text/json"                    -> JSON;
                default                             -> UNKNOWN;
            };
        }
    }

    /**
     * Detects the MIME type of file using magic-byte inspection of the file header,
     * falling back to OS-level and extension-based guessing for unrecognized types.
     * This is platform-independent for all binary formats handled here; text-based
     * formats (FITS text headers, VOTable, IPAC table, etc.) are handled downstream
     * by {@link FormatUtil#detect}.
     */
    @Nonnull
    public static MimeDesc getMimeType(String inFile) {
        try {
            // Read enough bytes to cover all magic signatures (TAR magic is at offset 257)
            byte[] hdr = new byte[264];
            try (var fis = new java.io.FileInputStream(inFile)) {
                fis.read(hdr);
            }

            // Parquet: PAR1 at offset 0
            if (magic(hdr, 0, 'P','A','R','1'))
                return new MimeDesc(PARQUET.mime(), "Parquet data file");
            // PNG: \x89PNG\r\n\x1a\n
            if (magic(hdr, 0, 0x89,'P','N','G','\r','\n',0x1a,'\n'))
                return new MimeDesc(PNG.mime(), "PNG image");
            // JPEG: FF D8 FF
            if (magic(hdr, 0, 0xFF,0xD8,0xFF))
                return new MimeDesc(JPEG.mime(), "JPEG image");
            // PDF: %PDF
            if (magic(hdr, 0, '%','P','D','F'))
                return new MimeDesc(PDF.mime(), "PDF document");
            // GZIP: 1F 8B
            if (magic(hdr, 0, 0x1F,0x8B))
                return new MimeDesc(GZIP.mime(), "gzip compressed data");
            // BZIP2: BZh
            if (magic(hdr, 0, 'B','Z','h'))
                return new MimeDesc(BZIP2.mime(), "bzip2 compressed data");
            // ZIP / JAR: PK\x03\x04
            if (magic(hdr, 0, 'P','K',0x03,0x04)) {
                String name = inFile.toLowerCase();
                if (name.endsWith(".jar")) return new MimeDesc(JAR.mime(), "Java archive");
                return new MimeDesc(ZIP.mime(), "Zip archive");
            }
            // TAR (ustar): "ustar" at offset 257
            if (magic(hdr, 257, 'u','s','t','a','r'))
                return new MimeDesc(TAR.mime(), "POSIX tar archive");
            // Binary FITS: header block starts with "SIMPLE  ="
            if (magic(hdr, 0, 'S','I','M','P','L','E',' ',' ','='))
                return new MimeDesc(FITS.mime(), "FITS image data");

        } catch (Exception ex) {
            Logger.getLogger().error(ex, "Failed to read header for mime detection: " + inFile);
        }

        // Fall back to OS content probing and extension-based guess for text formats
        try {
            String mime = java.nio.file.Files.probeContentType(java.nio.file.Path.of(inFile));
            if (mime == null) mime = java.net.URLConnection.guessContentTypeFromName(inFile);
            if (mime != null) return new MimeDesc(mime, mime);
        } catch (Exception ex) {
            Logger.getLogger().error(ex, "Failed to detect mime type for: " + inFile);
        }
        return new MimeDesc(UNKNOWN.mime(), "unknown");
    }

    /**
     * Determines the MIME type of the given file.
     *
     * @param inFile input file to detect
     * @return A String representing the MIME type of the file, or "application/x-unknown" otherwise
     */
    public static MimeDesc getMimeType(File inFile) {
        return getMimeType(inFile.getAbsolutePath());
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

        MimeDesc mimeDesc = getMimeType(inFile.getAbsolutePath());
        String mime = mimeDesc.mime();
        Format format = mapToFormat(mimeDesc.mime(), mimeDesc.desc());
        LOGGER.trace("detectFormat: " + inFile, "mime-type: " + mime, "description: " + mimeDesc.desc());

        if (isLoadable(format)) {
            LOGGER.debug("Format: %s resolved via mime-type/magic number".formatted(format));
            return format;
        }


        if ((format == BZIP2 || format == GZIP) &&
                inFile.getName().toLowerCase().contains("fit")) {
            // special case and a very heavy operation: we can handle fits bz2 or gzip files but don't try unless we are pretty sure
            // in archives: legacy files are often named a.fits.gz or a.fits.bz2
            try (var ignored = new Fits(inFile)) {
                return FITS;
            }
            catch (FitsException ignore) {}
        }

        format = ifNotNull(guessBySamplingContent(inFile)).getOrElse(format);
        if (isLoadable(format)) {
            LOGGER.debug("Format: %s resolved via file sampling".formatted(format));
            return format;
        }

        // all failed; fallback to trial and error
        if (EnumSet.of(TEXT, HTML, UNKNOWN).contains(format)) {
            if (isIpacTable(inFile)) {
                format = IPACTABLE;
            } else if (isRegionFile(inFile)) {
                format = REGION;
            } else if (format != HTML) {
                // check for csv or tsv
                Format dformat = DuckDbReadable.Csv.detect(inFile.getAbsolutePath());
                if (dformat != null) format = dformat;
            }
        }

        if (isLoadable(format)) {
            LOGGER.debug("Format: %s resolved via trial and error".formatted(format));
            return format;
        } else {
            LOGGER.debug("Failed to detect file: " + inFile,
                    "mime: " + mime,
                    "desc: " + mimeDesc.desc()
            );
            return format;
        }
    }

//====================================================================
//  Helper methods
//====================================================================

    private static boolean isRegionFile(File inFile) {
        try {
            var res = new RegionParser().processFile(inFile);
            return res != null && !res.regionList().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    @Nonnull
    private static Format mapToFormat(String mime, String desc) {
        Format format = Format.fromMime(mime);
        if (format != UNKNOWN) return format;
        // mime type failed, try desc
        if (desc != null) {
            desc = desc.toLowerCase();
            if (desc.contains("parquet"))       return PARQUET;
            if (desc.contains("csv"))           return CSV;
            if (desc.contains("json"))          return JSON;
            if (desc.contains("tab-separated")) return TSV;
        }
        return UNKNOWN;
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
            } else if (line.startsWith("#ASDF")) {      // not reliable
                return ASDF;

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

    /** Returns true when {@code data[offset..]} starts with the given byte values. */
    private static boolean magic(byte[] data, int offset, int... expected) {
        if (data.length < offset + expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if ((data[offset + i] & 0xFF) != (expected[i] & 0xFF)) return false;
        }
        return true;
    }

}

