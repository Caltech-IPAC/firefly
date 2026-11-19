import CoordinateSys from '../../visualize/CoordSys.js';
import {findCsysFromUCD, isUCDWith} from '../VoCoreUtils.js';
import {UCDSyntax} from '../VoConst.js';


describe('VoCoreUtils:', () => {

    describe('isUCDWith', () => {

        test('primary matches only the first word', () => {
            expect(isUCDWith('pos.eq.ra;meta.main', 'pos.eq.ra', UCDSyntax.primary)).toBe(true);
            // a proper motion in ra: pos.eq.ra qualifies pos.pm
            expect(isUCDWith('pos.pm;pos.eq.ra', 'pos.eq.ra', UCDSyntax.primary)).toBe(false);
        });

        test('secondary matches only a word after the first', () => {
            expect(isUCDWith('pos.eq.ra;meta.main', 'meta.main', UCDSyntax.secondary)).toBe(true);
            expect(isUCDWith('pos.eq.ra;meta.main', 'pos.eq.ra', UCDSyntax.secondary)).toBe(false);
        });

        test('any matches the word anywhere, and is the default', () => {
            expect(isUCDWith('pos.pm;pos.eq.ra', 'pos.eq.ra', UCDSyntax.any)).toBe(true);
            expect(isUCDWith('pos.pm;pos.eq.ra', 'pos.eq.ra')).toBe(true);
            // VoTableRecognizer passes get(ucdSyntaxMap, word), which is undefined for words not in that map
            expect(isUCDWith('stat.error;pos.galactic.lon', 'pos.galactic.lon', undefined)).toBe(true);
        });

        test('matches whole words, ignoring case', () => {
            expect(isUCDWith('POS.EQ.RA;META.MAIN', 'pos.eq.ra')).toBe(true);
            // the UCD1 word VoConst alternateMainPos lists in upper case, as a table may write it in lower case
            expect(isUCDWith('pos_eq_ra_main', 'POS_EQ_RA_MAIN')).toBe(true);
            expect(isUCDWith('pos.eq.ra', 'pos.eq')).toBe(false);
            expect(isUCDWith('pos.eq', 'pos.eq.ra')).toBe(false);
        });

        test('an empty UCD matches nothing', () => {
            expect(isUCDWith('', 'pos.eq.ra')).toBe(false);
        });
    });

    describe('findCsysFromUCD', () => {

        test('equatorial, galactic and ecliptic position words', () => {
            expect(findCsysFromUCD('pos.eq.ra;meta.main')).toBe(CoordinateSys.EQ_J2000);
            expect(findCsysFromUCD('pos.eq.dec')).toBe(CoordinateSys.EQ_J2000);
            expect(findCsysFromUCD('pos.galactic.lon')).toBe(CoordinateSys.GALACTIC);
            expect(findCsysFromUCD('pos.galactic.lat;meta.main')).toBe(CoordinateSys.GALACTIC);
            expect(findCsysFromUCD('pos.ecliptic.lon')).toBe(CoordinateSys.ECL_J2000);
            expect(findCsysFromUCD('pos.ecliptic.lat')).toBe(CoordinateSys.ECL_J2000);
        });

        test('UCD1 main position words are equatorial', () => {
            expect(findCsysFromUCD('POS_EQ_RA_MAIN')).toBe(CoordinateSys.EQ_J2000);
            expect(findCsysFromUCD('pos_eq_dec_main')).toBe(CoordinateSys.EQ_J2000);
        });

        test('a position word after the first still names the coordinate system', () => {
            // findCsysFromUCD gives the frame a UCD names; VoTableRecognizer decides whether the column is
            // the position itself or a related quantity like these proper motion and error columns
            expect(findCsysFromUCD('pos.pm;pos.galactic.lon')).toBe(CoordinateSys.GALACTIC);
            expect(findCsysFromUCD('stat.error;pos.eq.dec')).toBe(CoordinateSys.EQ_J2000);
        });

        test('no coordinate system for other UCDs or no UCD', () => {
            expect(findCsysFromUCD('phot.mag;em.opt.V')).toBeUndefined();
            // the single-column array word of VoConst posColArrayCoord is not a lon or lat word
            expect(findCsysFromUCD('pos.eq')).toBeUndefined();
            expect(findCsysFromUCD('')).toBeUndefined();
            expect(findCsysFromUCD(undefined)).toBeUndefined();
            expect(findCsysFromUCD(null)).toBeUndefined();
        });
    });
});
