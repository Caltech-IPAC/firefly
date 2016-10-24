/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';

import {LO_VIEW, LO_MODE, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {DEFAULT_PLOT2D_VIEWER_ID} from '../../visualize/MultiViewCntlr.js';

import {CloseButton} from '../../ui/CloseButton.jsx';
import {ChartsTableViewPanel} from './ChartsTableViewPanel.jsx';
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
                <ChartsTableViewPanel key={chartId} expandable={true} chartId={chartId}/>;
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
        <div style={{ display: 'flex', height: '100%', flexGrow: 1, flexDirection: 'column', overflow: 'hidden'}}>
            <div style={{marginBottom: 3}}>
                {closeable && <CloseButton style={{display: 'inline-block', paddingLeft: 10}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>}
            </div>
            <ChartsTableViewPanel key={'expanded-'+chartId} expandedMode={true} expandable={false} chartId={chartId}/>
        </div>
    );
}

ExpandedView.propTypes = {
    closeable: PropTypes.bool,
    chartId: PropTypes.string
};