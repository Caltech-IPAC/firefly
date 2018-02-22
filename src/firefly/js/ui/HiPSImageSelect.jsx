/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {HiPSId} from '../visualize/HiPSCntlr.js';
import {HiPSSurveyListSelection, makeHiPSSurveysTableName, HiPSPopupMsg} from './HiPSSurveyListDisplay.jsx';
import {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {ValidationField} from './ValidationField.jsx';
import {getTblById, getCellValue} from '../tables/TableUtil.js';
import {DEFAULT_FITS_VIEWER_ID} from '../visualize/MultiViewCntlr.js';
import WebPlotRequest from '../visualize/WebPlotRequest.js';
import {parseWorldPt} from '../visualize/Point.js';

import './ImageSelect.css';


export class HiPSImageSelect extends PureComponent {

    constructor(props) {
        super(props);
        this.state= {lastMod:new Date().getTime(),
                    imageSource: getFieldVal(props.groupKey, 'imageSource')};
    }

    render() {
        const {style} = this.props;
        const {imageSource} = this.state;
        const wrapperStyle = imageSource === 'url' ?
                            Object.assign({}, style, {width: 'calc(100% - 6pt)',
                                                      height: 35,
                                                      alignItems: 'center'}) :
                            Object.assign({}, style, {display: 'flex',
                                                       flexDirection:'column',
                                                       alignItems: 'center'});

        return (
            <div style={style} className='ImageSelect'>
                {imageSource === 'url' ? <SelectUrl style={wrapperStyle}/> :
                                        <HiPSSurveyListSelection
                                            surveysId={HiPSId}
                                            wrapperStyle={ wrapperStyle }
                                        />}
            </div>
        );
    }
}

HiPSImageSelect.propTypes = {
    groupKey: PropTypes.string.isRequired,
    style: PropTypes.object
};


function SelectUrl({style}) {
    return (
        <div className='ImageSearch__section' style={style} title={'enter url of HiPS image'}>
            <div className='ImageSearch__section--title'>Enter URL</div>
            <ValidationField
                labelWidth={150}
                style={{width: 475}}
                fieldKey='txURL'
            />
        </div>
    );
}

SelectUrl.propTypes = {
    style: PropTypes.object
};

/**
 * create webPlotRequest for HiPS image request
 * @param request
 * @param plotId
 */
export function makeHiPSWebPlotRequest(request, plotId) {
    let url;
    if (get(request, 'imageSource', 'archive') === 'url') {
        url = get(request, 'txURL');
    } else {
        const tblId = makeHiPSSurveysTableName();
        const tableModel = tblId ? getTblById(tblId) : null;
        if (!tableModel) {
            HiPSPopupMsg('No HiPS information found', 'HiPS search');
            return null;
        }
        const {highlightedRow=0} = tableModel;
        url = getCellValue(tableModel, highlightedRow, 'url');
    }
    const fov = get(request, 'sizeFov', 180);
    const wp = parseWorldPt(request.UserTargetWorldPt) || null;
    const wpRequest = WebPlotRequest.makeHiPSRequest(url, wp, fov);
    wpRequest.setPlotGroupId(DEFAULT_FITS_VIEWER_ID);
    wpRequest.setPlotId(plotId);
    return wpRequest;
}

