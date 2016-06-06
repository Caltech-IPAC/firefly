/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {fk5prc,fk5prp} from './Fk5prc.js';
import {ecleqx,equecx} from './Ecleqx.js';
import {nwcprc} from './Nwcprc.js';
import {Jgtfkc, jgtfkc, junfkc} from './Jgtfkc.js';

/**
 * 31jul92 - changes to jrmvet,jaddet; substituted more general purpose
 *     subs jgtobq,ecleqx,equec2 for limited- purpose subs gtoblq,ecleq2,equec2
 * 23jul92 to call nwcprc/nwcprp instead of jprecj.
 * 02jul92 - re; routines with propermotions:
 *    gtjulp will assume eqx,epoch B1950 in and yield eqx,epoch J2000 out.
 *    unjulp will assume eqx,epoch J2000 in and yield eqx,epoch B1950 out.
 * 30jun92 to apply fk5-fk4 sys. corr. to eqx B1950 position before any other
 *   corrections.
 * 05,06mar92 to use astron. suppl 1984 & 1961 e-term removal equations
 *    (in subs gtjul2,gtjulp,unjul,unjulp)
 * 13-15jan92 updated to reflect method described in FK5 Part I. Catalog
 * author:   judy bennett
 *
 * to precess old besselian equinox positions to equator and equinox of
 *   J2000.0.
 *
 *   Calls subroutines nwcprc ("old" Newcomb constants) and fk5prc and fk5prp
 *      ("new" Julian precession formulas).
 *
 *  Source of method:
 *    Fifth Fundamental Catalogue (FK5) Part I (1988, but not avail. till 1992)
 *     Section 8. (page 9)
 *     Summary of order:
 *  1.  Precess each object (using precessional values adopted in that cat-
 *        i.e. usually Newcomb) from the catalogue equinox to the mean equinox
 *        at the object's mean epoch (i.e. observation date).
 *  2.  The mean positions in all star catalogues published prior to 1984
 *        contain the terms of elliptic aberration (E-terms). These terms need
 *        to be removed from the star's mean position.
 *  3.  The equinox correction and the systematic correction FK5-FK4
 *       (given in the FK5 cat), computed for the object's mean epoch, need to
 *       be applied. Note: program applies fk5-fk4 sys.corr. at B1950 which
 *       seems consistent with procedure actually described in the FK5.
 *  4.  The corrected position has to be precessed to the new standard equinox
 *       J2000.0 using the precessional quantities as adopted in the IAU(1976)
 *       System of Astronomical Constants (also see Astronomical Almanac 1984,
 *       pages S34,S36)
 *
 *  NOTE: these routines apply the fk5-fk4 correction at B1950.
 *        if no fk5-fk4 systematic correction required, the user should
 *        include the following statements in his program:
 *  (a value of 0 for japply specifies no corrections; anything else uses corr.)
 *        external jfk5bd
 *        common /fkappl/ japply
 *        japply = 0
 *
 * ****************************************************************************
 *   All arguments are double precision, except ieflg which is integer.
 *  Input values:
 * @param eqxin is the equinox of the input position (if negative, absolute val.used)
 * @param ra    is right ascension (decimal degrees) at eqx
 * @param dec   is declination (decimal degrees) at eqx
 * @param tobsin is the year of observation (i.e. when the object was observed at
 *         this position (e.g. 1983.5d0) ; if 0.0d0, value of eqxin is used.
 * @param ieflg is flag allowing removal of E-terms of aberration if any (usually
 *        they are present, therefore ieflg should = +1):
 *         ieflg = -1 do not remove E-terms (there none).
 *         ieflg = any value except -1 indicates  E-terms are present and
 *                 are to be removed.
 * @return {{ra: number, dec: number}}
 * raout  is right ascension at equator and equinox of J2000.0
 * decout is declination at equator and equinox of J2000.0
 *
 * ****************************************************************************
 *
 * Files required: gtjul2.f (includes gtjul2,gtjulp,unjul2,unjulp,gtecle,
 *                           gtetrm,itere,iterec,jaddet,jclpos,jrmvet)
 *                 ecleqx.f (includes ecleqx,equecx,jgtobq)
 *                 fk5prc.f (includes fk5prc,fk5prp)
 *                 nwcprc.f (includes nwcprc,nwcprp)
 *                 jgtfkc.f (includes jgtfkc,junfkc,jnitfk.jfk5bd,dintrp,dintr2)
 *
 * ****************************************************************************
 */
