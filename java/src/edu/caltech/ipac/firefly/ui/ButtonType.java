package edu.caltech.ipac.firefly.ui;

public enum ButtonType {

    OK_CANCEL(BaseDialog.ButtonID.OK, BaseDialog.ButtonID.CANCEL),
    OK(BaseDialog.ButtonID.OK),
    OK_HELP(BaseDialog.ButtonID.OK, BaseDialog.ButtonID.HELP),
    OK_CANCEL_HELP(BaseDialog.ButtonID.OK, BaseDialog.ButtonID.CANCEL, BaseDialog.ButtonID.HELP),
    OK_APPLY_CANCEL_HELP(BaseDialog.ButtonID.OK, BaseDialog.ButtonID.APPLY, BaseDialog.ButtonID.CANCEL, BaseDialog.ButtonID.HELP),
    APPLY_HELP(BaseDialog.ButtonID.APPLY, BaseDialog.ButtonID.HELP),
    REMOVE(BaseDialog.ButtonID.REMOVE),
    REMOVE_HELP(BaseDialog.ButtonID.REMOVE, BaseDialog.ButtonID.HELP),
    YES_NO(BaseDialog.ButtonID.YES, BaseDialog.ButtonID.NO),
    YES_NO_HELP(BaseDialog.ButtonID.YES, BaseDialog.ButtonID.NO),
    NO_BUTTONS(),
    OTHER();

    private final BaseDialog.ButtonID _ids[];

    ButtonType(BaseDialog.ButtonID... ids) { _ids = ids; }
    ButtonType() { _ids= null; }

    public BaseDialog.ButtonID[] getIDs() {
        return _ids;
    }
}
