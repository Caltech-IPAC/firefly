package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;



@SearchProcessorImpl(id = "planckNotesQuery")
public class QueryPlanckNotes extends DynQueryProcessor {

    private static final String PLANCK_FILE_PROP= "planck.filesystem_basepath";
    private static final String CUTOUTS_BASE_DIR= AppProperties.getProperty(PLANCK_FILE_PROP) + "/cutouts/";
    private static final String CUTOUTS_AS_PFX_DIR= VisContext.replaceWithPrefix(new File(CUTOUTS_BASE_DIR),PLANCK_FILE_PROP);

    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {


        String name = request.getParam("name");
        name = name == null ? "" : name;
        if (!name.matches("\\w+ \\w{4}\\..+")) {
            // name does not match planck's naming scheme... return
            return null;
        }
        String fname = name.replace(' ', '_') + "_note.txt";
        String datadir = name.split(" ")[1];
        String subdir = datadir.split("\\.")[0];
        File notesFile = new File(new File(CUTOUTS_BASE_DIR, subdir), datadir + "/" + fname);

        String notes = "No notes available";
        if (notesFile.canRead()) {
            try {
                ByteArrayOutputStream outs = new ByteArrayOutputStream();
                FileUtil.writeFileToStream(notesFile, outs);
                outs.flush();
                notes = outs.toString();
            } catch(Exception e) {}
        }
        DataType[] columns = new DataType[]{new DataType("Notes", String.class)};
        DataGroup table = new DataGroup("", columns);
        DataObject row = new DataObject(table);
        columns[0].getFormatInfo().setWidth(notes.length());
        row.setDataElement(columns[0], notes.trim());
        table.add(row);
        File f = createFile(request);
        IpacTableWriter.save(f, table);
        return f;
    }

    @Override
    public boolean doCache() {
        return false;
    }

    @Override
    public boolean doLogging() {
        return false;
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
