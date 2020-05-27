package edu.caltech.ipac.firefly.data.sofia;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.io.VoTableWriter;
import edu.caltech.ipac.util.FitsHDUUtil;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadFactory;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.ArrayFuncs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

import static edu.caltech.ipac.table.TableUtil.Format.VO_TABLE_TABLEDATA;

/**
 * Util class to extract 1d spectra from SOFIA FITS file -
 * particular SOFIA instruments and data product levels encoded spectra in an image,
 * each row of the image represents a data vector flux, wavelength, etc.
 * Intruments ("processing, product type")
 * FORCAST (L2 rspscpec, mrgspec, combspec),
 * EXES (L3 mrgordspec, combspec, spec),
 * FLITECAM (L3 combspec, calspec)
 *
 * See processing:
 * https://irsa.ipac.caltech.edu/TAP/sync?QUERY=SELECT+distinct+o.instrument_name,jsonb_extract_path_text(p.provenance_keywords,%27PROCSTAT%27,%270%27)+as+processing+FROM+sofia.observation+o,sofia.plane+p+where+o.obsid=p.obsid&format=IPAC_TABLE
 * See product type:
 * https://irsa.ipac.caltech.edu/TAP/sync?QUERY=SELECT+distinct+o.instrument_name,jsonb_extract_path_text(p.provenance_keywords,%27PRODTYPE%27,%270%27)+as+product_type+FROM+sofia.observation+o,sofia.plane+p+where+o.obsid=p.obsid&format=IPAC_TABLE
 */
public class Sofia1DSpectraExtractor extends DataExtractUtil {

    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private final SofiaSpectraModel model;
    private final SofiaSpectraModel.SpectraInstrument instrument;
//    public static DataGroup extract1DSpectra(File sourceFile, VOSpectraModel model) throws FitsException, IOException {
////        List<DataType> meta = model.getMeta();//FIELDs
//        String[] meta = null;
//        return FITSTableReader.convertFitsToDataGroup(sourceFile.getAbsolutePath(),new String[]{"Flux", "Wavelength"}, meta, FITSTableReader.DEFAULT,0);
//    }

    public Sofia1DSpectraExtractor(SofiaSpectraModel.SpectraInstrument inst){
        this.model = new SofiaSpectraModel(inst);
        this.instrument = inst;
    }
    /**
     * Extract table flux,wavelength and other dataset into class Spectra1D
     * #   Confirm that the data HDU shape is as expected.  This kind of file has four stripes of data:
     * #     0: wavenumber
     * #     1: flux per wavenumber bin
     * #     2: flux error
     * #     3: reference spectrum
     * 4...
     * most likely file is a cache of the reference/source data (FITS)
     */
    private DataGroup extract1DSpectra(File file) throws FitsException {
        Fits fit = new Fits(file);
        FitsRead[] fitsReadArray = FitsReadFactory.createFitsReadArray(fit);
        int hdu = fitsReadArray.length;
        if (hdu > 1)
            throw new FitsException("Can't extract SOFIA spectra: number of HDU is " + hdu);
        BasicHDU p = fitsReadArray[0].getHDU();

        int axis[] = new int[]{0, 0};
        String xunit = null, yunit = null;
        Header hdr = p.getHeader();

        String xu = hdr.getStringValue("XUNITS");
        String yu = hdr.getStringValue("YUNITS");

        xunit = xu != null ? xu : "";
        yunit = yu != null ? yu : "";

        // Populate units, ucds, utype?
        model.setUnits(VOSpectraModel.SPECTRA_FIELDS.FLUX, yu);
        if(model.getMeta().get(VOSpectraModel.SPECTRA_FIELDS.WAVENUMBER.getKey())!=null) {
            model.setUnits(VOSpectraModel.SPECTRA_FIELDS.WAVENUMBER, xu);//TODO where is the unit, no idea.Ask scientist/PI
        }
        if(model.getMeta().get(VOSpectraModel.SPECTRA_FIELDS.WAVELENGTH.getKey())!=null) {
            model.setUnits(VOSpectraModel.SPECTRA_FIELDS.WAVELENGTH, xu);
        }
        model.setUnits(VOSpectraModel.SPECTRA_FIELDS.ERROR_FLUX, yu);
        if(model.getMeta().get(VOSpectraModel.SPECTRA_FIELDS.ATMOS_TRANSMISSION.getKey())!=null) {
            model.setUnits(VOSpectraModel.SPECTRA_FIELDS.ATMOS_TRANSMISSION, "");//TODO where is the unit, no idea.Ask scientist/PI
        }
        if(model.getMeta().get(VOSpectraModel.SPECTRA_FIELDS.INST_RESP_CURVE.getKey())!=null) {
            model.setUnits(VOSpectraModel.SPECTRA_FIELDS.INST_RESP_CURVE, "Me/s/Jy");//TODO where is the unit, no idea.Ask scientist/PI
        }
        axis[0] = hdr.getIntValue("NAXIS1");
        axis[1] = hdr.getIntValue("NAXIS2");

        //From the defined model above, iterate and populate a Datagroup witht the datatype derived from the model
        ArrayList<DataType> dt = new ArrayList<DataType>(model.getMeta().values());
        DataGroup dataGroup = new DataGroup(hdr.getStringValue("FILENAME"), dt);
        DataType[] dd;

        double[][] fdata = (double[][]) ArrayFuncs.convertArray(p.getKernel(), Double.TYPE, true);
        for (int row = 0; row < fdata[0].length; row++) {
            DataObject aRow = new DataObject(dataGroup);
            dd = dt.toArray(new DataType[dt.size()]);
            for (int dtIdx = 0; dtIdx < fdata.length; dtIdx++) {
                aRow.setDataElement(dd[dtIdx], fdata[dtIdx][row]);
            }
            dataGroup.add(aRow);
        }

        dataGroup.trimToSize();
        return dataGroup;

        //return new SofiaSpectra(new SofiaSpectra.Axis(fdata[0], xunit), new SofiaSpectra.Axis(fdata[1], yunit));
    }

