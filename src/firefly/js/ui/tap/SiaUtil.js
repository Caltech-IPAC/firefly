import {isArray} from 'lodash';
import {dispatchAddPreference, getAppOptions, getPreference} from '../../core/AppDataCntlr';
import {makeFileRequest, setNoCache} from '../../tables/TableRequestUtil';
import {doFetchTable} from '../../tables/TableUtil';
import {Logger, logger} from '../../util/Logger';
import {getServiceSelfDescription} from '../../voAnalyzer/VoCoreUtils';
import {mergeAdditionalServices} from './TapUtil';


/** * @type SiaBrowserState */
export const defSiaBrowserState= {
    serviceUrl:undefined,
    lastServicesShowing: true,
    constraintFragments: new Map()
};

export const SIA_SERVICE_URL = 'SIA_SERVICE_URL';
export const SIA_SERVICE_META = 'SIA_SERVICE_META';
export const SIA_USER_ENTERED_TITLE= 'USER_ENTERED_TITLE';
const USER_SERVICE_PREFS= 'SIAUserEnteredServices';

const SIA_SERVICES_FALLBACK= [];

const serviceMetaCache= {};

export const siaHelpId = (id) => `SIAv2.${id}`;

function getMetaFromTable(table) {
    // todo - get the meta
    if (!table) return undefined;
    const meta= getServiceSelfDescription(table);
    if (!meta) return;
    const inputParams= meta?.groups?.filter( (g) => g.name==='inputParams')?.[0];
    return inputParams;
}


export async function loadSiaV2Meta(serviceUrl) {
    if (serviceMetaCache[serviceUrl]) return serviceMetaCache[serviceUrl];
    serviceMetaCache[serviceUrl]= await doLoadSiaV2Meta(serviceUrl);
    return serviceMetaCache[serviceUrl];
}



async function doLoadSiaV2Meta(serviceUrl) {
    try {
        const req= makeFileRequest(serviceUrl,serviceUrl+'?MAXREC=0');
        setNoCache(req);
        const tableModel= await doFetchTable(req);
        return getMetaFromTable(tableModel);

    } catch(reason) {
        const error = `Failed to get meta for ${serviceUrl}: ${reason?.message ?? reason}`;
        logger.error(error);
        return undefined;
    }
}
export function getSiaServiceLabel(serviceUrl) {
    const siaOps= getSiaServiceOptions();
    return (serviceUrl && (siaOps.find( (e) => e.value===serviceUrl)?.label)) || '';
}

export const getServiceNamesAsKey= () => getSiaServiceOptions().map(({label}) => label).join('-');

export const getSiaServiceOptions= () =>
    getSiaServices().map(({label,value,userAdded=false})=>({label:value, value, labelOnly:label, userAdded}));

const hasElements= (a) => Boolean(isArray(a) && a?.length);

export function getServiceMetaOptions(name) {
    return getSiaServices().find( (s) => s.label===name)?.metaOptions;
}

export function getServiceHiPS(serviceUrl) {
    if (!serviceUrl) return '';
    return getSiaServices().find( (e) => e.value===serviceUrl)?.hipsUrl || '';
}


export function getSiaServices() {
    const {SIAv2} = getAppOptions();
    const startingSiaServices= hasElements(SIAv2?.services) ? [...SIAv2.services] : [...SIA_SERVICES_FALLBACK];
    const mergedServices= mergeAdditionalServices(startingSiaServices,SIAv2?.additional?.services);
    mergedServices.push(...getUserServiceAry());
    return mergedServices;
}

const baseName= 'User Entered';

function getUserServiceAry() {
    return getPreference(USER_SERVICE_PREFS, []);
}

