import {flux} from '../../core/ReduxFlux.js';
import {makePlotView} from '../reducer/PlotView.js';
import {WebPlotRequest} from '../WebPlotRequest.js';
import ImagePlotCntlr, {visRoot} from '../ImagePlotCntlr';



const makeMockPv = (plotId, plotGroupId, plotCnt, primeIdx=0) => {
    const req= WebPlotRequest.makeURLPlotRequest('http://a/b/c.fits');
    req.setPlotGroupId(plotGroupId);
    req.setPlotId(plotId);
    const pv= makePlotView(plotId, req);
    pv.primeIdx= primeIdx;
    pv.plots= [];
    for(let i= 0; i<plotCnt; i++) {
        pv.plots.push({
            plotId,
            plotType:'image',
            plotImageId:plotId+'-image'+i,
            title: 'Dummy Plot testId1 - image'+i
        });
    }
    return pv;
};


const ACTIVE_ID='testId-xxxx';

const pvAry= [
    makeMockPv('testId1', 'aTestGroup', 3, 1),
    makeMockPv(ACTIVE_ID, 'aTestGroup', 1),
    makeMockPv('testId-yyyy', 'aTestGroup', 3),
    makeMockPv('testId-zzzz', 'anotherGroup', 4),
];
// const visRoot= {plotViewAry: pvAry, activePlotId: ACTIVE_ID};



describe('Test ImagePlotCntlr', () => {

    test('visroot auto play on', () => {
        const before= visRoot().singleAutoPlay;
        flux.getRedux().dispatch( {
            type:ImagePlotCntlr.EXPANDED_AUTO_PLAY,
            payload: {autoPlayOn:true}
        });
        expect(before).toEqual(false);
        expect(visRoot().singleAutoPlay).toEqual(true);
    });

});


