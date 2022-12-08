import {isArray} from 'lodash';
import {MetaConst} from '../../data/MetaConst.js';
import {getJsonProperty} from '../../rpc/CoreServices.js';
import {makeFileRequest} from '../../tables/TableRequestUtil.js';
import {dispatchTableFetch} from '../../tables/TablesCntlr.js';
import {makeExamples} from '../WebApi.js';

const urlExamples= [
    {
        desc:'load to UI using url',
        params:{
            url: 'https://irsadev.ipac.caltech.edu:9028/IRSA_Registry/searchpage?collection=iras_iris',
        }
    },
];
const idExamples= [
    {
        desc:'load to UI using id',
        params:{
            id: 'iras_issa',
        }
    },
    {
        desc:'load to UI more than one id',
        params:{
            id: ['iras_issa','iras_iris']
        }
    },
];

function validate(params) {

    if (!params.id && !params.url) {
        return {valid:false, msg:'either UI or URL must be define'};
    }
    if (params.url) {
        const urlAry= isArray(params.url) ? params.url : [params.url];
        try {
            urlAry.forEach( (url) => new URL(url));
        } catch (e) {
            return {valid:false, msg:urlAry.length===1 ? 'url is not valid' : 'one of the urls in you list is not valid',
                badParams:['url']};
        }
    }
    return {valid:true};
}

const loadOptions=  {META_INFO:{[MetaConst.LOAD_TO_DATALINK_UI]: 'true'}};

function loadDLUITable(cmd,params, includeIdType) {
    let urlAry;
    if (params.url) {
        urlAry= isArray(params.url) ? params.url : [params.url];
        urlAry.forEach( (url) =>  {
            const tblReq = makeFileRequest('Data link UI', url, undefined, loadOptions);
            dispatchTableFetch(tblReq);
        });
    }
    else if (includeIdType) {
        void getServerAndLoadTablesById(cmd,params);
    }

}

async function getServerAndLoadTablesById(cmd,params) {
    const urlRootAry= await getJsonProperty('inventory.serverURLAry');
    const urlRoot= urlRootAry[0];
    const makeUrl= (id) =>  urlRoot +'?'+ new URLSearchParams({collection:id}).toString();
    const urlAry=  (isArray(params.id)) ? params.id.map( (id) => makeUrl(id))  : [makeUrl(params.id)];
    urlAry.forEach( (url) =>  dispatchTableFetch(makeFileRequest('Data link UI', url, undefined, loadOptions)));
}



/**
 * get the data link command api definition
 * @param {boolean} includeIdType
 * @return Array.<WebApiCommand>
 */
export function getDatalinkUICommands(includeIdType) {
    const examples= makeExamples('dl',  includeIdType ? [...urlExamples,...idExamples] : urlExamples);
    const cmd= 'dl';
    const execute= (cmd,params) => loadDLUITable(cmd,params,includeIdType);
    const overview= [ 'Load Service Descriptor UI' ];

    const urlOnlyParameters= { url: {desc:'url to datalink UI file'}, };
    const bothParameters= {...urlOnlyParameters, id : {desc:'ID to predefined data link UI service'}};
    
    return includeIdType ?
        [{ cmd, validate, execute, overview, examples, parameters:bothParameters}] :
        [{ cmd, validate, execute, overview, examples, parameters:urlOnlyParameters}];
}

