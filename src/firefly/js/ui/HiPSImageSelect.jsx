/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {HiPSId, updateHiPSTblHighlightOnUrl, HiPSSurveyTableColumn} from '../visualize/HiPSListUtil.js';
import {HiPSSurveyListSelection, HiPSPopupMsg, getTblModelOnPanel, getHiPSSourcesChecked} from './HiPSSurveyListDisplay.jsx';
import {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {ValidationField} from './ValidationField.jsx';
import {getCellValue} from '../tables/TableUtil.js';
import {DEFAULT_FITS_VIEWER_ID} from '../visualize/MultiViewCntlr.js';
import WebPlotRequest from '../visualize/WebPlotRequest.js';
import {parseWorldPt} from '../visualize/Point.js';

import './ImageSelect.css';

const hipsPanelId = HiPSId;

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
                                            surveysId={hipsPanelId}
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
 * @param {WebPlotRequest} request
 * @param {String} plotId
 * @param {String} groupId
 */
export function makeHiPSWebPlotRequest(request, plotId, groupId= DEFAULT_FITS_VIEWER_ID) {
    let url;
    if (get(request, 'imageSource', 'archive') === 'url') {
        url = get(request, 'txURL').trim();
        updateHiPSTblHighlightOnUrl(url, hipsPanelId, getHiPSSourcesChecked());
    } else {
        const sources = getHiPSSourcesChecked();
        if (!sources) {
            HiPSPopupMsg('No HiPS source selected', 'HiPS search');
            return null;
        }
        const tableModel = getTblModelOnPanel(hipsPanelId);
        if (!tableModel) {
            HiPSPopupMsg('No HiPS information found', 'HiPS search');
            return null;
        }
        const {highlightedRow=0} = tableModel;
        url = getCellValue(tableModel, highlightedRow, HiPSSurveyTableColumn.Url.key);
        if (url) {
            url = url.trim();
        }
    }

    if (!url) {
        HiPSPopupMsg('No HiPS URL found', 'HiPS search');
        return null;
    }

    const fov = get(request, 'sizeFov', 180);
    const wp = parseWorldPt(request.UserTargetWorldPt) || null;
    const wpRequest = WebPlotRequest.makeHiPSRequest(url, wp, Number(fov) || NaN);
    wpRequest.setPlotGroupId(groupId);
    wpRequest.setPlotId(plotId);
    return wpRequest;
}

