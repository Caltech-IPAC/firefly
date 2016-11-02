/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';

import {LO_VIEW, LO_MODE, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {DEFAULT_PLOT2D_VIEWER_ID} from '../../visualize/MultiViewCntlr.js';

import {CloseButton} from '../../ui/CloseButton.jsx';
import {ChartPanel} from './ChartPanel.jsx';
import {MultiChartViewer} from './MultiChartViewer.jsx';


/**
 * Default viewer
 */
export class ChartsContainer extends Component {
    constructor(props) {
        super(props);

    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }

    render() {
        const {chartId, expandedMode, closeable} = this.props;

        if (chartId) {
            return expandedMode ?
                <ExpandedView key='chart-expanded' closeable={closeable} chartId={chartId}/> :
                <ChartPanel key={chartId} expandable={true} chartId={chartId}/>;
        } else {
            return (
                <MultiChartViewer closeable={closeable} viewerId={DEFAULT_PLOT2D_VIEWER_ID} expandedMode={expandedMode}/>
            );
        }
    }
}

ChartsContainer.propTypes = {
    expandedMode: PropTypes.bool,
    closeable: PropTypes.bool,
    chartId: PropTypes.string,
    tblId: PropTypes.string
};

function ExpandedView(props) {
    const {closeable, chartId} = props;

    return (
        <div style={{position: 'absolute', top: 0, left: 0, right: 0, bottom: 0}}>
            <ChartPanel key={'expanded-'+chartId} expandedMode={true} expandable={false} chartId={chartId}/>
            {closeable && <CloseButton style={{position: 'absolute', top: 0, left: 0, paddingLeft: 10}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>}
        </div>
    );
}

ExpandedView.propTypes = {
    closeable: PropTypes.bool,
    chartId: PropTypes.string
};