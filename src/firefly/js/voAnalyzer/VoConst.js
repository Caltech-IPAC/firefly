/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import Enum from 'enum';
import {CoordinateSys} from '../visualize/CoordSys';


export const SEMANTICS = 'semantics';
export const LOCAL_SEMANTICS = 'local_semantics';
export const CONTENT_TYPE = 'content_type';
export const CONTENT_LENGTH = 'content_length';
export const CONTENT_QUALIFIER = 'content_qualifier';
export const ACCESS_URL = 'access_url';
export const ACCESS_FORMAT = 'access_format';
export const S_REGION = 's_region';
export const DESCRIPTION = 'description';
export const SERVICE_DEF= 'service_def';
export const ERROR_MESSAGE= 'error_message';
export const UCDCoord = new Enum(['eq', 'ecliptic', 'galactic']);
export const obsPrefix = 'obscore:';
export const ColNameIdx = 0;
export const UtypeColIdx = 2;

export const SERVICE_DESC_COL_NAMES = ['id', ACCESS_URL, SERVICE_DEF, ERROR_MESSAGE, SEMANTICS,
    DESCRIPTION, CONTENT_TYPE, CONTENT_LENGTH];

export const adhocServiceUtype = 'adhoc:service';
export const cisxAdhocServiceUtype = 'cisx:adhoc:service';
export const standardIDs = {
    tap: 'ivo://ivoa.net/std/tap',
    sia: 'ivo://ivoa.net/std/sia',
    ssa: 'ivo://ivoa.net/std/ssa',
    soda: 'ivo://ivoa.net/std/soda',
    datalink: 'ivo://ivoa.net/std/DataLink',
};

export const VO_TABLE_CONTENT_TYPE= 'application/x-votable+xml';


export const UCDSyntax = new Enum(['primary', 'secondary', 'any'], {ignoreCase: true});

export const ucdSyntaxMap = {
    'pos.eq.ra': UCDSyntax.any,
    'pos.eq.dec': UCDSyntax.any,
    'meta.main': UCDSyntax.secondary,
    'pos.outline': UCDSyntax.primary,
    'obs.field': UCDSyntax.secondary
};


export const obsCorePosColumns = ['s_ra', 's_dec'];
export const mainMeta = 'meta.main';
export const alternateMainPos = [['POS_EQ_RA_MAIN', 'POS_EQ_DEC_MAIN']];

