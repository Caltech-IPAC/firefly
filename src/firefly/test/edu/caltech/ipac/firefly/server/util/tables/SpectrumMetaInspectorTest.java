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
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.GroupInfo;
import edu.caltech.ipac.table.io.FITSTableReader;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.SpectrumMetaInspector;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.StringUtils;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static edu.caltech.ipac.firefly.TestUtil.getDataFile;
import static edu.caltech.ipac.table.io.SpectrumMetaInspector.SPEC_FL_AXIS;
import static edu.caltech.ipac.table.io.SpectrumMetaInspector.SPEC_SPECT_AXIS;
import static edu.caltech.ipac.table.io.SpectrumMetaInspector.VALUE;

/**
 * @author Trey Roby
 */
public class SpectrumMetaInspectorTest extends ConfigTest {
    
    @Before
    public void setUp() {
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
        readTableGuessSpec(getDataFile("spectra/2MASS_J12073346_3932539_b_3.10912_5457_1.tbl"));
        readTableGuessSpec(getDataFile("spectra/51_Eri_b_3.11801_3607_3.tbl"));
        readTableGuessSpec(getDataFile("spectra/55_Cnc_e_3.10924_3673_1.tbl"));
        readTableGuessSpec(getDataFile("spectra/55_Cnc_e_3.10924_3750_1.tbl"));
        readTableGuessSpec(getDataFile("spectra/CoRoT_1_b_3.10951_2058_1.tbl"));
        readTableGuessSpec(getDataFile("spectra/CoRoT_2_b_3.10954_3283_1.tbl"));
        readTableGuessSpec(getDataFile("spectra/CoRoT_2_b_3.10954_4094_1.tbl"));
        readTableGuessSpec(getDataFile("spectra/Kepler_20_c_3.101_3665_1.tbl"));
        readTableGuessSpec(getDataFile("spectra/Kepler_20_c_3.101_3665_2.tbl"));
        readTableGuessSpec(getDataFile("spectra/BAD-ipac-table-HOPS-103_Spitzer-IRS_spectrum.tbl.txt"));
        readTableGuessSpec(getDataFile("spectra/IRS-IRSX-no-units.tbl"));
        readVOTableWithSpecDefined(getDataFile("spectra/1342235804_averageSpectrumWML_7b_1897_2016-03-10T18-46-38UTC.xml"));
        readVOTableWithSpecDefined(getDataFile("spectra/SPITZER_S0_25343744_0001_3_E7173899_tune.votable.xml"));
        readVOTableWithSpecDefined(getDataFile("spectra/SPITZER_S0_25343744_0001_3_E7173899_tune.xml"));
        readVOTableWithSpecDefined(getDataFile("spectra/F0291_EX_SPE_04012012_EXEELONEXEECHL_CMB_0040-0043.votable"));
        readVOTableWithSpecDefined(getDataFile("spectra/HD_143006.votable"));
    }

    public static void readFITSWithSpecDefined(File f) throws IOException {
        BasicHDU<?>[] hdus;
        try (Fits fits = new Fits(f)) {
            hdus = fits.read();
            DataGroup dg= FITSTableReader.readFitsTable(f.getPath(), null, 0);
            SpectrumMetaInspector.searchForSpectrum(dg,hdus[0],false);
            hasSpecInfo(dg);
        }
    }

    public static void readTableGuessSpec(File f) throws IOException {
        DataGroup dg= IpacTableReader.read(f, (TableServerRequest) null);
        SpectrumMetaInspector.searchForSpectrum(dg,null,true);
        hasSpecInfo(dg);
    }

    public static void readVOTableWithSpecDefined(File f) throws IOException {
        DataGroup dg= VoTableReader.voToDataGroups(f.getPath())[0];
        SpectrumMetaInspector.searchForSpectrum(dg,false);
        hasSpecInfo(dg);
    }

    public static void hasSpecInfo(DataGroup dg) {
        Assert.assertTrue(
                dg.getGroupInfos().stream().anyMatch(g -> g.getName().equals("spec:Data.SpectralAxis"))
        );
        Assert.assertTrue(
                dg.getGroupInfos().stream().anyMatch(g -> g.getName().equals("spec:Data.FluxAxis"))
        );
        hasUnits(dg, SPEC_SPECT_AXIS+VALUE);
        hasUnits(dg, SPEC_FL_AXIS+VALUE);
    }

    public static void hasUnits(DataGroup dg, String utype) {
        GroupInfo.RefInfo ref= dg.getGroupInfos().stream()
                .flatMap(g -> g.getColumnRefs().stream())
                .filter(r -> utype.equals(r.getUtype()))
                .findFirst().orElse(null);
        Assert.assertNotNull("no column ref with utype "+utype, ref);
        DataType dt= dg.getDataDefintion(ref.getRef());
        Assert.assertNotNull("column not found: "+ref.getRef(), dt);
        Assert.assertFalse("no units for column "+ref.getRef(), StringUtils.isEmpty(dt.getUnits()));
    }
}