export function gtjul2( eqxin, ra, dec, tobsin, ieflg) {

    var raout, decout;
    var rat50=0.0, dect50=0.0;
    var rat=0.0, dect=0.0;
    var corra=0.0, corrd=0.0, corrpa=0.0, corrpd=0.0;

    var eqx  = Math.abs(eqxin);
    var tobs = Math.abs(tobsin);
    if(tobs == 0.0) tobs = eqx;

    if(Jgtfkc.japply===0) {
        rat50 = ra;
        dect50 = dec;
    }
    else {
          //   determine fk5-fk4 systematic correction using eqx B1950 postion

        if(eqx!==1950.0) {
            const ret= nwcprc(eqx,ra,dec,1950.0, rat50,dect50);
            rat50 = ret.raou;
            dect50= ret.decou;
        }
        else {
            rat50 = ra;
            dect50 = dec;
        }
        const r1= jgtfkc(rat50,dect50,0.0,tobs, corra,corrd,corrpa,corrpd);
        corra=  r1.corra;
        corrd=  r1.corrd;
        // corrpa= r1.corrpa;
        // corrpd= r1.corrpd;
        rat50 = rat50 + corra;
        dect50 = dect50 + corrd;
        const r3= jclpos(rat50,dect50);
        rat50 = r3.ra;
        dect50= r3.dec;
        eqx = 1950.0;
    }

    if (tobs != eqx) {
/*                     use old newcomb formula to prec. to tobs */
        const ret= nwcprc( eqx, rat50, dect50, tobs);
        rat = ret.raou;
        dect= ret.decou;
    }
    else {
        rat  = rat50;
        dect = dect50;
    }
/*
* compute the correction for right ascension at the mean epoch of observations.
*   note: this correction will generally be of order +0.06s for most modern 
*         catalogs.
* delt is fraction of Julian century; adjust tobs in besselian time frame
*   to be correct
*   in terms of Julian. use jde of b1950 and tropical year in days
*   to get Julian day of observations. Then compute equivalent year for 
*   Julian prec. prog. so fraction of Julian year will be correct.
*   B1950 = JDE 2433282.4235  365.2421988 tropical days per year.
*   J2000 = JDE 2451545.00    365.25 Julian days per year. 
*
*/
    var jde = ((tobs - 1950.0) * 365.2421988) + 2433282.4235;
    var tobsj = 2000.0 + ((jde - 2451545.0)/365.25);
/*
* Remove E-terms if necessary:
*/
    if(ieflg !==-1) {
        const ret= jrmvet(tobsj,rat,dect);
        rat=  ret.ra;
        dect= ret.dec;
    }
/*
* Apply the equinox correction (use tobs not tobsj):
*
*/
    var delt = (tobs - 1950.0) * 0.01;
    var dela = ((0.035 + 0.085*delt) * 15.0) / 3600.0;

    rat = rat + dela;
    const jcl1= jclpos(rat,dect);
    rat= jcl1.ra;
    dect= jcl1.dec;

/*
* now compute mean place of source at 2000 Jan. 1.5 using the mean place 
* at the mean epoch of observations tobs (with dela applied) and the
* new precession formulas (i.e. Julian prec. formulas)
*
*/
    const fk5prcRet= fk5prc( tobsj, rat, dect, 2000.0);
    raout=  fk5prcRet.raou;
    decout= fk5prcRet.decou;

    return {ra:raout,dec:decout};
}

