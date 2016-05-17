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
        this.state = {
            expandedMode: props.expandedMode,
            closeable: props.closeable
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
        const {expandedMode, closeable} = this.state;
        return expandedMode ? <ExpandedView closeable={closeable} {...this.props}/> : <ChartsTableViewPanel {...this.props}/>;
    }
}

function ExpandedView(props) {
    const {closeable} = props;
    return (
        <div style={{ display: 'flex', flex: 'auto', flexDirection: 'column', overflow: 'hidden'}}>
            <div style={{padding: 10, flex: '0 0 auto'}}>
                {closeable && <CloseButton onClick={() => dispatchSetLayoutMode(LO_EXPANDED.none)}/>}
            </div>
            <div style={{ display: 'flex', flex: 'auto', alignItems: 'stretch', flexDirection: 'row', justifyContent: 'flex-start', overflow: 'hidden'}}>
                <ChartsTableViewPanel expandedMode={true} expandable={false} {...props} />
            </div>
        </div>
    );

}
