/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, has, isArray, isEmpty, isObject, isString, intersection, unset} from 'lodash';
import Enum from 'enum';
import {
    getColumn,
    getColumnIdx,
    getColumnValues,
    getColumns,
    getTblById,
    getCellValue,
    columnIDToName,
    getColumnByRef, isTableUsingRadians
} from '../tables/TableUtil.js';
import {getCornersColumns} from '../tables/TableInfoUtil.js';
import {MetaConst} from '../data/MetaConst.js';
import {CoordinateSys} from '../visualize/CoordSys.js';
import {makeAnyPt, makeWorldPt} from '../visualize/Point';
import {getBooleanMetaEntry, getMetaEntry} from '../tables/TableUtil';


export const UCDCoord = new Enum(['eq', 'ecliptic', 'galactic']);
export const adhocServiceUtype= 'adhoc:service';
export const cisxAdhocServiceUtype= 'cisx:adhoc:service';

export const standardIDs = {
    tap: 'ivo://ivoa.net/std/tap',
    sia: 'ivo://ivoa.net/std/sia'
};

// known service IDs from service descriptor's standardId field (so far just one)
export const DATALINK_SERVICE= 'ivo://ivoa.net/std/DataLink';

export const UCDList = ['arith','arith.diff','arith.factor','arith.grad','arith.rate','arith.ratio','arith.squared','arith.sum','arith.variation','arith.zp','em','em.IR','em.IR.J','em.IR.H',
    'em.IR.K','em.IR.3-4um','em.IR.4-8um','em.IR.8-15um','em.IR.15-30um','em.IR.30-60um','em.IR.60-100um','em.IR.NIR','em.IR.MIR','em.IR.FIR','em.UV','em.UV.10-50nm','em.UV.50-100nm',
    'em.UV.100-200nm','em.UV.200-300nm','em.X-ray','em.X-ray.soft','em.X-ray.medium','em.X-ray.hard','em.bin','em.energy','em.freq','em.freq.cutoff','em.freq.resonance','em.gamma',
    'em.gamma.soft','em.gamma.hard','em.line','em.line.HI','em.line.Lyalpha','em.line.Halpha','em.line.Hbeta','em.line.Hgamma','em.line.Hdelta','em.line.Brgamma','em.line.OIII','em.line.CO',
    'em.mm','em.mm.30-50GHz','em.mm.50-100GHz','em.mm.100-200GHz','em.mm.200-400GHz','em.mm.400-750GHz','em.mm.750-1500GHz','em.mm.1500-3000GHz','em.opt','em.opt.U','em.opt.B',
    'em.opt.V','em.opt.R','em.opt.I','em.pw','em.radio','em.radio.20MHz','em.radio.20-100MHz','em.radio.100-200MHz','em.radio.200-400MHz','em.radio.400-750MHz','em.radio.750-1500MHz',
    'em.radio.1500-3000MHz','em.radio.3-6GHz','em.radio.6-12GHz','em.radio.12-30GHz','em.wavenumber','em.wl','em.wl.central','em.wl.effective','instr','instr.background','instr.bandpass',
    'instr.bandwidth','instr.baseline','instr.beam','instr.calib','instr.det','instr.det.noise','instr.det.psf','instr.det.qe','instr.dispersion','instr.experiment','instr.filter',
    'instr.fov','instr.obsty','instr.obsty.seeing','instr.offset','instr.order','instr.param','instr.pixel','instr.plate','instr.plate.emulsion','instr.precision','instr.rmsf',
    'instr.saturation','instr.scale','instr.sensitivity','instr.setup','instr.skyLevel','instr.skyTemp','instr.tel','instr.tel.focalLength','instr.voxel','meta','meta.abstract','meta.bib',
    'meta.bib.author','meta.bib.bibcode','meta.bib.fig','meta.bib.journal','meta.bib.page','meta.bib.volume','meta.calibLevel','meta.checksum','meta.code','meta.code.class',
    'meta.code.error','meta.code.member','meta.code.mime','meta.code.multip','meta.code.qual','meta.code.status','meta.cryptic','meta.curation','meta.dataset','meta.email','meta.file',
    'meta.fits','meta.id','meta.id.assoc','meta.id.CoI','meta.id.cross','meta.id.parent','meta.id.part','meta.id.PI','meta.main','meta.modelled','meta.note','meta.number','meta.preview',
    'meta.query','meta.record','meta.ref','meta.ref.doi','meta.ref.ivoid','meta.ref.ivorn','meta.ref.uri','meta.ref.url','meta.software','meta.table','meta.title','meta.ucd','meta.unit',
    'meta.version','obs','obs.airMass','obs.atmos','obs.atmos.extinction','obs.atmos.refractAngle','obs.calib','obs.calib.flat','obs.calib.dark','obs.exposure','obs.field','obs.image',
    'obs.observer','obs.occult','obs.transit','obs.param','obs.proposal','obs.proposal.cycle','obs.sequence','phot','phot.antennaTemp','phot.calib','phot.color','phot.color.excess',
    'phot.color.reddFree','phot.count','phot.fluence','phot.flux','phot.flux.bol','phot.flux.density','phot.flux.density.sb','phot.flux.sb','phot.limbDark','phot.mag','phot.mag.bc',
    'phot.mag.bol','phot.mag.distMod','phot.mag.reddFree','phot.mag.sb','phot.radiance','phys','phys.SFR','phys.absorption','phys.absorption.coeff','phys.absorption.gal','phys.absorption.opticalDepth',
    'phys.abund','phys.abund.Fe','phys.abund.X','phys.abund.Y','phys.abund.Z','phys.acceleration','phys.aerosol','phys.albedo','phys.angArea','phys.angMomentum','phys.angSize',
    'phys.angSize.smajAxis','phys.angSize.sminAxis','phys.area','phys.atmol','phys.atmol.branchingRatio','phys.atmol.collisional','phys.atmol.collStrength','phys.atmol.configuration',
    'phys.atmol.crossSection','phys.atmol.element','phys.atmol.excitation','phys.atmol.final','phys.atmol.initial','phys.atmol.ionStage','phys.atmol.ionization','phys.atmol.lande',
    'phys.atmol.level','phys.atmol.lifetime','phys.atmol.lineShift','phys.atmol.number','phys.atmol.oscStrength','phys.atmol.parity','phys.atmol.qn','phys.atmol.radiationType','phys.atmol.symmetry',
    'phys.atmol.sWeight','phys.atmol.sWeight.nuclear','phys.atmol.term','phys.atmol.transition','phys.atmol.transProb','phys.atmol.wOscStrength','phys.atmol.weight','phys.columnDensity',
    'phys.composition','phys.composition.massLightRatio','phys.composition.yield','phys.cosmology','phys.current','phys.current.density','phys.damping','phys.density','phys.density.phaseSpace',
    'phys.dielectric','phys.dispMeasure','phys.dust','phys.electCharge','phys.electField','phys.electron','phys.electron.degen','phys.emissMeasure','phys.emissivity','phys.energy',
    'phys.energy.Gibbs','phys.energy.Helmholtz','phys.energy.density','phys.enthalpy','phys.entropy','phys.eos','phys.excitParam','phys.fluence','phys.flux','phys.flux.energy','phys.gauntFactor',
    'phys.gravity','phys.ionizParam','phys.ionizParam.coll','phys.ionizParam.rad','phys.luminosity','phys.luminosity.fun','phys.magAbs','phys.magAbs.bol','phys.magField','phys.mass',
    'phys.mass.inertiaMomentum','phys.mass.loss','phys.mol','phys.mol.dipole','phys.mol.dipole.electric','phys.mol.dipole.magnetic','phys.mol.dissociation','phys.mol.formationHeat',
    'phys.mol.quadrupole','phys.mol.quadrupole.electric','phys.mol.rotation','phys.mol.vibration','phys.particle','phys.particle.neutrino','phys.particle.neutron','phys.particle.proton',
    'phys.particle.alpha','phys.phaseSpace','phys.polarization','phys.polarization.circular','phys.polarization.coherency','phys.polarization.linear','phys.polarization.rotMeasure',
    'phys.polarization.stokes','phys.polarization.stokes.I','phys.polarization.stokes.Q','phys.polarization.stokes.U','phys.polarization.stokes.V','phys.potential','phys.pressure',
    'phys.recombination.coeff','phys.reflectance','phys.reflectance.bidirectional','phys.reflectance.bidirectional.df','phys.reflectance.factor','phys.refractIndex','phys.size',
    'phys.size.axisRatio','phys.size.diameter','phys.size.radius','phys.size.smajAxis','phys.size.sminAxis','phys.size.smedAxis','phys.temperature','phys.temperature.effective',
    'phys.temperature.electron','phys.transmission','phys.veloc','phys.veloc.ang','phys.veloc.dispersion','phys.veloc.escape','phys.veloc.expansion','phys.veloc.microTurb','phys.veloc.orbital',
    'phys.veloc.pulsat','phys.veloc.rotat','phys.veloc.transverse','phys.virial','phys.volume','pos','pos.angDistance','pos.angResolution','pos.az','pos.az.alt','pos.az.azi','pos.az.zd',
    'pos.azimuth','pos.barycenter','pos.bodycentric','pos.bodygraphic','pos.bodyrc','pos.bodyrc.alt','pos.bodyrc.lat','pos.bodyrc.lon','pos.cartesian','pos.cartesian.x','pos.cartesian.y',
    'pos.cartesian.z','pos.centroid','pos.cmb','pos.cylindrical','pos.cylindrical.azi','pos.cylindrical.r','pos.cylindrical.z','pos.dirCos','pos.distance','pos.earth','pos.earth.altitude',
    'pos.earth.lat','pos.earth.lon','pos.ecliptic','pos.ecliptic.lat','pos.ecliptic.lon','pos.emergenceAng','pos.eop','pos.ephem','pos.eq','pos.eq.dec','pos.eq.ha','pos.eq.ra','pos.eq.spd',
    'pos.errorEllipse','pos.frame','pos.galactic','pos.galactic.lat','pos.galactic.lon','pos.galactocentric','pos.geocentric','pos.healpix','pos.heliocentric','pos.HTM','pos.incidenceAng',
    'pos.lambert','pos.lg','pos.lsr','pos.lunar','pos.lunar.occult','pos.nutation','pos.outline','pos.parallax','pos.parallax.dyn','pos.parallax.phot','pos.parallax.spect','pos.parallax.trig',
    'pos.phaseAng','pos.pm','pos.posAng','pos.precess','pos.resolution','pos.spherical','pos.spherical.azi','pos.spherical.colat','pos.spherical.r','pos.supergalactic','pos.supergalactic.lat',
    'pos.supergalactic.lon','pos.wcs','pos.wcs.cdmatrix','pos.wcs.crpix','pos.wcs.crval','pos.wcs.ctype','pos.wcs.naxes','pos.wcs.naxis','pos.wcs.scale','spect','spect.binSize','spect.continuum',
    'spect.dopplerParam','spect.dopplerVeloc','spect.dopplerVeloc.opt','spect.dopplerVeloc.radio','spect.index','spect.line','spect.line.asymmetry','spect.line.broad','spect.line.broad.Stark',
    'spect.line.broad.Zeeman','spect.line.eqWidth','spect.line.intensity','spect.line.profile','spect.line.strength','spect.line.width','spect.resolution','src','src.calib','src.calib.guideStar',
    'src.class','src.class.color','src.class.distance','src.class.luminosity','src.class.richness','src.class.starGalaxy','src.class.struct','src.density','src.ellipticity','src.impactParam',
    'src.morph','src.morph.param','src.morph.scLength','src.morph.type','src.net','src.orbital','src.orbital.eccentricity','src.orbital.inclination','src.orbital.meanAnomaly',
    'src.orbital.meanMotion','src.orbital.node','src.orbital.periastron','src.orbital.Tisserand','src.orbital.TissJ','src.redshift','src.redshift.phot','src.sample','src.spType',
    'src.var','src.var.amplitude','src.var.index','src.var.pulse','stat','stat.asymmetry','stat.correlation','stat.covariance','stat.error','stat.error.sys','stat.filling','stat.fit',
    'stat.fit.chi2','stat.fit.dof','stat.fit.goodness','stat.fit.omc','stat.fit.param','stat.fit.residual','stat.Fourier','stat.Fourier.amplitude','stat.fwhm','stat.interval',
    'stat.likelihood','stat.max','stat.mean','stat.median','stat.min','stat.param','stat.probability','stat.rank','stat.rms','stat.snr','stat.stdev','stat.uncalib','stat.value',
    'stat.variance','stat.weight','time','time.age','time.creation','time.crossing','time.duration','time.end','time.epoch','time.equinox','time.interval','time.lifetime','time.period',
    'time.period.revolution','time.period.rotation','time.phase','time.processing','time.publiYear','time.relax','time.release','time.resolution','time.scale','time.start'];