/**
 * author: judy bennett
 * 01jul92
 * 26jun92 to allow removal of fk5-fk4 systematic corrections
 * 17jan92
 * 05feb92 to use fk5prc instead of hcprec.
 *
 * unjul2 reverses what gtjul2  did - i.e. unjul2 precesses accurately
 *    back to the original besselian input.
 *
 * to precess new Julian  equinox positions (J2000.0)back to input besselian
 *   equinox positions (B1950.0).
 *
 *   Calls subroutines nwcprc ("old" Newcomb constants) and fk5prc ("new"
 *      Julian prec.formulas).
 *
 *  Source of method:
 *   See comments in gtjul2.
 *
 * ****************************************************************************
 *   All arguments are double precision, except ieflg which is integer.
 *  Input values:
 * @param rain is right ascension (decimal degrees) at J2000.0
 * @param decin is declination (decimal degrees) at J2000.0
 * @param tobsin is the year of observation (i.e. when the object was assigned this pos)
 *         if 0.0d0, eqxbou is used.
 * @param ieflg is flag allowing restore of E-terms of aberration if any (usually
 *        they are present; ieflg should usually = + 1).
 *         ieflg =-1 do not replace E-terms (there none).
 *         ieflg = anything but -1 indicates  E-terms are to be present
 *             and are to be replaced. Usually they are to be replaced.
 * @param eqxbou is output equinox (besselian): if 0.0d0, 1950.0d0 is used.
 * @return {{ra: number, dec: number}}
 * raout  is right ascension at besselian equinox of eqxbou.
 * decout is declination at besselian equinox of eqxbou.
 *
 * ****************************************************************************
 */
export function unjul2(rain, decin, tobsin, ieflg, eqxbou) {

    var raout, decout;
    var tobsb=0.0;
    var rat=0.0, dect=0.0;
    var eqx1=2000.0,eqx2=0.0,rat50=0.0,dect50=0.0;
    var corra=0.0, corrd=0.0, corrpa=0.0, corrpd=0.0;

    if(eqxbou!==0.0) {
        eqx2 = Math.abs(eqxbou);
    }
    else {
        eqx2 = 1950.0;
    }

    if(tobsin!==0.0) {
        tobsb = Math.abs(tobsin);
    }
    else {
        tobsb = eqx2;
    }
    var ra  = rain;
    var dec = decin;
/*
* tobsj is year in terms of Julian years; tobsb is year in terms of tropical.
*
*/
    var jde = ((tobsb - 1950.0) * 365.2421988) + 2433282.4235;
    var tobsj = 2000.0 + ((jde - 2451545.0)/365.25);

    const r0= fk5prc( eqx1, ra, dec, tobsj, rat, dect);
    rat = r0.raou;
    dect= r0.decou;
       
/*
* remove the equinox correction (use tobsb not tobsj):
*
*/
    var delt = (tobsb - 1950.0) * 0.01;
    var dela = ((0.035 + 0.085*delt) * 15.0) / 3600.0;

    rat = rat - dela;
    if(rat >= 360.0) rat = rat - 360.0;
    if(rat <   0.0)  rat = rat + 360.0;
/*
* Add back  E-terms if necessary:
*/
    if(ieflg!==-1)  {
        const ret= jaddet(tobsj,rat,dect);
        rat = ret.ra;
        dect= ret.dec;
    }

    if(Jgtfkc.japply===0) {
        if(tobsb!==eqx2)  {
            const ret= nwcprc( tobsb,rat, dect, eqx2);
            raout = ret.raou;
            decout= ret.decou;
        }
        else {
            raout = rat;
            decout = dect;
        }
    }
    else { //     find and remove fk5-fk4 systematic corrections:
        if(tobsb===1950.0) {
            rat50 = rat;
            dect50 = dect;
        }
        else {
            const ret= nwcprc(tobsb,rat,dect,1950.0);
            rat50 = ret.raou;
            dect50= ret.decou;
        }

        const r12= junfkc(rat50,dect50,0.0,tobsb, corra,corrd,corrpa,corrpd);
        corra=  r12.corra;
        corrd=  r12.corrd;
        // corrpa= r12.corrpa;
        // corrpd= r12.corrpd;

        rat50 = rat50 - corra;
        dect50 = dect50 - corrd;
        const r5= jclpos(rat50,dect50);
        rat50=  r5.ra;
        dect50= r5.dec;
        if(eqx2!==1950.0) {
            const ret= nwcprc(1950.0,rat50,dect50, eqx2);
            raout = ret.raou;
            decout= ret.decou;
        }
        else {
            raout = rat50;
            decout = dect50;
        }
    }

    return {ra:raout,dec:decout};
}

