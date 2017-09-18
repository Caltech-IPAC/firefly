/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.imagesources;
/**
 * User: roby
 * Date: 9/20/17
 * Time: 12:42 PM
 */


import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class ExternalMasterDataSource implements ImageMasterDataSourceType {

    @Override
    public List<ImageMasterDataEntry> getImageMasterData() {

        List<ImageMasterDataEntry> retval= new ArrayList<>();

        retval.addAll(sdss());
        retval.addAll(msx());
        retval.addAll(dss());

        return retval;
    }

    static private List<ImageMasterDataEntry> sdss() {
        JSONObject params;


        ImageMasterDataEntry u= sdssTemplate();
        u.setWavelength("0");
        u.setWavelengthDesc("0 micron");
        u.setTitle("u");
        u.setTooltip("This is SDSS u");
        u.setImageId("sdss-u");
        params= new JSONObject();
        params.put("Service", "SDSS");
        params.put("SurveyKey", "u");
        params.put("title", "SDSS u");
        params.put("drawingSubgroupID", "sdss");
        u.setPlotRequestParams(params);



        ImageMasterDataEntry g= sdssTemplate();
        g.setWavelength("0");
        g.setWavelengthDesc("0 micron");
        g.setTitle("g");
        g.setTooltip("This is SDSS g");
        g.setImageId("sdss-g");
        params= new JSONObject();
        params.put("Service", "SDSS");
        params.put("SurveyKey", "g");
        params.put("title", "SDSS g");
        params.put("drawingSubgroupID", "sdss");
        g.setPlotRequestParams(params);

        ImageMasterDataEntry r= sdssTemplate();
        r.setWavelength("0");
        r.setWavelengthDesc("0 micron");
        r.setTitle("r");
        r.setTooltip("This is SDSS r");
        r.setImageId("sdss-r");
        params= new JSONObject();
        params.put("Service", "SDSS");
        params.put("SurveyKey", "r");
        params.put("title", "SDSS r");
        params.put("drawingSubgroupID", "sdss");
        r.setPlotRequestParams(params);

        ImageMasterDataEntry i= sdssTemplate();
        i.setWavelength("0");
        i.setWavelengthDesc("0 micron");
        i.setTitle("i");
        i.setTooltip("This is SDSS i");
        i.setImageId("sdss-i");
        params= new JSONObject();
        params.put("Service", "SDSS");
        params.put("SurveyKey", "i");
        params.put("title", "SDSS i");
        params.put("drawingSubgroupID", "sdss");
        i.setPlotRequestParams(params);

        ImageMasterDataEntry z= sdssTemplate();
        z.setWavelength("0");
        z.setWavelengthDesc("0 micron");
        z.setTitle("z");
        z.setTooltip("This is SDSS Z");
        z.setImageId("sdss-z");
        params= new JSONObject();
        params.put("Service", "SDSS");
        params.put("SurveyKey", "z");
        params.put("title", "SDSS z");
        params.put("drawingSubgroupID", "sdss");
        z.setPlotRequestParams(params);


        List<ImageMasterDataEntry> retval= new ArrayList<>();
        retval.add(u);
        retval.add(g);
        retval.add(r);
        retval.add(i);
        retval.add(z);
        return retval;
    }


    static private List<ImageMasterDataEntry> msx() {
        JSONObject params;


        ImageMasterDataEntry a = msxTemplate();
        a.setWavelength("8.28");
        a.setWavelengthDesc("8.28 micron");
        a.setTitle("A (8.28 microns)");
        a.setTooltip("This is MSX A");
        a.setImageId("MSXA3");
        params= new JSONObject();
        params.put("Service", "MSX");
        params.put("SurveyKey", "3");
        params.put("title", "MSX A");
        params.put("drawingSubgroupID", "msx");
        a.setPlotRequestParams(params);


        ImageMasterDataEntry c = msxTemplate();
        c.setWavelength("12.13");
        c.setWavelengthDesc("12.13 micron");
        c.setTitle("C (12.13 microns)");
        c.setTooltip("This is MSX C");
        c.setImageId("MSXC4");
        params= new JSONObject();
        params.put("Service", "MSX");
        params.put("SurveyKey", "4");
        params.put("title", "MSX C");
        params.put("drawingSubgroupID", "msx");
        c.setPlotRequestParams(params);

        ImageMasterDataEntry d = msxTemplate();
        d.setWavelength("14.65");
        d.setWavelengthDesc("14.65 micron");
        d.setTitle("D (14.65 microns)");
        d.setTooltip("This is MSX D");
        d.setImageId("MSXD5");
        params= new JSONObject();
        params.put("Service", "MSX");
        params.put("SurveyKey", "5");
        params.put("title", "MSX D");
        params.put("drawingSubgroupID", "msx");
        d.setPlotRequestParams(params);


        ImageMasterDataEntry e = msxTemplate();
        e.setWavelength("21.3");
        e.setWavelengthDesc("21.3 micron");
        e.setTitle("E (21.3 microns)");
        e.setTooltip("This is MSX E");
        e.setImageId("MSXE6");
        params= new JSONObject();
        params.put("Service", "MSX");
        params.put("SurveyKey", "6");
        params.put("title", "MSX E");
        params.put("drawingSubgroupID", "msx");
        e.setPlotRequestParams(params);


        List<ImageMasterDataEntry> retval= new ArrayList<>();
        retval.add(a);
        retval.add(c);
        retval.add(d);
        retval.add(e);
        return retval;
    }

    static private List<ImageMasterDataEntry> dss() {
        JSONObject params;


        ImageMasterDataEntry red = dssTemplate();
        red.setWavelength("0");
        red.setWavelengthDesc("0 micron");
        red.setTitle("POSS2/UKSTU Red");
        red.setTooltip("This is DSS poss2ukstu_red");
        red.setImageId("dss-poss2ukstu_red");
        params= new JSONObject();
        params.put("Service", "DSS");
        params.put("SurveyKey", "poss2ukstu_red");
        params.put("title", "DSS poss2ukstu_red");
        params.put("drawingSubgroupID", "dss");
        red.setPlotRequestParams(params);

        ImageMasterDataEntry ir = dssTemplate();
        ir.setWavelength("0");
        ir.setWavelengthDesc("0 micron");
        ir.setTitle("POSS2/UKSTU Infrared");
        ir.setTooltip("This is DSS poss2ukstu_ir");
        ir.setImageId("dss-poss2ukstu_ir");
        params= new JSONObject();
        params.put("Service", "DSS");
        params.put("SurveyKey", "poss2ukstu_ir");
        params.put("title", "DSS poss2ukstu_ir");
        params.put("drawingSubgroupID", "dss");
        ir.setPlotRequestParams(params);

        ImageMasterDataEntry blue = dssTemplate();
        blue.setWavelength("0");
        blue.setWavelengthDesc("0 micron");
        blue.setTitle("POSS2/UKSTU Blue");
        blue.setTooltip("This is DSS poss2ukstu_blue");
        blue.setImageId("dss-poss2ukstu_blue");
        params= new JSONObject();
        params.put("Service", "DSS");
        params.put("SurveyKey", "poss2ukstu_blue");
        params.put("title", "DSS poss2ukstu_blue");
        params.put("drawingSubgroupID", "dss");
        blue.setPlotRequestParams(params);

        ImageMasterDataEntry pred = dssTemplate();
        pred.setWavelength("0");
        pred.setWavelengthDesc("0 micron");
        pred.setTitle("POSS1 Red");
        pred.setTooltip("This is DSS poss1_red");
        pred.setImageId("dss-poss1_red");
        params= new JSONObject();
        params.put("Service", "DSS");
        params.put("SurveyKey", "poss1_red");
        params.put("title", "DSS poss1_red");
        params.put("drawingSubgroupID", "dss");
        pred.setPlotRequestParams(params);

        ImageMasterDataEntry pblue = dssTemplate();
        pblue.setWavelength("0");
        pblue.setWavelengthDesc("0 micron");
        pblue.setTitle("POSS1 Blue");
        pblue.setTooltip("This is DSS poss1_blue");
        pblue.setImageId("dss-poss1_blue");
        params= new JSONObject();
        params.put("Service", "DSS");
        params.put("SurveyKey", "poss1_blue");
        params.put("title", "DSS poss1_blue");
        params.put("drawingSubgroupID", "dss");
        pblue.setPlotRequestParams(params);

        ImageMasterDataEntry quick = dssTemplate();
        quick.setWavelength("0");
        quick.setWavelengthDesc("0 micron");
        quick.setTitle("Quick-V Survey");
        quick.setTooltip("This is DSS quickv");
        quick.setImageId("dss-quickv");
        params= new JSONObject();
        params.put("Service", "DSS");
        params.put("SurveyKey", "quickv");
        params.put("title", "DSS quickv");
        params.put("drawingSubgroupID", "dss");
        quick.setPlotRequestParams(params);

        ImageMasterDataEntry phase2 = dssTemplate();
        phase2.setWavelength("0");
        phase2.setWavelengthDesc("0 micron");
        phase2.setTitle("HST Phase 2 (GSC 2)");
        phase2.setTooltip("This is DSS phase2_gsc2");
        phase2.setImageId("dss-phase2_gsc2");
        params= new JSONObject();
        params.put("Service", "DSS");
        params.put("SurveyKey", "phase2_gsc2");
        params.put("title", "DSS phase2_gsc2");
        params.put("drawingSubgroupID", "dss");
        phase2.setPlotRequestParams(params);

        ImageMasterDataEntry phase1 = dssTemplate();
        phase1.setWavelength("0");
        phase1.setWavelengthDesc("0 micron");
        phase1.setTitle("HST Phase 1 (GSC 1)");
        phase1.setTooltip("This is DSS phase2_gsc1");
        phase1.setImageId("dss-phase1_gsc1");
        params= new JSONObject();
        params.put("Service", "DSS");
        params.put("SurveyKey", "phase2_gsc1");
        params.put("title", "DSS phase2_gsc1");
        params.put("drawingSubgroupID", "dss");
        phase1.setPlotRequestParams(params);


        List<ImageMasterDataEntry> retval= new ArrayList<>();
        retval.add(red);
        retval.add(ir);
        retval.add(blue);
        retval.add(pred);
        retval.add(pblue);
        retval.add(quick);
        retval.add(phase2);
        retval.add(phase1);
        return retval;

    }



    static private ImageMasterDataEntry sdssTemplate() {
        ImageMasterDataEntry e= new ImageMasterDataEntry();
        e.setProject("SDSS");
        e.setSubProject("");
        e.setHelpUrl("http://irsa.ipac.caltech.edu/stuff");
        e.setProjectTypeKey("galactic");
        e.setProjectTypeDesc("Galactic");
        e.setMinRangeDeg("0.016");
        e.setMaxRangeDeg("0.5");
        return e;
    }

    static private ImageMasterDataEntry msxTemplate() {
        ImageMasterDataEntry e= new ImageMasterDataEntry();
        e.setProject("MSX");
        e.setSubProject("");
        e.setHelpUrl("http://irsa.ipac.caltech.edu/stuff");
        e.setProjectTypeKey("galactic");
        e.setProjectTypeDesc("Galactic");
        e.setMinRangeDeg("0.01");
        e.setMaxRangeDeg("1.5");
        return e;
    }


    static private ImageMasterDataEntry dssTemplate() {
        ImageMasterDataEntry e= new ImageMasterDataEntry();
        e.setProject("DSS");
        e.setSubProject("");
        e.setHelpUrl("http://irsa.ipac.caltech.edu/stuff");
        e.setProjectTypeKey("galactic");
        e.setProjectTypeDesc("Galactic");
        e.setMinRangeDeg("0.016");
        e.setMaxRangeDeg(".5");
        return e;
    }




}
