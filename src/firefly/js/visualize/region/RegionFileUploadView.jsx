/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * This Object contains the specifications of the DS9 region
 */
import React, {Component} from 'react';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {visRoot } from '../ImagePlotCntlr.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {getDrawLayerByType, isDrawLayerAttached } from '../PlotViewUtil.js';
import {dispatchCreateDrawLayer,
        dispatchAttachLayerToPlot,
        dispatchDetachLayerFromPlot,
        dispatchModifyCustomField} from '../DrawLayerCntlr.js';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {getDS9Region} from '../../rpc/PlotServicesJson.js';
import {RegionFactory} from './RegionFactory.js';
import {dispatchHideDialog} from '../../core/ComponentCntlr.js';
import RegionPlot from '../../drawingLayers/RegionPlot.js';
import {get} from 'lodash';

const popupId = 'RegionFilePopup';
const rgUploadGroupKey = 'RegionUploadGroup';
const rgUploadFieldKey = 'regionFileUpload';
const regionDrawLayerId = RegionPlot.TYPE_ID;

export function showRegionFileUploadPanel(popTitle) {
    var popup = (<PopupPanel title={popTitle}>
                    <RegionUpload />
                </PopupPanel>);

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}


/**
 * get region json from server, convert json into Region objects and make RegionPlot DrawLayer
 * @param request
 * @param rgComp
 */
function uploadAndProcessRegion(request, rgComp) {
    var regionFile = get(request, rgUploadFieldKey);
    var rgAry = null;
    var setStatus = (ret, message, plotId, rgComp)  => {
            var s = {upload: ret, message};

            if (rgComp) {
                rgComp.setState(s);
            }
            if (!ret) {
                dispatchDetachLayerFromPlot(regionDrawLayerId, plotId);
            }
    };

    var plotId = get(visRoot(), 'activePlotId');
    const dl = getDrawLayerByType(getDlAry(), regionDrawLayerId);

    if (!dl) {
        dispatchCreateDrawLayer(regionDrawLayerId);
    }

    if (!isDrawLayerAttached(dl, plotId)) {
        dispatchAttachLayerToPlot(regionDrawLayerId, plotId);
    }

    setStatus(true, '', rgComp);

    if (!regionFile) {
        setStatus(false, 'upload field key error', rgComp);
    } else {
        getDS9Region(regionFile)
            .then((result) => {
                if (!result.RegionData) {
                    // temporarily showing some hard coded regions which are defined but currently not identified by server
                    // remove the hard code setting later

                    result.RegionData = [
                        'J2000;ellipse 202.55556, 47.188286 20p 40p 3i # color=magenta text={ellipse 1}',
                        'physical;ellipse 100 400 20p 40p 30p 60p 40p 80p 2i # color=green text={ellipseannulus 2}',
                        'image;box 100 100 20p 40p 30p 50p 70p 100p 30 # color=red text={slanted box annulus 3}'];

                    //setStatus(false, 'region json error', plotId, rgComp);
                    //return;
                }
                rgAry = RegionFactory.parseRegionJson(result.RegionData);

                if (rgAry) {
                    dispatchModifyCustomField(regionDrawLayerId, {regions: rgAry}, plotId, false);
                } else {
                    setStatus(false, 'create drawing object error', plotId, rgComp);
                }
            }, ()=> {
                setStatus(false, 'get region file error', plotId, rgComp);
            });
    }
}

/**
 * popup panel for upload region file
 */
class RegionUpload extends Component {

    constructor(props) {
        super(props);
        this.state = {
            upload: true // upload fail
        };
        this.onUpload = this.onUpload.bind(this);
    }

    onUpload(request) {
        uploadAndProcessRegion(request, this);
    }

    render() {
        return (
            <div style={{padding: 10}}>
                <FieldGroup groupKey={rgUploadGroupKey}>
                    <FileUpload
                        wrapperStyle={{margin: '5px 0'}}
                        fieldKey={rgUploadFieldKey}
                        initialState={{
                            tooltip: 'Select a region file to upload',
                            label: 'Upload File:'}}
                    />
                    <div style={{color: 'red', height: 15}}>
                        {!this.state.upload && ( (this.state.message && `*${this.state.message}`) || '*region error')}
                    </div>

                    <div style={{marginTop: 40}}>
                        <CompleteButton
                            dialogId={popupId}
                            groupKey={rgUploadGroupKey}
                            onSuccess={this.onUpload}
                            closeOnValid={false}
                            text={'Draw'}/>
                    </div>
                </FieldGroup>
            </div>
        );
    }
}

