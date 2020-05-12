package edu.caltech.ipac.firefly.server.dpanalyze;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.sofia.Sofia1DSpectraExtractor;
import edu.caltech.ipac.firefly.data.sofia.SofiaSpectraModel;
import edu.caltech.ipac.firefly.data.sofia.VOSpectraModel;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.table.io.VoTableWriter;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import nom.tam.fits.Header;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static edu.caltech.ipac.table.TableUtil.Format.VO_TABLE_TABLEDATA;

/*
 Analyzer for SOFIA extracted Spectra from FITS 'image'
 */
@DataProductAnalyzerImpl(id = "analyze-sofia")
public class SofiaAnalyzer implements DataProductAnalyzer {


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


        String code = params.getOrDefault("product_type", "");
        String level = params.getOrDefault("processing_level", "");
        String inst = params.getOrDefault("instrument", "");
        //
        // - how to replace the analysis file with another file
        //
        Sofia1DSpectraExtractor model;
        if (inst.equalsIgnoreCase("FORCAST")) {
            model = new Sofia1DSpectraExtractor(SofiaSpectraModel.SpectraInstrument.FORCAST);
            if (level.equalsIgnoreCase("LEVEL_2") && (code.equals("rspspec") || code.equals("mrgspec"))) { //params.containsKey("replace") && Boolean.parseBoolean(params.get("replace"))) {
                return addingSpectraExtractedTableAPart(inputReport, model);
            } else if (level.equalsIgnoreCase("LEVEL_3") && code.equals("combspec")) { //params.containsKey("replace") && Boolean.parseBoolean(params.get("replace"))) {
                return addingSpectraExtractedTableAPart(inputReport, model);
            }
        } else if (inst.equalsIgnoreCase("EXES") && level.equalsIgnoreCase("LEVEL_3")) {
            model = new Sofia1DSpectraExtractor(SofiaSpectraModel.SpectraInstrument.EXES);
            if (code.equals("mrgordspec") || code.equals("combspec") || code.equals("spec")) { //params.containsKey("replace") && Boolean.parseBoolean(params.get("replace"))) {
                return addingSpectraExtractedTableAPart(inputReport, model);
            }
        } else if (inst.equalsIgnoreCase("FLITECAM") && level.equalsIgnoreCase("LEVEL_3")) {
            model = new Sofia1DSpectraExtractor(SofiaSpectraModel.SpectraInstrument.FLITECAM);
            if (code.equals("calspec") || code.equals("combspec")) { //params.containsKey("replace") && Boolean.parseBoolean(params.get("replace"))) {
                return addingSpectraExtractedTableAPart(inputReport, model);
            }
        }

