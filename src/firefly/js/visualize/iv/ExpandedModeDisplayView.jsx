/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes, Component} from 'react';
import {ExpandedSingleView}  from './ExpandedSingleView.jsx';
import {ExpandedGridView}  from './ExpandedGridView.jsx';
import {ExpandType} from '../ImagePlotCntlr.js';

export function ExpandedModeDisplayView({visRoot}) {
    if (visRoot.expandedMode===ExpandType.COLLAPSE) return <div></div>;
    return (
        visRoot.expandedMode===ExpandType.GRID ?
            <ExpandedGridView visRoot={visRoot}/> :
            <ExpandedSingleView visRoot={visRoot}/>
    );
}

ExpandedModeDisplayView.propTypes= {
    visRoot : PropTypes.object.isRequired
};

