package edu.caltech.ipac.firefly.server.dpanalyze;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.sofia.Sofia1DSpectraExtractor;
import edu.caltech.ipac.firefly.data.sofia.SofiaSpectraModel;
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
import java.util.Map;

import static edu.caltech.ipac.table.TableUtil.Format.VO_TABLE_TABLEDATA;

/*
 Analyzer for SOFIA extracted Spectra from FITS 'image'
 */
@DataProductAnalyzerImpl(id = "analyze-sofia")
public class SofiaAnalyzer implements DataProductAnalyzer {

    @Override
    public FileAnalysisReport analyzeFits(FileAnalysisReport inputReport,
                                          File inFile,
                                          String analyzerId,
                                          Map<String, String> params,
                                          Header[] headerAry) {


        String code = params.getOrDefault("product_type", "");
        String level = params.getOrDefault("processing_level", "").toUpperCase();
        String inst = params.getOrDefault("instrument", "").toUpperCase();
        //
        // Add spectra extracted data from image
        //
        boolean isSpectra = false;
        if (inst.equals("FORCAST")) {
            if (level.equals("LEVEL_2") && (code.equals("rspspec") || code.equals("mrgspec"))) { //params.containsKey("replace") && Boolean.parseBoolean(params.get("replace"))) {
                isSpectra = true;
            } else if (level.equals("LEVEL_3") && (code.equals("combspec") || code.equals("calspec"))) { //params.containsKey("replace") && Boolean.parseBoolean(params.get("replace"))) {
                isSpectra = true;
            }
        } else if (inst.equals("EXES") && level.equals("LEVEL_3")) {
            if (code.equals("mrgordspec") || code.equals("combspec") || code.equals("spec")) { //params.containsKey("replace") && Boolean.parseBoolean(params.get("replace"))) {
                isSpectra = true;
            }
        } else if (inst.equals("FLITECAM")) {
            if (level.equals("LEVEL_3") && (code.equals("calspec") || code.equals("combspec"))) { //params.containsKey("replace") && Boolean.parseBoolean(params.get("replace"))) {
                isSpectra = true;
            } else if(level.equals("LEVEL_2") && (code.equals("spec") || code.equals("rspspec"))){
                isSpectra = true;
            }
        }

        if(isSpectra) return addingSpectraExtractedTableAPart(inputReport, inst);

        FileAnalysisReport retRep = inputReport.copy();
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

    private FileAnalysisReport addingSpectraExtractedTableAPart(FileAnalysisReport inputReport, String inst) {
        try {
            SofiaSpectraModel.SpectraInstrument instrument = SofiaSpectraModel.SpectraInstrument.getInstrument(inst);
            File spectra = extractSpectraTable(inputReport.getFilePath(), instrument);
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
            String xCol = instrument.getXaxis().getKey();
            String yCol = instrument.getYaxis().getKey();
            cp.setxAxisColName(xCol);
            cp.setyAxisColName(yCol);
            cp.setMode("lines+markers");
            addPart.setChartParams(cp);
            retRep.addPart(addPart);

            return retRep;
        } catch (Exception e) {
            e.printStackTrace();
            return inputReport;
        }
    }

    private File extractSpectraTable(String fitsPath, SofiaSpectraModel.SpectraInstrument instrument) throws Exception {
        Sofia1DSpectraExtractor model = new Sofia1DSpectraExtractor(instrument);
        DataGroup dataObjects = model.extract(new File(fitsPath));
        File tempSpectraFile = File.createTempFile("sofia1d-spectra-", ".xml", ServerContext.getTempWorkDir());
        VoTableWriter.save(tempSpectraFile, dataObjects, VO_TABLE_TABLEDATA);
        return tempSpectraFile;
    }
}
