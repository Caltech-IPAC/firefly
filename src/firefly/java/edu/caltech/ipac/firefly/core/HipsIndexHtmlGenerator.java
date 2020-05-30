package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.server.util.VersionUtil;
import edu.caltech.ipac.firefly.server.visualize.hips.HiPSMasterListEntry;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.util.AppProperties;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.NotImplementedException;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.apache.commons.csv.CSVFormat.DEFAULT;

/**
 * Generate index html such as CDS generated for hips.
 * Example here: https://irsa.ipac.caltech.edu/data/hips/CDS/2MASS/Color/index.html
 * <p>
 * This Web resource contains HiPS(*) components for 2MASS-Color progressive survey.
 * <p>
 * Label: 2MASS-Color
 * Type: colored HiPS image
 * Best pixel angular resolution: 805.2mas
 * Max tile order: 9 (NSIDE=512)
 * Available encoding tiles: jpeg
 * Tile size: 512x512
 * Processing date: 03/11/14 16:00:39
 * HiPS builder: Aladin/HipsGen v8.116
 * Coordinate frame: equatorial
 * Raw property file: properties
 * Base URL:
 * https://irsa.ipac.caltech.edu/data/hips/CDS/2MASS/Color
 */
public class HipsIndexHtmlGenerator {

    static String INDEX = "<HTML>\n<HEAD>\n   <script type=\"text/javascript\" src=\"http://code.jquery.com/jquery-1.10.1.min.js\"></script>\n   <link rel=\"stylesheet\" href=\"http://aladin.u-strasbg.fr/AladinLite/api/v2/latest/aladin.min.css\" >\n   <script type=\"text/javascript\">var jqMenu = jQuery.noConflict();</script>\n   <script type=\"text/javascript\">\nvar hipsDir=null;</script>\n</HEAD>\n<H1>\"$LABEL\" progressive survey</H1>\nThis Web resource contains HiPS(*) components for <B>$LABEL</B> progressive survey.\n<script type=\"text/javascript\">\nhipsDir = location.href;\nhipsDir = hipsDir.substring(0,hipsDir.lastIndexOf(\"/\",hipsDir.length));\ndocument.getElementById(\"hipsBase\").innerHTML=hipsDir;\n</script>\n<TABLE>\n<TR>\n<TD>\n   <script type=\"text/javascript\" src=\"http://aladin.u-strasbg.fr/AladinLite/api/v2/latest/aladin.min.js\" charset=\"utf-8\"></script>\n<div id=\"aladin-lite-div\" style=\"width:350px;height:350px;\"></div>\n<script type=\"text/javascript\">\n//var hipsDir = location.href;\n//hipsDir = hipsDir.substring(0,hipsDir.lastIndexOf(\"/\",hipsDir.length));\nvar aladin = $.aladin(\"#aladin-lite-div\");\naladin.setImageSurvey(aladin.createImageSurvey('$LABEL', '$LABEL',\nhipsDir, '$SYS', $ORDER, {imgFormat: '$FMT'}));\n</script>    \n</TD>\n<TD>\n<UL>\n$INFO   <LI> <B>Raw property file:</B> <A HREF=\"properties\">properties</A>\n   <LI> <B>Base URL:<p id=\"hipsBase\"></p></B> \n</UL>\n</TD>\n</TR>\n</TABLE>\nThis survey can be displayed by <A HREF=\"http://aladin.u-strasbg.fr/AladinLite\">Aladin Lite</A> (see above), \nby <A HREF=\"http://aladin.u-strasbg.fr/java/nph-aladin.pl?frame=downloading\">Aladin Desktop</A> client\n(just open the base URL)<BR>or any other HiPS aware clients .\n<HR>\n<I>(*) The HiPS technology allows a dedicated client to access an astronomical survey at any location and at any scale. \nHiPS is based on HEALPix sky tessellation and it is designed for astronomical scientifical usages (low distorsion, true pixel values...).HiPS technical documentation is available <A HREF=\"http://aladin.u-strasbg.fr/HiPS/HiPS%20technical%20doc.pdf\">here<A></I>\n<script type=\"text/javascript\">\ndocument.getElementById(\"hipsBase\").innerHTML=hipsDir;\n</script>\n</HTML>\n";
    private String version = VersionUtil.getAppVersion().toString();
    private static String indexHtmlTemplate = "/edu/caltech/ipac/firefly/resources/index_template-hips.html";
    Charset charset = StandardCharsets.UTF_8;
    //Template FITS option image request '__fits_image'
    static String defaultFitsImageRequest = "{\n" +
            "                Service: 'WISE',\n" +
            "                Title: 'WISE 3a/2',\n" +
            "                SurveyKey: '3a',\n" +
            "                SurveyKeyBand: '2'\n" +
            "            };";
    /*
    String label = this.getLabel();
        if (label == null || label.length() == 0) {
            label = "XXX_" + System.currentTimeMillis() / 1000L;
        }

        int order = this.getOrder();
        if (order == -1) {
            order = cds.tools.pixtools.Util.getMaxOrderByPath(this.getOutputPath());
        }

        if (this.prop == null) {
            AppProperties.loadProperties();
        }

        String sys = this.prop.getProperty("hips_frame");
        if (sys == null) {
            sys = "galactic";
        }

        long nside = CDSHealpix.pow2((long) order);
        long nsideP = CDSHealpix.pow2((long) (order + this.getTileOrder()));
        double resol = CDSHealpix.pixRes(nsideP) / 3600.0D;
        int width = this.getTileSide();
        String tiles = this.getAvailableTileFormats();
        String fmt = tiles.indexOf("png") >= 0 ? "png" : "jpg";
        String res = this.INDEX.replace("$LABEL", label);
        StringBuilder info = new StringBuilder();
        info.append("   <LI> <B>Label:</B> " + label + "\n");
        info.append("   <LI> <B>Type:</B> " + (this.depth > 1 ? "HiPS cube (" + this.depth + " frames)" : (this.isColor() ? "colored HiPS image" : "HiPS image")) + "\n");
        info.append("   <LI> <B>Best pixel angular resolution:</B> " + Coord.getUnit(resol) + "\n");
        info.append("   <LI> <B>Max tile order:</B> " + order + " (NSIDE=" + nside + ")\n");
        info.append("   <LI> <B>Available encoding tiles:</B> " + tiles + "\n");
        info.append("   <LI> <B>Tile size:</B> " + width + "x" + width + "\n");
        if (this.bitpix != 0 && this.bitpix != -1) {
            info.append("   <LI> <B>FITS tile BITPIX:</B> " + this.bitpix + "\n");
        }

        info.append("   <LI> <B>Processing date:</B> " + getNow() + "\n");
        info.append("   <LI> <B>HiPS built:</B> " + this.version + "\n");
        info.append("   <LI> <B>Coordinate frame:</B> " + sys + "\n");
        if (this.moc != null) {
            double cov = getCoverage(); // Get it from Moc.fits if present under same folder.
            double degrad = Math.toDegrees(1.0D);
            double skyArea = 12.566370614359172D * degrad * degrad;
            info.append("   <LI> <B>Sky area:</B> " + Math.round(cov * 100.0D, 3) + "% of sky => " + Coord.getUnit(skyArea * cov, false, true) + "^2\n");
            info.append("   <LI> <B>Associated coverage map:</B> <A HREF=\"Moc.fits\">MOC</A>\n");
        }

//        String metadata = Util.concatDir(this.getHpxFinderPath(), "metadata.xml");
//        if ((new File(metadata)).exists()) {
//            info.append("   <LI> <B>Original data access template:</B> <A HREF=\"HpxFinder/metadata.xml\">metadata.xml</A>\n");
//        }

        res = res.replace("$INFO", info);
        res = res.replace("$ORDER", order + "");
        res = res.replace("$SYS", sys);
        res = res.replace("$FMT", fmt);
        String tmp = this.getOutputPath() + "index.html";
        File ftmp = new File(tmp);
        if (ftmp.exists()) {
            ftmp.delete();
        }

        FileOutputStream out = null;

        try {
            out = new FileOutputStream(ftmp);
            out.write(res.getBytes());
        } finally {
            if (out != null) {
                out.close();
            }

        }

     */

