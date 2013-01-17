package edu.caltech.ipac.vamp.commands;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.CommonRequestCmd;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.ui.table.builder.PrimaryTableUILoader;
import edu.caltech.ipac.vamp.core.Vamp;
import edu.caltech.ipac.vamp.ui.creator.ImageGridPanelCreator;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Apr 20, 2010
 * Time: 1:21:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class AvmSearchCmd extends CommonRequestCmd {
    public static final String COMMAND_NAME = "AvmSearch";
        public static final String SEARCH_FIELD_PROP = "AvmSearch.field.search";

        private InputField queryField;

        public AvmSearchCmd() {
            super(COMMAND_NAME);
        }

        protected Form createForm() {

            queryField = FormBuilder.createField(SEARCH_FIELD_PROP);
            Widget fieldPanel = FormBuilder.createPanel(50, queryField);

            HTML desc = GwtUtil.makeFaddedHelp("Enter the string on which you'd like to search.");
            Label spacer = new Label("");
            spacer.setHeight("5px");
            VerticalPanel vp = new VerticalPanel();
            vp.add(fieldPanel);
            vp.add(desc);
            vp.add(spacer);

            Form form = new Form();
            form.add(vp);
            form.setHelpId("searching.byAVM");
            return form;
        }

        @Override
        protected void onFormSubmit(Request req) {
            mask("Loading...");
        }

        protected void processRequest(final Request inputReq, final AsyncCallback<String> callback) {
            String queryId = "avmSearch";

            Map<String, String> tableParams = new HashMap<String, String>(3);
            Map<String, String> previewParams = new HashMap<String, String>(3);

            previewParams.put("PREVIEW_COLUMN", "download");
            previewParams.put("RESOURCE_TYPE", "URL");

            tableParams.put(ImageGridPanelCreator.TITLE, queryId);
            tableParams.put(ImageGridPanelCreator.SHORT_DESC, "AVM Search Results");


            TableServerRequest req= new TableServerRequest(queryId, inputReq);
            WidgetFactory factory = Application.getInstance().getWidgetFactory();

            final PrimaryTableUI primary = factory.createPrimaryUI(Vamp.IMAGE_TABLE, req, tableParams);
            final TablePreview dataViewer = factory.createObserverUI(Vamp.META_RESOURCE_VIEW, previewParams);
            //final TablePreview spectralViewer = factory.createObserverUI(Vamp.SPECTRAL_VIEW, previewParams);

            final SplitLayoutPanel sp = new SplitLayoutPanel();
            sp.setSize("100%", "75%");

            TablePreviewEventHub hub = new TablePreviewEventHub();
            primary.bind(hub);
            dataViewer.bind(hub);
            //spectralViewer.bind(hub);

            sp.addEast(GwtUtil.createShadowTitlePanel(dataViewer.getDisplay(), "Details"), 425);
            //sp.addWest(GwtUtil.createShadowTitlePanel(spectralViewer.getDisplay(), "Spectral"), 125);
            sp.add(GwtUtil.createShadowTitlePanel(primary.getDisplay(), primary.getShortDesc()));

            PrimaryTableUILoader loader = new PrimaryTableUILoader(this);
            loader.addTable(primary);
            loader.loadAll();

            this.setResults(sp);

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