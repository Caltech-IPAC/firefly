import {isArray} from 'lodash';
import {dispatchAddPreference, getAppOptions, getPreference} from '../../core/AppDataCntlr';
import {mergeServices} from '../../ui/tap/TapUtil';
import {Logger} from '../../util/Logger';

export const GP_SERVICE_URL = 'GP_SERVICE_URL';
export const GP_SERVICE_META = 'GP_SERVICE_META';
export const GP_USER_ENTERED_TITLE= 'USER_ENTERED_TITLE';

const USER_SERVICE_PREFS= 'GatorProtoUserEnteredServices';

const GP_SERVICES_FALLBACK= [];



export function getGatorProtoServices() {
    const {gatorProtocol} = getAppOptions();
    const startingGpServices= hasElements(gatorProtocol?.services) ? [...gatorProtocol.services] : [...GP_SERVICES_FALLBACK];
    const mergedServices= mergeServices(startingGpServices,gatorProtocol?.additional?.services);
    mergedServices.push(...getUserServiceAry());
    return mergedServices;
}

const hasElements= (a) => Boolean(isArray(a) && a?.length);

const baseName= 'User Entered';

export const getGatorProtoServiceOptions= () =>
    getGatorProtoServices().map(({label,value,userAdded=false})=>({label:value, value, labelOnly:label, userAdded}));

function getUserServiceAry() {
    return getPreference(USER_SERVICE_PREFS, []);
}

export const getServiceNamesAsKey= () => getGatorProtoServiceOptions().map(({label}) => label).join('-');

export function addGatorProtoUserService(serviceUrl) {
    const userServices= getUserServiceAry();
    if (getGatorProtoServices().some( (({value}) => value===serviceUrl))) { // don't add if service already exist
        return;
    }
    const usedNumAry= userServices
        .map( (s) => s.label)
        .filter((title) => title && title.startsWith(baseName))
        .map((title) => title.trim().split('-')?.[1])
        .map(Number)
        .filter(Boolean);

    const maxNum = usedNumAry?.length ? Math.max(...usedNumAry) : 0;
    const label= userServices.every( (s) => s.label!==baseName) ? baseName : baseName + ` - ${maxNum + 1}`;
    userServices.push({
        label,
        value: serviceUrl,
        userAdded: true,
        showSqlSection:false,
        searchOptionsMask: getAppOptions()?.gatorProtocol?.searchOptionsMask
    });
    dispatchAddPreference(USER_SERVICE_PREFS, userServices);
}

export function deleteUserService(serviceUrl) {
    const userServices= getUserServiceAry().filter( (s) => s.value!==serviceUrl);
    dispatchAddPreference(USER_SERVICE_PREFS, userServices);
}


export function getGatorProtoServiceId(serviceUrl) {
    if (!serviceUrl) return 'IRSA';
    const siaOps= getGatorProtoServices();
    const id= (serviceUrl && (siaOps.find( (e) => e.value===serviceUrl)?.serviceId));
    if (id) return id;
    return getGatorProtoServiceLabel(serviceUrl).replaceAll(/\s/g,'');
}

export function getGatorProtoServiceLabel(serviceUrl) {
    const gpOps= getGatorProtoServiceOptions();
    return (serviceUrl && (gpOps.find( (e) => e.value===serviceUrl)?.label)) || '';
}

export function getGatorProtoService(serviceUrl) {
    const gpOps= getGatorProtoServices();
    return serviceUrl && gpOps.find( (e) => e.value===serviceUrl);
}


export function getGatorProtoServicesByName(nameList) {
    const services= makeServices();
    if (!nameList) return services;

    return nameList.map( (name) => {
            const foundService= services.find( (s) => s.label===name);
            if (!foundService) Logger('TapKnownServices').warn(`TAP Service: '${name}' was not found`);
            return foundService;
        })
        .filter( (v) => v);
}

function makeServices() {
    return [
        gatorProtoEntry('IRSA', 'IRSA', 'https://irsa.ipac.caltech.edu', true),
        gatorProtoEntry('OTHER', 'OTHER', 'https://abc'),
        gatorProtoEntry('LSDB', 'LSDB', 'https://troyraen.irsakudev.ipac.caltech.edu', false)
    ];
}

const gatorProtoEntry= (serviceId, label,url, showSqlSection=false) =>
    ({ serviceId, label, value: url, showSqlSection});

