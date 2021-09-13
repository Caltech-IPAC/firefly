/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {isEmpty} from 'lodash';
import {sprintf} from '../externalSource/sprintf';
import {formatPosForTextField} from './PositionFieldDef.js';
import {parseWorldPt} from '../visualize/Point.js';

const DEF_STYLE= {color:'#a67e45', fontSize:'13px', padding:'5px 0px 5px 5px', whiteSpace: 'nowrap'}

export const SearchSummary =  memo(({request, style = DEF_STYLE}) => {
    if (!request) return ( <div style={style}/>);

    let wpMsg = request.filename?'Multi-Object':'';
    if(!request.filename && request.UserTargetWorldPt){
        const wp = parseWorldPt(request.UserTargetWorldPt);
        wpMsg = isEmpty(wp.getObjName())?formatPosForTextField(wp):wp.objName;
    }
    const target = 'Target= ' + wpMsg;
    const imageSize = 'Image Size=' + sprintf('%.4f deg', parseFloat(request.imageSizeAndUnit));
    const sources = 'Sources=' + request.selectImage.toUpperCase();

    let message = '';
    if (target) message += target + '; ';
    if (imageSize) message += imageSize + '; ';
    if (sources) message += sources;

    return ( <div style={style}> {message} </div> );
});

SearchSummary.propTypes = {
    style: PropTypes.object,
    request: PropTypes.object,
    message: PropTypes.string
};

