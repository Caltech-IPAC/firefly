/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {gtjul2, gtjulp, unjulp, unjul2} from './Gtjul2.js';
import {nwcprp} from './Nwcprc.js';
import {jbposa,jbelpt} from './Jbposa.js';


/*
* \ned\gtjpa.f  12-13mar92
*   03aug92; updated comments only.
*   23jul92 to call nwcprc/nwcprp instead of jprecj. Also, precesses
*     to 1950 (if necessary) before call to gtjulp or from 1950 (if
*     necessary) after call to unjulp (i.e. for propermotion types only).
*
*        contains gtjpa, unjpa, gtjpap, unjpap
*/

/**
 *
 *       ALL FLOATING POINT ARGS ARE DOUBLE PRECISION
 *
 * @param eqxin is the equinox(besselian) of the input position.
 * @param ra,dec is input position in decimal degrees at equinox eqxin.
 * @param dec
 * @param pab  = input position angle (0 to 360.0d0 deg.) at eqxin.
 * @param tobsin is year of observation (e.g. 1983.5 for iras sources)
 *         if 0.0d0, then tobsin is treated as though equal to eqxin.
 * @param ieflg  of -1 indicate to not remove E-terms from position in computing
 *             position angle (any other value allows E-term removal)
 * @return raout,decout is output position in decimal degrees at J2000.0
 * @return paj is output position angle (0 to 360.0d0 deg.) for J2000.0
 *         if ierr = 0 ( garbage otherwise).
 * @return ierr = 0 for normal return. -1 if paj could not be computed.
 *
 * NOTE:
 * This routine assumes input pos angle & input position are in the equatorial
 *    coordinate system.
 *
 * calls jbposa,jbelpt,gtjul2
 *
 */
export function gtjpa (eqxin, ra, dec, pab,   tobsin, ieflg) {

    var raout, decout, paj, ierr;
    var iflg=0,ipf=0;
    var plonp1=0.0,platp1=0.0;
    var dumd=0.0;
    var paou=0.0;
    var dist = 0.1;

   ierr = 0;
   var eqx = eqxin;
   if(eqxin == 0.0) eqx = 1950.0;
   var tobs = tobsin;
   if(tobsin == 0.0) tobs = eqxin;

   var plonp0 = ra;
   var platp0 = dec;
   var pain = pab;
   
   const r34= gtjul2(eqx, ra, dec, tobs, ieflg);
   raout = r34.ra;
   decout= r34.dec;

/*
* find end point at equinox eqxin
* note: plonp0 and platp0 are used for pos angle proc. and may
*        be slightly adjusted here.  To get proper central position 
*        one must precess ra and dec.
*/

    var plon0 = raout;
    var plat0 = decout;

    const r1= jbelpt(plonp0,platp0,dist,pain, plonp1,platp1,iflg);
    plonp1= r1.xlonp;
    platp1= r1.xlatp;
    iflg  = r1.iflg;
    if(iflg===3) {
        //          recompute plonp0,platp0 for pos angle processing if near a pole:
        plonp0 = 0.0;
        if(platp0 < 0.0) platp0 = -90.0;
        else             platp0 = 90.0;
        const ret= gtjul2(eqx,plonp0,platp0,tobs, ieflg);
        plon0= ret.ra;
        plat0= ret.dec;
    }

    if(iflg===4) {
        plonp1 = 0.0;
        if(platp1 < 0.0) platp1 = -90.0;
        else             platp1 = 90.0;
    }

            // precess pt at apr2ex of ellipse:

    const r3= gtjul2(eqx,plonp1,platp1,tobs, ieflg);
    var plon1= r3.ra;
    var plat1= r3.dec;
           //  get new pos angle
   const r5= jbposa(plon1,plat1,plon0,plat0, dumd,paou,ipf);
   paou= r5.pa;
   ipf=  r5.ipf;
   if(ipf===0) {
      ierr = -1;
      paou = 0;
   }

   paj = paou;

   return {ierr,raout,decout,paj};
}


/**
 *
 *       ALL FLOATING POINT ARGS ARE DOUBLE PRECISION
 *
 * @param ra,dec is input position in decimal degrees at J2000.0
 * @param dec
 * @param paj  = input position angle (0 to 360.0d0 deg.) at J2000.0
 * @param tobsin is year of observation (e.g. 1983.5 for iras sources)
 *         if 0.0d0, then tobsin is treated as though equal to eqxin.
 * @param ieflg  of -1 indicates to not add back E-terms
 *           +1  (any val other than -1) indicates E-terms are to be replaced.
 * @param eqxbou is the equinox(besselian) of the output position & position angle.
 * @return raout,decout is output position in decimal degrees at eqxbou.
 * @return pab is output position angle (0 to 360.0d0 deg.) for eqxbou;
 *         if ierr = 0 ( garbage otherwise).
 * @return ierr = 0 for normal return. -1 if paou could not be computed.
 *
 * NOTE:
 * This routine assumes input pos angle & input position are in the equatorial
 *    coordinate system.
 *
 * calls jbposa,jbelpt,unjul2
 *
 */
