package edu.caltech.ipac.firefly.server.dpanalyze;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.sofia.Sofia1DSpectraExtractor;
import edu.caltech.ipac.firefly.data.sofia.SofiaFitsConverterUtil;
import edu.caltech.ipac.firefly.data.sofia.SofiaSpectraModel;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.table.io.VoTableWriter;
import edu.caltech.ipac.util.FitsHDUUtil;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
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
        String configuration = params.getOrDefault("configuration", "").toUpperCase();

        if(! inst.equals("FORCAST") && ! configuration.equals("IMAGING")) {
            inputReport.setAdditionalImageParams(Collections.singletonMap("CUBE_FIRST_FRAME", "50%"));
        }

        try {
            switch (inst) {
                case "FIFI-LS":
                    if (level.equals("LEVEL_3") && code.equals("wavelength_resampled")) {
                        return convertFIFIImage(inFile.getAbsolutePath());
                    }
                    break;
                case "GREAT":
                    if (level.equals("LEVEL_4")) {
                        insertFrequencyParts(inputReport, inFile.getAbsolutePath());
                    } else if (level.equals("LEVEL_1")){
                        setReportDisplay(inputReport, FileAnalysisReport.ChartTableDefOption.showChart);
                    }
                    break;
                default:
                    if(isSpectra(inst, level, code)) addingSpectraExtractedTableAPart(inputReport, inst);
                    break;
              }

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return inputReport;
    }

    private boolean isSpectra(String inst, String level, String code){

        if (inst.equals("FORCAST")) {
            if (level.equals("LEVEL_2") && (code.equals("rspspec") || code.equals("mrgspec"))) { //params.containsKey("replace") && Boolean.parseBoolean(params.get("replace"))) {
                return  true;
            } else if ((level.equals("LEVEL_3") && (code.equals("combspec"))) || (level.equals("LEVEL_3") && (code.equals("combined_spectrum")))  || code.equals("calspec")) { //params.containsKey("replace") && Boolean.parseBoolean(params.get("replace"))) {
                return  true;
            }
        } else if (inst.equals("EXES") && level.equals("LEVEL_3")) {
            if (code.equals("mrgordspec") || code.equals("combspec") || code.equals("spec")) { //params.containsKey("replace") && Boolean.parseBoolean(params.get("replace"))) {
                return  true;
            }
        } else if (inst.equals("FLITECAM")) {
            if (level.equals("LEVEL_3") && (code.equals("calspec") || code.equals("combspec"))) { //params.containsKey("replace") && Boolean.parseBoolean(params.get("replace"))) {
                return  true;
            } else if(level.equals("LEVEL_2") && (code.equals("spec") || code.equals("rspspec"))){
                return  true;
            }
        }
        return false;
    }
    private void setReportDisplay(FileAnalysisReport report,FileAnalysisReport.ChartTableDefOption option ){

        FileAnalysisReport.Part[] parts=report.getParts().toArray(new FileAnalysisReport.Part[0]);
        for (int i=0; i<parts.length; i++){
            parts[i].setChartTableDefOption(option);
            report.setPart(parts[i], i);
        }

    }
    /**
     * This method is to convert the FIFI-LS data to standard WAVE-TAB format and then the converted FITs is displayed.
     * @param inputFile
     * @return
     * @throws Exception
     */
    private FileAnalysisReport convertFIFIImage(String inputFile) throws Exception {

             BasicHDU[] HDUs = new Fits(inputFile).read();
            //create a temp file to save the converted FITs file
            //need to use unique name since the filename as a key stored in the cache
            String[] strAry = inputFile.split("/");
            File outputFile = new File(ServerContext.getUploadDir(), strAry[strAry.length-1]);

            //save the converted FITs to the temp file
            SofiaFitsConverterUtil.doConvertFits(inputFile, outputFile.getAbsolutePath());


            //create a detail report for the newly created FITs file
            FileAnalysisReport report  = (FitsHDUUtil.analyze(outputFile, FileAnalysisReport.ReportType.Details)).getReport();

            List<FileAnalysisReport.Part> parts = report.getParts();
            //assign the convertedFileName to the part that is changed
            for (int i=0; i<parts.size();i++){
                Header header = HDUs[i].getHeader();
                if (header.containsKey("EXTNAME")) {
                    String extName = header.getStringValue("EXTNAME").toUpperCase();
                    if (extName.equalsIgnoreCase("flux") || extName.equalsIgnoreCase("wavelength")) {
                        parts.get(i).setConvertedFileName(ServerContext.replaceWithPrefix(outputFile));
                    }
                }
            }
            //delete the temp file once the application ends.
            outputFile.deleteOnExit();
            return report;


    }

    /**
     * This method is convert the image part of the GREAT to a table part
     * @param inputReport
     * @param inputFile
     * @return
     * @throws FitsException
     * @throws IOException
     */
    private void  insertFrequencyParts(FileAnalysisReport inputReport, String inputFile) throws Exception {

        Fits fits = new Fits(inputFile);
        BasicHDU[] hdus= fits.read();
        String[] strAry = inputFile.split("/");

        String fitsFileNameRoot = strAry[strAry.length-1].split(".fits")[0];
        FileAnalysisReport.Part[] parts=inputReport.getParts().toArray(new FileAnalysisReport.Part[0]);

        int partIndex=0;
        for (int i=0; i<hdus.length; i++){
            String fileName = fitsFileNameRoot+i;
            partIndex++;
            FileAnalysisReport.Part chartPart = makeChartPart(hdus[i], fileName,i, partIndex);
            if (chartPart!=null) {
                inputReport.insertPartAfter(parts[i], chartPart);
                partIndex++;

            }
        }

    }

    /**
     * This method made the chart part for each imageHDU where it has a frequency defined in the header.
     * It uses SofiaFitsConverterUtil class which is reading the FITs and converts to a DataGroup. The conversion
     * algorithm and formula are described in SofiaFitsConverterUtil.
     *
     * @param hdu
     * @param fileName
     * @param hduIndex
     * @param partIndex
     * @return
     * @throws Exception
     */
    private FileAnalysisReport.Part makeChartPart(BasicHDU hdu, String fileName, int hduIndex, int partIndex) throws Exception {
        DataGroup dg = SofiaFitsConverterUtil.makeDataGroupFromHDU(hdu, fileName);
        if (dg==null) return null;

        File frequencyTable = new File(ServerContext.getUploadDir(), fileName +".xml");

        VoTableWriter.save(frequencyTable, dg, VO_TABLE_TABLEDATA);
        FileAnalysisReport  freqReport = VoTableReader.analyze(frequencyTable, FileAnalysisReport.ReportType.Details);
        FileAnalysisReport.Part freqPart = freqReport.getPart(0);

        freqPart.setDesc("Spectrum in " + "HDU "+ hduIndex + " (table or chart)");
        freqPart.setFileLocationIndex(0);
        freqPart.setConvertedFileName(ServerContext.replaceWithPrefix(frequencyTable));
        freqPart.setConvertedFileFormat(freqReport.getFormat());
        freqPart.setIndex(partIndex); //set the index to the correct location in the part list
        freqPart.setUiRender(FileAnalysisReport.UIRender.Chart);
        freqPart.setUiEntry(FileAnalysisReport.UIEntry.UseAll);
        freqPart.setChartTableDefOption(FileAnalysisReport.ChartTableDefOption.showChart);
        freqPart.setDefaultPart(true);
        freqPart.setInterpretedData(true);

        FileAnalysisReport.ChartParams cp = getChartParm("GREAT", "markers", "");
        freqPart.setChartParams(cp);

        return freqPart;

    }

    /**
     * Create a chart parameter to be used among the parts created.
     * @param inst
     * @param mode
     * @param title
     * @return
     */
    private FileAnalysisReport.ChartParams getChartParm(String inst, String mode, String title){
        FileAnalysisReport.ChartParams cp= new FileAnalysisReport.ChartParams();
        SofiaSpectraModel.SpectraInstrument instrument = SofiaSpectraModel.SpectraInstrument.getInstrument(inst);
        String xCol = instrument.getXaxis().getKey();
        String yCol = instrument.getYaxis().getKey();
        cp.setxAxisColName(xCol);
        cp.setyAxisColName(yCol);
        //set the title instead of the default value stored in the part
        if (title!=null) cp.addLayout(Collections.singletonMap("title",""));
        cp.setMode(mode);
        return cp;

    }


    private void addingSpectraExtractedTableAPart(FileAnalysisReport inputReport, String inst) {
        try {
            SofiaSpectraModel.SpectraInstrument instrument = SofiaSpectraModel.SpectraInstrument.getInstrument(inst);
            File spectra = extractSpectraTable(inputReport.getFilePath(), instrument);

            String spectraName = "Extracted Data";
//            DataGroup dg = inputReport.getPart(0).getDetails();
//            for (int i = 0; i <dg.size();i++) {
//                String key = (String) dg.getData("key", i);
//                if(key.equals("FILENAME")){
//                    String tmpName = (String) dg.getData("value", i);
//                    spectraName += tmpName.substring(0,tmpName.lastIndexOf("."));
//                    break;
//                }
//            }
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
            addPart.setInterpretedData(true);
            //TODO add error bars?
            FileAnalysisReport.ChartParams cp = getChartParm(inst, "lines+markers", null);
            addPart.setChartParams(cp);
            inputReport.addPart(addPart);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private File extractSpectraTable(String fitsPath, SofiaSpectraModel.SpectraInstrument instrument) throws Exception {
        Sofia1DSpectraExtractor model = new Sofia1DSpectraExtractor(instrument);
        DataGroup dataObjects = model.extract(new File(fitsPath));
        File tempSpectraFile = File.createTempFile("sofia1d-spectra-", ".xml", QueryUtil.getTempDir());
        VoTableWriter.save(tempSpectraFile, dataObjects, VO_TABLE_TABLEDATA);
        return tempSpectraFile;
    }
}
