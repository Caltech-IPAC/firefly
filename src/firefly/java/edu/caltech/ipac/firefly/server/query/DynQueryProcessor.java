/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.QueryTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchTypeTag;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.dyn.DynConfigManager;
import edu.caltech.ipac.firefly.server.util.ipactable.TableDef;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static edu.caltech.ipac.firefly.util.DataSetParser.VISI_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.makeAttribKey;


abstract public class DynQueryProcessor extends IpacTablePartProcessor {

    public File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        File dataFile = loadDynDataFile(request);

        // no search results situation
        if (dataFile == null || !dataFile.exists() || dataFile.length() == 0) {
            return dataFile;
        }

        String projectId = request.getParam(DynUtils.HYDRA_PROJECT_ID);
        String searchName = request.getParam("searchName");
        String queryId = request.getParam(DynUtils.QUERY_ID);
        QueryTag q = findQueryTag(projectId, searchName, queryId);
        if (q != null) {
            for (ParamTag p : q.getParams()) {
                if (p.getKey().equals(SortInfo.SORT_INFO_TAG)) {
                    TableDef meta = IpacTableUtil.getMetaInfo(dataFile);
                    if (meta.getRowCount() < 100000) {
                        SortInfo si = SortInfo.parse(p.getValue());
                        if (si != null) {
                            // apply default sort if one is given
                            doSort(dataFile, dataFile, si, request);
                        }
                        break;
                    }

                }
            }
        }

        return dataFile;
    }

    protected abstract File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException;

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);

        if (!request.containsParam(DynUtils.HYDRA_PROJECT_ID)) return;

        String projectId = request.getParam(DynUtils.HYDRA_PROJECT_ID);
        String searchName = request.getParam("searchName");
        String queryId = request.getParam(DynUtils.QUERY_ID);

        QueryTag q = findQueryTag(projectId, searchName, queryId);
        if (q != null) {
            // add all metadata params to TableMeta
            List<ParamTag> pList = q.getMetadata();
            for (ParamTag p : pList) {
                meta.setAttribute(p.getKey(), p.getValue());
            }
        }

        // MetaData set Attributes of all SelectedColumns to visible
        String str = request.getParam(CatalogRequest.SELECTED_COLUMNS);
        if (!StringUtils.isEmpty(str)) {
            List<String> cols = StringUtils.asList(str, ",");
            for (String col : cols) {
                meta.setAttribute(makeAttribKey(VISI_TAG, col.toLowerCase()), "show");
            }
        }
    }

    private QueryTag findQueryTag(String projId, String searchName, String queryId) {

        if (projId == null || searchName == null || queryId == null) return null;

        ProjectTag obj = DynConfigManager.getInstance().getCachedProject(projId);
        // find searchType
        List<SearchTypeTag> stList = obj.getSearchTypes();
        for (SearchTypeTag st : stList) {

            String stName = st.getName();
            if (stName.equalsIgnoreCase(searchName)) {

                // find query
                List<QueryTag> qList = st.getQueries();
                for (QueryTag q : qList) {
                    String qId = q.getId();
                    if (qId.equalsIgnoreCase(queryId)) {
                        return q;
                    }
                }
            }
        }
        return null;
    }

    public static ServerRequest setXmlParams(ServerRequest req) {
        if (!req.containsParam(DynUtils.HYDRA_PROJECT_ID)) return req;

        // XML params only apply to Hydra applications
        String projectId = req.getParam(DynUtils.HYDRA_PROJECT_ID);
        if (StringUtils.isEmpty(projectId)) return req;

        ProjectTag obj = DynConfigManager.getInstance().getCachedProject(projectId);

        if (obj != null) {
            // add any project's wide parameters into the request
            List<ParamTag> params = obj.getParams();
            if (params != null && params.size() > 0) {
                for (ParamTag p : params) {
                    if (p != null && p.getKey() != null) {
                        req.setParam(p.getKey(), p.getValue());
                    }
                }
            }
        }

        String queryId = req.getParam(DynUtils.QUERY_ID);
        if (StringUtils.isEmpty(queryId)) return req;

        String searchName = req.getParam(DynUtils.SEARCH_NAME);

        // find searchType
        List<SearchTypeTag> stList = obj.getSearchTypes();
        for (SearchTypeTag st : stList) {
            boolean breakST = false;

            String stName = st.getName();
            if (stName.equalsIgnoreCase(searchName)) {

                // find query
                List<QueryTag> qList = st.getQueries();
                for (QueryTag q : qList) {
                    String qId = q.getId();
                    if (qId.equalsIgnoreCase(queryId)) {

                        // add all params to Request
                        List<ParamTag> pList = q.getParams();
                        for (ParamTag p : pList) {
                            if (req.getParam(p.getKey()) == null) {
                                req.setParam(p.getKey(), p.getValue());
                            }
                        }

                        breakST = true;
                        break;
                    }
                }

                if (breakST)
                    break;
            }
        }

        return req;
    }


    public static DataGroup.Attribute createAttribute(TemplateGenerator.Tag tag, String col, String value) {
        return new DataGroup.Attribute(tag.getName().replaceFirst(
                "@", col), value);
    }
}

