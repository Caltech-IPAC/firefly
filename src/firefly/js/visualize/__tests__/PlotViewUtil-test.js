import {primePlot, getPlotViewById, isActivePlotView, getActivePlotView} from '../PlotViewUtil.js';
import {makePlotView} from '../reducer/PlotView.js';
import {WebPlotRequest} from '../WebPlotRequest.js'



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
const visRoot= {plotViewAry: pvAry, activePlotId: ACTIVE_ID};



describe('Test PlotViewUtil.primePlot()', () => {

    test('primePlot() using parameters (plotView)', () => {
        const plot= primePlot(pvAry[0]);
        expect(plot).toBeDefined();
        expect(plot.plotId).toEqual('testId1');
        expect(plot.plotImageId).toEqual('testId1-image1');
    });

    test('primePlot() using parameters (plotView[], plotId)', () => {
        const plot= primePlot(pvAry, 'testId-yyyy');
        expect(plot).toBeDefined();
        expect(plot.plotId).toEqual('testId-yyyy');
        expect(plot.plotImageId).toEqual('testId-yyyy-image0');
    });

    test('primePlot() using parameters (visRoot)', () => {
        const visRoot= {plotViewAry: pvAry, activePlotId: 'testId-xxxx'};
        const plot= primePlot(visRoot);
        expect(plot).toBeDefined();
        expect(plot.plotId).toEqual('testId-xxxx');
        expect(plot.plotImageId).toEqual('testId-xxxx-image0');
    });

    test('primePlot() using parameters (visRoot, plotId)', () => {
        const plot= primePlot(visRoot, 'testId1');
        expect(plot).toBeDefined();
        expect(plot.plotId).toEqual('testId1');
        expect(plot.plotImageId).toEqual('testId1-image1');
    });

    test('primePlot() using parameters (visRoot, plotId) with a plotId that does not exist', () => {
        const plot= primePlot(visRoot, 'IAmNotDefined');
        expect(plot).toBeFalsy();
    });
});


describe('Test PlotViewUtil.getPlotViewById()', () => {

    test('getPlotViewById() using parameters (PlotView[], plotId)', () => {
        const pv= getPlotViewById(pvAry, 'testId-xxxx');
        expect(pv).toBeDefined();
        expect(pv.plotId).toEqual('testId-xxxx');
    });

    test('getPlotViewById() using parameters (visRoot, plotId)', () => {
        const pv= getPlotViewById(visRoot, 'testId-xxxx');
        expect(pv).toBeDefined();
        expect(pv.plotId).toEqual('testId-xxxx');
    });

    test('getPlotViewById() using parameters (visRoot, plotId) with a plotId that does not exist', () => {
        const pv= getPlotViewById(visRoot, 'IAmNotDefined');
        expect(pv).toBeFalsy();
    });

});

describe('Test PlotViewUtil active PlotView functions', () => {
    test('isActivePlotView(visRoot, plotId)', () => {
        expect(isActivePlotView(visRoot, ACTIVE_ID)).toBeTruthy();
        expect(isActivePlotView(visRoot,'testId-zzzz')).toBeFalsy();
    });

    test('getActivePlotView(visRoot) ', () => {
        const pv= getActivePlotView(visRoot);
        expect(pv).toBeDefined();
        expect(pv.plotId).toEqual(ACTIVE_ID);
    });
});