/**
 * 27jul92 to allow pmain,pmdin =0.0d0 to be processed as any other
 *         source with given proper motions (User must use gtjul2
 *         if proper motions are "intrinsically" zero (e.g. radio source)
 *         or if no proper motions are given for the object).
 * 02jul92 to apply fk5-fk4 systematic corrections at B1950.
 *  eqxin will be ignored; jgtjulp assumes equinox,epoch B1950 for inputs.
 * 24jun92 to apply fk5-fk4 systematic corrections if desired.
 * 05feb92 like gtjul2, except for objects with proper motions etc.
 *
 * author: judy bennett
 *
 * to precess old besselian equinox positions to equator, equinox and epoch
 *   of J2000.0.
 *
 *  Source of method:
 *    Fifth Fundamental Catalogue (FK5) Part I (1988, but not avail. till 1992)
 *     Section 8. (page 9) + discussion in FK5
 *
 * ****************************************************************************
 *   All arguments are double precision, except ieflg which is integer.
 *  Input values:
 * @param eqxin is ignored (assumes 1950.0d0);
 *       is the equinox & epoch of input position
 * @param rain  is right ascension (decimal degrees) at equinox & epoch of B1950
 * @param decin is declination (decimal degrees) at equinox & epoxh of B1950
 * @param pmain is proper motion in ra in seconds of time per tropical century
 * @param pmdin is proper motion in dec in seconds of arc per tropical century
 * @param pin is parallax in arc seconds.
 * @param vin is radial velocity in km/sec
 * @param ieflg is flag allowing removal of E-terms of aberration if any (usually
 *        E-terms are present; therefore, ieflg should = +1)
 *         ieflg = -1 do not remove E-terms (there none).
 *         ieflg = anything except -1 indicates  E-terms are present and
 *                 are to be removed.
 *  Returned values:
 * @return raout  is right ascension at epoch, equator and equinox of J2000.0
 * @return decout is declination at epoch, equator and equinox of J2000.0
 * @return pmaout is proper motion in seconds of time per Julian century.
 * @return pmdout is proper motion in seconds of arc per Julian century.
 * @return {{ra: number, dec: number, pmra: number, pmdec: number}}
 *
 * ****************************************************************************
 *
 */
