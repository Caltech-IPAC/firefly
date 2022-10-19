import {FileAnalysisType} from 'firefly/data/FileAnalysis';
import {showInfoPopup} from 'firefly/ui/PopupUtil';
import {getAppHiPSForMoc, isMOCFitsFromUploadAnalsysis} from 'firefly/visualize/HiPSMocUtil';
import {MetaConst} from 'firefly/data/MetaConst';
import {isLsstFootprintTable} from 'firefly/visualize/task/LSSTFootprintTask';
import {TableCtx} from '../visualize/ui/FileUploadViewPanel.jsx';
import { useContext } from 'react';


export function ResultSuccess(request) {
    //const tabCtx = useContext(TableCtx);
    /* if (tabCtx) return tablesOnlyResultSuccess(request);
     const fileCacheKey = getFileCacheKey();

     const tableIndices = getSelectedRows(FileAnalysisType.Table);
     const imageIndices = getSelectedRows(FileAnalysisType.Image);

     if (!isFileSupported()) {
         showInfoPopup(getFirstPartType() ? `File type of ${getFirstPartType()} is not supported.`: 'Could not recognize the file type');
         return false;
     }

     if (!isRegion() && tableIndices.length + imageIndices.length === 0) {
         if (getSelectedRows('HeaderOnly')?.length) {
             showInfoPopup('FITS HDU type of HeaderOnly is not supported. A header-only HDU contains no additional data.', 'Validation Error');
         }
         else {
             showInfoPopup('No extension is selected', 'Validation Error');
         }
         return false;
     }

     const isMocFits =  isMOCFitsFromUploadAnalsysis(currentReport);
     if (isRegion()) {
         sendRegionRequest(fileCacheKey);
     }
     else if (isMocFits.valid) {
         const mocMeta= {[MetaConst.PREFERRED_HIPS]: getAppHiPSForMoc()};
         if (request.mocOp==='table') mocMeta[MetaConst.IGNORE_MOC]='true';
         sendTableRequest(tableIndices, fileCacheKey, false, mocMeta, request.mocOp==='table');
     } else if ( isLsstFootprintTable(currentDetailsModel) ) {
         sendLSSTFootprintRequest(fileCacheKey, request.fileName, tableIndices[0]);
     } else {
         sendTableRequest(tableIndices, fileCacheKey, Boolean(request.tablesAsSpectrum==='spectrum'));
         sendImageRequest(imageIndices, request, fileCacheKey);
     }*/
}
