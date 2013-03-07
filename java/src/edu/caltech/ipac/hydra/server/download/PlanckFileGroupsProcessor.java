package edu.caltech.ipac.hydra.server.download;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.util.AppProperties;

import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@SearchProcessorImpl(id = "PlanckDownload")
public class PlanckFileGroupsProcessor extends FileGroupsProcessor {

    public static final String PLANCK_FILESYSTEM_BASEPATH = AppProperties.getProperty("planck.filesystem_basepath");

    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        assert (request instanceof DownloadRequest);
        try {
            return computeFileGroup((DownloadRequest) request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException(e.getMessage());
        }
    }

    private List<FileGroup> computeFileGroup(DownloadRequest request) throws IOException, IpacTableException, DataAccessException {
        Collection<Integer> selectedRows = request.getSelectedRows();
        DataGroupPart dgp = new SearchManager().getDataGroup(request.getSearchRequest());

        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();
        long fgSize = 0;

        String basePath = PLANCK_FILESYSTEM_BASEPATH;
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }

        basePath += "cutouts/ercsc_cutouts/";

        // get selected cutoutTypes (PE, WMAP, IRIS)
        String[] cutoutTypes = null;
        String cutoutTypesStr = request.getParam("cutoutTypes");
        if (cutoutTypesStr != null && cutoutTypesStr.length() > 0 && !cutoutTypesStr.equalsIgnoreCase("_none_")) {
            cutoutTypes = cutoutTypesStr.split(",");
        }

        // add catalog table
        String tablePath = dgp.getTableDef().getSource();
        File file = new File(tablePath);
        if (file != null && file.exists()) {
            ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
            FileInfo fi = new FileInfo(file.getName(), file.getName(), file.length());
            fiArr.add(fi);
            fgSize += file.length();

            FileGroup fg = new FileGroup(fiArr, file.getParentFile(), fgSize, "PLANCK Download Files");
            fgArr.add(fg);
        }

        if (cutoutTypes != null && cutoutTypes.length > 0) {
            // use selected row "name" field to determine subdirectory path to cutout files
            IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, "name");

            for (int rowIdx : selectedRows) {
                ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
                String name = (String) dgData.get(rowIdx, "name");

                int dirStartIdx = name.indexOf(" ") + 1;
                String sDir1 = name.substring(dirStartIdx, dirStartIdx + 4);
                String sDir2 = name.substring(dirStartIdx);
                File cutoutDir = new File(basePath + sDir1 + "/" + sDir2 + "/");
                String outDir = name.replace(' ', '_');

                for (int i = 0; i < cutoutTypes.length; i++) {
                    final String cutoutType = cutoutTypes[i];

                    // get selected cutout files based on filename prefix (PE, WMAP, IRIS, ...)
                    File[] files = cutoutDir.listFiles(new FilenameFilter() {
                        public boolean accept(File cutoutDir, String name) {
                            return name.startsWith(cutoutType);
                        }
                    });

                    if (files != null) {
                        for (int j = 0; j < files.length; j++) {
                            if (files[i] != null && files[i].exists()) {
                                FileInfo fi = new FileInfo(files[j].getName(), outDir + "/" + files[j].getName(), files[j].length());
                                fiArr.add(fi);
                                fgSize += files[j].length();
                            }
                        }
                    }
                }

                FileGroup fg = new FileGroup(fiArr, cutoutDir, fgSize, "PLANCK Download Files");
                fgArr.add(fg);
            }
        }

        return fgArr;
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
