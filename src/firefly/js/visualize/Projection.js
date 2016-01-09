/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*global window*/

import {parseWorldPt, makeProjectionPt} from './Point.js';



export const makeProjection= function(gwtProjStr) {
   if (!window.ffgwt) return null;
   var gwtProj= window.ffgwt.Visualize.ProjectionSerializer.deserializeProjection(gwtProjStr);
   return {
      isWrappingProjection() {
         return gwtProj.isWrappingProjection();
      },
      getPixelWidthDegree() {
         return gwtProj.getPixelWidthDegree();
      },
      getPixelHeightDegree() {
         return gwtProj.getPixelHeightDegree();
      },
      getPixelScaleArcSec() {
         return gwtProj.getPixelScaleArcSec();
      },
      getImageCoords(x,y) {
         var pt= gwtProj.getImageCoordsSilent(x,y);
         return pt ? makeProjectionPt(pt.getX(), pt.getY()) : null;
      },
      getWorldCoords(x,y) {
         var wpt= gwtProj.getWorldCoordsSilent(x,y);
         return wpt ? parseWorldPt(wpt.serialize()) : null;
      },
      isSpecified() {
         return gwtProj.isSpecified();
      }
   };
};

