package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.Constants;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.firefly.visualize.draw.WebGridLayer;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jun 12, 2012
 * Time: 3:49:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class PngRetrieve {
    private static final Logger.LoggerImpl _log = Logger.getLogger();

    public static File getFile(WebPlotRequest request, String plotStateStr, String drawInfoListStr,
                               List<FileInfo> artifactList) throws IOException {

        try {
            importPlotState(request, plotStateStr);
            ImagePlot plot= ImagePlotBuilder.create(request);
            List<StaticDrawInfo> drawInfoList = parseDrawInfoListStr(request, drawInfoListStr, artifactList);
            if (request.getPlotDescAppend()!=null) request.setTitle( plot.getPlotDesc());
            return VisContext.convertToFile(PlotPngCreator.createImagePng(plot, drawInfoList));
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
        request.setZoomType(ZoomType.STANDARD);
        request.setInitialRangeValues(state.getRangeValues(Band.NO_BAND));
    }

    private static List<StaticDrawInfo> parseDrawInfoListStr (WebPlotRequest request, String drawInfoListStr,
                                                              List<FileInfo> artifactList) {
        ArrayList <StaticDrawInfo> retval = new ArrayList <StaticDrawInfo>();

        List<String> drawInfoList = StringUtils.asList(drawInfoListStr, Constants.SPLIT_TOKEN);
        StaticDrawInfo sdi;
        FileInfo fi;
        String fiExt, sdiLbl;
        if (drawInfoList!=null) {
            for (int i=0; i< drawInfoList.size(); i++) {
                sdi = StaticDrawInfo.parse(drawInfoList.get(i));
                sdiLbl = sdi.getLabel();
                if (sdiLbl==null) continue;
                if (sdiLbl.equals("target")) {
                    WorldPt position[] = {request.getRequestArea().getCenter()};
                    sdi.setList(Arrays.asList(position));
                } else if (!sdiLbl.equals(WebGridLayer.DRAWER_ID) && !sdiLbl.contains("CatalogID")) {
                    for (int j=0; j<artifactList.size(); j++) {
                        fi = artifactList.get(j);
                        fiExt = fi.getExternalName();
                        if (fiExt==null) continue;
                        try {
                            if (fiExt.endsWith(".tbl") &&
                                    convertArtifactValue(fiExt).equals(sdiLbl)) {
                                readArtifact(fi.getInternalFilename(), sdi);
                                break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
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
                retval="glint_arti_";
                if (s.contains("_2massj_")) {
                    retval += "j";
                } else if (s.contains("_2massh_")) {
                    retval += "h";
                } else if (s.contains("_2massk_")) {
                    retval += "k";
                }
            } else if (s.contains("_art_persistence")) {
                retval="pers_arti_";
                if (s.contains("_2massj_")) {
                    retval += "j";
                } else if (s.contains("_2massh_")) {
                    retval += "h";
                } else if (s.contains("_2massk_")) {
                    retval += "k";
                }
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

    private static StaticDrawInfo readArtifact(String filename, StaticDrawInfo sdi) {

        File f = new File(filename);
        WorldPt wpt = null;
        double ra,dec;
        try {
            DataGroup dg=IpacTableReader.readIpacTable(f, "");

            for (DataObject o: dg) {
                ra= (Double)o.getDataElement("ra");
                dec= (Double)o.getDataElement("dec");

                wpt= new WorldPt(ra,dec);
                sdi.getList().add(wpt);
            }

        } catch (Exception e) {

        }

        return sdi;
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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