package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.persistence.TempTable;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.heritage.searches.SearchByPosition;
import edu.caltech.ipac.planner.io.FixedSingleTargetParser;
import edu.caltech.ipac.target.Fixed;
import edu.caltech.ipac.target.Target;
import edu.caltech.ipac.targetgui.TargetList;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.net.URLParms;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.caltech.ipac.firefly.util.DataSetParser.*;

/**
 * @author tatianag $Id: QueryByPosition.java,v 1.62 2012/11/21 21:38:33 tlau Exp $
 */
public class QueryByPosition {
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    private static final String BCD_SERVER_URL = AppProperties.getProperty("RTree.server.url") + "?dataset=" +
                                                 URLParms.encode(AppProperties.getProperty("RTree.level1.dataset")) + "&region=";
    private static final String PBCD_SERVER_URL = AppProperties.getProperty("RTree.server.url") + "?dataset=" +
                                                 URLParms.encode(AppProperties.getProperty("RTree.level2.dataset")) + "&region=";
    private static final String ENHANCED_IMAGES_SERVER_URL = AppProperties.getProperty("RTree.server.url") + "?dataset=" +
                                                 URLParms.encode(AppProperties.getProperty("RTree.enhancedImages.dataset")) + "&region=";


    private static String TEMP_TABLE = "temp_ids";
    private static String TEMP_ID_COL = "id";

    private static Collection<Integer> getAorKeys(TableServerRequest request) throws DataAccessException {
        DataGroup bcd = getBcdData(request);
        DataGroup pbcd = getPbcdData(request);

        return getReqKeys(bcd, pbcd);
    }

    private static SearchByPosition.Req assureType(TableServerRequest request) {
        if (request.containsParam(SearchByPosition.MultiTargetReq.UPLOAD_FILE_KEY)) {
            return QueryUtil.assureType(SearchByPosition.MultiTargetReq.class, request);
        } else if (request.containsParam(ReqConst.USER_TARGET_WORLD_PT)) {
            return QueryUtil.assureType(SearchByPosition.SingleTargetReq.class, request);
        } else {
            return null;
        }
    }


    @SearchProcessorImpl(id ="aorByPosition")
    public static class Aor extends AorQuery {
        private Collection<Integer> ids;

        @Override
        protected boolean onBeforeQuery(TableServerRequest request, DataSource datasource) throws IOException, DataAccessException {
            if (!super.onBeforeQuery(request, datasource)) { return false; }

            ids = getAorKeys(request);
            if (ids == null) {
                throw new IllegalArgumentException("Unable to extract reqkeys");
            }

            if (TempTable.useTempTable(ids)) {
                QueryByRequestID.loadTempTable(datasource, ids);
             }

            return ids.size() > 0;
        }

        @Override
        protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
            SearchByPosition.Req req = assureType(request);
            if (req instanceof SearchByPosition.MultiTargetReq) {
                return handleMultiTargetSearch((SearchByPosition.MultiTargetReq) req, this);
            } else {
                return super.loadDataFile(req);
            }
        }

        protected SqlParams makeSqlParams(TableServerRequest request) {
            return QueryByRequestID.getAorQueryParams(ids);
        }