        FileAnalysisReport retRep = inputReport.copy();

//        //
//        // - how to make a part the default
//        //
//        try {
//            int codeNum = Integer.parseInt(code);
//            if (codeNum > -1 && codeNum < inputReport.getParts().size()) {
//                retRep.getPart(codeNum).setDefaultPart(true);
//            }
//        } catch (NumberFormatException ignored) {
//        }
        return retRep;
    }


    /**
     * how to replace the analysis file with another file
     */
    private FileAnalysisReport demoReplacingData(FileAnalysisReport inputReport) {
        try {
            File f = new File(ServerContext.getUploadDir(), "x.fits");
            URL url = new URL("http://web.ipac.caltech.edu.s3-us-west-2.amazonaws.com/staff/roby/demo/wise-00.fits");
            URLDownload.getDataToFile(url, f);
            FileAnalysisReport retRep = inputReport.copy(false);
            FileAnalysisReport.Part replacePart =
                    new FileAnalysisReport.Part(FileAnalysisReport.Type.Image, "A image to replace");
            replacePart.setFileLocationIndex(0);
            replacePart.setDefaultPart(true);
            replacePart.setUiRender(FileAnalysisReport.UIRender.Image);
            replacePart.setUiEntry(FileAnalysisReport.UIEntry.UseSpecified);
            replacePart.setConvertedFileName(ServerContext.replaceWithPrefix(f));
            retRep.addPart(replacePart);
            return retRep;
        } catch (MalformedURLException | FailedRequestException e) {
            e.printStackTrace();
            return inputReport;
        }
    }

    private FileAnalysisReport addingSpectraExtractedTableAPart(FileAnalysisReport inputReport, Sofia1DSpectraExtractor model) {
        try {

            File spectra = extractSpectraTable(inputReport.getFilePath(), model);
            FileAnalysisReport retRep = inputReport.copy();
            DataGroup dg = retRep.getPart(0).getDetails();
            String spectraName = "Extracted Data ";
            for (int i = 0; i <dg.size();i++) {
                String key = (String) dg.getData("key", i);
                if(key.equals("FILENAME")){
                    String tmpName = (String) dg.getData("value", i);
                    spectraName += tmpName.substring(0,tmpName.lastIndexOf("."));
                    break;
                }
            }
            FileAnalysisReport tempRep = VoTableReader.analyze(spectra, FileAnalysisReport.ReportType.Details);
            FileAnalysisReport.Part tempPart = tempRep.getPart(0);
            FileAnalysisReport.Part addPart = tempPart.copy();
            addPart.setDesc(spectraName.trim());
            addPart.setFileLocationIndex(0);
            addPart.setConvertedFileName(ServerContext.replaceWithPrefix(spectra));
            addPart.setConvertedFileFormat(tempRep.getFormat());
            addPart.setIndex(1);
            addPart.setUiRender(FileAnalysisReport.UIRender.Chart);
            addPart.setUiEntry(FileAnalysisReport.UIEntry.UseSpecified);
            addPart.setChartTableDefOption(FileAnalysisReport.ChartTableDefOption.showChart);
            addPart.setDefaultPart(true);



            //TODO add error bars?
            FileAnalysisReport.ChartParams cp = new FileAnalysisReport.ChartParams();
            String xCol = VOSpectraModel.SPECTRA_FIELDS.WAVELENGTH.getKey();
            String yCol = VOSpectraModel.SPECTRA_FIELDS.FLUX.getKey();
            cp.setxAxisColName(xCol);
            cp.setyAxisColName(yCol);
            cp.setMode("markers");
            addPart.setChartParams(cp);
            retRep.addPart(addPart);

            return retRep;
        } catch (Exception e) {
            e.printStackTrace();
            return inputReport;
        }
    }

    private File extractSpectraTable(String fitsPath, Sofia1DSpectraExtractor model) throws Exception {
//        String example = "http://web.ipac.caltech.edu.s3-us-west-2.amazonaws.com/staff/ejoliet/demo/cds-votable-sample.xml";
//        File f = new File(ServerContext.getTempWorkDir(), "x.xml");
//        URL url = new URL(example);
//        URLDownload.getDataToFile(url, f);
//         return f;
        DataGroup dataObjects = model.extract(new File(fitsPath));
        File tempSpectraFile = File.createTempFile("sofia1d-spectra-", ".xml", ServerContext.getTempWorkDir());
        VoTableWriter.save(tempSpectraFile, dataObjects, VO_TABLE_TABLEDATA);
        return tempSpectraFile;
    }

    /**
     *Add column names and units to a fits image rendered as table
     */
    private FileAnalysisReport demoAddColumnNamesAndUnits(FileAnalysisReport inputReport, Header[] headerAry) {
        FileAnalysisReport retRep = inputReport.copy();
        List<FileAnalysisReport.Part> parts = retRep.getParts();
        for (FileAnalysisReport.Part part : parts) {
            Header h = headerAry[part.getIndex()];
            int naxis2 = h.getIntValue("NAXIS2", 0);
            String xtension = h.getStringValue("XTENSION");
            boolean isImage = xtension == null || xtension.equalsIgnoreCase("IMAGE");
            if (naxis2 > 0 && naxis2 < 30 && isImage) {
                part.setUiRender(FileAnalysisReport.UIRender.Chart);
                part.setUiEntry(FileAnalysisReport.UIEntry.UseSpecified);
                List<String> cNames = new ArrayList<>(naxis2 + 1);
                List<String> cUnits = new ArrayList<>(naxis2 + 1);
                cNames.add("The_Index");
                cUnits.add("count");
                for (int i = 1; i < naxis2 + 1; i++) cNames.add("Col-" + i);
                for (int i = 1; i < naxis2 + 1; i++) cUnits.add("Unit-" + i);
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
        FileAnalysisReport retRep = demoAddColumnNamesAndUnits(inputReport, headerAry);
        List<FileAnalysisReport.Part> parts = retRep.getParts();
        for (FileAnalysisReport.Part part : parts) {
            Header h = headerAry[part.getIndex()];
            int naxis2 = h.getIntValue("NAXIS2", 0);
            String xtension = h.getStringValue("XTENSION");
            boolean isImage = xtension == null || xtension.equalsIgnoreCase("IMAGE");
            if (naxis2 > 0 && naxis2 < 30 && isImage) {
                List<FileAnalysisReport.ChartParams> cpList = new ArrayList<>();

                FileAnalysisReport.ChartParams cp = new FileAnalysisReport.ChartParams();
                cp.setxAxisColName("Col-7");
                cp.setyAxisColName("Col-8");
                cp.setMode("markers");
                cp.addLayout(Collections.singletonMap("title", "My Customized Scatter Chart"));
                cpList.add(cp);


                cp = new FileAnalysisReport.ChartParams();
                cp.setNumBins(40);
                cp.setyAxisColName("Col-1");
                cp.setSimpleChartType(FileAnalysisReport.ChartParams.ChartType.Histogram);
                cp.addLayout(Collections.singletonMap("title", "My Customized Histogram"));
                cpList.add(cp);


                cp = new FileAnalysisReport.ChartParams();
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
        FileAnalysisReport retRep = demoAddColumnNamesAndUnits(inputReport, headerAry);
        List<FileAnalysisReport.Part> parts = retRep.getParts();
        for (FileAnalysisReport.Part part : parts) {
            Header h = headerAry[part.getIndex()];
            int naxis2 = h.getIntValue("NAXIS2", 0);
            String xtension = h.getStringValue("XTENSION");
            boolean isImage = xtension == null || xtension.equalsIgnoreCase("IMAGE");
            if (naxis2 > 0 && naxis2 < 30 && isImage) {
                FileAnalysisReport.ChartParams cp = new FileAnalysisReport.ChartParams();
                List<Map<String, Object>> traces = new ArrayList<>();

                // setup trace 1
                Map<String, Object> trace = new HashMap<>();
                trace.put("x", "tables::The_Index");
                trace.put("y", "tables::Col-1");
                trace.put("name", "col1 vs index");
                trace.put("mode", "lines+markers");
                traces.add(trace);

                // setup trace 2
                trace = new HashMap<>();
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
