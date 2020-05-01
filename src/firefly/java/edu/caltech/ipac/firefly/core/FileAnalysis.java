/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.firefly.messaging.JsonHelper;
import edu.caltech.ipac.firefly.server.dpanalyze.DataProductAnalyzer;
import edu.caltech.ipac.firefly.server.dpanalyze.DataProductAnalyzerFactory;
import edu.caltech.ipac.table.JsonTableUtil;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.table.TableUtil.Format;
import edu.caltech.ipac.table.io.DsvTableIO;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.FitsHDUUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Date: 2019-06-27
 *
 * @author loi
 * @version $Id: $
 */
public class FileAnalysis {

    public static FileAnalysisReport analyze(File infile, FileAnalysisReport.ReportType type) throws Exception {
        return analyze(infile,type,null,Collections.emptyMap());
    }

    public static FileAnalysisReport analyze(File infile, FileAnalysisReport.ReportType type, String analyzerId, Map<String,String> params) throws Exception {

        FileAnalysisReport.ReportType mtype = type == FileAnalysisReport.ReportType.Brief ? FileAnalysisReport.ReportType.Normal : type;

        Format format = TableUtil.guessFormat(infile);
        FileAnalysisReport report= null;
        DataProductAnalyzer dpA= DataProductAnalyzerFactory.getAnalyzer(analyzerId);
        FileAnalysisReport productReport= null;
        switch (format) {
            case VO_TABLE:
                report = VoTableReader.analyze(infile, mtype);
                break;
            case FITS:
                FitsHDUUtil.FitsAnalysisReport rep = FitsHDUUtil.analyze(infile, mtype);
                productReport= dpA.analyzeFits(rep.getReport(),infile,analyzerId,params,rep.getHeaderAry());
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
                report = new FileAnalysisReport(type, Format.UNKNOWN.name(), infile.length(), infile.getAbsolutePath());
        }

        if (format!=Format.FITS) {
            productReport= dpA.analyze(report,infile,analyzerId,params);
        }

        if (productReport!=null) {
            if (type == FileAnalysisReport.ReportType.Brief) productReport.makeBrief();
            if (analyzerId!=null) {
                productReport.setDataProductsAnalyzerId(analyzerId);
                productReport.setAnalyzerFound(DataProductAnalyzerFactory.hasAnalyzer(analyzerId));
            }
        }


        return productReport;
    }


    private static void putPartVal(JsonHelper helper, Object value, int idx, String key) {
        if (isEmpty(value)) return;
        helper.setValue(value, "parts", idx+"", key);
    }

    public static String toJsonString(FileAnalysisReport report) {
        JsonHelper h = new JsonHelper();
        h.setValue(report.getType().name(), "type");
        h.setValue(report.getFilePath(), "filePath");
        h.setValue(report.getFileName(), "fileName");
        h.setValue(report.getFileSize(), "fileSize");
        h.setValue(report.getFormat(), "fileFormat");
        h.setValue(report.getDataType(), "dataTypes");
        h.setValue(report.isAnalyzerFound(), "analyzerFound");
        if (report.isDisableAllImagesOption()) {
            h.setValue(report.isDisableAllImagesOption(), "disableAllImageOption");
        }
        if (report.getDataProductsAnalyzerId()!=null) {
            h.setValue(report.getDataProductsAnalyzerId(), "dataProductsAnalyzerId");
        }

        if (report.getParts() != null) {
            for(int i = 0; i < report.getParts().size(); i++) {
                FileAnalysisReport.Part p = report.getParts().get(i);
                putPartVal(h, p.getIndex(), i, "index");
                putPartVal(h, p.getType().name(), i, "type");
                putPartVal(h, p.getUiEntry().name(), i, "uiEntry");
                putPartVal(h, p.getUiRender().name(), i, "uiRender");
                putPartVal(h, p.getDesc(), i, "desc");
                putPartVal(h, p.getConvertedFileName(),i,"convertedFileName");
                putPartVal(h, p.getConvertedFileFormat(),i,"convertedFileFormat");
                putPartVal(h, p.getTableColumnNames(),i,"tableColumnNames");
                putPartVal(h, p.getTableColumnUnits(),i,"tableColumnUnits");
                putPartVal(h, p.getChartTableDefOption().name(),i,"chartTableDefOption");
                if (p.getFileLocationIndex()>-1) putPartVal(h, p.getFileLocationIndex(),i,"fileLocationIndex");
                if (p.isDefaultPart()) putPartVal(h,p.isDefaultPart(),i,"defaultPart");
                if (p.getTotalTableRows()>-1) putPartVal(h, p.getTotalTableRows(), i, "totalTableRows");
                if (!isEmpty(p.getDetails())) {
                    putPartVal(h, JsonTableUtil.toJsonDataGroup(p.getDetails(),true), i, "details");
                }
                putPartVal(h, getJsonChartParams(p.getChartParams()),i,"chartParamsAry");
            }
        }
        return h.toJson();
    }



    public static JSONArray getJsonChartParams(List<FileAnalysisReport.ChartParams> cpList) {
        if (cpList==null) return null;

        JSONArray jArray= new JSONArray();
        for(FileAnalysisReport.ChartParams cp : cpList ) {
            JSONObject jObj= new JSONObject();
            jObj.put("layoutAdds", cp.isLayoutAdds());
            jObj.put("simpleData", cp.isSimpleData());
            if (cp.getLayout()!=null) jObj.put("layout", cp.getLayout());
            if (cp.isSimpleData()) {
                if (cp.getxAxisColName()!=null) jObj.put("xAxisColName", cp.getxAxisColName());
                if (cp.getyAxisColName()!=null) jObj.put("yAxisColName", cp.getyAxisColName());
                if (cp.getMode()!=null) jObj.put("mode", cp.getMode());
                if (cp.getNumBins()>0) jObj.put("numBins", cp.getNumBins());
                jObj.put("simpleChartType", cp.getSimpleChartType().toString());
            }
            else {
                if (cp.getTraces()!=null) jObj.put("traces", cp.getTraces());
            }
            jArray.add(jObj);
        }
        return jArray;
    };

//====================================================================
//
//====================================================================

    public static FileAnalysisReport analyzePDF(File infile, FileAnalysisReport.ReportType type) {
        FileAnalysisReport report = new FileAnalysisReport(type, TableUtil.Format.PDF.name(), infile.length(), infile.getPath());
        report.addPart(new FileAnalysisReport.Part(FileAnalysisReport.Type.PDF, "PDF File"));
        return report;
    }

    public static FileAnalysisReport analyzeTAR(File infile, FileAnalysisReport.ReportType type) {
        FileAnalysisReport report = new FileAnalysisReport(type, TableUtil.Format.TAR.name(), infile.length(), infile.getPath());
        report.addPart(new FileAnalysisReport.Part(FileAnalysisReport.Type.TAR, "TAR File"));
        return report;
    }
}