        public BcdQuery getBcdQuery() {
            return new BcdQuery() {
                protected SqlParams makeSqlParams(TableServerRequest request) {
                    String sql= "select bcdid ";
                    if (TempTable.useTempTable(ids)) {
                        sql += " from bcdproducts, " + QueryByRequestID.TEMP_TABLE + " t" +
                            " where bcdproducts.reqkey = t."+ QueryByRequestID.TEMP_ID_COL;
                    } else {
                        // Should be using temp table and col from QueryByRequestID
                        // because SQLParams come from there
                        sql += " from bcdproducts " +
                            " where bcdproducts.reqkey in (" + CollectionUtil.toString(ids)+")";
                    }
                    return new HeritageQuery.SqlParams(sql);
                }
            };
        }

    }

    @SearchProcessorImpl(id ="bcdByPosition")
    public static class Bcd extends BcdQuery {

        private Set<Integer> ids;
        private Collection<Integer> reqkeys = null;

        @Override
        protected boolean onBeforeQuery(TableServerRequest request, DataSource datasource) throws IOException, DataAccessException {
            if (!super.onBeforeQuery(request, datasource)) { return false; }

            SearchByPosition.Req req = assureType(request);
            DataGroup dg = getBcdData(req);
            ids = getBcdIds(dg);
            if (ids.size() < 1) {
                return false;
            }

            if (req.isMatchByAOR()) {
                // we are interested in all bcds related to matching AORs
                reqkeys = getAorKeys(req);
                if (TempTable.useTempTable(reqkeys)) {
                    loadTempTable(datasource, reqkeys);
                 }
            } else {
                reqkeys = null;
                if (TempTable.useTempTable(ids)) {
                    loadTempTable(datasource, ids);
                 }
            }

            return true;
        }

        @Override
        protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
            SearchByPosition.Req req = assureType(request);
            if (req instanceof SearchByPosition.MultiTargetReq) {
                return handleMultiTargetSearch((SearchByPosition.MultiTargetReq) req, this);
            } else {
                return super.loadDataFile(req);
            }
        }

        protected SqlParams makeSqlParams(TableServerRequest request) {
            String sql;
            if (reqkeys == null) {
                if (TempTable.useTempTable(ids)) {
                    sql = "select p.* from bcdproducts p, "+TEMP_TABLE+" t where p.bcdid=t."+TEMP_ID_COL;
                } else {
                    sql = "select * from bcdproducts where bcdid in ("+CollectionUtil.toString(ids)+")";
                }
            } else {
                // return all bcds that are related to matching AORs
                if (TempTable.useTempTable(reqkeys)) {
                    sql = "select p.* from bcdproducts p, "+TEMP_TABLE+" t where p.reqkey=t."+TEMP_ID_COL;
                } else {
                    sql = "select * from bcdproducts where reqkey in ("+CollectionUtil.toString(reqkeys)+")";
                }
            }
            return new SqlParams(sql);
        }
    }

    @SearchProcessorImpl(id ="pbcdByPosition")
    public static class Pbcd extends PbcdQuery {
        private Set<Integer> ids;
        private Collection<Integer> reqkeys = null;

        @Override
        protected boolean onBeforeQuery(TableServerRequest request, DataSource datasource) throws IOException, DataAccessException {
            if (!super.onBeforeQuery(request, datasource)) { return false; }

            SearchByPosition.Req req = assureType(request);
            DataGroup dg = getPbcdData(req);
            ids = getPbcdIds(dg);
            if (ids.size() < 1) {
                return false;
            }

            if (req.isMatchByAOR()) {
                // we are interested in all pbcds related to matching AORs
                reqkeys = getAorKeys(req);
                if (TempTable.useTempTable(reqkeys)) {
                    loadTempTable(datasource, reqkeys);
                 }
            } else {
                reqkeys = null;
                if (TempTable.useTempTable(ids)) {
                    loadTempTable(datasource, ids);
                 }
            }

            return true;
        }

        @Override
        protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
            SearchByPosition.Req req = assureType(request);
            if (req instanceof SearchByPosition.MultiTargetReq) {
                return handleMultiTargetSearch((SearchByPosition.MultiTargetReq) req, this);
            } else {
                return super.loadDataFile(req);
            }
        }
        protected SqlParams makeSqlParams(TableServerRequest request) {
            String sql;
            if (reqkeys == null) {
                if (TempTable.useTempTable(ids)) {
                    sql = "select p.* from postbcdproducts p, "+TEMP_TABLE+" t where p.pbcdid=t."+TEMP_ID_COL;
                } else {
                    sql = "select * from postbcdproducts where pbcdid in ("+CollectionUtil.toString(ids)+")";
                }
            } else {
                if (TempTable.useTempTable(reqkeys)) {
                    sql = "select p.* from postbcdproducts p, "+TEMP_TABLE+" t where p.reqkey=t."+TEMP_ID_COL;
                } else {
                    sql = "select * from postbcdproducts where reqkey in ("+CollectionUtil.toString(reqkeys)+")";
                }
            }
            return new SqlParams(sql);
        }

        public BcdQuery getBcdQuery() {
            return new BcdQuery() {
                protected SqlParams makeSqlParams(TableServerRequest request) {
                    String sql;
                    SearchByPosition.Req req = (SearchByPosition.Req)request;
                    if (req.isMatchByAOR()) {
                        sql= "select bcdid ";
                        if (TempTable.useTempTable(reqkeys)) {
                            sql += " from bcdproducts, " + TEMP_TABLE + " t" +
                                    " where bcdproducts.reqkey = t."+ TEMP_ID_COL;
                        } else {
                            sql += " from bcdproducts " +
                                    " where bcdproducts.reqkey in (" + CollectionUtil.toString(ids)+")";
                        }
                    } else {
                        sql= "select bcdid ";
                        if (TempTable.useTempTable(ids)) {
                            sql += " from bcdproducts where dceid in (select dceid from dcesets ds, postbcdproducts pb, " + TEMP_TABLE + " t" +
                                    " where pb.pbcdid = t."+ TEMP_ID_COL +" and ds.dcesetid=pb.dcesetid)";
                        } else {
                            sql += " from bcdproducts where dceid in (select dceid from dcesets ds, postbcdproducts pb " +
                                    " where pb.pbcdid in (" + CollectionUtil.toString(ids)+") and ds.dcesetid=pb.dcesetid)";
                        }
                    }
                    return new HeritageQuery.SqlParams(sql);
                }
            };
        }
    }

    @SearchProcessorImpl(id ="smByPosition")
    public static class EnhancedImages extends HeritageQuery {

        private static final ClassProperties COL_META = new ClassProperties("EnhancedImages", QueryByPosition.class);


        @Override
        protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
            SearchByPosition.Req req = assureType(request);
            if (req instanceof SearchByPosition.MultiTargetReq) {
                return handleMultiTargetSearch((SearchByPosition.MultiTargetReq) req, this);
            } else {
                DataGroup dg = getEnhancedImagesData(req);
                File workDir = doCache() ? ServerContext.getPermWorkDir() : ServerContext.getTempWorkDir();
                File file = File.createTempFile(req.getRequestId(), ".tbl", workDir);
                DataGroupWriter.write(file, dg, req.getPageSize());
                return file;
            }
        }

        @Override
        protected SqlParams makeSqlParams(TableServerRequest request) {
            return null;
        }

        @Override
        DataGroup.Attribute[] getAttributes() {
            return Utils.getEnhancedImagesAttributes();
        }

        @Override
        public void prepareTableMeta(TableMeta defaults, List<edu.caltech.ipac.util.DataType> columns, ServerRequest request) {
            super.prepareTableMeta(defaults, columns, request);
            defaults.setAttribute(DATA_TYPE, edu.caltech.ipac.heritage.data.entity.DataType.SM.toString());
            String base = "dd";
            if (COL_META.getItems(base) != null) {
                for(String s : COL_META.getItems(base)) {
                    String prop = base+"."+s;
                    String label = COL_META.getTitle(prop);
                    String tips = COL_META.getTip(prop);
                    String visi = COL_META.getDefault(prop);

                    label = label.startsWith(prop) ? s : label;
                    tips = tips.startsWith(prop) ? s : tips;
                    visi = visi.startsWith(prop) ? "show" : visi;

                    defaults.setAttribute(makeAttribKey(LABEL_TAG, s), label);
                    defaults.setAttribute(makeAttribKey(DESC_TAG, s), tips);
                    defaults.setAttribute(makeAttribKey(VISI_TAG, s), visi);
                }
            }

        }

    }

    //====================================================================
    //
    //====================================================================

    private static void loadTempTable(DataSource ds, Collection<Integer> ids) throws DataAccessException {
        try {
            TempTable.loadIdsIntoTempTable(ds.getConnection(), ids, TEMP_TABLE, TEMP_ID_COL);
        } catch (Exception e) {
            Logger.error(e);
            throw new DataAccessException("Unable to load "+ids.size()+" ids into temp table.");
        }
    }


    private static void setAttributes(DataGroup.Attribute[] attribs, DataGroup dg) {
        if (attribs != null) {
            for (DataGroup.Attribute a : attribs) {
                dg.addAttributes(a);
            }
        }
    }

    public static DataGroup getBcdData(TableServerRequest request) throws DataAccessException {
        DataGroup dg = getData(BCD_SERVER_URL, request);
        setAttributes(Utils.getBcdAttributes(), dg);
        return dg;
    }

    public static DataGroup getPbcdData(TableServerRequest request) throws DataAccessException {
        DataGroup dg = getData(PBCD_SERVER_URL, request);
        setAttributes(Utils.getPbcdAttributes(), dg);
        return dg;

    }

    public static DataGroup getEnhancedImagesData(TableServerRequest request) throws DataAccessException {
        DataGroup dg = getData(ENHANCED_IMAGES_SERVER_URL, request);
        setAttributes(Utils.getEnhancedImagesAttributes(), dg);
        return dg;

    }

    private static DataGroup getData(String serverUrl, TableServerRequest request) throws DataAccessException {
        try {
            SearchByPosition.SingleTargetReq req =  QueryUtil.assureType(SearchByPosition.SingleTargetReq.class, request);
            File file = queryInventoryService(serverUrl, req.getPos(), req.getRadius(), req.getRequestId());
            //DataGroup dg = filterByWavelength(file, req.getRequestId(), req, null);
            DataGroup dg = DataGroupReader.read(file);
            dg.setTitle(req.getRequestId());
            return dg;
        } catch (Exception e) {
            throw new DataAccessException(e);
        }
    }


    private static File queryInventoryService(String serverURL, WorldPt pos, float radius, String filePrefix) throws IOException {
        pos =  Plot.convert(pos, CoordinateSys.EQ_J2000);

        URL url = new URL(serverURL + URLParms.encode(String.format("cone %f %f %f", pos.getLon(), pos.getLat(), radius)));

        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_FILE);
        File inf = (File) cache.get(new StringKey(url.toString()));
        if (inf == null) {
            BufferedReader ir = null;
            PrintWriter writer = null;
            try {
                LOGGER.debug("Position search using service URL:" + url);
                ir = new BufferedReader(new InputStreamReader(url.openStream()));
                inf = File.createTempFile("ps-"+filePrefix, ".tbl", ServerContext.getPermWorkDir());
                writer = new PrintWriter(inf);
                String l = ir.readLine();
                do {
                    writer.println(l);
                    l = ir.readLine();
                } while (l != null);
                cache.put(new StringKey(url.toString()), inf);
            } finally {
                if (ir != null) ir.close();
                if (writer != null) writer.close();
            }
        }
        return inf;

    }

    private static Collection<Integer> getReqKeys(DataGroup... dgs) {
        HashSet<Integer> keys = new HashSet<Integer>();
        if (dgs != null) {
            for (DataGroup dg : dgs) {
                for (int i = 0; i < dg.size(); i++) {
                    keys.add(Integer.parseInt(String.valueOf(dg.get(i).getDataElement("reqkey"))));
                }
            }
        }
        return keys;
    }

    private static Set<Integer> getBcdIds(DataGroup bcds) {
        HashSet<Integer> ids = new HashSet<Integer>();
        for (int i = 0; i < bcds.size(); i++) {
            ids.add(Integer.parseInt(String.valueOf(bcds.get(i).getDataElement("bcdid"))));
        }
        return ids;
    }

    private static Set<Integer> getPbcdIds(DataGroup pbcds) {
        HashSet<Integer> ids = new HashSet<Integer>();
        for (int i = 0; i < pbcds.size(); i++) {
            ids.add(Integer.parseInt(String.valueOf(pbcds.get(i).getDataElement("pbcdid"))));
        }
        return ids;
    }


    protected static DataGroup addDataToDataGroup(DataGroup source, DataGroup data, Param addtlCol) {

        edu.caltech.ipac.util.DataType trgCol = source == null ? null : source.getDataDefintion(addtlCol.getName());
        if (trgCol == null) {
            trgCol = new edu.caltech.ipac.util.DataType(addtlCol.getName(), String.class);
        }

        if (source == null) {
            ArrayList<DataType> defs = new ArrayList<DataType>();
            defs.add(0, trgCol);
            for(DataType dt : data.getDataDefinitions()) {
                try {
                    defs.add((DataType) dt.clone());
                } catch (CloneNotSupportedException e) {
                    LOGGER.error(e, e.getMessage());
                }
            }
            source = new DataGroup("combined", defs);
        }

        // make sure the columns are wide enough to accomadate the added data group.
        for(DataType dt : data.getDataDefinitions()) {
            DataType sdt = source.getDataDefintion(dt.getKeyName());
            if( dt.getFormatInfo().getWidth() > sdt.getFormatInfo().getWidth()) {
                sdt.getFormatInfo().setWidth(dt.getFormatInfo().getWidth());
            }
        }

        for (String key : data.getAttributeKeys()) {
            source.addAttributes(data.getAttribute(key));
        }

        for (DataObject d : data.values()) {
            DataObject sd = new DataObject(source);
            for (DataType dt : d.getDataDefinitions()) {
                sd.setDataElement(source.getDataDefintion(dt.getKeyName()), d.getDataElement(dt));
            }

            //check to make sure the column is wide enough for String value
            if(trgCol.getFormatInfo().getWidth() < addtlCol.getValue().length()){
                trgCol.getFormatInfo().setWidth(addtlCol.getValue().length() + 1);
            }

            sd.setDataElement(trgCol, addtlCol.getValue());
            source.add(sd);
        }
        return source;
    }


    private static File handleMultiTargetSearch(SearchByPosition.MultiTargetReq req, HeritageQuery handler)
                            throws IOException, DataAccessException {

        String uploadedFile = req.getUploadedFilePath();
        List<Target> targets = getTargetList(uploadedFile);

        if (targets == null) {
            throw new NullPointerException("Can not locate the uploaded file");
        }

        DataGroup dg = null;
        ArrayList<String> sources = new ArrayList<String>();
        for (Target t : targets) {
            if (!t.isFixed()) {
                // not supporting this for now
            } else {
                Fixed ft = (Fixed) t;
                String sourceID = ft.getName() != null ? ft.getName() :
                        ft.getPosition().convertLonToString() + " " + ft.getPosition().convertLonToString();

                SearchByPosition.SingleTargetReq sreq =  QueryUtil.assureType(SearchByPosition.SingleTargetReq.class, req);
                sreq.setPos( new WorldPt(ft.getPosition().getRa(), ft.getPosition().getDec()));
//                sreq.setRa((float) ft.getPosition().getRa());
//                sreq.setDec((float) ft.getPosition().getDec());
//                sreq.setCoordsys(CoordinateSys.EQ_J2000);
                sreq.removeParam(SearchByPosition.MultiTargetReq.UPLOAD_FILE_KEY);
                sreq.setPageSize(Integer.MAX_VALUE);
                sreq.setStartIndex(0);

                sources.add(sourceID);
                try {
                    File results = handler.getDataFile(sreq);
                    if (results != null) {
                        DataGroup moreData = DataGroupReader.read(results);
                        dg = addDataToDataGroup(dg, moreData, new Param("searchTgt", sourceID));
                    }
                } catch (IpacTableException e) {
                    LOGGER.error(e, "Failed to query data for target:" + sourceID);
                }
            }
        }
        if (dg != null) {
            dg.addAttributes(new DataGroup.Attribute("col.searchTgt.Label", "Search Tgt"));
            dg.addAttributes(new DataGroup.Attribute("col.searchTgt.ShortDescription", "Target from uploded targets file"));
            dg.addAttributes(new DataGroup.Attribute("col.searchTgt.Items", StringUtils.toString(sources, ",")));
        }

        File workDir = handler.doCache() ? ServerContext.getPermWorkDir() : ServerContext.getTempWorkDir();
        File file = File.createTempFile(req.getRequestId(), ".tbl", workDir);
        DataGroupWriter.write(file, dg, req.getPageSize());
        return file;
    }

    private static void useFixedSingleTargetParser(File f, TargetList targetList) throws IOException, EndUserException {
        FixedSingleTargetParser parser = new FixedSingleTargetParser(null);
        try {
            BufferedReader br = FileUtil.createBufferedReaderWithGuessedCharset(f);
            parser.parseTargets(br, targetList);
        } catch (UnsupportedEncodingException e) {
            throw new EndUserException("Input file uses unsupported encoding: "+e.getMessage(), e.getMessage());
        } catch (FixedSingleTargetParser.TargetParseException e) {
            throw new IOException(e);
        }
    }

    public static List<Target> getTargetList(String uploadedFile) throws DataAccessException, IOException {
        File ufile = VisContext.convertToFile(uploadedFile);
        if (ufile == null || !ufile.canRead()) {
            Logger.error("Unable to read uploaded file:" + uploadedFile);
            throw new DataAccessException(
                    new EndUserException("Position search failed.",
                               "Unable to read uploaded file.") );
        }
        //GNATS 9648: don't read binary file.
        if (FileUtil.isBinary(ufile)) {
            throw new DataAccessException(
                    new EndUserException(
                            "Exception while parsing the uploaded file: <br><b>Unable to parse binary file.</b>" ,
                            "Unable to parse file.") );
        }

        List<Target> targets=null;
        TargetList targetList= null;
        try {
            targetList= new TargetList();
            DataGroupReader.Format format = DataGroupReader.guessFormat(ufile);
            if (format.equals(DataGroupReader.Format.UNKNOWN) || format.equals(DataGroupReader.Format.FIXEDTARGETS)) {
                useFixedSingleTargetParser(ufile, targetList);
                targets = new ArrayList<Target>();
                for (Target t: targetList) {
                    if(t.getCoords() != null || t.getCoords().length() > 1){
                        targets.add(t);
                    }
                }
            }
            else
                targets = QueryUtil.getTargetList(ufile);


        } catch (IOException e) {
            if (targetList!=null && targetList.size()>0)
                throw new DataAccessException(
                        new EndUserException("Exception while parsing the uploaded file: <br>" + e.getMessage(),
                                e.getMessage()));
        } catch (EndUserException e) {
            throw new DataAccessException(e);
        }

        //if (targets==null || targets.size()==0) targets = QueryUtil.getTargetList(ufile);
        return targets;
    }
}
