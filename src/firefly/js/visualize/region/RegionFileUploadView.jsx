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
import {dispatchCreateDrawLayer,
        dispatchAttachLayerToPlot} from '../DrawLayerCntlr.js';
import {getDrawLayerById, isDrawLayerAttached } from '../PlotViewUtil.js';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {getDS9Region} from '../../rpc/PlotServicesJson.js';
import {RegionFactory} from './RegionFactory.js';
import {dispatchHideDialog} from '../../core/ComponentCntlr.js';
import RegionPlot from '../../drawingLayers/RegionPlot.js';
import {get, has, isEmpty} from 'lodash';

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

function* createDrawLayerId() {
    var idCnt = 0;

    while (true) {
        yield `${regionDrawLayerId}-${idCnt++}`;
    }
}

var drawLayerIdGen = createDrawLayerId();

/**
 * get region json from server, convert json into Region objects and make RegionPlot DrawLayer
 * @param request
 * @param rgComp
 * @param drawLayerId
 */
function uploadAndProcessRegion(request, rgComp, drawLayerId) {
    const [FieldKeyErr, RegionErr, DrawObjErr, JSONErr] = [
            'no region file uploaded yet',
            'invalid description in region file',
            'create drawing object error',
            'get region json error' ];

    var regionFile = get(request, rgUploadFieldKey);
    var setStatus = (ret, message)  => {
            var s = {upload: !!ret, message};

            if (rgComp) {
                rgComp.setState(s);
            }

            if (ret) {
                var plotId = get(visRoot(), 'activePlotId');

                if (isEmpty(drawLayerId)) {
                    drawLayerId = drawLayerIdGen.next().value;
                }
                var dl = getDrawLayerById(getDlAry(), drawLayerId);
                if (!dl) {
                    dispatchCreateDrawLayer(regionDrawLayerId, {title: message, regions: ret, drawLayerId});
                }
                if (!isDrawLayerAttached(dl, plotId)) {
                    dispatchAttachLayerToPlot(drawLayerId, plotId);
                }
                dispatchHideDialog(popupId);
            }
    };


    if (!regionFile) {
        setStatus(false, FieldKeyErr);
    } else {
        getDS9Region(regionFile)
            .then((result) => {
                if (!has(result, 'RegionData') || result.RegionData.length === 0) {
                    // temporarily showing some hard coded regions which are defined but currently not identified by server
                    /*
                    result.RegionData = [
                        'J2000;ellipse 202.55556, 47.188286 20p 40p 0i # color=#48f text={ellipse 1} width=10',
                        'physical;ellipse 100 400 20p 40p 30p 60p 40p 80p 2i # color=green text={ellipseannulus 2}',
                        'image;box 100 100 20p 40p 30p 50p 70p 100p 30 # color=red text={slanted box annulus 3}'];
                    */

                    return setStatus(false, RegionErr);
                }
                var rgAry = RegionFactory.parseRegionJson(result.RegionData);

                if (rgAry) {
                    setStatus(rgAry, get(result, 'Title', 'Region Plot'));
                } else {
                    setStatus(false, DrawObjErr);
                }
            }, ()=> {
                setStatus(false, JSONErr);
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
        uploadAndProcessRegion(request, this, drawLayerIdGen.next().value);
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