    protected void writeIndexHtml(String urlHips, String templateFile) throws Exception {
        String newContent = readAndReplaceTemplate(urlHips, templateFile);
//        File tmp = File.createTempFile("index",".html", new File("."));
        File tmp = new File("index.html");
        Path pathout = Paths.get(tmp.getAbsolutePath());
        Files.write(pathout, newContent.getBytes(charset));




    }

    private String readAndReplaceTemplate(String urlHips, String indexHtmlTemplate) throws IOException {
        Path path = Paths.get(indexHtmlTemplate);
        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceAll("__url_firefly","https://irsa.ipac.caltech.edu/irsaviewer/firefly_loader.js");
        content = content.replaceAll("__urlHips",urlHips);
        content = content.replaceAll("__props",listHtml(getProperties(urlHips)));
        content = content.replaceAll("__fits_image",defaultFitsImageRequest);

        return content;

    }

    private String listHtml(Properties properties) {
        String res="";
        Enumeration<Object> keys = properties.keys();
        while (keys.hasMoreElements()){
            Object key = keys.nextElement();
            String property = properties.getProperty(key.toString());
            res+="<li><b>"+key.toString()+": </b>"+property+"</li>";
        }
        return res;
    }

    private String getOutputPath() {
        throw new NotImplementedException();
    }