export function gtjulp( eqxin, rain,  decin, pmain, pmdin, pin,
                        vin, ieflg) {

    var raout, decout,pmaout,pmdout;
    var pma=0.0, pmd=0.0, corra=0.0, corrd=0.0, corrpa=0.0, corrpd=0.0;
    var tobs=1950.0;
    var work=0.0;
    var rat=0.0, dect=0.0;

    const dtor =  Math.atan(1.0) / 45.0;

    if(Jgtfkc.japply===0) {
        rat = rain;
        dect = decin;
        pma = pmain;
        pmd = pmdin;
    }
    else { // determine fk5-fk4 correction & apply
        rat = rain;
        dect = decin;
        const r2= jgtfkc(rat,dect,0.0,tobs, corra,corrd,corrpa,corrpd);
        corra=  r2.corra;
        corrd=  r2.corrd;
        corrpa= r2.corrpa;
        corrpd= r2.corrpd;

        rat = rat + corra;
        dect = dect + corrd;
        const r1= jclpos(rat,dect);
        rat= r1.ra;
        dect=r1.dec;
        pma = pmain + corrpa;
        pmd = pmdin + corrpd;
    }
/*
* compute the correction for right ascension at the mean epoch of observations.
*   note: this correction will generally be of order +0.06s for most modern 
*         catalogs.
* delt is fraction of Julian century; adjust tobs in besselian time frame
*   to be correct
*   in terms of Julian. use jde of b1950 and tropical year in days
*   to get Julian day of observations. Then compute equivalent year for 
*   Julian prec. prog. so fraction of Julian year will be correct.
*   B1950 = JDE 2433282.4235  365.2421988 tropical days per year.
*   J2000 = JDE 2451545.00    365.25 Julian days per year. 
*
*/
    var jde = ((tobs - 1950.0) * 365.2421988) + 2433282.4235;
    var tobsj = 2000.0 + ((jde - 2451545.0)/365.25);
/*
*
*  Remove E-terms if necessary:
*/
    if(ieflg!==-1) {
        const ret= jrmvet(tobsj,rat,dect);
        rat = ret.ra;
        dect= ret.dec;
    }
/*
* Apply the equinox correction (use tobs not tobsj):
*      equinox correction from fk5 cat (Section 3 (page 6).
*/

    var delt = (tobs - 1950.0) * 0.01;
    var dela = ((0.035 + 0.085*delt) * 15.0) / 3600.0;

    rat = rat + dela;
    const r2= jclpos(rat,dect);
    rat= r2.ra;
    dect=r2.dec;
/*
* convert proper motions from units per tropical century to units per Julian
*   century per p.S35 of Supp. to Astron.Alman.
* apply time-dependent portion of eqx corr to ra proper motion (0.0850 secs of
*   time per Julian century) per FK5 Catalog (Section 3 (page 6)).
* also adjust for change in precession constant per p. S35 of Supp. to
*   Astron. Alman. (note; misprint in Supp.: 0.6912 should be 0.06912)
*
*/

    const rar = rat*dtor;
    const decr = dect*dtor;
    if(Math.abs(dect) > 89.9999) work = 0.0;
    else                         work = 0.0291*Math.sin(rar)*Math.tan(decr);
    const pmat = (pma * 1.00002136) - 0.06912 - work + 0.0850;
    const pmdt = (pmd * 1.00002136) - 0.436*Math.cos(rar);
/*
* now compute mean place of source at 2000 Jan. 1.5 using the mean place 
* at at the mean epoch of observations tobs (with dela applied) and the
* new precession formulas (i.e. Julian prec. formulas)
*
*
*/

    const r4= fk5prp( tobsj, rat, dect, pmat, pmdt, pin, vin, 2000.0);
    raout=  r4.raou;
    decout= r4.decou;
    pmaout= r4.pmaou;
    pmdout= r4.pmdou;

    return {ra:raout,dec:decout,pmra:pmaout,pmdec:pmdout};
}


/**
 * 27jul92 to allow pmain,pmdin =0.0d0 to be processed as any other
 *         source with given proper motions (User must use unjul2
 *         if proper motions are "intrinsically" zero (e.g. radio source)
 *         or if no proper motions are given for the object).
 * 02jul92-eqxbou is ignored (assumed to be equinox,epoch B1950.0d0 for outputs)
 * 26jun92 to allow removal for fk5-fk4 correction.
 * author:  judy bennett
 *
 * unjulp reverses what gtjulp  did - i.e. unjulp precesses accurately
 *    back to the original besselian input ( B1950).
 *
 * to precess new Julian  equinox positions (J2000.0)back to input besselian
 *   equinox positions (B1950.0).
 *
 *  Source of method:
 *   See comments in gtjul2.
 *
 * ****************************************************************************
 *   All arguments are double precision, except ieflg which is integer.
 *  Input values:
 * @param rain is right ascension (decimal degrees) at epoch & equinox of J2000.0
 * @param decin is declination (decimal degrees) at epoch & equinox of J2000.0
 * @param pmain is proper motion in ra in seconds of time per Julian century.
 * @param pmdin is proper motion in dec in seconds of arc per Julian century.
 * @param pin is parallax in arc seconds.   (0.0d0 if unknown)
 * @param vin is radial velocity in km/sec. (0.0d0 if unknown)
 * @param ieflg is flag allowing restore of E-terms of aberration. Usually ieflg
 *   should = +1 (to restore the E-terms to the B1950 position).
 *         ieflg =-1 do not replace E-terms (there none).
 *         ieflg = anything but -1 indicates  E-terms are to be present
 *             and are to be replaced.
 * @param eqxbou is ignored (assumed to be 1950.0d0);
 *        is output equinox (besselian):
 * @return {{ra: number, dec: number, pmra: number, pmdec: number}}
 * raout  is right ascension (decimal degrees) at equinox,epoch B1950.
 * decout is declination (decimal degrees) at equinox,epoch B1950.
 * pmaout  is proper motion in ra in seconds of time per tropical century.
 * pmdout  is proper motion in dec in seconds of arc per tropical century.
 *
 * ****************************************************************************
 *
 */