export const UCDList = ['arith', 'arith.diff', 'arith.factor', 'arith.grad', 'arith.rate', 'arith.ratio', 'arith.squared', 'arith.sum', 'arith.variation', 'arith.zp', 'em', 'em.IR', 'em.IR.J', 'em.IR.H',
    'em.IR.K', 'em.IR.3-4um', 'em.IR.4-8um', 'em.IR.8-15um', 'em.IR.15-30um', 'em.IR.30-60um', 'em.IR.60-100um', 'em.IR.NIR', 'em.IR.MIR', 'em.IR.FIR', 'em.UV', 'em.UV.10-50nm', 'em.UV.50-100nm',
    'em.UV.100-200nm', 'em.UV.200-300nm', 'em.X-ray', 'em.X-ray.soft', 'em.X-ray.medium', 'em.X-ray.hard', 'em.bin', 'em.energy', 'em.freq', 'em.freq.cutoff', 'em.freq.resonance', 'em.gamma',
    'em.gamma.soft', 'em.gamma.hard', 'em.line', 'em.line.HI', 'em.line.Lyalpha', 'em.line.Halpha', 'em.line.Hbeta', 'em.line.Hgamma', 'em.line.Hdelta', 'em.line.Brgamma', 'em.line.OIII', 'em.line.CO',
    'em.mm', 'em.mm.30-50GHz', 'em.mm.50-100GHz', 'em.mm.100-200GHz', 'em.mm.200-400GHz', 'em.mm.400-750GHz', 'em.mm.750-1500GHz', 'em.mm.1500-3000GHz', 'em.opt', 'em.opt.U', 'em.opt.B',
    'em.opt.V', 'em.opt.R', 'em.opt.I', 'em.pw', 'em.radio', 'em.radio.20MHz', 'em.radio.20-100MHz', 'em.radio.100-200MHz', 'em.radio.200-400MHz', 'em.radio.400-750MHz', 'em.radio.750-1500MHz',
    'em.radio.1500-3000MHz', 'em.radio.3-6GHz', 'em.radio.6-12GHz', 'em.radio.12-30GHz', 'em.wavenumber', 'em.wl', 'em.wl.central', 'em.wl.effective', 'instr', 'instr.background', 'instr.bandpass',
    'instr.bandwidth', 'instr.baseline', 'instr.beam', 'instr.calib', 'instr.det', 'instr.det.noise', 'instr.det.psf', 'instr.det.qe', 'instr.dispersion', 'instr.experiment', 'instr.filter',
    'instr.fov', 'instr.obsty', 'instr.obsty.seeing', 'instr.offset', 'instr.order', 'instr.param', 'instr.pixel', 'instr.plate', 'instr.plate.emulsion', 'instr.precision', 'instr.rmsf',
    'instr.saturation', 'instr.scale', 'instr.sensitivity', 'instr.setup', 'instr.skyLevel', 'instr.skyTemp', 'instr.tel', 'instr.tel.focalLength', 'instr.voxel', 'meta', 'meta.abstract', 'meta.bib',
    'meta.bib.author', 'meta.bib.bibcode', 'meta.bib.fig', 'meta.bib.journal', 'meta.bib.page', 'meta.bib.volume', 'meta.calibLevel', 'meta.checksum', 'meta.code', 'meta.code.class',
    'meta.code.error', 'meta.code.member', 'meta.code.mime', 'meta.code.multip', 'meta.code.qual', 'meta.code.status', 'meta.cryptic', 'meta.curation', 'meta.dataset', 'meta.email', 'meta.file',
    'meta.fits', 'meta.id', 'meta.id.assoc', 'meta.id.CoI', 'meta.id.cross', 'meta.id.parent', 'meta.id.part', 'meta.id.PI', 'meta.main', 'meta.modelled', 'meta.note', 'meta.number', 'meta.preview',
    'meta.query', 'meta.record', 'meta.ref', 'meta.ref.doi', 'meta.ref.ivoid', 'meta.ref.ivorn', 'meta.ref.uri', 'meta.ref.url', 'meta.software', 'meta.table', 'meta.title', 'meta.ucd', 'meta.unit',
    'meta.version', 'obs', 'obs.airMass', 'obs.atmos', 'obs.atmos.extinction', 'obs.atmos.refractAngle', 'obs.calib', 'obs.calib.flat', 'obs.calib.dark', 'obs.exposure', 'obs.field', 'obs.image',
    'obs.observer', 'obs.occult', 'obs.transit', 'obs.param', 'obs.proposal', 'obs.proposal.cycle', 'obs.sequence', 'phot', 'phot.antennaTemp', 'phot.calib', 'phot.color', 'phot.color.excess',
    'phot.color.reddFree', 'phot.count', 'phot.fluence', 'phot.flux', 'phot.flux.bol', 'phot.flux.density', 'phot.flux.density.sb', 'phot.flux.sb', 'phot.limbDark', 'phot.mag', 'phot.mag.bc',
    'phot.mag.bol', 'phot.mag.distMod', 'phot.mag.reddFree', 'phot.mag.sb', 'phot.radiance', 'phys', 'phys.SFR', 'phys.absorption', 'phys.absorption.coeff', 'phys.absorption.gal', 'phys.absorption.opticalDepth',
    'phys.abund', 'phys.abund.Fe', 'phys.abund.X', 'phys.abund.Y', 'phys.abund.Z', 'phys.acceleration', 'phys.aerosol', 'phys.albedo', 'phys.angArea', 'phys.angMomentum', 'phys.angSize',
    'phys.angSize.smajAxis', 'phys.angSize.sminAxis', 'phys.area', 'phys.atmol', 'phys.atmol.branchingRatio', 'phys.atmol.collisional', 'phys.atmol.collStrength', 'phys.atmol.configuration',
    'phys.atmol.crossSection', 'phys.atmol.element', 'phys.atmol.excitation', 'phys.atmol.final', 'phys.atmol.initial', 'phys.atmol.ionStage', 'phys.atmol.ionization', 'phys.atmol.lande',
    'phys.atmol.level', 'phys.atmol.lifetime', 'phys.atmol.lineShift', 'phys.atmol.number', 'phys.atmol.oscStrength', 'phys.atmol.parity', 'phys.atmol.qn', 'phys.atmol.radiationType', 'phys.atmol.symmetry',
    'phys.atmol.sWeight', 'phys.atmol.sWeight.nuclear', 'phys.atmol.term', 'phys.atmol.transition', 'phys.atmol.transProb', 'phys.atmol.wOscStrength', 'phys.atmol.weight', 'phys.columnDensity',
    'phys.composition', 'phys.composition.massLightRatio', 'phys.composition.yield', 'phys.cosmology', 'phys.current', 'phys.current.density', 'phys.damping', 'phys.density', 'phys.density.phaseSpace',
    'phys.dielectric', 'phys.dispMeasure', 'phys.dust', 'phys.electCharge', 'phys.electField', 'phys.electron', 'phys.electron.degen', 'phys.emissMeasure', 'phys.emissivity', 'phys.energy',
    'phys.energy.Gibbs', 'phys.energy.Helmholtz', 'phys.energy.density', 'phys.enthalpy', 'phys.entropy', 'phys.eos', 'phys.excitParam', 'phys.fluence', 'phys.flux', 'phys.flux.energy', 'phys.gauntFactor',
    'phys.gravity', 'phys.ionizParam', 'phys.ionizParam.coll', 'phys.ionizParam.rad', 'phys.luminosity', 'phys.luminosity.fun', 'phys.magAbs', 'phys.magAbs.bol', 'phys.magField', 'phys.mass',
    'phys.mass.inertiaMomentum', 'phys.mass.loss', 'phys.mol', 'phys.mol.dipole', 'phys.mol.dipole.electric', 'phys.mol.dipole.magnetic', 'phys.mol.dissociation', 'phys.mol.formationHeat',
    'phys.mol.quadrupole', 'phys.mol.quadrupole.electric', 'phys.mol.rotation', 'phys.mol.vibration', 'phys.particle', 'phys.particle.neutrino', 'phys.particle.neutron', 'phys.particle.proton',
    'phys.particle.alpha', 'phys.phaseSpace', 'phys.polarization', 'phys.polarization.circular', 'phys.polarization.coherency', 'phys.polarization.linear', 'phys.polarization.rotMeasure',
    'phys.polarization.stokes', 'phys.polarization.stokes.I', 'phys.polarization.stokes.Q', 'phys.polarization.stokes.U', 'phys.polarization.stokes.V', 'phys.potential', 'phys.pressure',
    'phys.recombination.coeff', 'phys.reflectance', 'phys.reflectance.bidirectional', 'phys.reflectance.bidirectional.df', 'phys.reflectance.factor', 'phys.refractIndex', 'phys.size',
    'phys.size.axisRatio', 'phys.size.diameter', 'phys.size.radius', 'phys.size.smajAxis', 'phys.size.sminAxis', 'phys.size.smedAxis', 'phys.temperature', 'phys.temperature.effective',
    'phys.temperature.electron', 'phys.transmission', 'phys.veloc', 'phys.veloc.ang', 'phys.veloc.dispersion', 'phys.veloc.escape', 'phys.veloc.expansion', 'phys.veloc.microTurb', 'phys.veloc.orbital',
    'phys.veloc.pulsat', 'phys.veloc.rotat', 'phys.veloc.transverse', 'phys.virial', 'phys.volume', 'pos', 'pos.angDistance', 'pos.angResolution', 'pos.az', 'pos.az.alt', 'pos.az.azi', 'pos.az.zd',
    'pos.azimuth', 'pos.barycenter', 'pos.bodycentric', 'pos.bodygraphic', 'pos.bodyrc', 'pos.bodyrc.alt', 'pos.bodyrc.lat', 'pos.bodyrc.lon', 'pos.cartesian', 'pos.cartesian.x', 'pos.cartesian.y',
    'pos.cartesian.z', 'pos.centroid', 'pos.cmb', 'pos.cylindrical', 'pos.cylindrical.azi', 'pos.cylindrical.r', 'pos.cylindrical.z', 'pos.dirCos', 'pos.distance', 'pos.earth', 'pos.earth.altitude',
    'pos.earth.lat', 'pos.earth.lon', 'pos.ecliptic', 'pos.ecliptic.lat', 'pos.ecliptic.lon', 'pos.emergenceAng', 'pos.eop', 'pos.ephem', 'pos.eq', 'pos.eq.dec', 'pos.eq.ha', 'pos.eq.ra', 'pos.eq.spd',
    'pos.errorEllipse', 'pos.frame', 'pos.galactic', 'pos.galactic.lat', 'pos.galactic.lon', 'pos.galactocentric', 'pos.geocentric', 'pos.healpix', 'pos.heliocentric', 'pos.HTM', 'pos.incidenceAng',
    'pos.lambert', 'pos.lg', 'pos.lsr', 'pos.lunar', 'pos.lunar.occult', 'pos.nutation', 'pos.outline', 'pos.parallax', 'pos.parallax.dyn', 'pos.parallax.phot', 'pos.parallax.spect', 'pos.parallax.trig',
    'pos.phaseAng', 'pos.pm', 'pos.posAng', 'pos.precess', 'pos.resolution', 'pos.spherical', 'pos.spherical.azi', 'pos.spherical.colat', 'pos.spherical.r', 'pos.supergalactic', 'pos.supergalactic.lat',
    'pos.supergalactic.lon', 'pos.wcs', 'pos.wcs.cdmatrix', 'pos.wcs.crpix', 'pos.wcs.crval', 'pos.wcs.ctype', 'pos.wcs.naxes', 'pos.wcs.naxis', 'pos.wcs.scale', 'spect', 'spect.binSize', 'spect.continuum',
    'spect.dopplerParam', 'spect.dopplerVeloc', 'spect.dopplerVeloc.opt', 'spect.dopplerVeloc.radio', 'spect.index', 'spect.line', 'spect.line.asymmetry', 'spect.line.broad', 'spect.line.broad.Stark',
    'spect.line.broad.Zeeman', 'spect.line.eqWidth', 'spect.line.intensity', 'spect.line.profile', 'spect.line.strength', 'spect.line.width', 'spect.resolution', 'src', 'src.calib', 'src.calib.guideStar',
    'src.class', 'src.class.color', 'src.class.distance', 'src.class.luminosity', 'src.class.richness', 'src.class.starGalaxy', 'src.class.struct', 'src.density', 'src.ellipticity', 'src.impactParam',
    'src.morph', 'src.morph.param', 'src.morph.scLength', 'src.morph.type', 'src.net', 'src.orbital', 'src.orbital.eccentricity', 'src.orbital.inclination', 'src.orbital.meanAnomaly',
    'src.orbital.meanMotion', 'src.orbital.node', 'src.orbital.periastron', 'src.orbital.Tisserand', 'src.orbital.TissJ', 'src.redshift', 'src.redshift.phot', 'src.sample', 'src.spType',
    'src.var', 'src.var.amplitude', 'src.var.index', 'src.var.pulse', 'stat', 'stat.asymmetry', 'stat.correlation', 'stat.covariance', 'stat.error', 'stat.error.sys', 'stat.filling', 'stat.fit',
    'stat.fit.chi2', 'stat.fit.dof', 'stat.fit.goodness', 'stat.fit.omc', 'stat.fit.param', 'stat.fit.residual', 'stat.Fourier', 'stat.Fourier.amplitude', 'stat.fwhm', 'stat.interval',
    'stat.likelihood', 'stat.max', 'stat.mean', 'stat.median', 'stat.min', 'stat.param', 'stat.probability', 'stat.rank', 'stat.rms', 'stat.snr', 'stat.stdev', 'stat.uncalib', 'stat.value',
    'stat.variance', 'stat.weight', 'time', 'time.age', 'time.creation', 'time.crossing', 'time.duration', 'time.end', 'time.epoch', 'time.equinox', 'time.interval', 'time.lifetime', 'time.period',
    'time.period.revolution', 'time.period.rotation', 'time.phase', 'time.processing', 'time.publiYear', 'time.relax', 'time.release', 'time.resolution', 'time.scale', 'time.start']; // Columns required for heuristic matching

