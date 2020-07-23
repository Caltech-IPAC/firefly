package edu.caltech.ipac.firefly.server.dpanalyze;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.sofia.Sofia1DSpectraExtractor;
import edu.caltech.ipac.firefly.data.sofia.SofiaSpectraModel;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.table.io.VoTableWriter;
import edu.caltech.ipac.util.FitsHDUUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import edu.caltech.ipac.firefly.data.sofia.SofiaFitsConverterUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

        if (inst.equals("FIFI-LS")) {
            try {
                return convertFIFIImage(inFile.getAbsolutePath());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        if ( inst.equals("GREAT")){
            try {
                return convertToFrequencyParts(inputReport, inFile.getAbsolutePath());
            } catch (FitsException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


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

    /**
     * This method is to convert the FIFI-LS data to standard WAVE-TAB format and then the converted FITs is displayed.
     * @param inputFile
     * @return
     * @throws Exception
     */
    private FileAnalysisReport convertFIFIImage(String inputFile) throws Exception {
        
            //create a temp file to save the converted FITs file
            //need to use unique name since the filename as a key stored in the cache
            String[] strAry = inputFile.split("/");
            File outputFile = new File(ServerContext.getUploadDir(), strAry[strAry.length-1]);

            //save the converted FITs to the temp file

            SofiaFitsConverterUtil.doConvertFits(inputFile, outputFile.getAbsolutePath());


            //create a detail report for the newly created FITs file
            FileAnalysisReport report  = (FitsHDUUtil.analyze(outputFile, FileAnalysisReport.ReportType.Details)).getReport();

            List<FileAnalysisReport.Part> parts = report.getParts();
            //assign the convertedFileName so it can be used
            for (int i=0; i<parts.size();i++){
                parts.get(i).setConvertedFileName(ServerContext.replaceWithPrefix(outputFile));
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
    private FileAnalysisReport convertToFrequencyParts(FileAnalysisReport inputReport, String inputFile) throws Exception {

        Fits fits = new Fits(inputFile);
        BasicHDU[] hdus= fits.read();
        String[] strAry = inputFile.split("/");

        FileAnalysisReport retRep = inputReport.copy();
        String fitsFileNameRoot = strAry[strAry.length-1].split(".fits")[0];
        FileAnalysisReport.Part[] parts=retRep.getParts().toArray(new FileAnalysisReport.Part[0]);

        int partIndex=0;
        for (int i=0; i<hdus.length; i++){
            String fileName = fitsFileNameRoot+i;
            partIndex++;
            FileAnalysisReport.Part chartPart = makeChartPart(hdus[i], fileName,i, partIndex);
            if (chartPart!=null) {
                retRep.insertPartAfter(parts[i], chartPart);
                partIndex++;

            }
        }
        return retRep;
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

        freqPart.setDesc("Frequency Table Data in " + "HDU "+ hduIndex);
        freqPart.setFileLocationIndex(0);
        freqPart.setConvertedFileName(ServerContext.replaceWithPrefix(frequencyTable));
        freqPart.setConvertedFileFormat(freqReport.getFormat());
        freqPart.setIndex(partIndex); //set the index to the correct location in the part list
        freqPart.setUiRender(FileAnalysisReport.UIRender.Chart);
        freqPart.setUiEntry(FileAnalysisReport.UIEntry.UseAll);
        freqPart.setChartTableDefOption(FileAnalysisReport.ChartTableDefOption.showChart);
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
            FileAnalysisReport.ChartParams cp = getChartParm(inst, "lines+markers", null);
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