export function addUserService(serviceUrl) {
    const userServices= getUserServiceAry();
    if (getSiaServices().some( (({value}) => value===serviceUrl))) { // don't add if service already exist
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
    userServices.push({ label, value: serviceUrl, userAdded: true});
    dispatchAddPreference(USER_SERVICE_PREFS, userServices);
}

export function deleteUserService(serviceUrl) {
    const userServices= getUserServiceAry().filter( (s) => s.value!==serviceUrl);
    dispatchAddPreference(USER_SERVICE_PREFS, userServices);
}






export function getSIAv2Services(nameList) {
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
        siaEntry('IRSA', 'https://irsa.ipac.caltech.edu/SIA'),
        siaEntry('CADC', 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/sia/v2query', cadcMetaOptionsFallback),
    ];
}

const cadcMetaOptionsFallback= [
    {
        name: 'COLLECTION',
        options: 'CFHT,CFHTMEGAPIPE,CFHTTERAPIX,CFHTWIRWOLF,HSTHLA,HST,JWST,GEMINI,' +
            'GEMINICADC,JCMT,JCMTLS,DAO,DAOCADC,DAOPLATES,NEOSSAT,BRITE-Constellation,' +
            'RACS,WALLABY,SUBARU,SUBARUCADC,VGPS,MOST,MACHO,TESS,SDSS,NGVS,POSSUM,' +
            'BLAST,FUSE,DRAO,IRIS,CGPS,ALMA,APASS,VLASS,XMM,CHANDRA,UKIRT,OMM,NOAO',
    },
    {
        name: 'INSTRUMENT',
        options:
            'ACS, ACS/SBC, ACS/WFC, ALAIHI-ACSIS, ASKAP, AWEOWEO-ACSIS, Alopeke, COS, COS-STIS, COS/FUV, COS/NUV, CPAPIR, Cassegrain Spectrograph,'+
            'Cassegrain Spectropolarimeter, ESPaDOnS, F2, FGS, FGS/FGS1, FGS/FGS2, GHOST, GMOS-N, GMOS-S, GNIRS, GPI, GRACES, GSAOI, HARP-ACSIS,'+
            'IGRINS, KUNTUR-ACSIS, MIRI, MIRI/CORON, MIRI/IFU, MIRI/IMAGE, MIRI/SLIT, MIRI/SLITLESS, MIRI/TARGACQ, McKellar Spectrograph, MegaPrime,'+
            'NEOSSat_Science, NEOSSat_StarTracker, NIFS, NIRCAM, NIRCAM/CORON, NIRCAM/GRISM, NIRCAM/IMAGE, NIRCAM/TARGACQ, NIRI, NIRISS/AMI,'+
            'NIRISS/IMAGE, NIRISS/SOSS, NIRISS/WFSS, NIRSPEC, NIRSPEC/IFU, NIRSPEC/IMAGE, NIRSPEC/MSA, NIRSPEC/SLIT, Newtonian Imager, POL2-SCUBA-2,'+
            'Photometer, S-WIDAR, SCUBA-2, SITELLE, SPIRou, STIS, STIS/CCD, STIS/FUV-MAMA, STIS/NUV-MAMA, ScienceCCD, Sky Camera, UU-ACSIS, WFC3/IR,'+
            'WFC3/UVIS, WIRCam, WVM, Zorro, 1872 RETICON, 90prime, ACIS-I, ACIS-S, ACS/HRC, AOSC, APOGEE Spectrograph, ARCOIRIS, Apogee USB/Net, BEAR,'+
            'BLAST, BOSS Spectrograph, BRITE-AUSTRIA, BRITE-HEWELIUSZ, BRITE-LEM, BRITE-Toronto, Band 3, Band 4, Band 5, Band 6, Band 7, CBE,'+
            'CCD, CFH12K MOSAIC, CFHTIR, CGS3, CGS4, CIAO, CIRPASS, COUDE_F8, DAS, DRAO-ST, Direct image, EMOS1, EMOS2, EPIC PN, FCRAO, FCS, FES,'+
            'FILAO, FLAMINGOS, FOC/288, FOC/48, FOC/96, FOCAM, FOCAS, FOS/BL, FOS/RD, FTS, FTS2-SCUBA-2, FUV, Fabry image, GECKO, GMOS, GRIF,'+
            'Guide star, HERZBERG, HRC-I, HRC-S, HRCAM, HRS, HRS/1, HRS/2, HSC, HSP/PMT/VIS, HSP/POL, HSP/UNK/PMT, HSP/UNK/POL, HSP/UNK/UV1,'+
            'HSP/UNK/UV2, HSP/UNK/VIS, HSP/UV1, HSP/UV2, HSP/UV2/UV1, HSP/VIS, HSP/VIS/PMT, Hokupaa+QUIRC, IFD, IRAS, IRCAM3, ISIS, MACHO,'+
            'MARLIN, MARVELS Spectrograph, MOCAM, MOS, MOSFP, MPIRXE-DAS, Michelle, NICI, NICMOS, NICMOS/NIC1, NICMOS/NIC2, NICMOS/NIC3,'+
            'NIRISS/IFU, NIRISS/MSA, NIRISS/SLIT, NULL, OASIS, OSCIR, OSIS, Optical Monitor, PALILA, PHOENIX, POL-HARP-ACSIS, POL-RXA3-ACSIS,'+
            'POL-RXWB-ACSIS, POL2-FTS2-SCUBA-2, PUEO, PUMA, PYTHIAS, REDEYE, RGS1, RGS2, RXA, RXA2-DAS, RXA3-ACSIS, RXA3-DAS, RXA3M-ACSIS, RXB2,'+
            'RXB2-DAS, RXB3-DAS, RXB3I-DAS, RXC, RXC2-DAS, RXWB-ACSIS, RXWC-DAS, RXWD-DAS, RXWD2-ACSIS, SCUBA, SDSS Camera, SDSS Spectrograph, SIS,'+
            'SISFP, Suprime-Cam, TEXES, TIGER, TReCS, UFTI, UH8K, UH8K MOSAIC CAMERA, UIST, UKT14, UVPRIME, UniBRITE, VLA, WFC3, WFCAM, WFPC/PC,'+
            'WFPC/WFC, WFPC2, WFPC2/PC, WFPC2/WFC, andicam, aobir, aobvis, arcoiris, bHROS, bench, ccd_imager, ccd_spec, chiron, cosmos, cpapir,'+
            'decam, echelle, flamingos, goodman, hrwfs, hydra, ir_imager, ispi, kosmos, michelle, mosaic3, mosaic_1, mosaic_1_1, mosaic_2, null,'+
            'optic, osiris, sami, soi, spartan, whirc, wttm, y4kcam',
    },
    {
        name: 'DPTYPE',
        options: 'image,cube,event,catalog,measurements,spectrum,timeseries,visibility'
    }
];


export const ALL_FALLBACK_META_OPTIONS= [
    {
        name: 'DPTYPE',
        options: 'image,cube'
    },
    {
        name: 'CALIB',
        options: '0,1,2,3,4',
        optionNames: 'Raw instrumental data,Instrumental data in standard format (FITS VOTable),'+
            'Calibrated science-ready data,Enhanced data products,Analysis data products'
    }
];




const siaEntry= (label,url, metaOptions) =>
    ({ label, value: url, metaOptions});