const obsPrefix = 'obscore:';
const ColNameIdx = 0;
const UcdColIdx = 1;
const UtypeColIdx = 2;
const mainMeta = 'meta.main';
const obsCorePosColumns = ['s_ra', 's_dec'];
const obsCoreRegionColumn = 's_region';

const OBSTAPCOLUMNS = [
    ['dataproduct_type',  'meta.id',                    'ObsDataset.dataProductType'],
    ['calib_level',       'meta.code;obs.calib',        'ObsDataset.calibLevel'],
    ['obs_collection',    'meta.id',                    'DataID.collection'],
    ['obs_id',            'meta.id',                    'DataID.observationID'],
    ['obs_publisher_did', 'meta.ref.uri;meta.curation', 'Curation.publisherDID'],
    ['access_url',        'meta.ref.url',               'Access.reference'],
    ['access_format',     'meta.code.mime',             'Access.format'],
    ['access_estsize',    'phys.size;meta.file',        'Access.size'],
    ['target_name',       'meta.id;src',                'Target.name'],
    ['s_ra',              'pos.eq.ra',                  'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1'],
    ['s_dec',             'pos.eq.dec',                 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2'],
    ['s_fov',             'phys.angSize;instr.fov',     'Char.SpatialAxis.Coverage.Bounds.Extent.diameter'],
    ['s_region',          'pos.outline;obs.field',      'Char.SpatialAxis.Coverage.Support.Area'],
    ['s_resolution',      'pos.angResolution',          'Char.SpatialAxis.Resolution.Refval.value'],
    ['s_xel1',            'meta.number',                'Char.SpatialAxis.numBins1'],
    ['s_xel2',            'meta.number',                'Char.SpatialAxis.numBins2'],
    ['t_min',             'time.start;obs.exposure',    'Char.TimeAxis.Coverage.Bounds.Limits.StartTime'],
    ['t_max',             'time.end;obs.exposure',      'Char.TimeAxis.Coverage.Bounds.Limits.StopTime'],
    ['t_exptime',         'time.duration;obs.exposure', 'Char.TimeAxis.Coverage.Support.Extent'],
    ['t_resolution',      'time.resolution',            'Char.TimeAxis.Resolution.Refval.value'],
    ['t_xel',             'meta.number',                'Char.TimeAxis.numBins'],
    ['em_min',            'em.wl;stat.min',             'Char.SpectralAxis.Coverage.Bounds.Limits.LoLimit'],
    ['em_max',            'imit em.wl;stat.max',        'Char.SpectralAxis.Coverage.Bounds.Limits.HiLimit'],
    ['em_res_power',      'spect.resolution',           'Char.SpectralAxis.Resolution.ResolPower.refVal'],
    ['em_xel',            'Char.SpectralAxis.numBins',  'meta.number'],
    ['o_ucd',             'meta.ucd',                   'Char.ObservableAxis.ucd'],
    ['pol_states',        'meta.code;phys.polarization','Char.PolarizationAxis.stateList'],
    ['pol_xel',           'meta.number',                'Char.PolarizationAxis.numBins'],
    ['facility_name',     'meta.id;instr.tel',          'Provenance.ObsConfig.Facility.name'],
    ['instrument_name',   'meta.id;instr',              'Provenance.ObsConfig.Instrument.name']
];

const SERVICE_DESC_CNAMES = [ 'id', 'access_url', 'service_def', 'error_message', 'semantics',
    'description', 'content_type', 'content_length'];

const OBSTAP_OPTIONAL_CNAMES = [
    'dataproduct_subtype', 'target_class', 'obs_title', 'obs_creation_date', 'obs_creator_name',
    'obs_creator_did', 'obs_release_date', 'publisher_id', 'bib_reference', 'data_rights', 's_resolution_max',
    's_calib_status', 's_stat_error', 's_pixel_scale', 't_refpos', 't_calib_status', 't_stat_error',
    'em_ucd', 'em_unit', 'em_calib_status', 'em_res_power_min', 'em_res_power_max', 'em_resolution',
    'em_stat_error', 'o_unit', 'o_calib_status', 'o_stat_error', 'pol_states', 'proposal_id'
];

const OBSTAP_CNAMES = OBSTAPCOLUMNS.map((row) => row[ColNameIdx]).concat( OBSTAP_OPTIONAL_CNAMES);

// Columns required for heuristic matching
const OBSTAP_MATCH_COLUMNS = [
    's_region',
    't_min',
    't_max',
    'em_min',
    'em_max',
    'calib_level',
    'dataproduct_type',
    'obs_collection',
];

function getObsTabColEntry(title) {
    const e= OBSTAPCOLUMNS.find( (entry) => entry[ColNameIdx]===title);
    return e && {name:e[ColNameIdx], ucd: e[UcdColIdx], utype: e[UtypeColIdx]};
}

function getObsCoreTableColumn(tableOrId, name) {
    const entry= getObsTabColEntry(name);
    if (!entry) return;
    const table= getTableModel(tableOrId);
    const tblRec= TableRecognizer.newInstance(table);
    let cols= tblRec.getTblColumnsOnUType(entry.utype);
    if (cols.length) {
        if (cols.length===1) return cols[0];
        const prefUtype= cols.find( (c) => c.name===name);
        return prefUtype ? prefUtype : cols[0];
    }
    cols= tblRec.getTblColumnsOnDefinedUCDValue(entry.ucd);
    if (cols.length) {
        if (cols.length===1) return cols[0];
        const prefUcd= cols.find( (c) => c.name===name);
        return prefUcd ? prefUcd : cols[0];
    }
    return getColumn(table,name);
}


function getObsCoreTableColumnName(tableOrId,name) {
    const col = getObsCoreTableColumn(tableOrId,name);
    return col ? col.name : '';
}

export function getObsCoreCellValue(tableOrId, rowIdx, obsColName) {
    const table= getTableModel(tableOrId);
    if (!table) return '';
    return getCellValue(table, rowIdx, getObsCoreTableColumnName(table, obsColName)) || '';
}


const alternateMainPos = [['POS_EQ_RA_MAIN', 'POS_EQ_DEC_MAIN']];
export const posCol = {[UCDCoord.eq.key]: {ucd: [['pos.eq.ra', 'pos.eq.dec'],...alternateMainPos],
                                    coord: CoordinateSys.EQ_J2000, adqlCoord: 'ICRS'},
    [UCDCoord.ecliptic.key]: {ucd: [['pos.ecliptic.lon', 'pos.ecliptic.lat']],
                              coord: CoordinateSys.ECL_J2000, adqlCoord: 'ECLIPTIC'},
    [UCDCoord.galactic.key]: {ucd: [['pos.galactic.lon', 'pos.galactic.lat']],
                              coord: CoordinateSys.GALACTIC, adqlCoord: 'GALATIC'}};

function getLonLatIdx(tableModel, lonCol, latCol) {
    const lonIdx =  getColumnIdx(tableModel, lonCol);
    const latIdx =  getColumnIdx(tableModel, latCol);

    return (lonIdx >= 0 && latIdx >= 0) ? {lonIdx, latIdx} : null;
}

function centerColumnUTypesFromObsTap() {
    const obsTapColNames = OBSTAPCOLUMNS.map((col) => col[ColNameIdx]);

    const centerUTypes = obsCorePosColumns.map((posColName) => {
            const idx = obsTapColNames.indexOf(posColName);

            return (idx >= 0) ? OBSTAPCOLUMNS[idx][UtypeColIdx] : null;
    });

    return centerUTypes.findIndex((oneUtype) => !oneUtype) >= 0 ? null : centerUTypes;
}

const UCDSyntax = new Enum(['primary', 'secondary', 'any'], {ignoreCase: true});
const ucdSyntaxMap = {
            'pos.eq.ra':  UCDSyntax.any,
            'pos.eq.dec': UCDSyntax.any,
            'meta.main':  UCDSyntax.secondary,
            'pos.outline': UCDSyntax.primary,
            'obs.field': UCDSyntax.secondary
};


/**
 * check if ucd value contains the searched ucd word at the right position
 * @param ucdValue
 * @param ucdWord
 * @param syntaxCode 'P': only first word, 'S': only secondary, 'Q' either first or secondary
 */
function isUCDWith(ucdValue, ucdWord, syntaxCode = UCDSyntax.any) {
    const atoms = ucdValue.split(';');
    const idx = atoms.findIndex((atom) => {
        return atom.toLowerCase() === ucdWord.toLowerCase();
    });

    return (syntaxCode === UCDSyntax.primary && idx === 0) ||
           (syntaxCode === UCDSyntax.secondary && idx >= 1) ||
           (syntaxCode === UCDSyntax.any && idx >= 0);
}

/**
 * table analyzer based on table model for catalog or image metadata
 */
class TableRecognizer {
    constructor(tableModel, posCoord='eq') {
        this.tableModel = tableModel;
        this.columns = get(tableModel, ['tableData', 'columns'], []);
        this.obsCoreInfo = {isChecked: false, isObsCoreTable: false};
        this.posCoord = posCoord;
        this.centerColumnsInfo = null;
        this.centerColumnCandidatePairs = null;
        this.regionColumnInfo = null;
    }

    isObsCoreTable() {
        if (this.obsCoreInfo.isChecked) {
            return this.obsCoreInfo.isObsCoreTable;
        }

        const allColNames = this.columns.map((oneCol) => oneCol.name);

        const nonExistCol = OBSTAPCOLUMNS
                            .map((oneColumn) => (oneColumn[ColNameIdx]))
                            .some((oneName) => {
                                return !allColNames.includes(oneName);
                            });

        this.obsCoreInfo.isChecked = true;
        this.obsCoreInfo.isObsCoreTable = !nonExistCol;

        return this.obsCoreInfo.isObsCoreTable;
    }

    /**
     * find and fill center column info
     * @param colPair [lonCol, latCol]
     * @param csys
     * @returns {null|CoordColsDescription}
     */
    setCenterColumnsInfo(colPair, csys = CoordinateSys.EQ_J2000) {
        this.centerColumnsInfo = null;

        if (isArray(colPair) && colPair.length >= 2) {
            const lonCol = isString(colPair[0]) ? colPair[0] : colPair[0].name;
            const latCol = isString(colPair[1]) ? colPair[1] : colPair[1].name;

            const idxs = getLonLatIdx(this.tableModel, lonCol, latCol);

            if (idxs) {
                this.centerColumnsInfo = {
                    type: 'center',
                    lonCol,
                    latCol,
                    lonIdx: idxs.lonIdx,
                    latIdx: idxs.latIdx,
                    csys
                };
            }
        }
        return this.centerColumnsInfo;
    }

    setRegionColumnInfo(col) {
        this.regionColumnInfo = null;

        const idx = getColumnIdx(this.tableModel, col.name);
        if (idx >= 0) {
            this.regionColumnInfo = {
                type: 'region',
                regionCol: col.name,
                regionIdx: idx,
                unit: col.units
            };
        }

        return this.regionColumnInfo;
    }

    /**
     * filter the columns per ucd value defined in the UCD value of relevant OBSTAP column
     * @param ucds ucd value defined in OBSTAP, it may contain more than one ucd values
     * @returns {*}
     */
    getTblColumnsOnDefinedUCDValue(ucds) {
        const ucdList = ucds.split(';');

        return ucdList.reduce((prev, ucd) => {
                prev = prev.filter((oneCol) => {
                    return (has(oneCol, 'UCD') && isUCDWith(oneCol.UCD, ucd, get(ucdSyntaxMap, ucd)));
                });
                return prev;
        }, this.columns);

    }
    /**
     * get columns containing the same ucd value
     * @param ucd
     * @returns {Array}
     */
    getTblColumnsOnUCD(ucd) {
        return this.columns.filter((oneCol) => {
               return (has(oneCol, 'UCD') && isUCDWith(oneCol.UCD, ucd, get(ucdSyntaxMap, ucd)));
            });
    }


    /**
     * get columns containing the utype
     * @param utype
     * @returns {array}
     */
    getTblColumnsOnUType(utype) {
        return this.columns.filter((oneCol) => {
                return has(oneCol, 'utype') && oneCol.utype.includes(utype);
            });
    }

    /**
     * get columns containing ucd word by given table columns
     * @param cols
     * @param ucdWord
     * @returns {array}
     */
    getColumnsWithUCDWord(cols, ucdWord) {
        if (isEmpty(cols)) return [];

        return cols.filter((oneCol) => {
            return has(oneCol, 'UCD') && isUCDWith(oneCol.UCD, ucdWord, get(ucdSyntaxMap, ucdWord));
        });
    }

    /**
     * get center columns pairs by checking ucd values
     * @param coord
     * @returns {Array}  [[pair_1_col_ra, pair_1_col_dec], ...., [pair_n_col_ra, pair_n_col_dec]]
     */
    getCenterColumnPairsOnUCD(coord = this.posCoord || UCDCoord.eq.key) {
        const centerColUCDs = has(posCol, coord ) ? posCol[coord].ucd : null;
        const pairs = [];

        if (!centerColUCDs) {
            return pairs;
        }

        // get 'ra' column list and 'dec' column list
        const posPairs = centerColUCDs.reduce((prev, eqUcdPair) => {
            if (isArray(eqUcdPair) && eqUcdPair.length >= 2) {
                const colsRA = this.getTblColumnsOnUCD(eqUcdPair[0]);
                const colsDec = this.getTblColumnsOnUCD(eqUcdPair[1]);

                 prev[0].push(...colsRA);
                 prev[1].push(...colsDec);
            }
            return prev;
        }, [[], []]);


        const metaMainPair = posPairs.map((posCols, idx) => {
            const mainMetaCols = this.getColumnsWithUCDWord(posCols, mainMeta);
            if (!isEmpty(posCols) && isEmpty(mainMetaCols)) {
                alternateMainPos.find((oneAlt) => {
                    const altCols = this.getColumnsWithUCDWord(posCols, oneAlt[idx], ucdSyntaxMap.any);

                    mainMetaCols.push(...altCols);
                    return !isEmpty(altCols);
                });
            }
            return mainMetaCols;
        });

        if (metaMainPair[0].length || metaMainPair[1].length) {
            if (metaMainPair[0].length === metaMainPair[1].length) {
                for (let i = 0; i < metaMainPair[0].length; i++) {
                    pairs.push([metaMainPair[0][i], metaMainPair[1][i]]);    //TODO: need rules to match the rest pair
                }
            }
        } else if (posPairs[0].length > 0 && posPairs[1].length > 0){
            // find first exact match
            const basicPair = posPairs.map((cols, i)=>cols.find((c) => c.UCD === centerColUCDs[0][i]));
            if (basicPair[0] && basicPair[1]) {
                pairs.push(basicPair);
            } else if (posPairs[0].length === posPairs[1].length) {
                // TODO: how do we separate positions from the related fields, like variance?
                for (let i = 0; i < posPairs[0].length; i++) {
                    pairs.push([posPairs[0][i], posPairs[1][i]]);    //TODO: need rules to match the rest pair
                }
            }
        }

        return pairs;
    }

    getCenterColumnPairsOnUType(columnPairs) {
        const centerUTypes = centerColumnUTypesFromObsTap();

        if (isEmpty(centerUTypes)) return columnPairs;
        let pairs = [];

        /* filter out the column with unequal utype value */
        if (!isEmpty(columnPairs)) {
            pairs = columnPairs.filter((oneColPair) => {
                if ((!has(oneColPair[0], 'utype')) || (!has(oneColPair[1], 'utype')) ||
                    (oneColPair[0].utype.includes(centerUTypes[0]) && oneColPair[1].utype.includes(centerUTypes[1]))) {
                    return oneColPair;
                }
            });
        } else {   // check all table columns
            const posPairs = centerUTypes.map((posUtype) => {
                return this.getTblColumnsOnUType(posUtype);
            });

            if (posPairs[0].length === posPairs[1].length) {
                for (let i = 0; i < posPairs[0].length; i++) {
                    pairs.push([posPairs[0][i], posPairs[1][i]]);    //TODO: need rules to match the rest pair
                }
            }
        }
        return pairs;

    }

    getCenterColumnPairOnName(columnPairs) {
        if (!isEmpty(columnPairs)) {
            return columnPairs.find((onePair) => {
                return (onePair[0].name.toLowerCase() === obsCorePosColumns[0]) &&
                       (onePair[1].name.toLowerCase() === obsCorePosColumns[1]);
            });
        } else {
            const cols = obsCorePosColumns.map((colName) => {
                return getColumn(this.tableModel, colName);
            });
            return (cols[0] && cols[1]) ? cols : [];
        }
    }


    /**
     *
     * @return {String}
     */
    getCenterColumnMetaEntry() {
        this.centerColumnsInfo = null;

        //note: CATALOG_COORD_COLS,POSITION_COORD_COLS are both deprecated and will removed in the future
        const {tableMeta} = this.tableModel || {};
        const {CATALOG_COORD_COLS,POSITION_COORD_COLS,CENTER_COLUMN}= MetaConst;
        if (!tableMeta) return undefined;
        return tableMeta[CENTER_COLUMN] || tableMeta[CATALOG_COORD_COLS] || tableMeta[POSITION_COORD_COLS];
    }

    /**
     * @returns {Boolean}
     */
    isCenterColumnMetaDefined() {
        return Boolean(this.getCenterColumnMetaEntry());
    }


    /**
     * get center columns pair by checking the table meta
     * @returns {null|CoordColsDescription}
     */
    getCenterColumnsOnMeta() {
        const cenData= this.getCenterColumnMetaEntry();
        if (!cenData) return undefined;

        const s= cenData.split(';');
        if (!s || s.length !== 3) {
            return this.centerColumnsInfo;
        }

        return this.setCenterColumnsInfo(s, CoordinateSys.parse(s[2]));
    }

    getImagePtColumnsOnMeta() {
        const cenData= getMetaEntry(this.tableModel, MetaConst.IMAGE_COLUMN);
        if (!cenData) return undefined;

        const s= cenData.split(';');
        if (!s || s.length !== 2) {
            return;
        }

        return {
            type: 'ImageCenterPt',
            xCol: s[0],
            yCol: s[1],
            xIdx: getColumnIdx(this.table, s[0]),
            yIdx: getColumnIdx(this.table, s[1]),
        };
    }


    /**
     * search center columns pair by checking UCD value
     * @returns {null|CoordColsDescription}
     */
    getCenterColumnsOnUCD() {
        this.centerColumnsInfo = null;

        const colPairs = this.getCenterColumnPairsOnUCD(UCDCoord.eq.key);

        if (colPairs && colPairs.length === 1) {
            return this.setCenterColumnsInfo(colPairs[0], posCol[UCDCoord.eq.key].coord);
        } else {
            this.centerColumnCandidatePairs = colPairs;
        }

        return this.centerColumnsInfo;
    }

    /**
     * search center column pairs based on existing candidate pairs or all table columns
     * @param candidatePairs
     * @returns {null|CoordColsDescription}
     */
    getCenterColumnsOnObsCoreUType(candidatePairs) {
        this.centerColumnsInfo = null;

        const colPairs = this.getCenterColumnPairsOnUType(candidatePairs);

        if (colPairs && colPairs.length === 1) {
            this.setCenterColumnsInfo(colPairs[0], posCol[UCDCoord.eq.key].coord);
        }
        this.centerColumnCandidatePairs = colPairs;

        return this.centerColumnsInfo;
    }

    /**
     * search center column pair by checking ObsCore columns on existing candidate pairs or all table columns
     * @param candidatePairs
     * @returns {null|CoordColsDescription}
     */
    getCenterColumnsOnObsCoreName(candidatePairs) {
        this.centerColumnsInfo = null;

        const leftMostCol = (isEmpty(candidatePairs))
                            ? null : candidatePairs[0];

        const colPair = this.getCenterColumnPairOnName(candidatePairs);

        if (isArray(colPair) && colPair.length === 2) {
            return this.setCenterColumnsInfo(colPair, posCol[UCDCoord.eq.key].coord);
        } else {
            return leftMostCol?
                   this.setCenterColumnsInfo(leftMostCol, posCol[UCDCoord.eq.key].coord) :
                   this.centerColumnsInfo;
        }
    }

    /**
     * search center columns pair by guessing the column name
     * @returns {null|CoordColsDescription}
     */
    guessCenterColumnsByName() {
        this.centerColumnsInfo = undefined;

        const columns= getColumns(this.tableModel);

        const findColumn = (colName, regExp) => {
            return columns.find((c) => (c.name === colName || (regExp && regExp.test(c.name))) );
        };

        const guess = (lon, lat, useReg=false) => {

            let lonCol;
            let latCol;
            if (useReg) {
                const reLon= new RegExp(`^[A-z]*[-_]?(${lon})[1-9]*$`);
                const reLat= new RegExp(`^[A-z]*[-_]?(${lat})[1-9]*$`);
                lonCol = findColumn(lon,reLon);
                latCol = findColumn(lat,reLat);
                if (lonCol && latCol) {
                    if (lonCol.name.replace(lon,'') !== latCol.name.replace(lat,'')) return undefined;
                }
            }
            else {
                lonCol = findColumn(lon);
                latCol = findColumn(lat);
            }

            return (lonCol && latCol) ? this.setCenterColumnsInfo([lonCol, latCol]) : undefined;
        };

        return (guess('ra','dec') || guess('lon', 'lat') || guess('ra','dec',true) || guess('lon', 'lat',true));
    }

    /**
     * find center columns as defined in some vo standard
     * @returns {null|CoordColsDescription}
     */
    getVODefinedCenterColumns() {
        return  this.getCenterColumnsOnUCD() ||
            this.getCenterColumnsOnObsCoreUType(this.centerColumnCandidatePairs) ||
            this.getCenterColumnsOnObsCoreName(this.centerColumnCandidatePairs);
    }


    /**
     * return center position or catalog coordinate columns and the associate*d coordinate system
     * by checking table meta, UCD values, Utype, ObsCore column name and guessing.
     * @returns {null|CoordColsDescription}
     */
    getCenterColumns() {

        if (this.isCenterColumnMetaDefined()) return this.getCenterColumnsOnMeta();
        
        return  this.getVODefinedCenterColumns() ||
                (isEmpty(this.centerColumnCandidatePairs) && this.guessCenterColumnsByName());
    }

    getRegionColumnOnUCD(cols) {
        this.regionColumnInfo = null;
        const columns = !isEmpty(cols) ? cols : this.columns;
        const ucds = get(getObsTabColEntry(obsCoreRegionColumn), 'ucd', '').split(';');

        const regionCols = ucds.reduce((prev, oneUcd) => {
            if (prev.length > 0) {
                prev = this.getColumnsWithUCDWord(prev, oneUcd);
            }
            return prev;
        }, columns);

        if (regionCols.length === 1) {
            this.setRegionColumnInfo(regionCols[0]);
        } else if (regionCols.length > 1) {
            if (!this.getRegionColumnOnObsCoreName(regionCols)) {
                this.setRegionColumnInfo(regionCols[0]);
            }
        }
        return this.regionColumnInfo;
    }

    getRegionColumnOnObsCoreUType(cols) {
        const columns = !isEmpty(cols) ? cols : this.columns;
        const obsUtype = get(getObsTabColEntry(obsCoreRegionColumn), 'utype', '');

        this.regionColumnInfo = null;

        const regionCols = (obsUtype) && !isEmpty(columns) && columns.filter((col) => {
            return  (has(col, 'utype') && col.utype.includes(obsUtype));
        });

        if (regionCols.length === 1) {
            this.setRegionColumnInfo(regionCols[0]);
        } else if (regionCols.length > 1) {
            if (!this.getRegionColumnOnObsCoreName(regionCols)) {
                this.setRegionColumnInfo(regionCols[0]);
            }
        }

        return this.regionColumnInfo;
    }

    getRegionColumnOnObsCoreName(cols) {
        this.regionColumnInfo = null;
        const columns = !isEmpty(cols) ? cols : this.columns;

        const regionCol = !isEmpty(columns) && columns.find((oneCol) => oneCol.name.toLowerCase() === obsCoreRegionColumn);
        if (regionCol) {
            this.setRegionColumnInfo(regionCol);
        }
        return this.regionColumnInfo;
    }
    /**
     * return region column by checking column name or UCD values
     * @returns {null|RegionColDescription}
     */
    getVODefinedRegionColumn() {
         return this.getRegionColumnOnUCD() ||
                this.getRegionColumnOnObsCoreUType()||
                this.getRegionColumnOnObsCoreName();
    }


    getRegionColumn() {
        return this.getVODefinedRegionColumn();
    }

    static newInstance(tableModel) {
        return new TableRecognizer(tableModel);
    }
}

/**
 * find the center column base on the table model of catalog or image metadata
 * Investigate table meta data a return a CoordColsDescription for two columns that represent and object in the row
 * @param {TableModel|undefined} table
 * @return {CoordColsDescription|null|undefined}
 */
export function findTableCenterColumns(table) {
    const tblRecog = get(table, ['tableData', 'columns']) && TableRecognizer.newInstance(table);
    return tblRecog && tblRecog.getCenterColumns();
}

export function findImageCenterColumns(table) {
    const tblRecog = get(table, ['tableData', 'columns']) && TableRecognizer.newInstance(table);
    return getMetaEntry(table,MetaConst.FITS_FILE_PATH) && tblRecog?.getImagePtColumnsOnMeta();
}

/**
 * If there are center columns defined with this table then return a WorldPt
 * @param table
 * @param row
 * @return {WorldPt|undefined} a world point or undefined it no center columns exist
 */
export function makeWorldPtUsingCenterColumns(table,row) {
    const cen= findTableCenterColumns(table);
    return cen && makeWorldPt(getCellValue(table,row,cen.lonCol), getCellValue(table,row,cen.latCol), cen.csys);
}


/**
 * find ObsCore defined 's_region' column
 * @param table
 * @return {RegionColDescription|null}  return ObsCore defined s_region column
 */
export function findTableRegionColumn(table) {
    const tblRecog = get(table, ['tableData', 'columns']) && TableRecognizer.newInstance(table);
    return tblRecog && tblRecog.getRegionColumn();
}

/**
 * find the ObsCore defined 'access_url' column
 * @param table
 * @return {TableColumn|null} return ObsCore defined access_url column
 */
export function findTableAccessURLColumn(table) {
    const urlCol = getObsCoreTableColumn(table, 'access_url');
    return isEmpty(urlCol) ? undefined : urlCol;
}

/**
 * Given a TableModel or a table id return a table model
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {TableModel|undefined} return the table model if found or undefined
 */
function getTableModel(tableOrId) {
    if (isString(tableOrId)) return getTblById(tableOrId);  // was passed a table Id
    if (isObject(tableOrId)) return tableOrId;
}


/**
 * table analyzer based on the table model for columns which contains column_name & ucd columns
 */
class ColumnRecognizer {
    constructor(columnsModel, posCoord = 'eq') {
        this.columnsModel = columnsModel;
        this.ucds = getColumnValues(columnsModel, 'ucd').map((v) => v || '');
        this.column_names = getColumnValues(columnsModel, 'column_name');
        this.centerColumnsInfo = null;
        this.posCoord = posCoord;
    }


    setCenterColumnsInfo(colPair, csys = CoordinateSys.EQ_J2000) {
        this.centerColumnsInfo = {
            lonCol: colPair[0],
            latCol: colPair[1],
            csys
        };

        return this.centerColumnsInfo;
    }

    getColumnsWithUCDWord(cols, ucdWord) {
        if (isEmpty(cols)) return [];

        return cols.filter((oneCol) => {
            return has(oneCol, 'ucd') && isUCDWith(oneCol.ucd, ucdWord, get(ucdSyntaxMap, ucdWord));
        });
    }

    getCenterColumnPairsOnUCD(coord) {
        const centerColUCDs = has(posCol, coord ) ? posCol[coord].ucd : null;
        const pairs = [];

        if (!centerColUCDs) {
            return pairs;
        }

        // get 'ra' column list and 'dec' column list
        // output in form of [ <ra column array>, <dec column array> ] and each column is like {ucd: column_name: }
        const posPairs = centerColUCDs.reduce((prev, eqUcdPair) => {
            if (isArray(eqUcdPair) && eqUcdPair.length >= 2) {
                const colsRA = this.ucds.reduce((p, ucd, i) => {
                    if (ucd.includes(eqUcdPair[0])) {
                        p.push({ucd, column_name: this.column_names[i]});
                    }
                    return p;
                }, []);
                const colsDec = this.ucds.reduce((p, ucd, i) => {
                    if (ucd.includes(eqUcdPair[1])) {
                        p.push({ucd, column_name: this.column_names[i]});
                    }
                    return p;
                }, []);

                prev[0].push(...colsRA);
                prev[1].push(...colsDec);
            }
            return prev;
        }, [[], []]);


        const metaMainPair = posPairs.map((posCols, idx) => {
            const mainMetaCols = this.getColumnsWithUCDWord(posCols, mainMeta);
            if (!isEmpty(posCols) && isEmpty(mainMetaCols)) {
                alternateMainPos.find((oneAlt) => {
                    const altCols = this.getColumnsWithUCDWord(posCols, oneAlt[idx], ucdSyntaxMap.any);

                    mainMetaCols.push(...altCols);
                    return !isEmpty(altCols);
                });
            }
            return mainMetaCols;
        });

        if (metaMainPair[0].length || metaMainPair[1].length) {  // get the column with ucd containing meta.main
            if (metaMainPair[0].length === metaMainPair[1].length) {
                for (let i = 0; i < metaMainPair[0].length; i++) {
                    pairs.push([metaMainPair[0][i], metaMainPair[1][i]]);    //TODO: need rules to match the rest pair
                }
            }
        } else if (posPairs[0].length > 0 && posPairs[1].length > 0){
            // find first exact match
            const basicPair = posPairs.map((cols, i)=>cols.find((c) => c.ucd === centerColUCDs[0][i]));
            if (basicPair[0] && basicPair[1]) {
                pairs.push(basicPair);
            } else if (posPairs[0].length === posPairs[1].length) {
                // TODO: how do we separate positions from the related fields, like variance?
                for (let i = 0; i < posPairs[0].length; i++) {
                    pairs.push([posPairs[0][i], posPairs[1][i]]);    //TODO: need rules to match the rest pair
                }
            }
        }

        return pairs;
    }

    getCenterColumnsOnUCD() {
        let colPairs;
        const coordSet = this.posCoord ? [UCDCoord[this.posCoord].key] :
                         [UCDCoord.eq.key, UCDCoord.galactic.key, UCDCoord.ecliptic.key];

        coordSet.find((oneCoord) => {
            colPairs = this.getCenterColumnPairsOnUCD(oneCoord);
            if (colPairs && colPairs.length >= 1) {
                this.setCenterColumnsInfo(colPairs[0], posCol[oneCoord].coord);  // get the first pair
                return true;
            } else {
                return false;
            }
        });
        return this.centerColumnsInfo;
    }

    guessCenterColumnsByName() {
        this.centerColumnsInfo = null;

        const findColumn = (colName, regExp) => {
            let col;
            this.column_names.find((name, i) => {
                if (name === colName || (regExp && regExp.test(name))) {
                    col = {column_name: name, ucd: this.ucds[i]};
                    return true;
                } else {
                    return false;
                }
            });
            return col;
        };


        const guess = (lon, lat, useReg=false) => {

            let lonCol;
            let latCol;
            if (useReg) {
                const reLon= new RegExp(`^[A-z]?[-_]?(${lon})[1-9]*$`);
                const reLat= new RegExp(`^[A-z]?[-_]?(${lat})[1-9]*$`);
                lonCol = findColumn(lon,reLon);
                latCol = findColumn(lat,reLat);
            }
            else {
                lonCol = findColumn(lon);
                latCol = findColumn(lat);
            }

            return (lonCol && latCol) ? this.setCenterColumnsInfo([lonCol, latCol]) : this.centerColumnsInfo;
        };
        return (guess('ra','dec') || guess('lon', 'lat') || guess('ra','dec',true) || guess('lon', 'lat',true));
    }


    getCenterColumns() {
        return this.getCenterColumnsOnUCD()||
               this.guessCenterColumnsByName();
    }

    static newInstance(tableModel) {
        return new ColumnRecognizer(tableModel);
    }
}

/**
 * find the center columns based on the columns table model
 * @param columnsModel
 * @returns {*|{lonCol: {ucd, column_name}, latCol: {ucd, column_name}, csys}|*}
 */
export function findCenterColumnsByColumnsModel(columnsModel) {
    const colRecog = columnsModel && get(columnsModel, ['tableData', 'columns']) && ColumnRecognizer.newInstance(columnsModel);

    return colRecog && colRecog.getCenterColumns();
}


export function isOrbitalPathTable(tableOrId) {
    const table= getTableModel(tableOrId);
    if (!table) return false;
    return getBooleanMetaEntry(table,MetaConst.ORBITAL_PATH,false);
}

/**
 * Test to see it this is a catalog. A catalog must have one of the following:
 *  - CatalogOverlayType meta data entry defined and not equal to 'FALSE' and we must be able to find the columns
 *                            either by meta data or by guessing
 *  - We find the columns by some vo standard
 *
 *  Note- if the CatalogOverlayType meta toUpperCase === 'FALSE' then we will treat it as not a catalog no matter how the
 *  vo columns might be defined.
 *
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {boolean} True if the table is a catalog
 * @see MetaConst.CATALOG_OVERLAY_TYPE
 */
export function isCatalog(tableOrId) {
    const table= getTableModel(tableOrId);

    if (!table) return false;
    if (isTableWithRegion(table)) return false;
    const {tableMeta, tableData}= table;
    if (!get(tableData, 'columns') || !tableMeta) return false;

    if (isOrbitalPathTable(table)) return false;

    const catOverType= getMetaEntry(table,MetaConst.CATALOG_OVERLAY_TYPE)?.toUpperCase();
    if (catOverType==='FALSE')  return false;
    if (isString(catOverType)) {
        if (catOverType==='IMAGE_PTS') return Boolean(findImageCenterColumns(table));
        else return Boolean(TableRecognizer.newInstance(table).getCenterColumns());
    }
    else {
        return Boolean(TableRecognizer.newInstance(table).getVODefinedCenterColumns());
    }
}


export function isTableWithRegion(tableOrId) {
    const table= getTableModel(tableOrId);
    if (!table) return false;

    return Boolean(TableRecognizer.newInstance(table).getVODefinedRegionColumn());
}

/**
 * @summary check if there is center column or corner columns defined, if so this table has coverage information
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @returns {boolean} True if there  is coverage data in this table
 */
export function hasCoverageData(tableOrId) {
    const table= getTableModel(tableOrId);
    if (!getBooleanMetaEntry(table,MetaConst.COVERAGE_SHOWING,true)) return false;
    if (!table) return false;
    if (!table.totalRows) return false;
    return !isEmpty(findTableRegionColumn(table)) || !isEmpty(findTableCenterColumns(table)) || !isEmpty(getCornersColumns(table));
}


/**
 * Guess if this table contains image meta data. It contains image meta data if IMAGE_SOURCE_ID is defined
 * or a DATA_SOURCE column name is defined, or it is an obscore table, or it has service descriptors
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {boolean} true if there is image meta data
 * @see MetaConst.DATA_SOURCE
 * @see MetaConst.IMAGE_SOURCE_ID
 */
export function isDataProductsTable(tableOrId) {
    const table= getTableModel(tableOrId);
    if (isEmpty(table)) return false;
    const {tableMeta, totalRows} = table;
    if (!tableMeta || !totalRows) return false;

    const dataSourceColumn= getDataSourceColumn(table);
    if (dataSourceColumn===false) return false;  // DataSource meta data may be specifically set to false, if so disable all metadata processing

    return Boolean(
        tableMeta[MetaConst.IMAGE_SOURCE_ID] ||
        tableMeta[MetaConst.DATASET_CONVERTER] ||
        dataSourceColumn ||
        hasObsCoreLikeDataProducts(table) ||
        hasServiceDescriptors(table) ||
        isTableWithRegion(tableOrId));
}


function columnMatches(table, cName) {
    if (!table || !cName) return undefined;
    if (getColumn(table,cName)) return cName;
    const cUp= cName.toUpperCase();
    const col= table.tableData.columns.find( (c) => cUp===c.name.toUpperCase());
    return col && col.name;
}

/**
 * Find the a data source column if is is defined in the metadata and a column exist with that name. The
 * meta data entry DataSource is case insensitive matched. The column name is also match case insensitive.
 * The meta data entry can have two forms 'abc' or '[abc,efe,hij]' if it is the second form the the first
 * entry in the array to match a column is returned. The second form is useful when the code defining the DataSource
 * entry is handling a set of table where the data source could be one of several name such
 * as '[url,fileurl,file_url,data_url,data]'
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {boolean|undefined|string} the column name if it exist, if the meta data is not included,
 *                                             false if defined but set to 'false' (case insensitive)
 */
export function getDataSourceColumn(tableOrId) {
    const table= getTableModel(tableOrId);
    if (!table || !get(table,'tableData.columns') || !table.tableMeta) return undefined;
    const dsCol= getMetaEntry(table,MetaConst.DATA_SOURCE,'').trim();
    if (!dsCol) return undefined;
    if (dsCol.toLocaleLowerCase()==='false') return false;

    if (dsCol.startsWith('[') && dsCol.endsWith(']')) {
        return columnMatches(table,dsCol
            .substring(1,dsCol.length-1)
            .split(',')
            .find( (s) => columnMatches(table,s.trim())));
    }
    else {
        return columnMatches(table,dsCol);
    }
}



/**
 * Return true this this table can be access as an ObsCore data
 * @param tableOrId
 * @return {boolean}
 */
export function hasObsCoreLikeDataProducts(tableOrId) {
    const table= getTableModel(tableOrId);
    const hasUrl= getObsCoreTableColumn(table,'access_url');
    const hasFormat= getObsCoreTableColumn(table,'access_format');
    const hasProdType= getObsCoreProdTypeCol(table);
    return Boolean(hasUrl && hasFormat && hasProdType);

}


/**
 * @global
 * @public
 * @typedef {Object} ServiceDescriptorInputParam
 *
 * @prop UCD
 * @prop [arraySize] - might be a number if size of an array, or '*', or number if as length of string
 * @prop name - param name
 * @prop {string} type - one of - 'char', 'double', 'float', 'int'
 * @prop [ref]
 * @prop {boolean} optionalParam
 * @prop [colName]
 * @prop {string} value
 * @prop {String} [minValue] - might be a single value or an array of numbers, look at arraySize and type
 * @prop {String} [maxValue] - might be a single value or an array of numbers, look at arraySize and type
 * @prop {String} [options] - a set of options in one string, separated by commas 'op1,op2,op3'
 */


/**
 * @global
 * @public
 * @typedef {Object} ServiceDescriptorDef
 *
 * @summary The service descriptor info extracted from the table meta data
 *
 * @prop {string} title
 * @prop {string} accessURL
 * @prop {string} standardID
 * @prop {string} value
 * @prop {boolean} allowsInput - use may change the parameter
 * @prop {boolean} inputRequired - user must enter something
 * @prop {Array.<ServiceDescriptorInputParam>} [cisxUI] - names should be one of: HiPS, FOV, hips_initial_ra, hips_initial_dec, moc, examples, hipsCtype1, hipsCtype2
 * @prop {Array.<ServiceDescriptorInputParam>} [cisxTokenSub]
 * @prop {Array.<ServiceDescriptorInputParam>} serDefParams
 */

/**
 * determine is a service descriptor is a datalink service descriptor
 * @param {ServiceDescriptorDef} sd
 * @return {boolean}
 */
export const isDataLinkServiceDesc= (sd) => false && sd?.standardID?.includes(DATALINK_SERVICE); //todo - remove the false after irsa fixes it's service

/**
 * return true if there are service descriptor blocks in this table, false otherwise
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {boolean} true if there are service descriptors
 */
export const hasServiceDescriptors= (tableOrId) => Boolean(getServiceDescriptors(tableOrId));


function getSDDescription(table, ID) {
    if (!ID) return;
    const serviceDefCol= getColumnIdx(table,'service_def');
    const descriptionCol= getColumnIdx(table,'description');
    if (descriptionCol===-1 || serviceDefCol===-1) return;
    if (table.totalRows>50) return;
    const sdRow= table.tableData.data.find( (dAry) => dAry[serviceDefCol]===ID);
    if (!sdRow) return;
    return sdRow[descriptionCol];
}

const gNameMatches= (group,name) => group?.name.toLowerCase()===name?.toLowerCase();


/**
 * return a list of service descriptors found in the table or false
 * @param {String|TableModel} tableOrId
 * @param {boolean} removeAsync
 * @return {Array.<ServiceDescriptorDef>|false}
 */
export function getServiceDescriptors(tableOrId, removeAsync=true) {
    const table= getTableModel(tableOrId);
    if (!table || !isArray(table.resources)) return false;
    const sResources= table.resources.filter(
        (r) => {
            if (!r?.utype || r?.type.toLowerCase()!=='meta') return false;
            const utype= r.utype.toLowerCase();
            return  (utype===adhocServiceUtype || utype===cisxAdhocServiceUtype) &&
                         r.params.some( (p) => (p.name==='accessURL' && p.value));
        });
    if (!sResources.length) return false;
    const sdAry= sResources.map( ({desc,params,ID,groups,utype}, idx) => (
        {
            ID,
            utype,
            title: desc ?? getSDDescription(table,ID) ??'Service Descriptor '+idx,
            accessURL: params.find( ({name}) => name==='accessURL')?.value,
            standardID: params.find( ({name}) => name==='standardID')?.value,
            serDefParams: groups
                .find( (g) => gNameMatches(g,'inputParams'))
                ?.params.map( (p) => {
                    const optionalParam= !p.ref && !p.value && !p.options;
                    return {...p,
                        colName: columnIDToName(table,p.ref),
                        optionalParam,
                        allowsInput: !p.ref,
                        inputRequired: !p.ref && !p.value && !optionalParam
                    };
                }),
            cisxUI: groups.find( (g) => gNameMatches(g,'CISX:ui'))?.params.map( (p) => ({...p})),
            cisxTokenSub: groups.find( (g) => gNameMatches(g,'CISX:tokenSub'))?.params.map( (p) => ({...p})),
        }
    ));
    if (!removeAsync)return sdAry.length ? sdAry : false;
    const sdAryNoAsync= sdAry.filter( ({standardID}) => !standardID?.toLowerCase().includes('async')); // filter out async
    return sdAryNoAsync.length ? sdAryNoAsync : false;
}


/**
 * Check to see if the file analysis report indicates the file is a service descriptor
 * @param {FileAnalysisReport} report
 * @returns {boolean} true if the file analysis report indicates a service descriptor
 */
export function isAnalysisTableDatalink(report) {
    if (report?.parts.length !== 1 || report?.parts[0]?.type !== 'Table' || !report?.parts[0]?.details) {
        return false;
    }

    /**@type FileAnalysisPart*/
    const part= report.parts[0];
    const {tableData}= part.details;
    if (!tableData.data?.length) return;
    const tabColNames= tableData.data.map((d) => d?.[0]?.toLowerCase());
    const hasCorrectCols= SERVICE_DESC_CNAMES.every( (cname) => tabColNames.includes(cname) );
    if (!hasCorrectCols) return false;
    return hasCorrectCols && part.totalTableRows<50; // 50 is arbitrary, it is protections from dealing with files that are very big
}

export function isTableDatalink(table) {
    return SERVICE_DESC_CNAMES.every( (cname) => getColumn(table,cname,true));
}



/**
 * return access_format cell data
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @return {string}
 */
export const getObsCoreAccessFormat= (tableOrId, rowIdx) => getObsCoreCellValue(tableOrId,rowIdx, 'access_format');


/**
 * return s_region cell data
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @return {string}
 */
export const getObsCoreSRegion= (tableOrId, rowIdx) => getObsCoreCellValue(tableOrId,rowIdx, 's_region');

/**
 * return obs_title cell data
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @return {string}
 */
export const getObsTitle= (tableOrId, rowIdx) => getObsCoreCellValue(tableOrId,rowIdx, 'obs_title');


/**
 * return access_url cell data
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @return {string}
 */
export const getObsCoreAccessURL= (tableOrId, rowIdx) => getObsCoreCellValue(tableOrId,rowIdx, 'access_url');
/**
 * return dataproduct_type cell data
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @return {string}
 */
export const getObsCoreProdType= (tableOrId, rowIdx) => getObsCoreCellValue(tableOrId,rowIdx, 'dataproduct_type');

/**
 * Return the dataproduct_type column
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {TableColumn}
 */
export const getObsCoreProdTypeCol= (tableOrId) => getObsCoreTableColumn(tableOrId, 'dataproduct_type');

/**
 * check to see if dataproduct_type cell a votable
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @return {boolean}
 */
export const isFormatVoTable= (tableOrId, rowIdx) => getObsCoreAccessFormat(tableOrId, rowIdx).toLowerCase().includes('votable');


/**
 * check to see if dataproduct_type is a datalink
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @param rowIdx
 * @return {boolean}
 */
export function isFormatDataLink(tableOrId, rowIdx) {
    const accessFormat= getObsCoreAccessFormat(tableOrId,rowIdx).toLowerCase();
    return accessFormat.includes('votable') && accessFormat.includes('content=datalink');
}

export function isFormatPng(tableOrId, rowIdx) {
    const accessFormat= getObsCoreAccessFormat(tableOrId,rowIdx).toLowerCase();
    return accessFormat.includes('jpeg') || accessFormat.includes('jpg') || accessFormat.includes('png');
}


/**
 * @param {TableModel} dataLinkTable - a TableModel that is a datalink call result
 * @return {Array.<{url, contentType, size, semantics, serviceDefRef}>} array of object with important data link info
 */
export function getDataLinkData(dataLinkTable) {
    return (dataLinkTable?.tableData?.data ?? [])
        .map( (row,idx) =>
            ({
                url: getCellValue(dataLinkTable,idx,'access_url' ),
                contentType: getCellValue(dataLinkTable,idx,'content_type' ) ||'',
                size: Number(getCellValue(dataLinkTable,idx,'content_length' )),
                semantics: getCellValue(dataLinkTable,idx,'semantics' ),
                serviceDefRef: getCellValue(dataLinkTable,idx,'service_def' ),
            }))
        .filter( ({url, serviceDefRef}) =>
            serviceDefRef || url?.startsWith('http') || url?.startsWith('ftp') );
}

const DEFAULT_TNAME_OPTIONS = [
    'name',         // generic
    'pscname',      // IRAS
    'target',       //  our own table output
    'designation',  // 2MASS
    'starid'        // PCRS
];

// moved from java, if we decide to use it this is what we had before
export const findTargetName = (columns) => columns.find( (c) => DEFAULT_TNAME_OPTIONS.includes(c));

/**
 * @global
 * @public
 * @typedef {Object} CoordColsDescription
 *
 * @summary And object that describes a pairs of columns in a table that makes up a coordinate
 *
 * @prop {string} type -   content type of the columns, 'center'
 * @prop {string} lonCol - name of the longitudinal column
 * @prop {string} latCol - name of the latitudinal column
 * @prop {number} lonIdx - column index for the longitudinal column
 * @prop {number} latIdx - column index for the latitudinal column
 * @prop {CoordinateSys} csys - the coordinate system to use
 */

/**
 * @global
 * @public
 * @typedef {Object} ColsDescription
 *
 * @summary An object that describe a single column in a table
 *
 * @prop {string} colName - name of the column
 * @prop {number} colIdx - column index for the column
 * @prop {string} unit - unit for the column
 */


/**
 * @global
 * @public
 * @typedef {Object} RegionColDescription
 *
 * @summary An object that describes the column which is ObsCore defined 's_region'
 *
 * @prop {string} type -   content type of the column, 'region'
 * @prop {string} regionCol - name of the column
 * @prop {number} regionIdx - column index for the column
 * @prop {string} unit - unit of the measurement of the region
 */

/**
 * @see {@link http://www.ivoa.net/documents/VOTable/20130920/REC-VOTable-1.3-20130920.html#ToC54}
 * A.1 link substitution
 * @param tableModel    table model with data and columns info
 * @param href          the href value of the LINK
 * @param rowIdx        row index to be resolved
 * @param fval          the field's value, or cell data.  Append field's value to href, if no substitution is needed.
 * @returns {string}    the resolved href after subsitution
 */
export function applyLinkSub(tableModel, href='', rowIdx, fval='') {
    const encode = !!href && !href.match(/^\${[\w -.]+}$/g);      // don't encode if href is blank or consists of exact one token.
    if (encode) fval = encodeURIComponent(fval);                // if encoding is needed, then fval needs to be encoded.
    const rhref = applyTokenSub(tableModel, href, rowIdx, '', encode);
    if (rhref === href) {
        return fval ? href + fval : '';       // no substitution given, append defval to the url.  set A.1
    }
    return rhref;
}

/**
 * applies token substitution if any.  If the resulting value is nullish, return the def val.
 * @see {@link http://www.ivoa.net/documents/VOTable/20130920/REC-VOTable-1.3-20130920.html#ToC54}
 * A.1 link substitution
 * @param tableModel    table model with data and columns info
 * @param val           the value to resolve
 * @param rowIdx        row index to be resolved
 * @param def           return value if val is nullish
 * @param encode        apply url encode to val if true.  default to false
 * @returns {string}    the resolved href after subsitution
 */
export function applyTokenSub(tableModel, val='', rowIdx, def, encode=false) {

    const vars = val?.match?.(/\${[\w -.]+}/g);
    let rval = val;
    if (vars) {
        vars.forEach((v) => {
            const [,cname] = v.match(/\${([\w -.]+)}/) || [];
            const col = getColumnByRef(tableModel, cname);
            let cval = col ? getCellValue(tableModel, rowIdx, col.name) : '';  // if the variable cannot be resolved, return empty string
            if (encode) cval = encodeURIComponent(cval);
            rval = (!cval && v === rval) ? cval : rval.replace(v, cval);
        });
    }
    return rval ? rval : rval === 0 ? 0 : def;
}


/**
 * Guess if this table has enough ObsCore attributes to be considered an ObsCore table.
 * - any column contains utype with 'obscore:' prefix
 * - matches 3 or more of ObsCore column names
 * @param tableModel
 * @returns {boolean}
 */
export function isObsCoreLike(tableModel) {

    const cols = getColumns(tableModel);
    if (cols.findIndex((c) => get(c, 'utype', '').startsWith(obsPrefix)) >= 0) {
        return true;
    }
    const v = intersection(cols.map( (c) => c.name), OBSTAP_CNAMES);
    return v.length > 2;
}



/**
 * @global
 * @public
 * @typedef {Object} SpectrumDM - spectrum data model information:  https://ivoa.net/documents/SpectrumDM/20111120/REC-SpectrumDM-1.1-20111120.pdf
 * @prop {DataAxis}  spectralAxis
 * @prop {DataAxis}  fluxAxis
 * @prop {DataAxis}  timeAxis
 */

/**
 * @global
 * @public
 * @typedef {Object} DataAxis - spectrum data axis.. can be one of FluxAxis, TimeAxis, or SpectralAxis
 * @prop {string}  value                 column name containing the axis values
 * @prop {string}  [unit]
 * @prop {string}  [ucd]
 * @prop {string}  [statError]
 * @prop {string}  [statErrLow]
 * @prop {string}  [statErrHigh]
 * @prop {string}  [lowerLimit]
 * @prop {string}  [upperLimit]
 * @prop {string}  [order]
 */

const spectralAxisPrefix = ['spec:Spectrum.Data.SpectralAxis', 'spec:Data.SpectralAxis', 'ipac:Spectrum.Data.SpectralAxis', 'ipac:Data.SpectralAxis'];
const fluxAxisPrefix = ['spec:Spectrum.Data.FluxAxis', 'spec:Data.FluxAxis', 'ipac:Spectrum.Data.FluxAxis', 'ipac:Data.FluxAxis'];
const timeAxisPrefix = ['spec:Spectrum.Data.TimeAxis', 'spec:Data.TimeAxis'];
const dataAxis = {
    value: 'Value',
    statError: 'Accuracy.StatError',
    statErrLow: 'Accuracy.StatErrLow',
    statErrHigh: 'Accuracy.StatErrHigh',
    upperLimit: 'Accuracy.UpperLimit',
    lowerLimit: 'Accuracy.LowerLimit',
    binLow: 'Accuracy.BinLow',
    binHigh: 'Accuracy.BinHigh',
    // binSize: 'Accuracy.BinSize',
    order: 'Order'
};

/**
 *
 * @param tableModel
 * @returns {SpectrumDM|undefined}
 */
export function getSpectrumDM(tableModel) {
    const utype = tableModel?.tableMeta?.utype?.toLowerCase();
    const isSpectrum = utype === 'spec:Spectrum'.toLowerCase();

    const isSED = utype === 'ipac:Spectrum.SED'.toLowerCase();

    if (!isSpectrum && !isSED) return;

    const findAxisData = (prefix) => {
        const data = {};
        Object.entries(dataAxis).forEach(([key, utype]) => {
            const col = findColByUtype(tableModel, prefix, utype);
            if (col) {
                data[key] = col.name;
                if (key === 'value') {      // defaults to column's attribs if not given as params
                    data.ucd = findParamByUtype(tableModel, prefix, 'UCD')?.value || col.UCD;
                    data.unit = findParamByUtype(tableModel, prefix, 'Unit')?.value || col.units;
                }
            }
        });

        return isEmpty(data) ? undefined : data;
    };
    const fixStatErr = (axis, isSpectral) => {
        if (!axis) return;
        const {statError, statErrLow, statErrHigh} = axis;
        if (statError) {
            axis.statErrLow = axis.statErrHigh = undefined;         // if statError is defined, ignore low/high
        } else if (!statErrLow !== !statErrHigh) {                  // logical xor equivalent (only one with value)
            axis.statError = statErrLow || statErrHigh;             // treat it as statError
            axis.statErrLow = axis.statErrHigh = undefined;
        }
        if (isSpectral) {
            unset(axis, 'upperLimit');
            unset(axis, 'lowerLimit');
        } else {
            unset(axis, 'binHigh');
            unset(axis, 'binLow');
        }
    };

    const spectralAxis  = findAxisData(spectralAxisPrefix);
    const fluxAxis      = findAxisData(fluxAxisPrefix);
    const timeAxis      = findAxisData(timeAxisPrefix);

    fixStatErr(spectralAxis, true);
    fixStatErr(fluxAxis, false);

    if (fluxAxis?.order && !spectralAxis?.order)  spectralAxis.order = fluxAxis.order;      // temporarily: if order is given for the flux axis, treat it as a spectral order.. in the case for Spitzer

    if (spectralAxis && (fluxAxis || timeAxis)) {
        return {spectralAxis, fluxAxis, timeAxis, isSED};
    }
}

function findColByUtype(tableModel, prefixes, suffix) {

    const cols = allRefCols(tableModel, tableModel?.groups) || [];
    for(const p of prefixes) {
        const utype = p + (suffix ? '.' + suffix: '');
        const col =  cols.find( (c) => c?.utype?.toLowerCase() === utype.toLowerCase());
        if (col) return col;
    }
}

function findParamByUtype(tableModel, prefixes, suffix) {

    const params = allParams(tableModel?.groups) || [];
    for(const p of prefixes) {
        const utype = p + (suffix ? '.' + suffix: '');
        const param =  params.find( (c) => c?.utype?.toLowerCase() === utype.toLowerCase());
        if (param) return param;
    }
}

function allRefCols(tableModel, groups) {
    if (!Array.isArray(groups)) return ;
    let cols = [];
    for (const g of groups) {
        cols = cols.concat( g?.columnRefs?.map((r) => {
            const col = getColumnByRef(tableModel, r?.ref);
            if (col) {
                if (r?.utype) col.utype = r.utype;
                if (r?.UCD)   col.UCD = r.UCD;
            }
            return col;
        }) || [] );
        if (g?.groups) {
            cols = cols.concat( allRefCols(tableModel, g.groups) );
        }
    }
    return cols;
}

function allParams(groups) {
    if (!Array.isArray(groups)) return ;
    let params = [];
    for (const g of groups) {
        if (Array.isArray(g?.params)) {
            params = params.concat( g.params );
        }
        if (g?.groups) {
            params = params.concat( allParams(g.groups) );
        }
    }
    return params;
}

/**
 * Based on scheman name, table name, and column names - determine if this
 * is ObsCore-like enough for different ObsCore/ObsTAP widgets.
 * @param schemaName
 * @param tableName
 * @param columnsModel
 * @returns {boolean}
 */
export function matchesObsCoreHeuristic(schemaName, tableName, columnsModel) {
    if (tableName?.toLowerCase() === 'ivoa.obscore'){
        return true;
    }
    if (schemaName?.toLowerCase() === 'ivoa' && tableName?.toLowerCase() === 'obscore') {
        return true;
    }
    if (columnsModel) {
        const column_names = getColumnValues(columnsModel, 'column_name');
        return OBSTAP_MATCH_COLUMNS.every((columnName) => {
            return column_names.indexOf(columnName) >= 0;
        });
    }
    return false;
}

export function getWorldPtFromTableRow(table) {
    const centerColumns = findTableCenterColumns(table);
    if (!centerColumns) return undefined;
    const {lonCol,latCol,csys}= centerColumns;
    const ra = Number(getCellValue(table, table.highlightedRow, lonCol));
    const dec = Number(getCellValue(table, table.highlightedRow, latCol));
    const usingRad= isTableUsingRadians(table, [lonCol,latCol]);
    const raDeg= usingRad ? ra * (180 / Math.PI) : ra;
    const decDeg= usingRad ? dec * (180 / Math.PI)  : dec;
    return makeAnyPt(raDeg,decDeg,csys||CoordinateSys.EQ_J2000);
}
