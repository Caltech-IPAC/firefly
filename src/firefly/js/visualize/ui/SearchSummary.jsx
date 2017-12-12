/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../../Firefly.js';
import {parseWorldPt} from '../Point.js';
import numeral from 'numeral';

export class SearchSummary extends PureComponent {
    constructor(props) {
        super(props);
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        if (this.removeListener) this.removeListener();
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }


    /**
     * If the object changed then check if any of the following changed.
     *  This is a optimization so that the element does not re-render every time
     */
    storeUpdate() {

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
    request: PropTypes.object
};