export function unjulp(rain, decin, pmain, pmdin, pin, vin, ieflg,eqxbou) {
    var raout, decout,pmaout,pmdout;
    var tobsb=1950.0;
    var rat=0.0, dect=0.0, pmat=0.0;
    var pmdt=0.0;
    var eqx1 = 2000.0;
    var corra=0.0, corrd=0.0, corrpa=0.0, corrpd=0.0;
    var work=0.0;

    const dtor = Math.atan(1.0)/45.0;

    var ra = rain;
    var dec = decin;

    var jde = ((tobsb - 1950.0) * 365.2421988) + 2433282.4235;
    var tobsj = 2000.0 + ((jde - 2451545.0)/365.25);

    const r1= fk5prp( eqx1, ra, dec, pmain, pmdin, pin, vin, tobsj, rat, dect, pmat, pmdt);
    rat=  r1.raou;
    dect= r1.decou;
    pmat= r1.pmaou;
    pmdt= r1.pmdou;
/*
* re: proper motions:  remove adjustment for precession constant;
*     remove equinox corr. for proper motion in ra;
*     convert from units per Julian centry to units per tropical century:
*/
    var rar  = dtor*rat;
    var decr = dtor*dect;
    if(Math.abs(dect) > 89.9999) {
        work = 0.0;
    }
    else {
        work = 0.0291*Math.sin(rar)*Math.tan(decr);
    }
    pmat = pmat + 0.06912 + work - 0.0850;
    pmaout = pmat / 1.00002136;
    pmdt = pmdt + 0.436 * Math.cos(rar);
    pmdout = pmdt / 1.00002136;
/*
* remove the equinox correction (use tobsb not tobsj):
*
*/

    var delt = (tobsb - 1950.0) * 0.01;
    var dela = ((0.035 + 0.085*delt) * 15.0) / 3600.0;

    rat = rat - dela;
    if(rat >= 360.0)   rat = rat - 360.0;
    else if(rat < 0.0) rat = rat + 360.0;

//Add back  E-terms if necessary:

    if(ieflg !==-1) {
        const ret= jaddet(tobsj,rat,dect);
        rat=  ret.ra;
        dect= ret.dec;
    }

    if(Jgtfkc.japply != 0) { // remove the fk5-fk4 systematic corrections:

        const r14= junfkc(rat,dect,0.0,tobsb, corra,corrd,corrpa,corrpd);
        corra=  r14.corra;
        corrd=  r14.corrd;
        corrpa= r14.corrpa;
        corrpd= r14.corrpd;
        rat = rat - corra;
        dect = dect - corrd;
        pmaout = pmaout - corrpa;
        pmdout = pmdout - corrpd;
        const ret= jclpos(rat,dect);
        rat= ret.ra;
        dect=ret.dec;
    }

    raout = rat;
    decout = dect;

    return {ra:raout,dec:decout,pmra:pmaout,pmdec:pmdout};
}

/**
 *for adding E-term back (note: w/ supp. formulas, edela,edeld are added to
 *
 * @param ra and dec to remove the E-term; therefore subtract here.
 * @param dec
 * @return {{ra: number, dec: number}}
 */
export function itere(ra, dec) {
    var edela,edeld;
    var i,iend1;
    var rwork,dwork;
    rwork = ra;
    dwork = dec;
    iend1 = 3;

    var loopRet;
    for (i=0; i<iend1;i++) {
        loopRet= gtetrm(rwork,dwork);
        edela= loopRet.ra;
        edeld= loopRet.dec;
        if (i == 2) return {ra:edela,dec:edeld};
        rwork = ra    - edela;
        dwork = dec   - edeld;
        loopRet= jclpos(rwork,dwork);
        rwork= loopRet.ra;
        dwork= loopRet.dec;
    }
    return {ra:edela,dec:edeld};
}

var nthrue=0;
var e1, e2, e3, e4, dtor;

