/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.dpanalyze;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.core.FileAnalysisReport.Part;
import nom.tam.fits.Header;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.core.FileAnalysisReport.ChartTableDefOption.*;
import static edu.caltech.ipac.firefly.core.FileAnalysisReport.Type.*;

/**
 * @author Kartik Puri
 */
@DataProductAnalyzerImpl(id = "alert")
public class AlertAnalyzer implements DataProductAnalyzer {

    private static final int REQUIRED_TABLE_COUNT = 2;
    private static final int REQUIRED_IMAGE_COUNT = 3;
    private static final String TABLE_FROM_1D_PREFIX = "1D image, load as table, ";

    @Override
    public FileAnalysisReport analyzeFits(FileAnalysisReport inputReport,
                                          File inFile,
                                          String analyzerId,
                                          Map<String, String> params,
                                          Header[] headerAry) {

        if (inputReport == null || inputReport.getParts() == null || inputReport.getParts().isEmpty()) {
            return inputReport;
        }

        final List<Part> tableCandidates = new ArrayList<>();
        final List<Part> imageCandidates = new ArrayList<>();

        for (Part p : inputReport.getParts()) {
            if (p == null) continue;

            if (p.getType() == Table) {
                tableCandidates.add(copyTablePart(p));
            }
            else if (p.getType() == Image) {
                if (isTableLikeImage(p, headerAry)) {
                    tableCandidates.add(makeTablePartFrom1DImage(p));
                }
                else {
                    imageCandidates.add(copyImagePart(p));
                }
            }
        }

        if (tableCandidates.size() < REQUIRED_TABLE_COUNT || imageCandidates.size() < REQUIRED_IMAGE_COUNT) {
            return makeErrorReport(
                    inputReport,
                    analyzerId,
                    "Need at least 2 tables and 3 images. Found " +
                            tableCandidates.size() + " table-like HDUs and " +
                            imageCandidates.size() + " image HDUs."
            );
        }

        final List<Part> picked = new ArrayList<>(REQUIRED_TABLE_COUNT + REQUIRED_IMAGE_COUNT);

        //table 1: main table/chart
        Part mainTable = tableCandidates.get(0);
        mainTable.setChartTableDefOption(showChart);
        picked.add(mainTable);

        //table 2: details table
        Part detailsTable = tableCandidates.get(1);
        detailsTable.setChartTableDefOption(showTable);
        picked.add(detailsTable);

        //3 images
        for (int i = 0; i < REQUIRED_IMAGE_COUNT; i++) {
            Part img = imageCandidates.get(i);
            img.setChartTableDefOption(showImage);
            picked.add(img);
        }

        FileAnalysisReport out = inputReport.copy(false);
        out.replaceParts(picked);
        out.setDataProductsAnalyzerId(analyzerId);
        out.setAnalyzerFound(true);

        return out;
    }

    /**
     * Mirror Firefly's current client behavior:
     * makeSummaryModel treats Image with NAXIS==1 as table-like
     * findSingleAxisImages/getSelectedRows treats Image with NAXIS2==1 as table-like
     */
    private static boolean isTableLikeImage(Part p, Header[] headerAry) {
        if (p == null || headerAry == null || headerAry.length == 0) return false;

        int hduIdx = getHduIndex(p);
        if (hduIdx < 0 || hduIdx >= headerAry.length) return false;

        Header h = headerAry[hduIdx];
        if (h == null) return false;

        int naxis = h.getIntValue("NAXIS", -1);
        int naxis2 = h.getIntValue("NAXIS2", -1);

        return naxis == 1 || naxis2 == 1;
    }

    private static int getHduIndex(Part p) {
        return p.getFileLocationIndex() > -1 ? p.getFileLocationIndex() : p.getIndex();
    }

    private static Part makeTablePartFrom1DImage(Part src) {
        String desc = src.getDesc() == null ? "" : src.getDesc();
        Part p = new Part(Table, TABLE_FROM_1D_PREFIX + desc);

        return getPart(src, p);
    }

    private static Part getPart(Part src, Part p) {
        p.setIndex(src.getIndex());
        p.setFileLocationIndex(src.getFileLocationIndex());
        p.setDetails(src.getDetails());
        p.setTotalTableRows(src.getTotalTableRows());
        p.setTableDataType(src.getTableDataType());

        if (src.getTableColumnNames() != null) {
            p.setTableColumnNames(src.getTableColumnNames());
        }
        if (src.getTableColumnUnits() != null) {
            p.setTableColumnUnits(src.getTableColumnUnits());
        }
        if (src.getChartParams() != null) {
            p.setChartParams(src.getChartParams());
        }

        return p;
    }

    private static Part copyTablePart(Part src) {
        Part p = new Part(Table, src.getDesc());
        return getPart(src, p);
    }

    private static Part copyImagePart(Part src) {
        Part p = new Part(Image, src.getDesc());
        p.setIndex(src.getIndex());
        p.setFileLocationIndex(src.getFileLocationIndex());
        p.setDetails(src.getDetails());

        if (src.getChartParams() != null) {
            p.setChartParams(src.getChartParams());
        }

        return p;
    }

    private static FileAnalysisReport makeErrorReport(FileAnalysisReport inputReport,
                                                      String analyzerId,
                                                      String msg) {
        FileAnalysisReport out = inputReport.copy(false);
        Part err = new Part(ErrorResponse, "Invalid FITS for Alert Viewer");
        err.setDesc(msg);
        out.replaceParts(List.of(err));
        out.setDataProductsAnalyzerId(analyzerId);
        out.setAnalyzerFound(true);
        return out;
    }
}



