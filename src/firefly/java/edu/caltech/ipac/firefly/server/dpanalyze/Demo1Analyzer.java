package edu.caltech.ipac.firefly.server.dpanalyze;
/**
 * User: roby
 * Date: 3/27/20
 * Time: 9:25 AM
 */


import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import nom.tam.fits.Header;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
@DataProductAnalyzerImpl(id ="analyze-demo1")
public class Demo1Analyzer implements DataProductAnalyzer {


    @Override
    public FileAnalysisReport analyze(FileAnalysisReport inputReport, File inFile, String analyzerId, Map<String, String> params) {
        return inputReport;
    }

    @Override
    public FileAnalysisReport analyzeFits(FileAnalysisReport inputReport,
                                          File inFile,
                                          String analyzerId,
                                          Map<String, String> params,
                                          Header[] headerAry) {



        String code= params.getOrDefault("Code", "");
        //
        // - how to replace the analysis file with another file
        //
        if (code.equals("REP") && params.containsKey("replace") && Boolean.parseBoolean(params.get("replace"))) {
            return demoReplacingData(inputReport);
        }

        //
        // - how to add another file to return with analysis file
        //
        if (code.equals("ADD") && params.containsKey("addAPart") && Boolean.parseBoolean(params.get("addAPart"))) {
            return demoAddingAPart(inputReport);
        }

        //
        // - Add column names and units to a fits image rendered as table
        //
        FileAnalysisReport retRep= inputReport.copy();
        if (code.equals("TAB")) {
            retRep= demoAddColumnNamesAndUnits(inputReport,headerAry);
        }
        //
        // - Add column names and units to a fits image rendered as table
        // - Create a customized chart
        // - make chart the defult
        //
        else if (code.equals("TABCH")) {
            retRep= demoCreate3Charts(inputReport,headerAry);
        }

        //
        // - make a chart with two traces
        //
        else if (code.equals("TAB2CH")) {
            retRep= demoCreate2Traces(inputReport,headerAry);
        }


        //
        // - how to make a part the default
        //
        try {
            int codeNum= Integer.parseInt(code);
            if (codeNum>-1 && codeNum<inputReport.getParts().size()) {
                retRep.getPart(codeNum).setDefaultPart(true);
            }
        } catch (NumberFormatException ignored) { }

        //
        // - how to disable the "All Image in file option
        //
        if (code.equals("NOALL") && params.containsKey("noimages") && Boolean.parseBoolean(params.get("noimages"))) {
            retRep.setDisableAllImagesOption(true);
        }


        return retRep;
    }



    /**
     * how to replace the analysis file with another file
     */
    private FileAnalysisReport demoReplacingData(FileAnalysisReport inputReport) {
        try {
            File f= new File(ServerContext.getUploadDir(),"x.fits");
            URL url = new URL("http://web.ipac.caltech.edu.s3-us-west-2.amazonaws.com/staff/roby/demo/wise-00.fits");
            URLDownload.getDataToFile(url, f);
            FileAnalysisReport retRep= inputReport.copy(false);
            FileAnalysisReport.Part replacePart=
                    new FileAnalysisReport.Part( FileAnalysisReport.Type.Image, "A image to replace");
            replacePart.setFileLocationIndex(0);
            replacePart.setDefaultPart(true);
            replacePart.setUiRender(FileAnalysisReport.UIRender.Image);
            replacePart.setUiEntry(FileAnalysisReport.UIEntry.UseSpecified);
            replacePart.setConvertedFileName(ServerContext.replaceWithPrefix(f));
            retRep.addPart(replacePart);
            return retRep;
        } catch (MalformedURLException|FailedRequestException  e) {
            e.printStackTrace();
            return inputReport;
        }
    }

    private FileAnalysisReport demoAddingAPart(FileAnalysisReport inputReport) {
        try {
            File f= new File(ServerContext.getUploadDir(),"x.fits");
            URL url = new URL("http://web.ipac.caltech.edu.s3-us-west-2.amazonaws.com/staff/roby/demo/wise-00.fits");
            URLDownload.getDataToFile(url, f);
            FileAnalysisReport retRep= inputReport.copy();
            FileAnalysisReport.Part addPart=
                    new FileAnalysisReport.Part( FileAnalysisReport.Type.Image, "An additional image ");
            addPart.setFileLocationIndex(0);
            addPart.setUiRender(FileAnalysisReport.UIRender.Image);
            addPart.setUiEntry(FileAnalysisReport.UIEntry.UseSpecified);
            addPart.setConvertedFileName(ServerContext.replaceWithPrefix(f));
            retRep.addPart(addPart);
            return retRep;
        } catch (MalformedURLException|FailedRequestException  e) {
            e.printStackTrace();
            return inputReport;
        }

    }

