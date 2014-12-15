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
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static edu.caltech.ipac.firefly.util.DataSetParser.VISI_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.makeAttribKey;


abstract public class DynQueryProcessor extends IpacTablePartProcessor {

    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

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
                    DataGroupPart.TableDef meta = IpacTableParser.getMetaInfo(dataFile);
                    if (meta.getRowCount() < 100000) {
                        SortInfo si = SortInfo.parse(p.getValue());
                        if (si != null) {
                            // apply default sort if one is given
                            doSort(dataFile, dataFile, si, request.getPageSize());
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
        if (projectId == null) return req;

        String queryId = req.getParam(DynUtils.QUERY_ID);
        if (queryId == null) return req;

        String searchName = req.getParam(DynUtils.SEARCH_NAME);
        ProjectTag obj = DynConfigManager.getInstance().getCachedProject(projectId);

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

