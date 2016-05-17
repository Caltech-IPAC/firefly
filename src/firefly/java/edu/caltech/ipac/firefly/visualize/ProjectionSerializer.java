/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import edu.caltech.ipac.visualize.plot.projection.ProjectionParams;

import java.util.HashMap;
import java.util.Map;
/**
 * User: roby
 * Date: Apr 7, 2010
 * Time: 9:21:53 AM
 */


/**
 * @author Trey Roby
 */
@JsExport
@JsType
public class ProjectionSerializer {

    private final static String SPLIT_TOKEN= "--ProjSer--";

    public static String serializeProjection(Projection proj) {
        if (proj==null) return null;
        Map<String,String> map= new HashMap<String,String>();
        map.put("coorindateSys", proj.getCoordinateSys().toString());
        serializeProjectionParams(map,proj.getProjectionParams());
//        return MapPropertyLoader.convertToString(map);
        StringBuilder sb = new StringBuilder(500);
        for(Map.Entry<String,String> entry : map.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            sb.append(SPLIT_TOKEN);
        }
        return sb.toString();
    }


    @JsExport
    public static Projection deserializeProjection(String projStr) {
        if (projStr==null) return null;
        Map<String,String> map= new HashMap<String,String>(50);
//        MapPropertyLoader.load(map,projStr);
        String sAry[]= projStr.split(SPLIT_TOKEN,60);
        Projection proj= null;
        if (sAry.length<60) {
            String pairAry[];
            for(String s : sAry) {
                pairAry= s.split("=",2);
                if (pairAry.length==2) {
                    map.put(pairAry[0],pairAry[1]);
                }
            }
            String csStr= map.get("coorindateSys");
            CoordinateSys csys= CoordinateSys.parse(csStr);
            ProjectionParams pp= deserializeProjectionParams(map);
            proj=  new Projection(pp,csys);
        }




        return proj;
    }






    private static void serializeProjectionParams (Map<String,String> map, ProjectionParams p) {

        map.put("bitpix", p.bitpix+"");
        map.put("naxis",  p.naxis+"");
        map.put("naxis1", p.naxis1+"");
        map.put("naxis2", p.naxis2+"");
        map.put("naxis3", p.naxis3+"");
        map.put("crpix1", p.crpix1+"");
        map.put("crpix2", p.crpix2+"");
        map.put("crval1", p.crval1+"");
        map.put("crval2", p.crval2+"");
        map.put("cdelt1", p.cdelt1+"");
        map.put("cdelt2", p.cdelt2+"");
        map.put("crota1", p.crota1+"");
        map.put("crota2", p.crota2+"");
        map.put("file_equinox", p.file_equinox+"");
        map.put("ctype1", p.ctype1+"");
        map.put("ctype2", p.ctype2+"");
        map.put("radecsys", p.radecsys+"");
        map.put("datamax", p.datamax+"");
        map.put("datamin", p.datamin+"");
        //map.put("bscale", p.bscale+"");
        //map.put("bzero", p.bzero+"");
        //map.put("bunit", p.bunit+"");
        //map.put("blank_value", p.blank_value+"");
        map.put("maptype", p.maptype+"");
        map.put("cd1_1", p.cd1_1+"");
        map.put("cd1_2", p.cd1_2+"");
        map.put("cd2_1", p.cd2_1+"");
        map.put("cd2_2", p.cd2_2+"");
        map.put("dc1_1", p.dc1_1+"");
        map.put("dc1_2", p.dc1_2+"");
        map.put("dc2_1", p.dc2_1+"");
        map.put("dc2_2", p.dc2_2+"");
        map.put("using_cd", p.using_cd+"");
        map.put("plate_ra", p.plate_ra+"");
        map.put("plate_dec", p.plate_dec+"");
        map.put("x_pixel_offset", p.x_pixel_offset+"");
        map.put("y_pixel_offset", p.y_pixel_offset+"");
        map.put("x_pixel_size", p.x_pixel_size+"");
        map.put("y_pixel_size", p.y_pixel_size+"");
        map.put("plt_scale", p.plt_scale+"");
        map.put("ppo_coeff", arrayToStr(p.ppo_coeff));
        map.put("amd_x_coeff", arrayToStr(p.amd_x_coeff));
        map.put("amd_y_coeff", arrayToStr(p.amd_y_coeff));
        map.put("a_order", p.a_order+"");
        map.put("ap_order", p.ap_order+"");
        map.put("b_order", p.b_order+"");
        map.put("bp_order", p.bp_order+"");
        map.put("a", arrayToStr(p.a));
        map.put("ap", arrayToStr(p.ap));
        map.put("b", arrayToStr(p.b));
        map.put("bp", arrayToStr(p.bp));
        map.put("map_distortion", p.map_distortion+"");
        map.put("keyword", p.keyword+"");


    }


