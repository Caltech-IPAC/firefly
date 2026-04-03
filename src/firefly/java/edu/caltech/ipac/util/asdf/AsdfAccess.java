package edu.caltech.ipac.util.asdf;


import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.TableParseHandler;
import edu.caltech.ipac.util.FormatUtil;
import nom.tam.fits.FitsException;
import org.asdfformat.asdf.Asdf;
import org.asdfformat.asdf.AsdfFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static edu.caltech.ipac.firefly.core.FileAnalysisReport.Type.Image;
import static edu.caltech.ipac.firefly.core.FileAnalysisReport.Type.Table;
import static edu.caltech.ipac.util.asdf.AsdfCommon.AsdfFileInfo;

/**
 * @author Trey Roby
 *
 */
public class AsdfAccess {

    static {
        Asdf.configure((b) ->  {
            File asdfTmp= new File(ServerContext.getTempWorkDir(), "asdf");
            if (!asdfTmp.exists()) asdfTmp.mkdirs();
            b.tempPath(asdfTmp.toPath());
        } );
    }

    public static FileAnalysisReport analyze(File infile, FileAnalysisReport.ReportType type) throws Exception {
        FileAnalysisReport report = new FileAnalysisReport(type, FormatUtil.Format.ASDF.name(), infile.length(), infile.getPath());
        Path path= infile.toPath();
        try (AsdfFile asdfFile = Asdf.open(path)) {
            DataAccessModel model = ModelFactory.get(asdfFile);
            if (model != null) {
                AsdfFileInfo asdfInfo = model.getAsdfFileInfo(asdfFile);
                if (asdfInfo != null) buildReport(asdfFile, model, report, asdfInfo);
            }
            if (report.getParts() == null || report.getParts().isEmpty()) {
                report.setMessage(ModelFactory.getError(asdfFile));
            }

        } catch (Exception e) {
            report.setMessage("Could not read ASDF file. ASDF reading is a beta feature, there are still many issues being worked out.");
        }
        return report;
    }


    public static void convertAsdfTableToDataGroup(TableParseHandler handler, File file, int tableIdx) throws IOException {
        Path path= file.toPath();
        try (AsdfFile asdfFile = Asdf.open(path)) {
            DataAccessModel model = ModelFactory.get(asdfFile);
            if (model == null) return;
            AsdfFileInfo asdfInfo = model.getAsdfFileInfo(asdfFile);
            model.tableToDataGroup(handler,asdfInfo, asdfFile, tableIdx);
        } catch (Exception e) {
            throw new IOException(e.toString(),e);
        } finally {
            handler.endTable(0);
            handler.end();
        }
    }

    public static DataGroup convertAsdfTableToDataGroup(File file, TableServerRequest request, int tableIdx) throws IOException {
        Path path = file.toPath();
        try (AsdfFile asdfFile = Asdf.open(path)) {
            DataAccessModel model = ModelFactory.get(asdfFile);
            if (model == null) return null;
            AsdfFileInfo asdfInfo = model.getAsdfFileInfo(asdfFile);
            return model.tableToDataGroup(file,asdfFile, asdfInfo,request,tableIdx);
        }
    }

    public static File createFitsFromAsdfFile(File infile) throws FitsException, IOException {
        Path path= infile.toPath();
        try (AsdfFile asdfFile = Asdf.open(path)) {
            DataAccessModel model = ModelFactory.get(asdfFile);
            if (model==null) return null;
            return model.createFitsFile(asdfFile, infile);
        } catch (RuntimeException e) {
            throw new IOException(e.toString(),e);
        }
    }


    private static void buildReport(AsdfFile asdfFile, DataAccessModel model, FileAnalysisReport report, AsdfFileInfo asdfInfo) {
        var imageInfo = model.getImageInfo(asdfFile, asdfInfo);
        int index = 0;
        for (AsdfCommon.ImageDetails id : imageInfo) {
            FileAnalysisReport.Part part = new FileAnalysisReport.Part(Image, "asdf image");
            part.setIndex(index++);
            part.setDesc(id.extName() + AsdfCommon.axisDesc(id.axisInfo()));
            part.setDetails(id.dg());
            report.addPart(part);
        }
        for (var table : asdfInfo.tables().entrySet()) {
            FileAnalysisReport.Part part = new FileAnalysisReport.Part(Table, "asdf table");
            part.setIndex(index++);
            var tableDetails = model.getTableDetails(asdfInfo.tablesRootNode(), table.getKey(), table.getValue());
            part.setDesc(table.getKey() + String.format(" (%d cols x %d rows)", tableDetails.col(), tableDetails.row()));
            part.setDetails(tableDetails.detailDG());
            report.addPart(part);
        }
    }
}
