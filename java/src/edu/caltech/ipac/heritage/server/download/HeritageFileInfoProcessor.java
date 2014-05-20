package edu.caltech.ipac.heritage.server.download;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.SpecificPoints;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileInfoProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.util.MinMax;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.data.entity.download.HeritageFileRequest;
import edu.caltech.ipac.heritage.server.persistence.HeritageSecurityModule;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.Set;

/**
 * @author tatianag
 *         $Id: HeritageFileInfoProcessor.java,v 1.19 2012/09/06 22:18:10 tatianag Exp $
 */
@SearchProcessorImpl(id ="heritageFileRequest")
public class HeritageFileInfoProcessor extends FileInfoProcessor {

    protected FileInfo loadData(ServerRequest sr) throws IOException, DataAccessException {
        HeritageFileRequest req= QueryUtil.assureType(HeritageFileRequest.class,sr);
        if (req.hasDataType(DataType.LEGACY)) {
            // always public access
            String filePath = InventoryFileGroupsProcessor.INVENTORY_BASE_PREFIX+ File.separator+req.getDataIdAsStr();
            return new FileInfo(filePath, FileUtil.getBase(filePath), 0);
        } else if (req.hasDataType(DataType.SM)) {
            // always public access
            String filePath = HeritageFileGroupsProcessor.SM_BASE_PREFIX+File.separator+req.getDataIdAsStr();
            return new FileInfo(filePath, FileUtil.getBase(filePath), 0);
        } else if (req.hasDataType(DataType.IRS_ENHANCED)) {
                return getIrsEnhancedSpectra(req);
        } else {
            return super.loadData(sr);
        }
    }


    private static FileInfo getIrsEnhancedSpectra(HeritageFileRequest req) throws IOException, DataAccessException {
        String filePath= req.getDataIdAsStr();

        File origFile = new File(filePath);
        if (!VisContext.isFileInPath(origFile)) {
            throw new DataAccessException("Can not access "+filePath);
        }

        if (origFile.exists() && origFile.canRead()) {
            String title = origFile.getName();

            // find specific points info
            // if present, add attribute for Specific Points
            try {
                // TODO: make more efficient: just add an Attribute line
                DataGroup dg = IpacTableReader.readIpacTable(origFile, title);
                dg.getDataDefintion("wavelength").setUnits("microns");
                SpecificPoints specificPoints = new SpecificPoints();
                specificPoints.setDescription("Synthetic Photometry");
                String [] specificKeys = {"IRAC8", "IRAS12", "WISE12", "IRS16", "IRS22", "WISE22", "MIPS24", "IRAS25"};
                Set<String> attrKeys = dg.getAttributeKeys();
                for (String key : specificKeys) {
                    if (attrKeys.contains(key) && attrKeys.contains(key+"U")) {
                        addSpecificPoint(specificPoints, key, dg.getAttribute(key).formatValue(),dg.getAttribute(key+"U").formatValue());
                    }
                }
                if (specificPoints.getNumPoints() > 0) {
                    dg.addAttributes(new DataGroup.Attribute(SpecificPoints.SERIALIZATION_KEY, specificPoints.toString()));
                    dg.addAttributes(new DataGroup.Attribute("DOWNLOAD_SOURCE", VisContext.replaceWithPrefix(origFile)));
                    // save as a local file
                    File newFile = File.createTempFile("irsEnhanced", ".tbl", ServerContext.getTempWorkDir());
                    IpacTableWriter.save(newFile, dg);
                    return new FileInfo(newFile.getAbsolutePath(), origFile.getName(), newFile.length());
                } else {
                    return new FileInfo(origFile.getAbsolutePath(), origFile.getName(), origFile.length());
                }
            } catch (IpacTableException e) {
                Logger.error(e, "Unable to read "+title);
                return null;
            }
        } else {
            Logger.error("File does not exist or is not readable: "+filePath);
            return null;
        }
    }

