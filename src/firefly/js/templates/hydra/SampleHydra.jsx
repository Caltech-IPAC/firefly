/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import {get} from 'lodash';
import SplitPane from 'react-split-pane';

import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {LO_VIEW} from '../../core/LayoutCntlr.js';
import {SplitContent} from '../../ui/panel/DockLayoutPanel.jsx';
import {TABLE_SEARCH} from '../../tables/TablesCntlr.js';

import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {TriViewImageSection} from '../../visualize/ui/TriViewImageSection.jsx';
import {dispatchLoadSearches} from '../../core/AppDataCntlr.js';


export function sampleHydra () {
    dispatchLoadSearches(sampleSearches);
}

const params = {
    id: 'GatorQuery',
    use: 'catalog_primary',
    SearchMethod: 'Cone',
    catalogProject: 'WISE',
    catalog: 'allwise_p3as_psd'
};

function resultDesc(request) {
    return `${request.catalog}_${request.radius}`;
}

const group1 = {
    title: 'Sample Group',
    searchItems : {
        'search1 tri-view': {
            desc: 'a short description of search1',
            form: {
                params,
                render: WiseForm,
                title: resultDesc,
                action: TABLE_SEARCH
            },
            renderStandardView: Triview
        },
        'search2 table only': {
            desc: 'a short description of search2',
            form: {
                params,
                render: WiseForm,
                title: resultDesc,
                action: TABLE_SEARCH
            },
            renderStandardView: TableOnly
        }
    }
};

const group2 = {
    title: 'Sample Group 2',
    searchItems : {
        'search3 table only': {
            title: 'using title to override label w/o desc',
            form: {
                params,
                render: WiseForm,
                title: resultDesc,
                action: TABLE_SEARCH
            },
            renderStandardView: TableOnly
        },
        'search4 table-chart': {
            desc: 'a short description of search4',
            form: {
                params,
                render: WiseForm,
                title: resultDesc,
                action: TABLE_SEARCH
            },
            renderStandardView: TableChart
        }
    }
};

const sampleSearches = [group1, group2];


function WiseForm ({searchItem}) {
    const groupKey = searchItem.name;
    return (
        <FieldGroup key={groupKey} groupKey={groupKey} keepState={true}>
            <h3 style={{textAlign:'center'}}> {searchItem.desc} </h3>

            <div style={{padding:'5px 0 5px 0'}}>
                <TargetPanel />
                <ValidationField fieldKey='radius' initialState= {{value: 300, size: 4, label : 'Radius:'}}
                />
            </div>
        </FieldGroup>
    );
}

function Triview({layout}) {
    const {mode={}, images={}} = layout;
    const {expanded, closeable} = mode;

    return (
        <div style={{flexGrow:1, display:'flex', flexDirection:'column'}}>
            <div style={{flexGrow:1, position:'relative'}}>
                <SplitPane split='horizontal' maxSize={-20} minSize={20} defaultSize={'60%'}>
                    <SplitPane split='vertical' maxSize={-20} minSize={20} defaultSize={'50%'}>
                        <SplitContent>
                            <TriViewImageSection key='res-tri-img'
                                                 closeable={closeable}
                                                 imageExpandedMode={expanded===LO_VIEW.images}
                                {...images}  />
                        </SplitContent>
                        <SplitContent>
                            <TablesContainer expandedMode={expanded===LO_VIEW.tables}
                                             tableOptions={{help_id:'main1TSV.table'}}/>
                        </SplitContent>
                    </SplitPane>
                    <SplitContent>
                        <ChartsContainer closeable={true}
                                         expandedMode={expanded===LO_VIEW.xyPlots}/>
                    </SplitContent>
                </SplitPane>
            </div>
        </div>
    );
}


function TableChart ({layout}) {
    const expanded = get(layout, 'mode.expanded');
    return (
        <SplitPane split='horizontal' maxSize={-20} minSize={20} defaultSize={'60%'}>
            <SplitContent>
                <TablesContainer expandedMode={expanded===LO_VIEW.tables}
                                 tableOptions={{help_id:'main1TSV.table'}}/>
            </SplitContent>
            <SplitContent>
                <ChartsContainer closeable={true}
                                 expandedMode={expanded===LO_VIEW.xyPlots}/>
            </SplitContent>
        </SplitPane>
    );
}

function TableOnly ({layout}) {
    const expanded = get(layout, 'mode.expanded');
    return (
        <TablesContainer expandedMode={expanded===LO_VIEW.tables}
                         tableOptions={{help_id:'main1TSV.table'}}/>
    );
}