    private static ProjectionParams deserializeProjectionParams(Map<String,String> map) {


        ProjectionParams p= new ProjectionParams();

        p.bitpix= Integer.parseInt(map.get("bitpix"));
        p.naxis= Integer.parseInt(map.get("naxis"));
        p.naxis1= Integer.parseInt(map.get("naxis1"));
        p.naxis2= Integer.parseInt(map.get("naxis2"));
        p.naxis3= Integer.parseInt(map.get("naxis3"));

        p.crpix1= parseDouble(map.get("crpix1"));
        p.crpix2= parseDouble(map.get("crpix2"));
        p.crval1= parseDouble(map.get("crval1"));
        p.crval2= parseDouble(map.get("crval2"));
        p.cdelt1= parseDouble(map.get("cdelt1"));
        p.cdelt2= parseDouble(map.get("cdelt2"));
        p.crota1= parseDouble(map.get("crota1"));
        p.crota2= parseDouble(map.get("crota2"));
        p.file_equinox= parseDouble(map.get("file_equinox"));

        p.ctype1= checkNull(map.get("ctype1"));
        p.ctype2= checkNull(map.get("ctype2"));
        p.radecsys= checkNull(map.get("radecsys"));


        p.datamax= parseDouble(map.get("datamax"));
        p.datamin= parseDouble(map.get("datamin"));
        //p.bscale= parseDouble(map.get("bscale"));
        //p.bzero= parseDouble(map.get("bzero"));

        //p.bunit= checkNull(map.get("bunit"));
        //p.blank_value= parseDouble(map.get("blank_value"));
        p.maptype= Integer.parseInt(map.get("maptype"));


        p.cd1_1= parseDouble(map.get("cd1_1"));
        p.cd1_2= parseDouble(map.get("cd1_2"));
        p.cd2_1= parseDouble(map.get("cd2_1"));
        p.cd2_2= parseDouble(map.get("cd2_2"));


        p.dc1_1= parseDouble(map.get("dc1_1"));
        p.dc1_2= parseDouble(map.get("dc1_2"));
        p.dc2_1= parseDouble(map.get("dc2_1"));
        p.dc2_2= parseDouble(map.get("dc2_2"));

        p.using_cd= Boolean.parseBoolean(map.get("using_cd"));

        p.plate_ra= parseDouble(map.get("plate_ra"));
        p.plate_dec= parseDouble(map.get("plate_dec"));
        p.x_pixel_offset= parseDouble(map.get("x_pixel_offset"));
        p.y_pixel_offset= parseDouble(map.get("y_pixel_offset"));
        p.x_pixel_size= parseDouble(map.get("x_pixel_size"));
        p.y_pixel_size= parseDouble(map.get("y_pixel_size"));
        p.plt_scale= parseDouble(map.get("plt_scale"));

        p.ppo_coeff= strTo1DimArray(map.get("ppo_coeff"));
        p.amd_x_coeff= strTo1DimArray(map.get("amd_x_coeff"));
        p.amd_y_coeff= strTo1DimArray(map.get("amd_y_coeff"));

        p.a_order= parseDouble(map.get("a_order"));
        p.ap_order= parseDouble(map.get("ap_order"));
        p.b_order= parseDouble(map.get("b_order"));
        p.bp_order= parseDouble(map.get("bp_order"));


        int len= ProjectionParams.MAX_SIP_LENGTH;
        p.a= strTo2DimArray(map.get("a"),len,len);
        p.ap= strTo2DimArray(map.get("ap"),len,len);
        p.b= strTo2DimArray(map.get("b"),len,len);
        p.bp= strTo2DimArray(map.get("bp"),len,len);

        map.put("map_distortion", p.map_distortion+"");
        map.put("keyword", p.keyword+"");

        p.map_distortion= Boolean.parseBoolean(map.get("map_distortion"));
        p.keyword= checkNull(map.get("keyword"));

        return p;
    }



    private static String arrayToStr(double dAry[]) {
        if (dAry==null) return "null";
        StringBuffer sb= new StringBuffer(dAry.length*10);
        for(int i= 0; (i<dAry.length); i++) {
            sb.append(dAry[i]);
            if (i<dAry.length-1) sb.append(",");
        }
        return sb.toString();
    }

    private static double[] strTo1DimArray(String s) {
        if (s.equals("null")) return null;
        String sAry[]= s.split(",");
        double retAry[]= new double[sAry.length];
        for(int i=0; (i<sAry.length); i++) {
            retAry[i]= parseDouble(sAry[i]);
        }
        return retAry;
    }

    private static double[][] strTo2DimArray(String s,int x, int y) {
        if (s.equals("null")) return null;
        double retAry[][]= new double[x][y];
        int sIdx= 0;
        String sAry[]= s.split(",");

        for(int j= 0; (j<retAry.length); j++) {
            for(int i= 0; (i<retAry[j].length); i++) {
                retAry[j][i]= parseDouble(sAry[sIdx++]);
            }
        }
        return retAry;
    }


    private static String arrayToStr(double dAry[][]) {
        if (dAry==null) return "null";
        StringBuffer sb= new StringBuffer(dAry.length*10);
        for(int j= 0; (j<dAry.length); j++) {
            for(int i= 0; (i<dAry[j].length); i++) {
                sb.append(dAry[j][i]);
                if (i<dAry.length-1 || j<dAry[j].length-1) sb.append(",");
            }
        }
        return sb.toString();
    }

    public static double parseDouble(String s) throws NumberFormatException {
        return s.equals("NaN") ? Double.NaN : Double.parseDouble(s);
    }

    public static String checkNull(String s) {
        if (s==null)               return null;
        else if (s.equals("null")) return null;
        else                       return s;
    }
}

