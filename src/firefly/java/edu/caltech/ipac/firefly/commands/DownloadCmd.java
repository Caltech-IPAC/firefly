/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.table.DownloadSelectionIF;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.WebClassProperties;


public class DownloadCmd extends GeneralCommand {

    public static String CommandName = "download";
    private static WebClassProperties _prop;
    private final TableDataView _dataset;
    private DownloadSelectionIF _selectionDialog;
    private DownloadSelectionIF.Validator _validator;


    public DownloadCmd(TableDataView dataset, DownloadSelectionIF selDialog, String cmdName) {
        super(cmdName);
        CommandName = cmdName;
        _prop= new WebClassProperties(CommandName);

        _dataset= dataset;
        _selectionDialog = selDialog;
        _validator = _selectionDialog.getValidator() == null ?
                        new DownloadSelectionIF.MinMaxValidator(_selectionDialog) : _selectionDialog.getValidator();

        setHighlighted(computeHighlighted());

        _dataset.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent pce) {
                setHighlighted(computeHighlighted());
            }
        });
    }

    public DownloadCmd(TableDataView dataset, DownloadSelectionIF selDialog) {
        this(dataset, selDialog, CommandName);
    }


    protected boolean computeHighlighted() {
        return _dataset != null && _dataset.getSelectionInfo().getSelectedCount() > 0;
    }

    protected boolean computeEnabled() {
        return true;
    }

    protected void doExecute() {

       if (_validator.validate()) {
           _selectionDialog.show();
       }
       else {
           PopupUtil.showError(_validator.getErrorTitle(), _validator.getErrorMsg());
       }
    }

}