import {dispatchHideDropDown} from '../../core/LayoutCntlr';
import {toBoolean} from '../ApiUtil';
import {findExtraParams, makeExamples} from '../WebApi';
import {isEmpty} from 'lodash';
import {makeFileRequest} from '../../tables/TableRequestUtil';
import {dispatchTableSearch} from '../../tables/TablesCntlr';




const tableOverview= {
    overview: [
        'Load table to firefly, any parameters not listed below will be used as table metadata'
    ],
    parameters: {
        source: 'URL of the table - true/false',
        title: 'title of the table',
        showTitle : 'show the title - true/false',
        selectable : 'table is selectable - true/false',
        removable: 'table is removable - true/false',
        showUnits: 'show the units - true/false',
        showFilters: 'show the filters - true/false',
        showToolbar: 'show the toolbar - true/false',
        showPaging: 'show the paging controls - true/false',
        pageSize: 'page size - number',
    },
};
const tableExamples= [
    {desc:'Load A Table, set title to "a table"',
        params: {
            source: 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl',
            title: 'a table'
        }
    },
    {desc:'Load A Table.  Use meta data to set subHighlight on column "scangrp" and rename band column to "My Band". Set ShowFilter to false',
        params: {
            source: 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl',
            showFilters: false,
            'tbl.relatedCols': 'scangrp',
            'col.band.label': 'My Band',
        }
    }
];


const boolKeys=[ 'selectable' , 'showTitle', 'removable', 'showUnits', 'showFilters', 'showToolbar', 'showPaging'];



function validateTable(params) {
    // if (isEmpty(params)) {
    //     return {valid:false,msg:`url contains unsupported params: ${badParams?badParams.join():''}`,badParams};
    // }
    if (!params.source) {
        return {valid:false,msg:'the source parameter is required'};
    }
    return {valid:true};
}

const tableRootStr= 'API_tableId';
let nextId= 1;

function showTable(cmd,params) {
    setTimeout(() => {
        // const extraParams= findExtraParams(Object.keys(tableOverview.parameters),params);
        const metaKeys= findExtraParams(Object.keys(tableOverview.parameters),params);
        const primeAry= Object
            .entries(params)
            .filter( ([k,v]) => !metaKeys.includes(k))
            .map( ([k,v]) =>
                boolKeys.includes(k) ? [k,v?toBoolean(v,false,['true','t']):true] : [k,v] );

        const primeParams= Object.fromEntries(primeAry);

        const metaParams= Object.fromEntries(Object.entries(params).filter( ([k,v]) => metaKeys.includes(k) ));


        const {title='', source, ...rest}= primeParams;
        const options= {tbl_id: `${tableRootStr}${nextId++}`, startIdx:0, ...rest};
        if (metaKeys.length) options.META_INFO= metaParams;
        const dataTableReq= makeFileRequest(title, source, undefined,options);
        dispatchTableSearch(dataTableReq,options);
    },10);
}

/**
 * @return Array.<WebApiCommand>
 */
export function getTableCommands() {
    return [
        {
            cmd : 'table',
            validate : validateTable,
            execute:  showTable,
            allowAdditionalParameters:true,
            ...tableOverview,
            examples: makeExamples('table', tableExamples),
        },
    ];
}


