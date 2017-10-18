/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.imagesources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String,String> params;


        ImageMasterDataEntry u= sdssTemplate();
        u.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "0.2910");
        u.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"0.2910 micron");
        u.set(ImageMasterDataEntry.PARAMS.TITLE,"u");
        u.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is SDSS u");
        u.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"sdss-u");
        params= new HashMap<>();
        params.put("Service", "SDSS");
        params.put("SurveyKey", "u");
        params.put("title", "SDSS u");
        params.put("drawingSubgroupID", "sdss");
        u.setPlotRequestParams(params);



        ImageMasterDataEntry g= sdssTemplate();
        g.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "0.4810");
        g.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"0.4810 micron");
        g.set(ImageMasterDataEntry.PARAMS.TITLE,"g");
        g.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is SDSS g");
        g.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"sdss-g");

        params= new HashMap<>();
        params.put("Service", "SDSS");
        params.put("SurveyKey", "g");
        params.put("title", "SDSS g");
        params.put("drawingSubgroupID", "sdss");
        g.setPlotRequestParams(params);

        ImageMasterDataEntry r= sdssTemplate();
        r.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "0.6230");
        r.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"0.6230 micron");
        r.set(ImageMasterDataEntry.PARAMS.TITLE,"r");
        r.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is SDSS r");
        r.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"sdss-r");

        params= new HashMap<>();
        params.put("Service", "SDSS");
        params.put("SurveyKey", "r");
        params.put("title", "SDSS r");
        params.put("drawingSubgroupID", "sdss");
        r.setPlotRequestParams(params);

        ImageMasterDataEntry i= sdssTemplate();
        i.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "0.7640");
        i.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"0.7640 micron");
        i.set(ImageMasterDataEntry.PARAMS.TITLE,"i");
        i.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is SDSS i");
        i.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"sdss-i");

        params= new HashMap<>();
        params.put("Service", "SDSS");
        params.put("SurveyKey", "i");
        params.put("title", "SDSS i");
        params.put("drawingSubgroupID", "sdss");
        i.setPlotRequestParams(params);

        ImageMasterDataEntry z= sdssTemplate();
        z.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "0.9060");
        z.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"0.9060 micron");
        z.set(ImageMasterDataEntry.PARAMS.TITLE,"z");
        z.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is SDSS z");
        z.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"sdss-z");

        params= new HashMap<>();
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
        Map<String,String> params;


        ImageMasterDataEntry a = msxTemplate();
        a.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "8.28");
        a.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"8.28 micron");
        a.set(ImageMasterDataEntry.PARAMS.TITLE,"A (8.28 microns)");
        a.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is MSX A");
        a.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"MSXA3");

        params= new HashMap<>();
        params.put("Service", "MSX");
        params.put("SurveyKey", "3");
        params.put("title", "MSX A");
        params.put("drawingSubgroupID", "msx");
        a.setPlotRequestParams(params);


        ImageMasterDataEntry c = msxTemplate();
        c.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "12.13");
        c.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"12.13 micron");
        c.set(ImageMasterDataEntry.PARAMS.TITLE,"C (12.13 microns)");
        c.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is MSX C");
        c.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"MSXC4");

        params= new HashMap<>();
        params.put("Service", "MSX");
        params.put("SurveyKey", "4");
        params.put("title", "MSX C");
        params.put("drawingSubgroupID", "msx");
        c.setPlotRequestParams(params);

        ImageMasterDataEntry d = msxTemplate();
        d.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "14.65");
        d.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"14.65 micron");
        d.set(ImageMasterDataEntry.PARAMS.TITLE,"D (14.65 microns)");
        d.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is MSX D");
        d.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"MSXD5");

        params= new HashMap<>();
        params.put("Service", "MSX");
        params.put("SurveyKey", "5");
        params.put("title", "MSX D");
        params.put("drawingSubgroupID", "msx");
        d.setPlotRequestParams(params);


        ImageMasterDataEntry e = msxTemplate();
        e.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "21.3");
        e.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"21.3 micron");
        e.set(ImageMasterDataEntry.PARAMS.TITLE,"E (21.3 microns)");
        e.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is MSX E");
        e.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"MSXE6");

        params= new HashMap<>();
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
        Map<String,String> params;


        ImageMasterDataEntry red = dssTemplate();
        red.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "0.65");
        red.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"0.65 micron");
        red.set(ImageMasterDataEntry.PARAMS.TITLE,"POSS2/UKSTU Red");
        red.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is DSS poss2ukstu_red");
        red.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"dss-poss2ukstu_red");


        params= new HashMap<>();
        params.put("Service", "DSS");
        params.put("SurveyKey", "poss2ukstu_red");
        params.put("title", "DSS poss2ukstu_red");
        params.put("drawingSubgroupID", "dss");
        red.setPlotRequestParams(params);

        ImageMasterDataEntry ir = dssTemplate();
        ir.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "0.85");
        ir.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"0.85 micron");
        ir.set(ImageMasterDataEntry.PARAMS.TITLE,"POSS2/UKSTU Infrared");
        ir.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is DSS poss2ukstu_ir");
        ir.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"dss-poss2ukstu_ir");

        params= new HashMap<>();
        params.put("Service", "DSS");
        params.put("SurveyKey", "poss2ukstu_ir");
        params.put("title", "DSS poss2ukstu_ir");
        params.put("drawingSubgroupID", "dss");
        ir.setPlotRequestParams(params);

        ImageMasterDataEntry blue = dssTemplate();
        blue.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "0.41");
        blue.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"0.41 micron");
        blue.set(ImageMasterDataEntry.PARAMS.TITLE,"POSS2/UKSTU Blue");
        blue.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is DSS poss2ukstu_blue");
        blue.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"dss-poss2ukstu_blue");

        params= new HashMap<>();
        params.put("Service", "DSS");
        params.put("SurveyKey", "poss2ukstu_blue");
        params.put("title", "DSS poss2ukstu_blue");
        params.put("drawingSubgroupID", "dss");
        blue.setPlotRequestParams(params);

        ImageMasterDataEntry pred = dssTemplate();
        pred.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "0.65");
        pred.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"0.65 micron");
        pred.set(ImageMasterDataEntry.PARAMS.TITLE,"POSS1 Red");
        pred.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is DSS poss1_red");
        pred.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"dss-poss1_red");

        params= new HashMap<>();
        params.put("Service", "DSS");
        params.put("SurveyKey", "poss1_red");
        params.put("title", "DSS poss1_red");
        params.put("drawingSubgroupID", "dss");
        pred.setPlotRequestParams(params);

        ImageMasterDataEntry pblue = dssTemplate();
        pblue.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "0.48");
        pblue.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"0.48 micron");
        pblue.set(ImageMasterDataEntry.PARAMS.TITLE,"POSS1 Blue");
        pblue.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is DSS poss1_blue");
        pblue.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"dss-poss1_blue");

        params= new HashMap<>();
        params.put("Service", "DSS");
        params.put("SurveyKey", "poss1_blue");
        params.put("title", "DSS poss1_blue");
        params.put("drawingSubgroupID", "dss");
        pblue.setPlotRequestParams(params);

        ImageMasterDataEntry quick = dssTemplate();
        quick.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "0");
        quick.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"0 micron");
        quick.set(ImageMasterDataEntry.PARAMS.TITLE,"Quick-V Survey");
        quick.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is DSS quickv");
        quick.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"dss-quickv");

        params= new HashMap<>();
        params.put("Service", "DSS");
        params.put("SurveyKey", "quickv");
        params.put("title", "DSS quickv");
        params.put("drawingSubgroupID", "dss");
        quick.setPlotRequestParams(params);

        ImageMasterDataEntry phase2 = dssTemplate();
        phase2.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "0");
        phase2.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"0 micron");
        phase2.set(ImageMasterDataEntry.PARAMS.TITLE,"HST Phase 2 (GSC 2)");
        phase2.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is DSS phase2_gsc2");
        phase2.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"dss-phase2_gsc2");


        params= new HashMap<>();
        params.put("Service", "DSS");
        params.put("SurveyKey", "phase2_gsc2");
        params.put("title", "DSS phase2_gsc2");
        params.put("drawingSubgroupID", "dss");
        phase2.setPlotRequestParams(params);

        ImageMasterDataEntry phase1 = dssTemplate();
        phase1.set(ImageMasterDataEntry.PARAMS.WAVELENGTH, "0");
        phase1.set(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC,"0 micron");
        phase1.set(ImageMasterDataEntry.PARAMS.TITLE,"HST Phase 1 (GSC 1)");
        phase1.set(ImageMasterDataEntry.PARAMS.TOOL_TIP,"This is DSS phase1_gsc1");
        phase1.set(ImageMasterDataEntry.PARAMS.IMAGE_ID,"dss-phase1_gsc1");


        params= new HashMap<>();
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

        e.set(ImageMasterDataEntry.PARAMS.PROJECT,"SDSS");
        e.set(ImageMasterDataEntry.PARAMS.MISSION_ID,"sdss");
        e.set(ImageMasterDataEntry.PARAMS.SUB_PROJECT,"");
        e.set(ImageMasterDataEntry.PARAMS.HELP_URL,"http://irsa.ipac.caltech.edu/stuff");
        e.set(ImageMasterDataEntry.PARAMS.PROJECT_TYPE_KEY,"galactic");
        e.set(ImageMasterDataEntry.PARAMS.PROJECT_TYPE_DESC,"Galactic");
        e.set(ImageMasterDataEntry.PARAMS.MIN_RANGE_DEG,"0.016");
        e.set(ImageMasterDataEntry.PARAMS.MAX_RANGE_DEG,"0.5");
        return e;
    }

    static private ImageMasterDataEntry msxTemplate() {
        ImageMasterDataEntry e= new ImageMasterDataEntry();
        e.set(ImageMasterDataEntry.PARAMS.PROJECT,"MSX");
        e.set(ImageMasterDataEntry.PARAMS.MISSION_ID,"msx");
        e.set(ImageMasterDataEntry.PARAMS.SUB_PROJECT,"");
        e.set(ImageMasterDataEntry.PARAMS.HELP_URL,"http://irsa.ipac.caltech.edu/stuff");
        e.set(ImageMasterDataEntry.PARAMS.PROJECT_TYPE_KEY,"galactic");
        e.set(ImageMasterDataEntry.PARAMS.PROJECT_TYPE_DESC,"Galactic");
        e.set(ImageMasterDataEntry.PARAMS.MIN_RANGE_DEG,"0.01");
        e.set(ImageMasterDataEntry.PARAMS.MAX_RANGE_DEG,"1.5");

        return e;
    }


    static private ImageMasterDataEntry dssTemplate() {
        ImageMasterDataEntry e= new ImageMasterDataEntry();
        e.set(ImageMasterDataEntry.PARAMS.PROJECT,"DSS");
        e.set(ImageMasterDataEntry.PARAMS.MISSION_ID,"dss");
        e.set(ImageMasterDataEntry.PARAMS.SUB_PROJECT,"");
        e.set(ImageMasterDataEntry.PARAMS.HELP_URL,"http://irsa.ipac.caltech.edu/stuff");
        e.set(ImageMasterDataEntry.PARAMS.PROJECT_TYPE_KEY,"galactic");
        e.set(ImageMasterDataEntry.PARAMS.PROJECT_TYPE_DESC,"Galactic");
        e.set(ImageMasterDataEntry.PARAMS.MIN_RANGE_DEG,"0.016");
        e.set(ImageMasterDataEntry.PARAMS.MAX_RANGE_DEG,"0.5");

        return e;
    }




}