/**
 *  from Suppl. to Astron. Alman. 1984 (also 1961 supp. page 144)
 *    see also, Standish, A&A 115, 20-22 (1982)
 *
 * compute E-terms to be removed for  object at ra and dec.
 *  all args. double precision and in decimal degrees)
 *
 *  Since the E-terms (terms of elliptic aberration) change so slowly
 *    (Smart,Textbook on Spherical Astronomy, Sixth Ed. Section 108,p186)
 *     these values do not require t as input and will be valid in the
 *     1950 to 2000 time span we are dealing with.
 *
 *  The 1961 supp called these equations an approximation and stated that
 *  small errors in this procedure are usually negligible. However, they
 *  did not explain what lead up to the procedure:  "The form of the equations
 *  of condition and their solution are not discussed here."
 *
 * @return {{ra: number, dec: number}}
 *
 */
export function gtetrm(ra, dec) {
    var dela, deld;
    var dcosd, alplus;

    if(nthrue===0) {
        dtor = Math.atan(1.0) / 45.0;
        // note: e1 = (0.0227 * 15.0) / 3600.0 = 0.341/3600 = e3 */
        e2 = 11.25 * 15.0;
        e3 = 0.341 / 3600.0;
        e4 = 0.029 / 3600.0;
        e1 = e3;
        nthrue = 1;
    }

    alplus = ra    + e2;
    if(alplus >= 360.0) alplus = alplus - 360.0;
    alplus = alplus * dtor;

    dcosd = Math.cos(dtor * dec);
    if(Math.abs(dec) >= 90.0 || Math.abs(dcosd) < 1.0e0-27) {
        dela = 0.0;
        deld = 0.0;
    }
    else {
        dela = (e1 * Math.sin(alplus)) / dcosd;
    }

    deld = (e3 * Math.cos(alplus) * Math.sin(dec*dtor)) + (e4 * dcosd);

    return {ra:dela,dec:deld};
}

var nthrue2 = 0;
var kappa, lepoch=-1.0, e, pirad;

/**
 *
 * compute E-terms at epoch for ecliptic lambda, beta input (returned in dela,
 *  deld (degrees).
 *  epoch in years (e.g. 1950.000), lambda,beta,,dela,deld in decimal degrees.
 *  All arguments double precision.
 *
 * E-term formulas from ASTRONOMICAL ALGORITHMS by Jean Meeus (1991) ch.22
 *   Note: equations as presented are for computing E-terms from position
 *         that does not contain E-terms.  To get better answer (when
 *         splitting hairs), iterate to get best E-term (for position that
 *         has E-terms) to be removed. Subroutine iterec may be called to
 *         to do the iteration.
 *   Note 2: these formulas for E-terms, as function of ecliptic lon and lat,
 *           also appear in  Spherical Astronomy by R.Green (1985),page 192;
 *           and in Textbook on Spherical Astronomy, Sixth Ed.,by Smart (1977),
 *           page 186.
 *
 *   To remove E-terms, subtract dela & deld from lamba & beta, respectively,
 *   in the calling program.
 *   To add back E-terms, add dela & deld to lambda & beta, respectively, in
 *   the calling program.
 *
 * @return {{ra: number, dec: number}}
 *
 */
export function gtecle( epoch, lambda, beta) {
    var dela,  deld;
    var t, t2, pi, lrad, brad;

    if(nthrue2===0) {
        dtor = Math.atan(1.0) / 45.0;
        // constant of aberration, kappa = 20.49552" "new" ("old" was 20.496) */
        kappa = 0.0056932;
        nthrue2 = 1;
    }
    dela = 0.0;
    deld = 0.0;

    if(epoch!==lepoch) {
        t = (epoch - 2000.0) * 0.01;
        t2 = t*t;
        lepoch = epoch;
                    //e = eccentricity of the Earth's orbit
                    //pi= longitude of the perihelion of this orbit
         e = 0.016708617 - 0.000042037*t - 0.0000001236*t2;
         pi= 102.93735 + 0.71953*t + 0.00046*t2;
         pirad = dtor * pi;
    }

    if(Math.abs(beta) > 89.999) return {ra:dela,dec:deld};
    lrad = dtor*lambda;
    brad = dtor*beta;

    dela = e * kappa * Math.cos(pirad-lrad) / Math.cos(brad);
    deld = e * kappa * Math.sin(pirad-lrad) * Math.sin(brad);

    return {ra:dela,dec:deld};
}