    public boolean isSpectra(String key, String value) {
        return key.equals("NAXIS") && Integer.parseInt(value) == 2;
    }

    static boolean is64bits(Header hdr, String value) {
        int val = hdr.getIntValue(value);
        if (val == -64) {
            return true;
        } else {
            return false;
        }
    }

    public static FileInfo exportDg(DataGroup dg, File file) throws IOException {

        OutputStream out = new FileOutputStream(file, false);
        dataGroup2VOTable(dg, out);
        return new FileInfo(file);
    }
    public static void dataGroup2VOTable(DataGroup dg, OutputStream out) throws IOException {
        VoTableWriter.save(out, dg, VO_TABLE_TABLEDATA);
    }

    public static void main(String[] args) throws Exception {
        String url = "https://irsa.ipac.caltech.edu/data/SOFIA/FORCAST/OC4G/20160713_F320/proc/p3374/data/g2/F0320_FO_GRI_9000493_FORG227_MRG_0454.fits"; //mrgspec forcast
                //"https://irsa.ipac.caltech.edu/data/SOFIA/EXES/OC3A/20150305_F198/proc/p956/data/g13/F0198_EX_SPE_0200793_EXEELONEXEECHL_MRD_5131-5134.fits";
        //"https://irsa.ipac.caltech.edu/data/SOFIA/FLITECAM/OC2A/20140225_F148/proc/p322/data/02_0066_Rivkin_Level3/Pallas_FLT_C2_LM_apcombined.fits";
        //"https://irsa.ipac.caltech.edu/data/SOFIA/FORCAST/OC4G/20160718_F323/proc/p3342/data/g1/F0323_FO_GRI_9000492_FORG227_CMB_0312-0326.fits\n";
        //https://irsa.ipac.caltech.edu/data/SOFIA/FORCAST/OC5K/20170926_F433/proc/p4792/data/g2/F0433_FO_GRI_9000644_FORG063_RSP_0105.fits";

        Sofia1DSpectraExtractor ss = new Sofia1DSpectraExtractor(SofiaSpectraModel.SpectraInstrument.FORCAST);

        File tempFile = File.createTempFile("Sofia1DSpectraExtractor-", ".fits");
        FileInfo file = URLDownload.getDataToFile(new URL(url), tempFile);
        tempFile.deleteOnExit();
        FitsHDUUtil.FitsAnalysisReport retRep = FitsHDUUtil.analyze(tempFile, FileAnalysisReport.ReportType.Details);
        DataGroup dg = retRep.getReport().getPart(0).getDetails();
        String spectraName = "Extracted Spectra";
        for (int i = 0; i <dg.size();i++) {
            String key = (String) dg.getData("key", i);
            if(key.equals("FILENAME")){
                String tmpName = (String) dg.getData("value", i);
                spectraName = tmpName.substring(0,tmpName.lastIndexOf("."));
                break;
            }
        }
//        FileAnalysis.Report analyze = FitsHDUUtil.analyze(file.getFile(), FileAnalysis.ReportType.Details);
//        BasicHDU[] parts = new Fits(file.getFile()).read();

        DataGroup headerDg = FitsHDUUtil.fitsHeaderToDataGroup(tempFile.getAbsolutePath());

//        SofiaSpectra s = extract1DSpectra(file.getFile());
        DataGroup dataObjects = ss.extract(file.getFile());

//        Iterator<DataObject> iterator = dataObjects.iterator();
//        while(iterator.hasNext()){
//            DataObject next = iterator.next();
//            Object[] data = next.getData();
//            for (int i = 0; i < data.length ; i++) {
//                System.out.println(data[i].toString());
//            }
//        }
        File tempSpectraFile = File.createTempFile("Sofia1DSpectraExtractor-", ".xml");
        VoTableWriter.save(tempSpectraFile, dataObjects, VO_TABLE_TABLEDATA);
        System.out.println("Wrote VOTable xml here:" + tempSpectraFile.getAbsolutePath());


//        int axis[] = new int[]{0, 0};
//        String xunit, yunit;
//        double[] flux, wave;
//        for (BasicHDU p : parts) {
//            System.out.println(p.getHeader());
//            xunit = p.getHeader().getStringValue("XUNIT");
//            axis[0] = p.getHeader().getIntValue("NAXIS1");
//            axis[1] = p.getHeader().getIntValue("NAXIS2");
//            xunit = p.getHeader().getStringValue("XUNIT");
//            yunit = p.getHeader().getStringValue("YUNIT");
//            double[][] fdata = (double[][]) p.getKernel();
//            flux = fdata[0];
//            wave = fdata[1];
//            s = SofiaSpectra.makeSpectra(fdata, new String[]{xunit, yunit});
//        }


//        BasicHDU[] parts = new Fits(file.getFile()).read();
//
//        FitsHDUUtil.fitsHeaderToDataGroup(file.getFile());

    }

    @Override
    public DataGroup extract(File inf) throws Exception {
        return extract1DSpectra(inf);
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
 * HOWEVER USED.
 *
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 *
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
 * OF THE SOFTWARE.
 */
