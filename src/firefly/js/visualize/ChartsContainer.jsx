/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import sCompare from 'react-addons-shallow-compare';

import {flux} from '../Firefly.js';

import {LO_EXPANDED, dispatchSetLayoutMode, getExpandedMode} from '../core/LayoutCntlr.js';
import {CloseButton} from '../ui/CloseButton.jsx';

import {ChartsTableViewPanel} from '../visualize/ChartsTableViewPanel.jsx';

export class ChartsContainer extends Component {
    constructor(props) {
        super(props);
        const expandedMode = getExpandedMode() === LO_EXPANDED.xyPlots.view;
        this.state = {
            expandedMode
        };
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }

    storeUpdate() {
        const expandedMode = getExpandedMode() === LO_EXPANDED.xyPlots.view;
        this.setState({expandedMode});
    }

    render() {
        const {expandedMode} = this.state;
        return expandedMode ? <ExpandedView {...this.props} /> : <ChartsTableViewPanel {...this.props} />;
    }
}

function ExpandedView(props) {
    return (
        <div style={{ display: 'flex', flex: 'auto', flexDirection: 'column', overflow: 'hidden'}}>
            <div style={{marginBottom: 3}}>
                <CloseButton style={{display: 'inline-block', paddingLeft: 10}}
                             onClick={() => dispatchSetLayoutMode(LO_EXPANDED.none)}/>
            </div>
            <div style={{ display: 'flex', flex: 'auto', flexDirection: 'row', overflow: 'hidden'}}>
                <ChartsTableViewPanel expandedMode={true} expandable={false} {...props} />
            </div>
        </div>
    );

}
