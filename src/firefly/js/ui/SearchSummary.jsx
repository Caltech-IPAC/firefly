/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import numeral from 'numeral';

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
                wpMsg = wp.getObjName();
            }
            const target = 'Target= ' + wpMsg;
            const imageSize = 'Image Size=' + numeral(request.imageSizeAndUnit).format('#.0[000]')+' deg';
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

