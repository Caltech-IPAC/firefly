/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.JsonToDataGroup;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.DsvTableIO;
import edu.caltech.ipac.table.io.FITSTableReader;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.SpectrumMetaInspector;
import edu.caltech.ipac.table.io.TableParseHandler;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.FormatUtil;

import static edu.caltech.ipac.util.FormatUtil.Format.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Date: 10/24/24
 *
 * @author loi
 * @version : $
 */
public class DbDataIngestor {

    public static FileInfo ingestData(TableServerRequest req, DbAdapter dbAdapter, Consumer<DataGroup> extraMetaSetter, File srcFile, int tblIdx) throws DataAccessException {
        return ingestData(req, dbAdapter, extraMetaSetter, srcFile, tblIdx, null);
    }

    public static FileInfo ingestData(TableServerRequest req, DbAdapter dbAdapter, Consumer<DataGroup> extraMetaSetter, File srcFile, int tblIdx, FormatUtil.Format format) throws DataAccessException {

        // spectrum hint as well as some other parameters in `req` are coupled with how FITSTableReader read the table.
        // this is why `req` is needed as a parameter here.
        boolean searchForSpectrum = SpectrumMetaInspector.hasSpectrumHint(req);
        try {
            if (srcFile == null || !srcFile.canRead()) throw new FileNotFoundException("File not found or inaccessible: " + srcFile);

            String source = srcFile.getAbsolutePath();
            format = format == null ? FormatUtil.detect(srcFile) : format;
            return switch (format) {
                case IPACTABLE -> ingestIpacTable(dbAdapter, srcFile, extraMetaSetter, searchForSpectrum);
                case VO_TABLE -> ingestVoTable(dbAdapter, source, extraMetaSetter, tblIdx, searchForSpectrum);
                case CSV, TSV, PARQUET -> ingestDuckReadable(format, dbAdapter, source, extraMetaSetter, searchForSpectrum);
                case FITS -> ingestFitsTable(req, dbAdapter, source, tblIdx);
                case JSON -> ingestJsonTable(req, dbAdapter, srcFile, searchForSpectrum);
                default -> throw new DataAccessException("Unsupported format (%s), file: %s".formatted(format, source));
            };
        } catch (IOException e) {
            throw new DataAccessException(e);
        }
    }

    public static FileInfo ingestData(TableServerRequest req, DbAdapter dbAdapter, DbAdapter.DataGroupSupplier dataGroupSupplier) throws DataAccessException {

        if (dataGroupSupplier == null) return null;
        boolean searchForSpectrum = SpectrumMetaInspector.hasSpectrumHint(req);
        return ingestTable(dbAdapter, dataGroupSupplier.get(), searchForSpectrum);
    }

    //====================================================================
    //  Supporting functions
    //====================================================================

    static FileInfo ingestDuckReadable(FormatUtil.Format format, DbAdapter dbAdapter, String source, Consumer<DataGroup> extraMetaSetter, boolean searchForSpectrum)  throws IOException, DataAccessException {
        if (dbAdapter instanceof DuckDbAdapter) {
            return DuckDbReadable.castInto(format, dbAdapter).ingestDataDirectly(source, extraMetaSetter);
        } else if (format == PARQUET) {
            throw new DataAccessException("Unsupported format (%s), file: %s".formatted(format, source));
        } else {
            DataGroup table = DsvTableIO.parse(new File(source), format);
            return ingestTable(dbAdapter, table, searchForSpectrum);
        }
    }

    static FileInfo ingestVoTable(DbAdapter dbAdapter, String source, Consumer<DataGroup> extraMetaSetter, int tblIdx, boolean searchForSpectrum) throws IOException, DataAccessException {
        if (dbAdapter instanceof DuckDbAdapter) {
            VoTableReader.parse(new TableParseHandler.DbIngest(dbAdapter, extraMetaSetter, searchForSpectrum), source, tblIdx);
            return new FileInfo(dbAdapter.getDbFile());
        } else {
            DataGroup table = VoTableReader.voToDataGroups(source, tblIdx)[0];
            return ingestTable(dbAdapter, table, searchForSpectrum);
        }
    }

    static FileInfo ingestIpacTable(DbAdapter dbAdapter, File source, Consumer<DataGroup> extraMetaSetter, boolean searchForSpectrum) throws IOException, DataAccessException {
        if (dbAdapter instanceof DuckDbAdapter) {
            IpacTableReader.parseTable(new TableParseHandler.DbIngest(dbAdapter, extraMetaSetter, searchForSpectrum), source);   // only the first table.
            return new FileInfo(dbAdapter.getDbFile());
        } else {
            DataGroup table = IpacTableReader.read(source);
            return ingestTable(dbAdapter, table, searchForSpectrum);
        }
    }

    static FileInfo ingestFitsTable(TableServerRequest req, DbAdapter dbAdapter, String source, int tableIndex) throws IOException, DataAccessException {
        var table = FITSTableReader.convertFitsToDataGroup(source, req, FITSTableReader.DEFAULT, tableIndex);
        return ingestTable(dbAdapter, table, false);        //logic is already in the reader
    }

    static FileInfo ingestJsonTable(TableServerRequest req, DbAdapter dbAdapter, File srcFile, boolean searchForSpectrum) throws IOException, DataAccessException {
        var table = JsonToDataGroup.parse(srcFile, req);
        return ingestTable(dbAdapter, table, searchForSpectrum);
    }

    static FileInfo ingestTable(DbAdapter dbAdapter, DataGroup table, boolean searchForSpectrum) throws DataAccessException {
        if (searchForSpectrum) SpectrumMetaInspector.searchForSpectrum(table,true);
        dbAdapter.ingestData(() -> table, dbAdapter.getDataTable());
        return new FileInfo(dbAdapter.getDbFile());
    }
}