    /**
     *Add column names and units to a fits image rendered as table
     */
    private FileAnalysisReport demoAddColumnNamesAndUnits(FileAnalysisReport inputReport, Header[] headerAry) {
        FileAnalysisReport retRep= inputReport.copy();
        List<FileAnalysisReport.Part> parts= retRep.getParts();
        for(FileAnalysisReport.Part part : parts) {
            Header h= headerAry[part.getIndex()];
            int naxis2= h.getIntValue("NAXIS2",0);
            String xtension= h.getStringValue("XTENSION");
            boolean isImage= xtension==null || xtension.equalsIgnoreCase("IMAGE");
            if (naxis2>0 && naxis2<30 && isImage) {
                part.setUiRender(FileAnalysisReport.UIRender.Chart);
                part.setUiEntry(FileAnalysisReport.UIEntry.UseSpecified);
                List<String> cNames= new ArrayList<>(naxis2+1);
                List<String> cUnits= new ArrayList<>(naxis2+1);
                cNames.add("The_Index");
                cUnits.add("count");
                for(int i=1; i<naxis2+1;i++) cNames.add("Col-"+i);
                for(int i=1; i<naxis2+1;i++) cUnits.add("Unit-"+i);
                part.setTableColumnNames(cNames);
                part.setTableColumnUnits(cUnits);
            }
        }
        return retRep;
    }

    /*
     * - Add column names and units to a fits image rendered as table
     * - Create a customized chart
     * - make chart the default
     * - also demonstrates chaining - the image rendered as a table is set up by demoAddColumnNamesAndUnits
     */
    private FileAnalysisReport demoCreate3Charts(FileAnalysisReport inputReport, Header[] headerAry) {
        FileAnalysisReport retRep= demoAddColumnNamesAndUnits(inputReport,headerAry);
        List<FileAnalysisReport.Part> parts= retRep.getParts();
        for(FileAnalysisReport.Part part : parts) {
            Header h= headerAry[part.getIndex()];
            int naxis2= h.getIntValue("NAXIS2",0);
            String xtension= h.getStringValue("XTENSION");
            boolean isImage= xtension==null || xtension.equalsIgnoreCase("IMAGE");
            if (naxis2>0 && naxis2<30 && isImage) {
                List<FileAnalysisReport.ChartParams> cpList= new ArrayList<>();

                FileAnalysisReport.ChartParams cp= new FileAnalysisReport.ChartParams();
                cp.setxAxisColName("Col-7");
                cp.setyAxisColName("Col-8");
                cp.setMode("markers");
                cp.addLayout(Collections.singletonMap("title", "My Customized Scatter Chart"));
                cpList.add(cp);


                cp= new FileAnalysisReport.ChartParams();
                cp.setNumBins(40);
                cp.setyAxisColName("Col-1");
                cp.setSimpleChartType(FileAnalysisReport.ChartParams.ChartType.Histogram);
                cp.addLayout(Collections.singletonMap("title", "My Customized Histogram"));
                cpList.add(cp);


                cp= new FileAnalysisReport.ChartParams();
                cp.setxAxisColName("The_Index");
                cp.setyAxisColName("Col-1");
                cp.setMode("lines+markers");
                cp.setSimpleChartType(FileAnalysisReport.ChartParams.ChartType.XYChart);
                cp.addLayout(Collections.singletonMap("title", "My Customized XY Chart"));
                cpList.add(cp);

                part.setChartParams(cpList);
                part.setChartTableDefOption(FileAnalysisReport.ChartTableDefOption.showChart);
            }
        }
        return retRep;
    }

    /*
     * - make a chart with two traces
     * - also demonstrates chaining - the image rendered as a table is set up by demoAddColumnNamesAndUnits
     */
    private FileAnalysisReport demoCreate2Traces(FileAnalysisReport inputReport, Header[] headerAry) {
        FileAnalysisReport retRep= demoAddColumnNamesAndUnits(inputReport,headerAry);
        List<FileAnalysisReport.Part> parts= retRep.getParts();
        for(FileAnalysisReport.Part part : parts) {
            Header h= headerAry[part.getIndex()];
            int naxis2= h.getIntValue("NAXIS2",0);
            String xtension= h.getStringValue("XTENSION");
            boolean isImage= xtension==null || xtension.equalsIgnoreCase("IMAGE");
            if (naxis2>0 && naxis2<30 && isImage) {
                FileAnalysisReport.ChartParams cp= new FileAnalysisReport.ChartParams();
                List<Map<String,Object>> traces= new ArrayList<>();

                // setup trace 1
                Map<String,Object> trace= new HashMap<>();
                trace.put("x", "tables::The_Index");
                trace.put("y", "tables::Col-1");
                trace.put("name", "col1 vs index");
                trace.put("mode", "lines+markers");
                traces.add(trace);

                // setup trace 2
                trace= new HashMap<>();
                trace.put("x", "tables::The_Index");
                trace.put("y", "tables::Col-10");
                trace.put("name", "col10 vs index");
                trace.put("mode", "lines+markers");
                traces.add(trace);


                cp.addLayout(Collections.singletonMap("title", "Chart With 2 Traces"));
                cp.addLayout(Collections.singletonMap("xaxis", Collections.singletonMap("title", "the index")));
                cp.addLayout(Collections.singletonMap("yaxis", Collections.singletonMap("title", "value")));
                cp.setTraces(traces);
                part.setChartParams(Collections.singletonList(cp));
                part.setChartTableDefOption(FileAnalysisReport.ChartTableDefOption.showChart);
            }
        }
        return retRep;
    }
}