/**
 *
 * @param tobsj
 * @param lambda
 * @param beta
 * @return {{ra: number, dec: number}}
 */
export function iterec(tobsj,lambda, beta) {
    var edela,edeld;
    var i, iend1 = 3;
    var lwork,bwork;
    lwork = lambda;
    bwork = beta;
    var rdRet;
    for (i=0; i<iend1; i++) {
        rdRet= gtecle(tobsj,lwork,bwork);
        edela= rdRet.ra;
        edeld= rdRet.dec;
        lwork = lambda - edela;
        bwork = beta - edeld;
        rdRet= jclpos(lwork,bwork);
        lwork= rdRet.ra;
        bwork= rdRet.dec;
    }
    return {ra:edela,dec:edeld};
}

/** Remove E-terms:
 *  31jul92 update to use equecx and ecleqx instead of equec2 and ecleq2.
 * @param tobsj
 * @param rat
 * @param dect
 * @return {{ra: number, dec: number}}
 */
export function jrmvet( tobsj, rat, dect) {
    var edela= 0.0,edeld= 0.0;
    var lambda= 0.0, beta= 0.0, pole=89.999;

    if(Math.abs(dect) < pole) {
        const r15= gtetrm( rat, dect);
        edela= r15.ra;
        edeld= r15.dec;
        rat = rat + edela;
        dect = dect + edeld;
        const ret= jclpos(rat,dect);
        rat= ret.ra;
        dect=ret.dec;
    }
    else {
                 // note: using "Julian system" (iaus=2 for equecx and ecleqx) here -
                 // makes no. dif in resulting E-terms and simplifies argument
                 // list required for jrmvet.

        const r18= equecx(2,tobsj,rat,dect,lambda,beta);
        lambda= r18.xlam;
        beta  = r18.beta;
        const iterRet= iterec(tobsj,lambda,beta,edela,edeld);
        edela= iterRet.ra;
        edeld= iterRet.dec;
        lambda = lambda - edela;
        beta = beta - edeld;
        const ret= jclpos(lambda, beta);
        lambda= ret.ra;
        beta=   ret.dec;
        const r24= ecleqx(2,tobsj,lambda,beta,rat,dect);
        rat=  r24.rad;
        dect= r24.decd;
    }

    return {ra:rat,dec:dect};
}

/** Add back  E-terms:
 *  31jul92 update to use equecx and ecleqx instead of equec2 and ecleq2.
 * @param tobsj
 * @param rat
 * @param dect
 * @return {{ra: number, dec: number}}
 */
export function jaddet(tobsj,rat,dect) {
    var edela= 0.0, edeld= 0.0, lambda= 0.0, beta= 0.0, pole = 89.999;

    if(Math.abs(dect) < pole) {
        const iterRet= itere( rat, dect);
        edela= iterRet.ra;
        edeld= iterRet.dec;
        rat = rat - edela;
        dect = dect - edeld;
        const ret= jclpos(rat,dect);
        rat= ret.ra;
        dect=ret.dec;
    }
    else {
        const r20= equecx(2,tobsj, rat, dect, lambda, beta);
        lambda= r20.xlam;
        beta  = r20.beta;
        const r30= gtecle(tobsj, lambda, beta, edela, edeld);
        edela= r30.ra;
        edeld= r30.dec;
        lambda = lambda + edela;
        beta = beta + edeld;
        const ret= jclpos(lambda, beta);
        lambda= ret.ra;
        beta  = ret.dec;
        const r26= ecleqx(2,tobsj, lambda, beta, rat, dect);
        rat = r26.rad;
        dect= r26.decd;
    }
    return {ra:rat,dec:dect};
}

/** to put ra into 0 to 360 and dec into -90 to +90 ranges.*/
export function jclpos(rat,dect) {

    if(rat > 360.0)    rat = rat - 360.0;
    else if(rat < 0.0) rat = rat + 360.0;

    if(Math.abs(dect) > 90.0) {
        rat = rat + 180.0;
        if(rat >= 360.0) rat = rat - 360.0;
        if(dect > 0.0) dect =   180.0 - dect;
        else           dect = -(180.0 + dect);
    }
    return {ra:rat,dec:dect};
}

