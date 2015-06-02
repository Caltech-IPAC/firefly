/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.Constants;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.RegionPoint;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jun 12, 2012
 * Time: 3:49:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class PngRetrieve {
    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final Pattern ARTI_PATTERN = Pattern.compile("^(diff_spikes|halos|ghosts|latents|glint|pers)");


    public static File getFile(WebPlotRequest request, String plotStateStr, String drawInfoListStr,
                               List<FileInfo> artifactList) throws IOException {

        try {
            importPlotState(request, plotStateStr);
            ImagePlotBuilder.SimpleResults plotR= ImagePlotBuilder.create(request);
            List<StaticDrawInfo> drawInfoList = parseDrawInfoListStr(request, drawInfoListStr, artifactList);
            if (request.getPlotDescAppend()!=null) request.setTitle( plotR.getPlot().getPlotDesc());
            return ServerContext.convertToFile(PlotPngCreator.createImagePng(plotR.getPlot(),plotR.getFrGroup() ,drawInfoList));
        } catch (Exception e) {
            _log.error(e,"Could not create png file");

        }
        return null;
    }

    private static void importPlotState(WebPlotRequest request, String plotStateStr) {
        PlotState state = PlotState.parse(plotStateStr);
        if (state==null) return;
        request.setInitialColorTable(state.getColorTableId());
        request.setInitialZoomLevel(state.getZoomLevel());
        ZoomType zt= state.getWebPlotRequest(state.firstBand()).getZoomType();
        request.setZoomType(zt);
        request.setInitialRangeValues(state.getRangeValues(Band.NO_BAND));
    }

    private static List<StaticDrawInfo> parseDrawInfoListStr (WebPlotRequest request, String drawInfoListStr,
                                                              List<FileInfo> artifactList) {
        ArrayList <StaticDrawInfo> retval = new ArrayList <StaticDrawInfo>();

        List<String> drawInfoList = StringUtils.asList(drawInfoListStr, Constants.SPLIT_TOKEN);
        FileInfo fi;
        String fiExt, sdiLbl;
        if (drawInfoList!=null) {
            for (int i=0; i< drawInfoList.size(); i++) {
                String str = drawInfoList.get(i);
                if (StringUtils.isEmpty(str)) continue;
                StaticDrawInfo sdi = StaticDrawInfo.parse(drawInfoList.get(i));
                sdiLbl = sdi.getLabel();

                if (sdiLbl==null) continue;

                if (sdiLbl.equals("target") || ARTI_PATTERN.matcher(sdiLbl).lookingAt()) {
                    // if the layer is an artifact or an active target, replace it with the real data from this request.
                    StaticDrawInfo sdiTemplate = sdi;
                    RegionPoint rgtpl = getTemplate(sdiTemplate);
                    sdiLbl = sdiTemplate.getLabel();
                    sdi = new StaticDrawInfo();
                    sdi.setLabel(sdiTemplate.getLabel());
                    sdi.setDrawType(StaticDrawInfo.DrawType.REGION);

                    if (sdiLbl.equals("target")) {
                        WorldPt position[] = {request.getRequestArea().getCenter()};
                        for (WorldPt wp : position) {
                            RegionPoint rg = new RegionPoint(wp, rgtpl.getPointType(), rgtpl.getPointSize());
                            rg.setOptions(rgtpl.getOptions());
                            sdi.addRegion(rg);
                        }
                    } else {
                        for (int j = 0; j < artifactList.size(); j++) {
                            fi = artifactList.get(j);
                            fiExt = fi.getExternalName();
                            if (fiExt == null) continue;
                            try {
                                if (fiExt.endsWith(".tbl") &&
                                        convertArtifactValue(fiExt).equals(sdiLbl)) {
                                    readArtifact(fi.getInternalFilename(), sdi, rgtpl);
                                    break;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
                retval.add(sdi);
            }
        } else {
            retval.clear();    
        }

        return retval;
    }

    private static String convertArtifactValue(String s) {
        String retval = null;
        if (s!=null) {
            if (s.contains("_art_glint")) {
                retval="glint_arti";   // glint is same for all
//                if (s.contains("_2massj_")) {
//                    retval += "j";
//                } else if (s.contains("_2massh_")) {
//                    retval += "h";
//                } else if (s.contains("_2massk_")) {
//                    retval += "k";
//                }
            } else if (s.contains("_art_persistence")) {
                retval="pers_arti";
//                if (s.contains("_2massj_")) {
//                    retval += "j";
//                } else if (s.contains("_2massh_")) {
//                    retval += "h";
//                } else if (s.contains("_2massk_")) {
//                    retval += "k";
//                }
            } else if (s.contains("_art_D")) {
                retval="diff_spikes_3_";
                if (s.contains("_wise1_")) {
                    retval += "1";
                } else if (s.contains("_wise2_")) {
                    retval += "2";
                } else if (s.contains("_wise3_")) {
                    retval += "3";
                } else if (s.contains("_wise4_")) {
                    retval += "4";
                }
            } else if (s.contains("_art_H")) {
                retval="halos_";
                if (s.contains("_wise1_")) {
                    retval += "1";
                } else if (s.contains("_wise2_")) {
                    retval += "2";
                } else if (s.contains("_wise3_")) {
                    retval += "3";
                } else if (s.contains("_wise4_")) {
                    retval += "4";
                }
            } else if (s.contains("_art_O")) {
                retval="ghosts_";
                if (s.contains("_wise1_")) {
                    retval += "1";
                } else if (s.contains("_wise2_")) {
                    retval += "2";
                } else if (s.contains("_wise3_")) {
                    retval += "3";
                } else if (s.contains("_wise4_")) {
                    retval += "4";
                }
            } else if (s.contains("_art_P")) {
                retval="latents_";
                if (s.contains("_wise1_")) {
                    retval += "1";
                } else if (s.contains("_wise2_")) {
                    retval += "2";
                } else if (s.contains("_wise3_")) {
                    retval += "3";
                } else if (s.contains("_wise4_")) {
                    retval += "4";
                }
            }
        }
        return retval;
    }

    private static StaticDrawInfo readArtifact(String filename, StaticDrawInfo sdi, RegionPoint rgtpl) {

        File f = new File(filename);
        WorldPt wpt = null;
        double ra,dec;
        try {
            DataGroup dg=IpacTableReader.readIpacTable(f, "");

            for (DataObject o: dg) {
                ra= (Double)o.getDataElement("ra");
                dec= (Double)o.getDataElement("dec");

                wpt= new WorldPt(ra,dec);
                RegionPoint rg = new RegionPoint(wpt, rgtpl.getPointType(), rgtpl.getPointSize());
                rg.setOptions(rgtpl.getOptions());
                sdi.addRegion(rg);
            }

        } catch (Exception e) {

        }

        return sdi;
    }

    private static RegionPoint getTemplate(StaticDrawInfo sdi) {
        if (sdi.getRegionList().size() > 0) {
            return (RegionPoint) sdi.getRegionList().get(0);
        }
        return new RegionPoint(null, RegionPoint.PointType.Circle, 4);
    }
}