export const CUTOUT_UCDs= ['phys.size','phys.size.radius','phys.angSize', 'pos.spherical.r'];
export const RA_UCDs= ['pos.eq.ra'];
export const DEC_UCDs= ['pos.eq.dec'];

export const OBSTAPCOLUMNS = [
    ['dataproduct_type', 'meta.id', 'ObsDataset.dataProductType'],
    ['calib_level', 'meta.code;obs.calib', 'ObsDataset.calibLevel'],
    ['obs_collection', 'meta.id', 'DataID.collection'],
    ['obs_id', 'meta.id', 'DataID.observationID'],
    ['obs_publisher_did', 'meta.ref.uri;meta.curation', 'Curation.publisherDID'],
    [ACCESS_URL, 'meta.ref.url', 'Access.reference'],
    [ACCESS_FORMAT, 'meta.code.mime', 'Access.format'],
    ['access_estsize', 'phys.size;meta.file', 'Access.size'],
    ['target_name', 'meta.id;src', 'Target.name'],
    ['s_ra', 'pos.eq.ra', 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1'],
    ['s_dec', 'pos.eq.dec', 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2'],
    ['s_fov', 'phys.angSize;instr.fov', 'Char.SpatialAxis.Coverage.Bounds.Extent.diameter'],
    [S_REGION, 'pos.outline;obs.field', 'Char.SpatialAxis.Coverage.Support.Area'],
    ['s_resolution', 'pos.angResolution', 'Char.SpatialAxis.Resolution.Refval.value'],
    ['s_xel1', 'meta.number', 'Char.SpatialAxis.numBins1'],
    ['s_xel2', 'meta.number', 'Char.SpatialAxis.numBins2'],
    ['t_min', 'time.start;obs.exposure', 'Char.TimeAxis.Coverage.Bounds.Limits.StartTime'],
    ['t_max', 'time.end;obs.exposure', 'Char.TimeAxis.Coverage.Bounds.Limits.StopTime'],
    ['t_exptime', 'time.duration;obs.exposure', 'Char.TimeAxis.Coverage.Support.Extent'],
    ['t_resolution', 'time.resolution', 'Char.TimeAxis.Resolution.Refval.value'],
    ['t_xel', 'meta.number', 'Char.TimeAxis.numBins'],
    ['em_min', 'em.wl;stat.min', 'Char.SpectralAxis.Coverage.Bounds.Limits.LoLimit'],
    ['em_max', 'imit em.wl;stat.max', 'Char.SpectralAxis.Coverage.Bounds.Limits.HiLimit'],
    ['em_res_power', 'spect.resolution', 'Char.SpectralAxis.Resolution.ResolPower.refVal'],
    ['em_xel', 'Char.SpectralAxis.numBins', 'meta.number'],
    ['o_ucd', 'meta.ucd', 'Char.ObservableAxis.ucd'],
    ['pol_states', 'meta.code;phys.polarization', 'Char.PolarizationAxis.stateList'],
    ['pol_xel', 'meta.number', 'Char.PolarizationAxis.numBins'],
    ['facility_name', 'meta.id;instr.tel', 'Provenance.ObsConfig.Facility.name'],
    ['obs_title', 'meta.title;obs', 'DataID.title'],
    ['instrument_name', 'meta.id;instr', 'Provenance.ObsConfig.Instrument.name']
];
export const SSA_COV_UTYPE = 'char.spatialaxis.coverage.location.value';
export const SSA_TITLE_UTYPE = 'dataid.title';
export const POS_EQ_UCD = 'pos.eq';

const OBSTAP_OPTIONAL_CNAMES = [
    'dataproduct_subtype', 'target_class', 'obs_title', 'obs_creation_date', 'obs_creator_name',
    'obs_creator_did', 'obs_release_date', 'publisher_id', 'bib_reference', 'data_rights', 's_resolution_max',
    's_calib_status', 's_stat_error', 's_pixel_scale', 't_refpos', 't_calib_status', 't_stat_error',
    'em_ucd', 'em_unit', 'em_calib_status', 'em_res_power_min', 'em_res_power_max', 'em_resolution',
    'em_stat_error', 'o_unit', 'o_calib_status', 'o_stat_error', 'pol_states', 'proposal_id'
];
export const OBSTAP_CNAMES = OBSTAPCOLUMNS.map((row) => row[ColNameIdx]).concat(OBSTAP_OPTIONAL_CNAMES);

export const OBSTAP_MATCH_COLUMNS = [
    S_REGION,
    't_min',
    't_max',
    'em_min',
    'em_max',
    'calib_level',
    'dataproduct_type',
    'obs_collection',
];
export const DEFAULT_TNAME_OPTIONS = [
    'name',         // generic
    'pscname',      // IRAS
    'target',       //  our own table output
    'designation',  // 2MASS
    'starid'        // PCRS
];


export const posCol = {
    [UCDCoord.eq.key]: {
        ucd: [['pos.eq.ra', 'pos.eq.dec'], ...alternateMainPos],
        coord: CoordinateSys.EQ_J2000,
        adqlCoord: 'ICRS'
    },
    [UCDCoord.ecliptic.key]: {
        ucd: [['pos.ecliptic.lon', 'pos.ecliptic.lat']],
        coord: CoordinateSys.ECL_J2000,
        adqlCoord: 'ECLIPTIC'
    },
    [UCDCoord.galactic.key]: {
        ucd: [['pos.galactic.lon', 'pos.galactic.lat']],
        coord: CoordinateSys.GALACTIC,
        adqlCoord: 'GALATIC'
    }
};

