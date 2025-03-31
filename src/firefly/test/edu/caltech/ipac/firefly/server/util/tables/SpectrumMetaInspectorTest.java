package edu.caltech.ipac.firefly.server.util.tables;
/**
 * User: roby
 * Date: 3/28/25
 * Time: 10:02 AM
 */


import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.FITSTableReader;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.SpectrumMetaInspector;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static edu.caltech.ipac.firefly.TestUtil.getDataFile;

/**
 * @author Trey Roby
 */
public class SpectrumMetaInspectorTest extends ConfigTest {
    
    @Before
    public void setUp() {
        setupServerContext(null);
        if (false) Logger.setLogLevel(Level.TRACE);			// for debugging.
    }

    @Test
    public void readSpec() throws IOException {
        readFITSWithSpecDefined(getDataFile("spectra/UTYP_utyps_spec_Data.fits"));
        readFITSWithSpecDefined(getDataFile("spectra/UTYP_utyps_spec_Spec.fits"));
        readFITSWithSpecDefined(getDataFile("spectra/VOC_UTYP_utyps_spec_Data.fits"));
        readFITSWithSpecDefined(getDataFile("spectra/VOC_UTYP_utyps_spec_Spec.fits"));
        readFITSWithSpecDefined(getDataFile("spectra/VOC_utyps_spec_Spec.fits"));
        readFITSWithSpecDefined(getDataFile("spectra/WORKS_VOC_utyps_spec_Data.fits"));
        readTableGuessSpec(getDataFile("spectra/r24191232_ch0.tbl"));
        readTableGuessSpec(getDataFile("spectra/1RXJS_J161410.6-230542_SH.tbl"));
    }

    public static void readFITSWithSpecDefined(File f) throws IOException {
        BasicHDU<?>[] hdus;
        try (Fits fits = new Fits(f)) {
            hdus = fits.read();
            DataGroup dg= FITSTableReader.convertFitsToDataGroup(f.getPath(), null, FITSTableReader.DEFAULT, 0);
            SpectrumMetaInspector.searchForSpectrum(dg,hdus[0],false);
            hasSpecInfo(dg);
        }
    }

    public static void readTableGuessSpec(File f) throws IOException {
        DataGroup dg= IpacTableReader.read(f, (TableServerRequest) null);
        SpectrumMetaInspector.searchForSpectrum(dg,null,true);
        hasSpecInfo(dg);
    }

    public static void hasSpecInfo(DataGroup dg) {
        Assert.assertTrue(
                dg.getGroupInfos().stream().anyMatch(g -> g.getName().equals("spec:Data.SpectralAxis"))
        );
        Assert.assertTrue(
                dg.getGroupInfos().stream().anyMatch(g -> g.getName().equals("spec:Data.FluxAxis"))
        );
    }
}
