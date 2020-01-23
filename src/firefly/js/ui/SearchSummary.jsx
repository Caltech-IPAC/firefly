/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {sprintf} from '../externalSource/sprintf';
import {isEmpty} from 'lodash';
import {formatPosForTextField} from '../data/form/PositionFieldDef.js';

import {parseWorldPt} from '../visualize/Point.js';


export class SearchSummary extends PureComponent {
    constructor(props) {
        super(props);
    }

    render() {
        const {style = {color:'#a67e45', fontSize:'13px', padding:'5px 0px 5px 5px', whiteSpace: 'nowrap'}, request} = this.props;
        let message = '';

        if (request) {
            let wpMsg = request.filename?'Multi-Object':'';
            if(!request.filename && request.UserTargetWorldPt){
                const wp = parseWorldPt(request.UserTargetWorldPt);
                wpMsg = isEmpty(wp.getObjName())?formatPosForTextField(wp):wp.objName;
            }
            const target = 'Target= ' + wpMsg;
            const imageSize = 'Image Size=' + sprintf('%.4f',request.imageSizeAndUnit)+ ' deg';
            const sources = 'Sources=' + request.selectImage.toUpperCase();

            if (target) message += target + '; ';

            if (imageSize) message += imageSize + '; ';

            if (sources) message += sources;
        }
        return (
            <div style={style}>
                {message}
            </div>
        );
    }


}


SearchSummary.propTypes = {
    style: PropTypes.object,
    message: PropTypes.string
};