export function unjpa ( ra, dec, paj, tobsin, ieflg, eqxbou) {

   var raout, decout, pab, ierr;
   var iflg=0;
   var plonp1=0.0,platp1=0.0;
   var dist = 0.1;

    ierr = 0;
    var eqx = eqxbou;
    if(eqxbou == 0.0) eqx = 1950.0;
    var tobs = tobsin;
    if(tobsin == 0.0) tobs = eqx;

    var plonp0 = ra;
    var platp0 = dec;
    var pain = paj;

    const r7 = unjul2(ra, dec,tobs,ieflg,eqx);
    raout=  r7.ra;
    decout= r7.dec;
    var plon0 = raout;
    var plat0 = decout;


             // find end point at equinox J2000.0

    const r1= jbelpt(plonp0,platp0,dist,pain, plonp1,platp1,iflg);
    plonp1= r1.xlonp;
    platp1= r1.xlatp;
    iflg  = r1.iflg;
    if(iflg===3) {
        //    recompute plonp0,platp0 for pos angle processing if near a pole:
        plonp0 = 0.0;
        if(platp0 < 0.0) platp0 = -90.0;
        else             platp0 = 90.0;

        const r55 = unjul2(plonp0,platp0,tobs,ieflg, eqx);
        plon0= r55.ra;
        plat0= r55.dec;
    }
    if(iflg===4) {
        plonp1 = 0.0;
        if(platp1 < 0.0) platp1 = -90.0;
        else             platp1 = 90.0;
    }

       
    const r8 = unjul2(plonp1,platp1,tobs,ieflg, eqx);
    var plon1= r8.ra;
    var plat1= r8.dec;

                  // get new pos angle

    const r5= jbposa(plon1,plat1,plon0,plat0);
    var paou= r5.pa;
    var ipf = r5.ipf;
    if(ipf===0) {
         ierr = -1;
         paou = 0;
    }
    pab = paou;

    return {ierr,raout,decout,pab};
}


/**
 *
 *       ALL FLOATING POINT ARGS ARE DOUBLE PRECISION
 *
 * @param eqxin is the equinox(besselian) of the input position & position angle.
 * @param ra,dec is input position in decimal degrees at equinox eqxin.
 * @param dec
 * @param pab  = input position angle (0 to 360.0d0 deg.) at eqxin.
 * @param pma is proper motion in ra in seconds per tropical century.
 * @param pmd is proper motion in dec in seconds of arc per tropical century.
 * @param pin is parallax in arc seconds.
 * @param vin is radial velocity in km/sec
 * @param ieflg  of -1 indicate to not remove E-terms from position in computing
 *             position angle (any other value allows E-term removal)
 * @return raout,decout is output position in decimal degrees at J2000.0
 * @return paj is output position angle (0 to 360.0d0 deg.) for J2000.0
 *         if ierr = 0 ( garbage otherwise).
 * @return pmaout is proper motion in seconds of time per Julian century.
 * @return pmdout is proper motion in seconds of arc per Julian century.
 * @return ierr = 0 for normal return. -1 if paj could not be computed.
 *
 * NOTE:
 * This routine assumes input pos angle & input position are in the equatorial
 *    coordinate system.
 *
 * calls jbposa,jbelpt,gtjulp,gtjul2
 *
 */
export function gtjpap( eqxin, ra, dec, pab, pma, pmd, pin, vin, ieflg) {
    var paj;
    var ipf=0;
    var dumd=0.0;
    var ra50=0.0,dec50=0.0,pma50=0.0,pmd50=0.0;
    var dist=0.1,paou=0.0;

    var ierr = 0;
    var eqx = eqxin;
    if(eqxin == 0.0) eqx = 1950.0;
    var tobs = eqx;

    var plonp0 = ra;
    var platp0 = dec;
    var pain = pab;

             // get central position (w/ proper motions) at J2000.0:
    if(eqx===1950.0) {
        ra50  = ra;
        dec50 = dec;
        pma50 = pma;
        pmd50 = pmd;
    }
    else {
        const r20= nwcprp(eqx,ra,dec,pma,pmd,pin, vin,1950.0, ra50,dec50,pma50,pmd50);
        ra50 = r20.raou;
        dec50= r20.decou;
        pma50= r20.pmaou;
        pmd50= r20.pmdou;
    }
    const r54= gtjulp(1950.0,ra50,dec50,pma50,pmd50, pin,vin,ieflg);
    var raout=  r54.ra;
    var decout= r54.dec;
    var pmaout= r54.pmra;
    var pmdout= r54.pmdec;
/*
* find end point at equinox eqxin - ignore proper motion for pos.ang. calc.
*  note: plonp0 and platp0 are used for pos angle proc. and may
*        be slightly adjusted here. 
*
*/

    const r1= jbelpt(plonp0,platp0,dist,pain);
    var plonp1= r1.xlonp;
    var platp1= r1.xlatp;
    var iflg  = r1.iflg;
    if(iflg===3) {
                    //       recompute plonp0,platp0 for pos angle processing if near a pole: */
        plonp0 = 0.0;
        if(platp0 < 0.0) platp0 = -90.0;
        else             platp0 = 90.0;
    }

    const r3= gtjul2(eqx,plonp0,platp0,tobs, ieflg);
    var plon0= r3.ra;
    var plat0= r3.dec;

    if(iflg===4) {
        plonp1 = 0.0;
        if(platp1 < 0.0) platp1 = -90.0;
        else platp1 = 90.0;
    }

          // precess pt at apex of ellipse:
    const r4= gtjul2(eqx,plonp1,platp1,tobs, ieflg);
    const plon1= r4.ra;
    const plat1= r4.dec;
         //get new pos angle
    const r5= jbposa(plon1,plat1,plon0,plat0, dumd,paou,ipf);
    paou= r5.pa;
    ipf = r5.ipf;
    if(ipf == 0) {
        ierr = -1;
        paou = 0;
    }
    paj = paou;

    return {ierr,raout,decout,paj,pmaout,pmdout};
}


