package edu.caltech.ipac.firefly.data.sofia;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.VoTableWriter;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static edu.caltech.ipac.util.FormatUtil.Format.VO_TABLE_TABLEDATA;

public class ExtractSpectraSample {

    static FileInfo getSpectraFileSampleFromURL(String url) throws IOException, FailedRequestException {
//        String url = "https://irsa.ipac.caltech.edu/data/SOFIA/FORCAST/OC4G/20160713_F320/proc/p3374/data/g2/F0320_FO_GRI_9000493_FORG227_MRG_0454.fits"; //mrgspec forcast
        //"https://irsa.ipac.caltech.edu/data/SOFIA/EXES/OC3A/20150305_F198/proc/p956/data/g13/F0198_EX_SPE_0200793_EXEELONEXEECHL_MRD_5131-5134.fits";
        //"https://irsa.ipac.caltech.edu/data/SOFIA/FLITECAM/OC2A/20140225_F148/proc/p322/data/02_0066_Rivkin_Level3/Pallas_FLT_C2_LM_apcombined.fits";
        //"https://irsa.ipac.caltech.edu/data/SOFIA/FORCAST/OC4G/20160718_F323/proc/p3342/data/g1/F0323_FO_GRI_9000492_FORG227_CMB_0312-0326.fits\n";
        //https://irsa.ipac.caltech.edu/data/SOFIA/FORCAST/OC5K/20170926_F433/proc/p4792/data/g2/F0433_FO_GRI_9000644_FORG063_RSP_0105.fits";
        File tempFile = File.createTempFile("Sofia1DSpectraExtractor-", ".fits");
        FileInfo file = URLDownload.getDataToFile(new URL(url), tempFile);
        tempFile.deleteOnExit();
        return file;
    }

    public static void main(String[] args) throws Exception {


        Sofia1DSpectraExtractor ss = new Sofia1DSpectraExtractor(SofiaSpectraModel.SpectraInstrument.FORCAST);

        FileInfo spectraSample = getSpectraFileSampleFromURL("https://irsa.ipac.caltech.edu/data/SOFIA/FORCAST/OC4G/20160713_F320/proc/p3374/data/g2/F0320_FO_GRI_9000493_FORG227_MRG_0454.fits");

        DataGroup dataObjects = ss.extract(spectraSample.getFile());

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

        tempSpectraFile.deleteOnExit();

    }
}