    private String getLabel() {
        throw new NotImplementedException();
    }

    private double getCoverage() {
        throw new NotImplementedException();
    }

    Properties getProperties(String hipsUrl) throws IOException {
        // Take properties file under root URL where the HiPS are and parse it with delimiter '='
        InputStream inf = null;
        Properties proper = null;
        try {
            inf = new URL(hipsUrl+"/properties").openStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inf, "UTF-8"), IpacTableUtil.FILE_IO_BUFFER_SIZE);

            CSVParser parser = new CSVParser(reader, DEFAULT);
            List<CSVRecord> records = parser.getRecords();
            Iterator<CSVRecord> iterator = records.iterator();
            proper = new Properties();
            int i = 0;
            for (Iterator<CSVRecord> it = iterator; it.hasNext(); ) {
                CSVRecord r = it.next();
                String rec = "";
                for (String str : r) {
                    rec += str;
                }
                String val = r.iterator().next();
                if (!val.startsWith("#")) {
                    String prop = rec.split("=")[0].trim();
                    String value = rec.split("=")[1].trim();//val.substring(r.toString().indexOf("="),r.toString().length()).trim();
                    proper.setProperty(prop, value);
                } else {
                    proper.setProperty("comment" + i++, val);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (inf != null) {
                inf.close();
            }
        }
        return proper;
    }

    private static Map<String, String> paramsMap = new HashMap<>();

    static {
        HiPSMasterListEntry.setParamsMap(paramsMap, HiPSMasterListEntry.PARAMS.IVOID, "creator_did");
        HiPSMasterListEntry.setParamsMap(paramsMap, HiPSMasterListEntry.PARAMS.URL, "hips_service_url");
        HiPSMasterListEntry.setParamsMap(paramsMap, HiPSMasterListEntry.PARAMS.TITLE, "obs_title");
        HiPSMasterListEntry.setParamsMap(paramsMap, HiPSMasterListEntry.PARAMS.ORDER, "hips_order");
        HiPSMasterListEntry.setParamsMap(paramsMap, HiPSMasterListEntry.PARAMS.TYPE, "dataproduct_type");
        HiPSMasterListEntry.setParamsMap(paramsMap, HiPSMasterListEntry.PARAMS.FRACTION, "moc_sky_fraction");
        HiPSMasterListEntry.setParamsMap(paramsMap, HiPSMasterListEntry.PARAMS.FRAME, "hips_frame");
        HiPSMasterListEntry.setParamsMap(paramsMap, HiPSMasterListEntry.PARAMS.WAVELENGTH, "obs_regime");
        HiPSMasterListEntry.setParamsMap(paramsMap, HiPSMasterListEntry.PARAMS.RELEASEDATE, "hips_release_date");
        HiPSMasterListEntry.setParamsMap(paramsMap, HiPSMasterListEntry.PARAMS.PIXELSCALE, "hips_pixel_scale");
    }

    public static void main(String[] args) throws Exception {
        String hipsUrl = "https://irsa.ipac.caltech.edu/data/hips/ESAC/PACS-color/";//https://irsa.ipac.caltech.edu/data/hips/CDS/2MASS/Color";
//        new HipsIndexHtmlGenerator().getProperties(hipsUrl);
//        InputStream inf= FileUtils.openInputStream(new File(//IrsaMasterDataSource.class.getResourceAsStream(indexHtmlTemplate);
          String template = "/Users/ejoliet/devspace/branch/dev/firefly/src/firefly/java" + indexHtmlTemplate;
        new HipsIndexHtmlGenerator().writeIndexHtml(hipsUrl,template);
    }
}