/**
 *       ALL FLOATING POINT ARGS ARE DOUBLE PRECISION
 *
 * @param ra,dec is input position in decimal degrees at J2000.0
 * @param dec
 * @param paj  = input position angle (0 to 360.0d0 deg.) at J2000.0
 * @param pma is proper motion in ra in seconds per Julian century.
 * @param pmd is proper motion in dec in seconds of arc per Julian century.
 * @param pin is parallax in arc seconds.
 * @param vin is radial velocity in km/sec
 * @param ieflg  of -1 indicates to not add back E-terms
 *           +1  (any val other than -1) indicates E-terms are to be replaced.
 * @param eqxbou is the equinox(besselian) of the output position & position angle.
 * @return raout,decout is output position in decimal degrees at eqxbou.
 * @return pab is output position angle (0 to 360.0d0 deg.) for eqxbou;
 *         if ierr = 0 ( garbage otherwise).
 * @return pmaout is proper motion in seconds of time per tropical century.
 * @return pmdout is proper motion in seconds of arc per tropical century.
 * @return ierr = 0 for normal return. -1 if paou could not be computed.
 *
 * NOTE:
 * This routine assumes input pos angle & input position are in the equatorial
 *    coordinate system.
 *
 * calls jbposa,jbelpt,unjulp,unjul2
 *
 */
export function unjpap( ra, dec, paj, pma, pmd, pin, vin, ieflg, eqxbou) {

    var raout, decout, pab, pmaout, pmdout;
    var dist=0.1;

    var ierr = 0;
    var eqx = eqxbou;
    if(eqxbou===0.0) eqx = 1950.0;
    const tobs = eqx;

    var plonp0 = ra;
    var platp0 = dec;
    var pain = paj;
    const r31= unjulp( ra, dec,pma,pmd,pin,vin,ieflg, eqx);
    var ra50 = r31.ra;
    var dec50= r31.dec;
    var pma50= r31.pmra;
    var pmd50= r31.pmdec;

    if(eqx===1950.0) {
        raout  = ra50;
        decout = dec50;
        pmaout = pma50;
        pmdout = pmd50;
    }
    else {
        const r21=nwcprp(1950.0,ra50,dec50, pma50,pmd50,pin,vin,eqx);
        raout = r21.raou;
        decout= r21.decou;
        pmaout= r21.pmaou;
        pmdout= r21.pmdou;
    }
               //find end point at equinox J2000.0
    const r32= jbelpt(plonp0,platp0,dist,pain);
    var plonp1= r32.xlonp;
    var platp1= r32.xlatp;
    const iflg  = r32.iflg;
    if(iflg===3) {
                 // recompute plonp0,platp0 for pos angle processing if near a pole: */
        plonp0 = 0.0;
        if(platp0 < 0.0) platp0 = -90.0;
        else             platp0 = 90.0;
    }

    const r9 = unjul2(plonp0,platp0,tobs,ieflg, eqx);
    const plon0= r9.ra;
    const plat0= r9.dec;

    if(iflg===4) {
        plonp1 = 0.0;
        if(platp1 < 0.0) platp1 = -90.0;
        else             platp1 = 90.0;
    }

    const r6 = unjul2(plonp1,platp1,tobs,ieflg,eqx);
    const plon1= r6.ra;
    const plat1= r6.dec;

/* get new pos angle
*
*/
    const r5= jbposa(plon1,plat1,plon0,plat0);
    var paou= r5.pa;
    var ipf = r5.ipf;
    if(ipf===0) {
        ierr = -1;
        paou = 0;
    }
    pab = paou;

    return {ierr,raout,decout,pab,pmaout,pmdout};
}
