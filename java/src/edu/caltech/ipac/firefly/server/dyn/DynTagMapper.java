package edu.caltech.ipac.firefly.server.dyn;

import com.thoughtworks.xstream.XStream;
import edu.caltech.ipac.firefly.data.dyn.xstream.AccessTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.AndTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.CatalogTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ConditionTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ConstrainedParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ConstraintsTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.DownloadTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.EventWorkerTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.FieldGroupTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.FormEventWorkerTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.FormTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.HelpTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.HtmlLoaderTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LabelTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutAreaTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutContentTypeTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutTypeTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.OrTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.PreDefFieldTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.PreviewTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectItemTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectListTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.QueryTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ResultTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchFormParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchGroupTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchTypeTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SplitPanelTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.TableTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ViewTag;
import edu.caltech.ipac.firefly.server.dyn.xstream.CatalogConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.EventWorkerConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.FieldGroupConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.FormEventWorkerConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.HtmlLoaderConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.LayoutAreaConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.LayoutConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.PreviewConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.ProjectConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.QueryConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.ResultConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.SplitPanelConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.TableConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.ViewConverter;
import edu.caltech.ipac.firefly.server.util.Logger;


public class DynTagMapper {

    private static final Logger.LoggerImpl logger = Logger.getLogger();

    public static void doMappings(XStream xstream) {

        // since XML contains 'id', must alias the System's 'id'
        xstream.aliasSystemAttribute("refid", "id");

        // process annotations & register custom converters
        xstream.registerConverter(new ProjectConverter());
        xstream.registerConverter(new CatalogConverter());
        xstream.registerConverter(new FieldGroupConverter());
        xstream.registerConverter(new LayoutConverter());
        xstream.registerConverter(new LayoutAreaConverter());
        xstream.registerConverter(new SplitPanelConverter());
        xstream.registerConverter(new PreviewConverter());
        xstream.registerConverter(new TableConverter());
        xstream.registerConverter(new ViewConverter());
        xstream.registerConverter(new FormEventWorkerConverter());
        xstream.registerConverter(new EventWorkerConverter());
        xstream.registerConverter(new QueryConverter());
        xstream.registerConverter(new ResultConverter());
        xstream.registerConverter(new HtmlLoaderConverter());

        Class[] classArr = {AccessTag.class, AndTag.class, CatalogTag.class, ConditionTag.class, ConstrainedParamTag.class, ConstraintsTag.class,
                DownloadTag.class, EventWorkerTag.class, FieldGroupTag.class, FormEventWorkerTag.class, FormTag.class,
                HelpTag.class, HtmlLoaderTag.class, LabelTag.class, LayoutTag.class, OrTag.class, ParamTag.class, PreDefFieldTag.class,
                PreviewTag.class, ProjectItemTag.class, ProjectListTag.class, ProjectTag.class, QueryTag.class, ResultTag.class,
                SearchFormParamTag.class, SearchGroupTag.class, SearchTypeTag.class, SplitPanelTag.class, TableTag.class, ViewTag.class};
        xstream.processAnnotations(classArr);
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
