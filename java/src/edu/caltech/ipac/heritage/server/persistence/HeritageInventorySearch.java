package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.inventory.InventorySearch;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.heritage.server.download.InventoryFileGroupsProcessor;
import edu.caltech.ipac.util.DataType;

import java.util.List;

/**
 * @author tatianag
 *         $Id: HeritageInventorySearch.java,v 1.2 2011/07/13 15:15:09 tatianag Exp $
 */
@SearchProcessorImpl(id ="heritageInventorySearch")
public class HeritageInventorySearch extends InventorySearch {

    private static final String COL_TO_PREVIEW_KEY = edu.caltech.ipac.heritage.ui.HeritagePreviewData.COL_TO_PREVIEW_KEY;
    public static final String INVENTORY_BASE_PREFIX_KEY = edu.caltech.ipac.heritage.ui.HeritagePreviewData.INVENTORY_BASE_PREFIX_KEY;

    private static String INVENTORY_BASE_PREFIX = InventoryFileGroupsProcessor.INVENTORY_BASE_PREFIX;

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);

        String sname = request.getParam("sname");
        String datatype = request.getParam("datatype");

        if (datatype == null) return;

        if (datatype.equals("spectra")) {
            // Not necessarily spectra table, can be FITS
            String spectraPreviewColumn = getSpectraPreviewColumn(columns);
            if (spectraPreviewColumn != null) {
                meta.setAttribute(COL_TO_PREVIEW_KEY, spectraPreviewColumn);
            } else {
                Logger.error("unable to find spectra preview column for "+sname);
            }
        } else if (datatype.equals("images")) {
            String imagePreviewColumn = getImagePreviewColumn(columns);
            if (imagePreviewColumn != null) {
                meta.setAttribute(COL_TO_PREVIEW_KEY, imagePreviewColumn);
            } else {
                Logger.error("unable to find image preview column for "+sname);
            }
        }
        meta.setAttribute(HeritageQuery.DATA_TYPE, edu.caltech.ipac.heritage.data.entity.DataType.LEGACY.toString());
        if (INVENTORY_BASE_PREFIX.contains("://")) {
            // inventory file path is URL, record base in meta
            meta.setAttribute(INVENTORY_BASE_PREFIX_KEY,INVENTORY_BASE_PREFIX);
        }
    }


    private String getSpectraPreviewColumn(List<DataType> columns) {
        String cName;
        for (DataType c : columns) {
            cName = c.getKeyName();
            if (!cName.endsWith("_u")) { continue; }
            if (cName.equals("Spectrum_TBL_u") ||
                    cName.equals("SpectrumTBL_u") ||
                    cName.equals("SpecTBL_u") ||
                    cName.equals("Spectrum_Download_u") ||
                    cName.equals("IRS_lo_TBL_u") ||
                    cName.equals("tbl_u") ||
                    cName.equals("CUBE_FITS_u")) {
                return cName;
            }
        }
        return null;
    }

    private String getImagePreviewColumn(List<DataType> columns) {
        String cName;
        for (DataType c : columns) {
            cName = c.getKeyName();
            if (cName.equals("fname")) {
                return cName;
            }
        }
        return null;
    }

}
