import {isValidFullUrl} from '../../util/WebUtil';
import CoordinateSys from '../../visualize/CoordSys';
import {makeWorldPt} from '../../visualize/Point';


/*
 * -------------------------------------
 * Supports the processing of IRSA extension of service descriptor: <RESOURCE type="meta" utype="CISX:adhoc:service"
 * -------------------------------------
 */



/**
 * Pull the service descriptor (xml) moc related keys out and convert them to a form that can be later processed.
 * The service descriptor might define multiple MOCs
 * @param cisxUI
 * @return {*} and object the defines multiple mocs that will be process by other parts of the application
 */

function findAndTranslateMocParameters(cisxUI) {
    const translateMocXml = {
        moc_color: 'mocColor',
        moc_style: 'mocStyle',
        moc_short_description: 'shortTitle',
    };
    const itemsList = Object.keys(translateMocXml);

    const allMocsObj= cisxUI.filter(({name}) => {
            if (!name) return false;
            const key = name.toLowerCase();
            return key === 'moc' || key.match(/moc\d+$/i) || itemsList.some((s) => key.startsWith(s));
        })
        .reduce((allMocs, {name, value, desc}) => {
            const key = itemsList.find((k) => name.startsWith(k));
            const id= name.substring(key?.length ?? 'moc'.length);
            if (!allMocs[id]) allMocs[id] = {};
            if (key) {
                allMocs[id][translateMocXml[key]] = value;
            } else {
                allMocs[id].mocUrl= value;
                allMocs[id].title= desc;
            }
            return allMocs;
        }, {});
    return Object.values(allMocsObj);
}

/**
 *
 * @param {Object} cisxUI
 * @param {number} defaultMaxMOCFetchDepth
 * @return {SearchAreaInfo}
 */
export function makeSearchAreaInfo(cisxUI, defaultMaxMOCFetchDepth) {
    if (!cisxUI) return;
    const tmpObj = cisxUI.reduce((obj, {name, value, UCD}) => {
        switch (name) {
            case 'hips_initial_fov':
                obj[name] = Number(value);
                break;
            case 'hips_initial_dec':
            case 'hips_initial_ra':
                obj[name] = Number(value);
                obj.ptIsGalactic = UCD?.includes('galactic');
                break;
            case 'polygon_examples':
            case 'examples':
                obj[name] = makeExamples(value);
                break;
            default:
                if (!name?.startsWith('moc')) obj[name] = value;
                break;
        }
        return obj;
    }, {});

    // const mocList= getMOCList(findAndTranslateMocParameters(cisxUI));
    const mocList= findAndTranslateMocParameters(cisxUI);
    const {hips_initial_ra, hips_initial_dec, hips_frame, ptIsGalactic} = tmpObj;
    const hipsProjCsys = hips_frame?.trim().toLowerCase() === 'galactic' ? CoordinateSys.GALACTIC : CoordinateSys.EQ_J2000;
    const ptCsys = ptIsGalactic ? CoordinateSys.GALACTIC : CoordinateSys.EQ_J2000;
    const centerWp = makeWorldPt(hips_initial_ra, hips_initial_dec, ptCsys);
    return {
        ...tmpObj, mocList, centerWp,
        coordinateSys: hipsProjCsys.toString(), maxFetchDepth: defaultMaxMOCFetchDepth
    };
}

function makeExamples(inExample) {
    if (!inExample) return {targetPanelExampleRow1: undefined, targetPanelExampleRow2: undefined};
    const examples = inExample.split('|');
    if (examples?.length > 1) {
        const cnt = examples.length;
        return {
            targetPanelExampleRow1: examples.slice(0, Math.trunc(cnt / 2)),
            targetPanelExampleRow2: examples.slice(Math.trunc(cnt / 2))
        };
    } else {
        return {targetPanelExampleRow1: [inExample], targetPanelExampleRow2: []};
    }
}

/**
 * @param {QueryAnalysis|ServiceDescriptorDef} qAnaOrSd - accept a QueryAnalysis or a ServiceDescriptorDef
 * @return {CISXui|Array} ui parameters or an empty array
 */
export function getCisxUI(qAnaOrSd) {
    if (!qAnaOrSd) return [];
    if (qAnaOrSd.primarySearchDef) { // is QueryAnalysis
        return qAnaOrSd.primarySearchDef[0]?.serviceDef?.cisxUI ?? [];
    } else if (qAnaOrSd.accessURL) { // is ServiceDescriptorDef
        return qAnaOrSd.cisxUI ?? [];
    }
    return [];
}

/**
 * @param {QueryAnalysis|ServiceDescriptorDef} qAnaOrSd - accept a QueryAnalysis or a ServiceDescriptorDef
 * @param {String} name
 * @return the value
 */
export function getCisxUIValue(qAnaOrSd, name) {
    return getCisxUI(qAnaOrSd).find((e) => e.name === name)?.value;
}

/**
 * @param {QueryAnalysis|ServiceDescriptorDef} qAnaOrSd - accept a QueryAnalysis or a ServiceDescriptorDef
 * @param {String} name
 * @return the UCD
 */
export function getCisxUIUCD(qAnaOrSd, name) {
    return getCisxUI(qAnaOrSd).find((e) => e.name === name)?.UCD;
}