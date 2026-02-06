/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.fitseval;

import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.ASDFUtil;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.FormatUtil;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadFactory;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Trey Roby
 */
public class FitsEvaluation {

    public interface Eval {
        List<RelatedData> evaluate(File f, FitsRead[] frAry, BasicHDU<?>[] HDUs, int fitsReadIndex, int hduIndex, WebPlotRequest req);
    }

    static final private List<Eval> evalList= Arrays.asList(new MaskEval(), new SpectralCubeEval());

    public static FitsDataEval readAndEvaluate(File f, boolean clearHdu, WebPlotRequest req) throws FitsException, IOException  {
        return FormatUtil.detect(f)==FormatUtil.Format.ASDF
                ? readAndEvaluateAsdfFile(f,clearHdu,req)
                : readAndEvaluateFits(new Fits(f), f, clearHdu, req);
    }

    public static FitsDataEval readAndEvaluateFits(Fits fits, File f, boolean clearHdu, WebPlotRequest req) throws FitsException, IOException {
        FitsReadUtil.UncompressFitsInfo uFitsInfo= null;
        try  {
            BasicHDU<?>[] HDUs= fits.read();
            if (FileUtil.isBz2OrGzipFile(f) || FitsReadUtil.hasCompressedImageHDUS(HDUs)) {
                uFitsInfo = FitsReadUtil.createdUncompressVersionOfFile(HDUs,f);
            }

            File fitsFile= uFitsInfo!=null ? uFitsInfo.file() : f;
            BasicHDU<?>[]  workingHDUS= uFitsInfo!=null ? uFitsInfo.HDUs() : HDUs;
            if (workingHDUS.length==0) throw new FitsException("Bad format in FITS file, no HDUs found");
            FitsRead[] frAry = FitsReadFactory.createFitsReadArray(workingHDUS, f, clearHdu);
            FitsDataEval fitsDataEval= new FitsDataEval(frAry,fitsFile);
            addRelated(fitsFile,workingHDUS,frAry,fitsDataEval,req);
            return fitsDataEval;
        } finally {
            FitsReadUtil.closeFits(fits);
            if (uFitsInfo!=null) FitsReadUtil.closeFits(uFitsInfo.fits());
        }
    }

    public static FitsDataEval readAndEvaluateAsdfFile(File asdfFile, boolean clearHdu, WebPlotRequest req) throws FitsException, IOException {
        Fits fits= null;
        try  {
            File fitsFile= ASDFUtil.createFitsVersionOfRomandAsdfFile(asdfFile);
            if (fitsFile==null) throw new FitsException("Could not convert ASDF file to FITS file");
            fits= new Fits(fitsFile);
            BasicHDU<?>[] HDUs= fits.read();
            if (HDUs.length==0) throw new FitsException("Bad format in FITS file, no HDUs found");
            FitsRead[] frAry = FitsReadFactory.createFitsReadArray(HDUs, fitsFile, clearHdu);
            var fitsDataEval= new FitsDataEval(frAry,fitsFile, FitsDataEval.ImageFileType.ASDF);
            addRelated(fitsFile,HDUs,frAry,fitsDataEval,req);
            return fitsDataEval;
        } finally {
            FitsReadUtil.closeFits(fits);
        }
    }

    public static void addRelated(File fitsFile, BasicHDU<?>[] workingHDUS, FitsRead[] frAry, FitsDataEval fitsDataEval, WebPlotRequest req) {
        if (workingHDUS.length==1) return;
        for(int i= 0; i<frAry.length; i++) {
            if (frAry[i].getPlaneNumber()==0) {
                for (Eval e : evalList) {
                    List<RelatedData> rdList= e.evaluate(fitsFile, frAry, workingHDUS, i, frAry[i].getHduNumber(), req);
                    fitsDataEval.addAllRelatedData(i,rdList);
                }
            }
            else { // if cube, then duplicate related data for the other planes, todo: improve this
                fitsDataEval.addAllRelatedData(i,fitsDataEval.getRelatedData(i-1));
            }
        }
    }
}
