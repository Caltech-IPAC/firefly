package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.FormPanel;
import edu.caltech.ipac.firefly.data.ServletReply;
import edu.caltech.ipac.firefly.data.form.FileUploadFieldDef;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.MaskPane;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.util.dd.ValidationException;

/**
 * Date: Aug 10, 2010
 * @author loi
 *         $Id: FileUploadField.java,v 1.8 2011/02/19 01:25:23 loi Exp $
 */
public class FileUploadField extends InputField implements HasSubmitField {
    private static int cnt= 1;
    private FileUploadFieldDef fieldDef;
    private FileUpload field;
    private MaskPane uploadMask;
    private FormPanel uploadForm;
    private String _fieldValue= "";
    private AsyncCallback<String> callback;
    private FieldLabel _label= null;
    private String uploadKey= "FileUploadField_"+cnt;

    public FileUploadField(FileUploadFieldDef fieldDef) {
        this.fieldDef = fieldDef;
        field = new FileUpload();
        field.addChangeHandler(new ChangeHandler(){
            public void onChange(ChangeEvent event) {
                GWT.log(event.toDebugString(), null);
            }
        });
        int width = fieldDef.getPreferWidth() > 0 ? fieldDef.getPreferWidth() : 200;
        field.setName(fieldDef.getName());
        field.setWidth(width + "px");

        // use size to set field size
        String size = fieldDef.getSize();
        if (!StringUtils.isEmpty(size)) {
            GwtUtil.setFileUploadSize(field, size);
        }


        uploadForm = new FormPanel();
        // Because we're going to add a FileUpload widget, we'll need to set the
        // form to use the POST method, and multipart MIME encoding.
        uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        uploadForm.setMethod(FormPanel.METHOD_POST);
        uploadForm.addSubmitHandler(new FormPanel.SubmitHandler() {
            public void onSubmit(FormPanel.SubmitEvent event) {
                uploadMask = GwtUtil.mask("uploading file...", field);
            }
        });
        uploadForm.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
            public void onSubmitComplete(FormPanel.SubmitCompleteEvent event) {
                if (uploadMask != null) {
                    uploadMask.hide();
                }

                ServletReply reply = ServletReply.parse(event.getResults());
                //if (reply == null || reply.getValue() == null) {
                if (reply!=null && reply.getStatus() != 200) {
                    if (callback != null) {
                        callback.onFailure(new RuntimeException(reply.getMessage()));
                    }
                } else {
                    if (callback != null) {
                        if (reply==null) {
                            _fieldValue= uploadKey;
                            callback.onSuccess(uploadKey);
                        }
                        else {
                            _fieldValue= reply.getValue();
                            callback.onSuccess(reply.getValue());
                        }
                    }
                }
            }
        });
        uploadForm.add(field);
        initWidget(uploadForm);
        cnt++;
    }

    public FieldDef getFieldDef() {
        return fieldDef;
    }

    public FieldLabel getFieldLabel() {
        if (_label==null) {
            _label= new HTMLImmutableLabel(fieldDef.getLabel(), fieldDef.getShortDesc());
        }
        return _label;
    }

    public FocusWidget getFocusWidget() {
        return null;
    }

    public void reset() {
    }

    public boolean validate() {
        try {
            return fieldDef.validate(field.getFilename());
        } catch (ValidationException e) {
            return false;
        }
    }

    public void forceInvalid(String errorText) {
    }

    //TODO: what should this return;
    //TODO: I think it should return the cache key but it needs to be cleaned when it changes
    //TODO: it clearing out on keypress event posible with a FileUpload widget
    public String getValue() {
//        return field.getFilename();
        return _fieldValue;
    }

    public String getUploadFilename() {
        return field.getFilename();
    }


    public void setValue(String v) {
    }

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h) {
        return addHandler(h, ValueChangeEvent.getType());
    }

    public void submit(AsyncCallback<String> callback) {
        submit(null, callback);
    }

    public void submit(String action, AsyncCallback<String> callback) {

        this.callback = callback;
        if (action==null) {
            if (FFToolEnv.isAPIMode()) {
                action= GWT.getModuleBaseURL() + "/sticky/Firefly_FileUpload?cacheKey="+uploadKey;
            }
            else {
                action= GWT.getModuleBaseURL() + "/sticky/Firefly_FileUpload";
            }

        }

        if (validate()) {
            _fieldValue= "";
            if (StringUtils.isEmpty(field.getFilename())) {
                callback.onSuccess("");
                return;
            }
            uploadForm.setAction(action);
            uploadForm.submit();
        }
    }
}