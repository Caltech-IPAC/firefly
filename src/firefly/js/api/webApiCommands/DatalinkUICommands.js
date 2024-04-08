import {isArray} from 'lodash';
import {getJsonProperty} from '../../rpc/CoreServices.js';
import {fetchDatalinkUITable} from '../../ui/dynamic/FetchDatalinkTable.js';
import {makeExamples, ReservedParams} from '../WebApi.js';

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
        desc:'load to UI using id',
        params:{
            id: 'wise_z0mgs',
        }
    },
    {
        desc:'load to UI using id and show the data set chooser',
        params:{
            id: 'dss',
            showChooser: 'true'
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

function loadDLUITable(cmd,params, includeIdType,dlAction) {
    let urlAry;
    if (params.url) {
        urlAry= isArray(params.url) ? params.url : [params.url];
        const initArgs={urlApi:params};
        urlAry.forEach( (url) =>  {
            void fetchDatalinkUITable(url,0,initArgs);
        });
    }
    else if (includeIdType) {
        void getServerAndLoadTablesById(cmd,params,dlAction);
    }
}

async function getServerAndLoadTablesById(cmd,inParams, dlAction) {
    const urlRootAry= await getJsonProperty('inventory.serverURLAry');
    const params= {showChooser:false, ...inParams};
    const initArgs={urlApi:params};
    const urlRoot= urlRootAry[0];
    const makeUrl= (id) =>  urlRoot +'?'+ new URLSearchParams({collection:id}).toString();
    const urlAry=  (isArray(params.id)) ? params.id.map( (id) => makeUrl(id))  : [makeUrl(params.id)];
    initArgs.urlApi.url= urlAry;
    urlAry.forEach( (url) =>  void fetchDatalinkUITable(url,0,initArgs,dlAction));
}



/**
 * get the data link command api definition
 * @param {boolean} includeIdType
 * @param {String} dlAction
 * @return Array.<WebApiCommand>
 */
export function getDatalinkUICommands(includeIdType,dlAction) {
    const examples= makeExamples('dl',  includeIdType ? [...urlExamples,...idExamples] : urlExamples);
    const cmd= 'dl';
    const execute= (cmd,params) => loadDLUITable(cmd,params,includeIdType,dlAction);
    const overview= [ 'Load Service Descriptor UI' ];

    const urlOnlyParameters= {
        url: {desc:'url to datalink UI file'},
        [ReservedParams.POSITION.name]: ['coordinates of the search',...ReservedParams.POSITION.desc],
        [ReservedParams.SR.name]: ['radius of search  (optional)',...ReservedParams.SR.desc],
        showChooser: {desc:'show the dataset chooser'},
        user: {desc:'internal only'},
        execute: 'true or false - if true execute the search'
    };
    const bothParameters= {...urlOnlyParameters, id : {desc:'ID to predefined data link UI service'}};
    const allowAdditionalParameters= true;
    
    return includeIdType ?
        [{ cmd, allowAdditionalParameters, validate, execute, overview, examples, parameters:bothParameters}] :
        [{ cmd, allowAdditionalParameters, validate, execute, overview, examples, parameters:urlOnlyParameters}];
}