    // flux JY
    private static void addSpecificPoint(SpecificPoints specificPoints, String key, String flux_str, String fluxU_str) {
        int id;
        String desc, label;
        MinMax xMinMax;
        double flux = Double.parseDouble(flux_str);
        double fluxU = Double.parseDouble(fluxU_str);
        // Some filters have no valid value.  This is indicated by a flux value of -99.0
        if (flux < -90) { return; }

        // filters
        if (key.equals("IRAC8")) {
            label = "IRAC8";
            desc = "Synthetic Photometry: <br>IRAC8: "+flux_str+ " Jy at 8 um"+
                    "<br>Uncertainty: "+fluxU_str+ " Jy";
            xMinMax = new MinMax(6.28,9.57,7.67);
            id = 0;
        } else if (key.equals("IRAS12")) {
            label="IRAS12";
            desc = "Synthetic Photometry: <br>IRAS12: "+flux_str+ " Jy at 12 um"+
                    "<br>Uncertainty: "+fluxU_str+ " Jy";
            xMinMax =  new MinMax(7.52,14.92,12.00);
            id = 1;
        } else if (key.equals("WISE12")) {
            label="WISE12";
            desc = "Synthetic Photometry: <br>WISE12: "+flux_str+ " Jy at 12 um"+
                    "<br>Uncertainty: "+fluxU_str+ " Jy";
            xMinMax = new MinMax(7.46,17.37,11.29);
            id = 6;
        } else if (key.equals("IRS16")) {
            label = "IRS16";
            desc = "Synthetic Photometry: <br>IRS16: "+flux_str+ " Jy at 16 um"+
                    "<br>Uncertainty: "+fluxU_str+ " Jy";
            xMinMax = new MinMax(12.96,19.23,15.38);
            id = 2;
        } else if (key.equals("IRS22")) {
            label = "IRS22";
            desc = "Synthetic Photometry: <br>IRS22: "+flux_str+ " Jy at 22 um"+
                    "<br>Uncertainty: "+fluxU_str+ " Jy";
            xMinMax = new MinMax(18.05,30.75,21.77);
            id = 3;
        } else if (key.equals("WISE22")) {
            label = "WISE22";
            desc = "Synthetic Photometry: <br>WISE22: "+flux_str+ " Jy at 22 um"+
                    "<br>Uncertainty: "+fluxU_str+ " Jy";
            xMinMax = new MinMax(19.57,27.19,22.02);
            id = 7;
        } else if (key.equals("MIPS24")) {
            label = "MIPS24";
            desc = "Synthetic Photometry: <br>MIPS24: "+flux_str+ " Jy at 24 um"+
                    "<br>Uncertainty: "+fluxU_str+ " Jy";
            xMinMax = new MinMax(19.91,30.75,23.51);
            id = 4;
        } else if (key.equals("IRAS25")) {
            label = "IRAS25";
            desc = "Synthetic Photometry: <br>IRAS25: "+flux_str+ " Jy at 25 um"+
                    "<br>Uncertainty: "+fluxU_str+ " Jy";
            xMinMax = new MinMax(16.02,30.41,25.00);
            id = 5;
        } else {
            throw new IllegalArgumentException("Unknown specific point "+key);
        }
        MinMax yMinMax = new MinMax(flux-fluxU, flux+fluxU, flux);
        specificPoints.addPoint(id, label, desc, xMinMax, yMinMax);
    }


    protected FileInfoRowMapper makeRowMapper(TableServerRequest request) {
        return new FileInfoRowMapper(){

            public FileInfo mapRow(ResultSet rs, int rowNum) {
                try {
                    FileInfo fi = new FileInfo(rs.getString(1), rs.getString(2), rs.getLong(3));
                    //set access for proprietary data owners
                    fi.setHasAccess(HeritageSecurityModule.checkHasAccess(rs.getString(4)));
                    return fi;
                } catch (Exception e) {
                    Logger.error(e, "Failed to map result set to file info");
                    return null;
                }
            }
        };
    }

    public DbInstance getDbInstance() {
        return DbInstance.archive;
    }

    public String getSql(TableServerRequest request) {
//        assert (request instanceof HeritageFileRequest);
        HeritageFileRequest req= QueryUtil.assureType(HeritageFileRequest.class,request);
        String sql = "select heritagefilename, externalname, filesize, reqkey ";
        if (req.hasDataType(DataType.BCD) || req.hasDataType(DataType.MOS)) {
            sql += "from bcdproducts where bcdid=";
        } else if (req.hasDataType(DataType.PBCD)){
            sql += "from postbcdproducts where pbcdid=";
        } else {
            assert false;
        }
        sql += req.getDataId();
        return sql;
    }

    public Object[] getSqlParams(TableServerRequest request) {
        return new Object[0];
    }
}
