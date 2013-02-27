package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.servlets.AnyFileDownload;
import edu.caltech.ipac.firefly.server.util.ImageGridSupport;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


@SearchProcessorImpl(id = "planckImagesQuery")
public class QueryPlanckImages extends DynQueryProcessor {

    private static final String PLANCK_FILE_PROP= "planck.filesystem_basepath";
    private static final String CUTOUTS_BASE_DIR= AppProperties.getProperty(PLANCK_FILE_PROP) + "/cutouts/";
    private static final String CUTOUTS_AS_PFX_DIR= VisContext.replaceWithPrefix(new File(CUTOUTS_BASE_DIR),PLANCK_FILE_PROP);
    private static final String BASE_SERVLET = "servlet/Download?"+ AnyFileDownload.FILE_PARAM+"="+CUTOUTS_AS_PFX_DIR;

    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {
        String sname = request.getParam("name");
        String subdir = sname.substring(11,15);
        String sdir = sname.substring(11);
        String desc, expandedDesc, groupName = "";
        File CutoutDir = new File(CUTOUTS_BASE_DIR + subdir + "/" + sdir + "/");

        ArrayList<DataType> defs = ImageGridSupport.createBasicDataDefinitions();
        DataGroup table = ImageGridSupport.createBasicDataGroup(defs, true);

        table.addAttributes(new DataGroup.Attribute("JPEG_SELECTION_HILITE", "F"));
        table.addAttributes(new DataGroup.Attribute("JPEG_SELECTION_DOUBLE_CLICK", "T"));

        // add a row for every file under planckDir
        //
        File[] listOfPlanckFiles = listFiles(CutoutDir, new FilenameFilter() {
            public boolean accept(File CutoutDir, String name) {
                return name.startsWith("PE");
            }
        });
        groupName = "Planck";
        if (listOfPlanckFiles!=null)
            for (int i = 0; i < listOfPlanckFiles.length; i++) {
                if (listOfPlanckFiles[i].isFile()) {
                    String fileURL= BASE_SERVLET +  URLEncoder.encode("/" + subdir + "/" + sdir + "/" + listOfPlanckFiles[i].getName(), "UTF-8");

                    DataObject row = new DataObject(table);
                    String planckfilename = listOfPlanckFiles[i].getName();
                    String[] planckimgdesc = planckfilename.split("_");
                    String planckdesc = planckimgdesc[0];
                    if (planckdesc.equals("PE030"))      { planckdesc = "30GHz";
                    }else if (planckdesc.equals("PE044")){ planckdesc = "44GHz";
                    }else if (planckdesc.equals("PE070")){ planckdesc = "70GHz";
                    }else if (planckdesc.equals("PE100")){ planckdesc = "100GHz";
                    }else if (planckdesc.equals("PE143")){ planckdesc = "143GHz";
                    }else if (planckdesc.equals("PE217")){ planckdesc = "217GHz";
                    }else if (planckdesc.equals("PE353")){ planckdesc = "353GHz";
                    }else if (planckdesc.equals("PE545")){ planckdesc = "545GHz";
                    }else if (planckdesc.equals("PE857")){ planckdesc = "857GHz";
                    }
                    row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.TYPE.toString()), "jpg");
                    row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.THUMBNAIL.toString()), fileURL);
                    row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.DESC.toString()), planckdesc);
                    row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.GROUP.toString()), groupName);
                    table.add(row);
                } else if (listOfPlanckFiles[i].isDirectory()) {
                  System.out.println("Directory " + listOfPlanckFiles[i].getName());
                }
            }

        // TLau: for 11/4/2010 Planck Beta
        // 8: all three cutout display to use the same color table, will get the Planck cutout color table as standard
        // default for IRAS and WMAP display  -> Use Color Table 4
        // 9: use the 1-99 Linear stretch in display for IRAS and WMAP images
        String url;

        String req;
        RangeValues rv= new RangeValues(RangeValues.PERCENTAGE,1.0,RangeValues.PERCENTAGE,99.0,RangeValues.STRETCH_LINEAR);
        WebPlotRequest wpReq;
        //for WMAP
        File[] listOfwmapFiles = listFiles(CutoutDir, new FilenameFilter() {
            public boolean accept(File CutoutDir, String name) {
                return name.startsWith("WMAP");
            }
        });
        groupName = "WMAP";
        if (listOfwmapFiles!=null)
            for (int i = 0; i < listOfwmapFiles.length; i++) {
                if (listOfwmapFiles[i].isFile()) {
//                    url=imgurl + subdir + "/" +sdir + "/" + listOfwmapFiles[i].getName();

                    String wmapfilename = listOfwmapFiles[i].getName();
                    String[] wmapimgdesc = wmapfilename.split("_");
                    expandedDesc= wmapimgdesc[0].replaceAll(groupName,groupName+" ");
                    desc = expandedDesc.split(" ")[1];
                    wpReq= WebPlotRequest.makeFilePlotRequest(listOfwmapFiles[i].getPath(),1.0F);
                    wpReq.setInitialColorTable(4);
                    wpReq.setInitialRangeValues(rv);
                    wpReq.setExpandedTitle(expandedDesc);
                    wpReq.setTitle(desc);
                    wpReq.setHideTitleDetail(true);
                    req= wpReq.toString();

//                    row.setDataElement(columns[1], req+url);
                    addToRow(table, req, desc, groupName);
                } else if (listOfwmapFiles[i].isDirectory()) {
                    System.out.println("Directory " + listOfwmapFiles[i].getName());
                }
            }

        //for IRAS
        //File[] listOfirasFiles = irasDir.listFiles();
        File[] listOfirasFiles = listFiles(CutoutDir, new FilenameFilter() {
            public boolean accept(File CutoutDir, String name) {
                return name.startsWith("IRIS");
            }
        });
        groupName = "IRAS";
        desc="";
        if (listOfirasFiles!=null) {
            for (int i = 0; i < listOfirasFiles.length; i++) {
                if (listOfirasFiles[i].isFile()) {
                    String irasfilename = listOfirasFiles[i].getName();
                    irasfilename = irasfilename.replace("IRIS","IRAS");
                    String[] irasimgdesc = irasfilename.split("_");
                    String irasimgdescband = irasimgdesc[0].substring(4,5);
                    if (irasimgdescband.equals("1"))      { desc= "12 microns";
                    }else if (irasimgdescband.equals("2")){ desc= "25 microns";
                    }else if (irasimgdescband.equals("3")){ desc= "60 microns";
                    }else if (irasimgdescband.equals("4")){ desc= "100 microns";
                    }
                    expandedDesc= groupName + " " + desc;
                    wpReq= WebPlotRequest.makeFilePlotRequest(listOfirasFiles[i].getPath(),1.0F);
                    wpReq.setInitialColorTable(4);
                    wpReq.setInitialRangeValues(rv);
                    wpReq.setExpandedTitle(expandedDesc);
                    wpReq.setTitle(desc);
                    wpReq.setHideTitleDetail(true);
                    req= wpReq.toString();
                    addToRow(table, req, desc, groupName);
                } else if (listOfirasFiles[i].isDirectory()) {
                    System.out.println("Directory " + listOfirasFiles[i].getName());
                }
            }
        }
        // write out table into ipac-table format..
        if (table.size()==0) {
            table.addAttributes(new DataGroup.Attribute("INFO", "Image data not found!"));
        }
        File f = createFile(request);
        IpacTableWriter.save(f, table);
        return f;

    }

    private static void addToRow(DataGroup table, String req, String desc, String groupName) {
        DataObject row = new DataObject(table);
        row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.TYPE.toString()), "req");
        row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.THUMBNAIL.toString()), req);
        row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.DESC.toString()), desc);
        row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.GROUP.toString()), groupName);
        table.add(row);
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        meta.setAttribute("GRID_THUMBNAIL_COLUMN", "thumbnail");
        meta.setAttribute("PLOT_REQUEST_COLUMN", "req");
    }

    private File[] listFiles(File parent, FilenameFilter filenameFilter) {
        File[] files = parent.listFiles(filenameFilter);
        
        if (files!=null) {
            Arrays.sort(files, new Comparator() {
                public int compare(final Object o1, final Object o2) {
                    return ((File)o1).getName().compareTo(((File)o2).getName());
                }
            });
        }
        return files;
    }
}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
