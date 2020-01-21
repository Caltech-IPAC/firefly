/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.firefly.messaging.JsonHelper;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.JsonTableUtil;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.table.TableUtil.Format;
import edu.caltech.ipac.table.io.DsvTableIO;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.FitsHDUUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Date: 2019-06-27
 *
 * @author loi
 * @version $Id: $
 */
public class FileAnalysis {
    public enum ReportType {Brief,              // expect to only get a report with one part without details
                            Normal,             // a report with all parts populated, but not details
                            Details}            // a full report with details
    public enum Type {Image, Table, Spectrum, HeaderOnly, PDF, TAR, Unknown}


    public static Report analyze(File infile, ReportType type) throws Exception {

        ReportType mtype = type == ReportType.Brief ? ReportType.Normal : type;

        Format format = TableUtil.guessFormat(infile);
        Report report;
        switch (format) {
            case VO_TABLE:
                report = VoTableReader.analyze(infile, mtype);
                break;
            case FITS:
                report = FitsHDUUtil.analyze(infile, mtype);
                break;
            case IPACTABLE:
                report = IpacTableReader.analyze(infile, mtype);
                break;
            case CSV:
            case TSV:
                report =  DsvTableIO.analyze(infile, format.type, mtype);
                break;
            case PDF:
                report =  analyzePDF(infile, mtype);
                break;
            case TAR:
                report =  analyzeTAR(infile, mtype);
                break;
            default:
                report = new Report(type, Format.UNKNOWN.name(), infile.length(), infile.getAbsolutePath());
        }

        if (type == ReportType.Brief) {
            report.makeBrief();
        }
        return report;
    };

    public static String toJsonString(Report report) {
        JsonHelper helper = new JsonHelper();
        helper.setValue(report.filePath, "filePath");
        helper.setValue(report.fileName, "fileName");
        helper.setValue(report.fileSize, "fileSize");
        helper.setValue(report.type.name(), "type");
        helper.setValue(report.fileFormat, "fileFormat");
        helper.setValue(report.getDataType(), "dataTypes");
        if (report.getParts() != null) {
            for(int i = 0; i < report.getParts().size(); i++) {
                Part p = report.getParts().get(i);
                helper.setValue(p.index, "parts", i+"", "index");
                helper.setValue(p.type.name(), "parts", i+"", "type");
                helper.setValue(p.desc, "parts", i+"", "desc");
                if (p.totalTableRows>-1) helper.setValue(p.totalTableRows, "parts", i+"", "totalTableRows");
                if (!isEmpty(p.getDetails())) {
                    helper.setValue(JsonTableUtil.toJsonDataGroup(p.getDetails(),true), "parts", i+"", "details");
                }
            }
        }
        return helper.toJson();
    }


//====================================================================
//
//====================================================================

    public static class Report {
        private ReportType type;
        private long fileSize;
        private String filePath;
        private String fileName;
        private String fileFormat;
        private List<Part> parts;
        private String dataType;

        public Report(ReportType type, String fileFormat, long fileSize, String filePath) {
            this.type = type;
            this.fileFormat = fileFormat;
            this.fileSize = fileSize;
            this.filePath = filePath;
        }

        public ReportType getType() {
            return type;
        }

        public String getFormat() { return fileFormat; }

        public long getFileSize() {
            return fileSize;
        }

        public String getFilePath() {
            return filePath;
        }

        public List<Part> getParts() {
            return parts;
        }

        public String getFileName() { return fileName; }

        public void setFileName(String fileName) { this.fileName = fileName; }

        public void addPart(Part part) {
            if (parts == null) parts = new ArrayList<>();
            parts.add(part);
        }

        public String getDataType() {
            if (dataType == null) {
                if (parts != null) {
                    if (parts.size() == 1) {
                        dataType = parts.get(0).type.name();
                    } else {
                        List<String> types = parts.stream().map(part -> part.type.name()).distinct().collect(Collectors.toList());
                        dataType = StringUtils.toString(types);
                    }
                } else {
                    dataType = "";
                }
            }
            return dataType;
        }

        /**
         * convert this report into a Brief version.
         */
        void makeBrief() {
            if (type == ReportType.Brief) return;       // nothing to do
            getDataType();  // init dataType
            if (parts != null) {
                // keep only the first part with data.
                Part first = parts.stream()
                        .filter(p -> !Arrays.asList(Type.HeaderOnly, Type.Unknown).contains(p.getType()))
                        .findFirst()
                        .orElse(null);
                if (first != null) {
                    parts = Collections.singletonList(first);
                }
            }
        }
    }

    public static class Part {
        private Type type;
        private int index;
        private String desc;
        private DataGroup details;


        private int totalTableRows=-1;

        public Part(Type type) {
            this.type = type;
        }

        public Part(Type type, int index, String desc) {
            this.type = type;
            this.index = index;
            this.desc = desc;
        }

        public Type getType() { return type; }
        public void setType(Type type) { this.type = type; }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }

        public String getDesc() { return desc;}
        public void setDesc(String desc) { this.desc = desc; }

        public DataGroup getDetails() {
            return details;
        }
        public void setDetails(DataGroup details) {
            this.details = details;
        }

        public int getTotalTableRows() { return totalTableRows; }
        public void setTotalTableRows(int totalTableRows) { this.totalTableRows = totalTableRows; }

    }




    public static FileAnalysis.Report analyzePDF(File infile, FileAnalysis.ReportType type) {
        FileAnalysis.Report report = new FileAnalysis.Report(type, TableUtil.Format.PDF.name(), infile.length(), infile.getPath());
        report.addPart(new FileAnalysis.Part(FileAnalysis.Type.PDF, 0, "PDF File"));
        return report;
    }

    public static FileAnalysis.Report analyzeTAR(File infile, FileAnalysis.ReportType type) {
        FileAnalysis.Report report = new FileAnalysis.Report(type, TableUtil.Format.TAR.name(), infile.length(), infile.getPath());
        report.addPart(new FileAnalysis.Part(FileAnalysis.Type.TAR, 0, "TAR File"));
        return report;
    }
}
