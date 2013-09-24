package edu.caltech.ipac.hydra.ui.wise;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.WiseRequest;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseFormEventWorker;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.HashSet;
import java.util.Set;


/**
 * @author tatianag
 *         $Id: WiseFormEventWorker.java,v 1.9 2012/03/13 23:54:27 tatianag Exp $
 */
public class WiseFormEventWorker extends BaseFormEventWorker {

    private static final String SOURCE_ID_TO_SOURCE_IMAGE_SET_RULE = "SourceIdToSourceImageSetRule";
    private static final String SOURCE_ID_TO_IMAGE_SET_RULE = "SourceIdToImageSetRule";
    private static final String SOURCE_ID_TO_PROD_LEVEL_RULE = "SourceIdToProductLevelRule";
    private static final String SCAN_ID_TO_IMAGE_SET_RULE = "ScanIdToImageSetRule";


    private String rule;
    private String fieldName;
    private String syncFieldName;
    private boolean publicRelease;
    private boolean validationOnly;

    public WiseFormEventWorker(String rule, String fieldName, String syncFieldName, boolean publicRelease, boolean validationOnly) {
        this.rule = rule;
        this.fieldName = fieldName;
        this.syncFieldName = syncFieldName;
        this.publicRelease = publicRelease;
        this.validationOnly = validationOnly;
    }


    public void bind(FormHub hub) {
        if (hub!=null) {
            addHub(hub);

            WebEventListener wel = new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    // we never want to change a value of the visible field
                    // it looks awkward to the user
                    // the best we can do is to give a validation error message
                    String value = null;
                    if (ev.getName().equals(FormHub.FIELD_VALUE_CHANGE)) {
                        Param param= (Param)ev.getData();
                        String name = param.getName();
                        if (name.equals(fieldName)) value = param.getValue();
                    } else {
                        value = getValue(fieldName);
                    }

                    if (!StringUtils.isEmpty(value)) {
                        if (rule.equals(SOURCE_ID_TO_IMAGE_SET_RULE)) {
                            // image set is visible in internal release, but hidden in public release
                            // for non-preliminary data
                            boolean completed = false;
                            String currentSchema = getValue(WiseRequest.SCHEMA);
                            if (publicRelease && ev.getName().equals(FormHub.FIELD_VALUE_CHANGE)) {
                                // "yes" in selection field means it's preliminary release
                                String preliminarySelection = getValue("preliminary_data");
                                if (StringUtils.isEmpty(preliminarySelection) || preliminarySelection.equals("no")) {
                                    currentSchema = WiseRequest.ALLSKY_4BAND;
                                    String imageSet = WiseRequest.getImageSetFromSourceId(value, publicRelease, currentSchema);
                                    if (!StringUtils.isEmpty(imageSet)) {
                                        setValue(new Param(syncFieldName, imageSet));
                                    }
                                    completed = true;
                                }
                            }

                            if (completed) return;

                            if (ev.getName().equals(FormHub.FORM_VALIDATION)) {
                                String imageSet = WiseRequest.getImageSetFromSourceId(value, publicRelease, currentSchema);
                                if (!StringUtils.isEmpty(imageSet)) {
                                    String syncFieldValue = getValue(syncFieldName);
                                    if (StringUtils.isEmpty(syncFieldValue) || !syncFieldValue.contains(imageSet)) {
                                        FormHub.Validated validated = (FormHub.Validated)ev.getData();
                                        validated.invalidate("For the given Source ID, Image Set must be "+WiseRequest.getImageSetDescription(imageSet)+".");
                                    }
                                }
                            }
                        } else if (rule.equals(SOURCE_ID_TO_SOURCE_IMAGE_SET_RULE)) {
                            // source image set is always a hidden field
                            String imageSet = WiseRequest.getImageSetFromSourceId(value, publicRelease, getValue(WiseRequest.SCHEMA));
                            if (!StringUtils.isEmpty(imageSet)) {
                                setValue(new Param(syncFieldName, imageSet));
                            }
                        } else if (rule.equals(SOURCE_ID_TO_PROD_LEVEL_RULE)) {
                            // product level is a hidden field in ref source panel
                            String productLevel = WiseRequest.getProductLevelFromSourceId(value);
                            if (!StringUtils.isEmpty(productLevel)) {
                                setValue(new Param(syncFieldName, productLevel));
                            }
                        } else if (rule.equals(SCAN_ID_TO_IMAGE_SET_RULE)) {
                            // image set is visible in internal release but hidden in public release
                            // for non-preliminary data
                            boolean completed = false;
                            if (publicRelease && ev.getName().equals(FormHub.FIELD_VALUE_CHANGE)) {
                                // "yes" in selection field means it's preliminary release
                                String preliminarySelection = getValue("preliminary_data");
                                if (StringUtils.isEmpty(preliminarySelection) || preliminarySelection.equals("no")) {
                                    setValue(new Param(syncFieldName, WiseRequest.ALLSKY_4BAND+","+WiseRequest.CRYO_3BAND+","+WiseRequest.POSTCRYO));
                                    completed = true;
                                }
                            }

                            if (completed) return;

                            if (ev.getName().equals(FormHub.FORM_VALIDATION)) {
                                Set<String> imageSets = new HashSet<String>();
                                String[] scanIdArray = value.split("[,; ]+");
                                for (String scanId : scanIdArray) {
                                    for (String imageSet : WiseRequest.getImageSetsForScanID(scanId, publicRelease)) {
                                        imageSets.add(imageSet);
                                    }
                                }
                                if (imageSets.size() > 0) {
                                    String syncFieldValue = getValue(syncFieldName);
                                    boolean validValue = false; {
                                        for (String s : imageSets) {
                                            if (syncFieldValue.contains(s)) {
                                                validValue = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!validValue) {
                                        Object[] validSets = imageSets.toArray();
                                        String validSetsStr = WiseRequest.getImageSetDescription((String)validSets[0])+
                                                (validSets.length == 1 ? "." : " or "+WiseRequest.getImageSetDescription((String)validSets[1])+".");

                                        FormHub.Validated validated = (FormHub.Validated)ev.getData();
                                        validated.invalidate("For the given Scan ID(s), Image Set must be "+validSetsStr);
                                    }
                                }
                            }

                        }
                    }
                }

            };
            if (!validationOnly) {
                hub.getEventManager().addListener(FormHub.FIELD_VALUE_CHANGE, wel);
                hub.getEventManager().addListener(FormHub.ON_SHOW, wel);
            }
            hub.getEventManager().addListener(FormHub.FORM_VALIDATION, wel);
        }
    }

    public void bind(EventHub hub) {
        // not applied
    }
}
