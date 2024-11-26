import {findExtraParams, makeExamples} from '../WebApi';
import {isEmpty} from 'lodash';
import {makeFileRequest} from '../../tables/TableRequestUtil';
import {dispatchTableSearch} from '../../tables/TablesCntlr';




const tableOverview= {
    overview: [
        'Load table to firefly'
    ],
    parameters: {
        source: 'URL of the table - true/false',
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
    {desc:'Load A Table', params:{source: 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl'}}
];





function validateTable(params) {
    const badParams= findExtraParams(Object.keys(tableOverview.parameters),params);
    if (!isEmpty(badParams) || isEmpty(params)) {
        return {valid:false,msg:`url contains unsupported params: ${badParams?badParams.join():''}`,badParams};
    }
    if (!params.source) {
        return {valid:false,msg:'the source parameter is required'};
    }
    return {valid:true};
}

const tableRootStr= 'API_tableId';
let nextId= 1;

function showTable(cmd,params) {
    const {title='', source, ...rest}= params;
    const options= {tbl_id: `${tableRootStr}${nextId++}`, startIdx:0, ...rest};

    const dataTableReq= makeFileRequest(title, source, undefined,options);
    dispatchTableSearch(dataTableReq,options);
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
            ...tableOverview,
            examples: makeExamples('table', tableExamples),
        },
    ];
}


