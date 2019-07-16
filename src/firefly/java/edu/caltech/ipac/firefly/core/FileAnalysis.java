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
    public enum Type {Image, Table, Spectrum, ImageNoData, Unknown}


    public static Report analyze(File infile, ReportType type) throws Exception {

        Format format = TableUtil.guessFormat(infile);
        switch (format) {
            case VO_TABLE:
                return VoTableReader.analyze(infile, type);
            case FITS:
                return FitsHDUUtil.analyze(infile, type);
            case IPACTABLE:
                return IpacTableReader.analyze(infile, type);
            case CSV:
            case TSV:
                return DsvTableIO.analyze(infile, format.type, type);
            default:

        }
        Report report = new Report(type, infile.length(), infile.getAbsolutePath());
        report.addPart(new Part(Type.Unknown, 0, "Unknown Type"));
        return report;
    };

    public static String toJsonString(Report report) {
        JsonHelper helper = new JsonHelper();
        helper.setValue(report.filePath, "filePath");
        helper.setValue(report.fileName, "fileName");
        helper.setValue(report.fileSize, "fileSize");
        helper.setValue(report.type.name(), "type");
        helper.setValue(report.getDataType(), "dataTypes");
        for(int i = 0; i < report.getParts().size(); i++) {
            Part p = report.getParts().get(i);
            helper.setValue(p.index, "parts", i+"", "index");
            helper.setValue(p.type.name(), "parts", i+"", "type");
            helper.setValue(p.desc, "parts", i+"", "desc");
            if (!isEmpty(p.getDetails())) {
                helper.setValue(JsonTableUtil.toJsonDataGroup(p.getDetails()), "parts", i+"", "details");
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
        private List<Part> parts;

        public Report(ReportType type, long fileSize, String filePath) {
            this.type = type;
            this.fileSize = fileSize;
            this.filePath = filePath;
        }

        public ReportType getType() {
            return type;
        }

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
            if (parts != null) {
                if (parts.size() == 1) {
                    return parts.get(0).type.name();
                } else {
                    List<String> types = parts.stream().map(part -> part.type.name()).distinct().collect(Collectors.toList());
                    return StringUtils.toString(types);
                }
            }
            return "";
        }
    }

    public static class Part {
        private Type type;
        private int index;
        private String desc;
        private DataGroup details;

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
    }
}
