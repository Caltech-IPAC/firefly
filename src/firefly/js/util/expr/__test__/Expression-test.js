/*eslint-env node, mocha */

// to run: from firefly/src/firefly
// node ../../node_modules/mocha/bin/mocha js/util/expr/__test__/*test.js --compilers js:babel/register --requires ignore-styles


import {expect} from 'chai';
//import {assert} from 'chai';

import {Expression} from '../Expression.js';

describe('A test suite for expr/Expression.js', function () {


    /* run once before testing */
    before(() => {


        }
    );
    /* run once testing is done */
    after(() => {
        }
    );

    /* run before every test-case*/
    beforeEach(() => {
        }
    );
    /* run after every test-case*/
    afterEach(function () {

        }
    );

    /* Valid operators: * / + - ^ < > = , ( )
     * Supported functions:
     *       abs, acos, asin, atan,
     *       ceil, cos, exp, floor,
     *       log, ln, round, sin, sqrt,
     *       tan, atan2, max, min
     * Supported conditional if(condition, then, alternative)
     * Supported constants: pi
     */
    it('valid expression (functions)', function () {
        var x,y;

        for (x=-1; x<=1; x+=0.2) {
            const utests = [
                {e: 'abs(x)', r: Math.abs(x)},
                {e: 'acos(x)', r: Math.acos(x)},
                {e: 'asin(x)', r: Math.asin(x)},
                {e: 'atan(x)', r: Math.atan(x)},
                {e: 'ceil(x)', r: Math.ceil(x)},
                {e: 'cos(x)', r: Math.cos(x)},
                {e: 'exp(x)', r: Math.exp(x)},
                {e: 'floor(x)', r: Math.floor(x)},
                {e: 'log(x)', r: Math.log10(x)},
                {e: 'ln(x)', r: Math.log(x)},
                {e: 'round(x)', r: Math.round(x)},
                {e: 'sin(x)', r: Math.sin(x)},
                {e: 'sqrt(x)', r: Math.sqrt(x)},
                {e: 'tan(x)', r: Math.tan(x)}
            ];
            utests.forEach((t)=> {
                //console.log(`${t.e} with ${x}`);
                const e = new Expression(t.e, ['x']);
                expect(e.isValid()).to.be.true;
                expect(!e.getError()).to.be.true;
                e.setVariableValue('x', x);
                const r = e.getValue();
                !(Number.isNaN(r)&&Number.isNaN(t.r)) && expect(r).to.be.equal(t.r);
            });
        }

        for (x=-2; x<=2; x+=0.5) {
            for (y=-2; y<=2; y+=0.5) {
                const btests = [
                    {e: 'min(x,y)', r: Math.min(x,y)},
                    {e: 'max(x,y)', r: Math.max(x,y)},
                    {e: 'atan2(x,y)', r: Math.atan2(x,y)},
                    {e: 'if(x>y,x,y)', r: (x>y?x:y)}
                ];
                btests.forEach((t)=> {
                    //console.log(`${t.e} with ${x} and ${y}`);
                    const e = new Expression(t.e, ['x', 'y']);
                    expect(e.isValid()).to.be.true;
                    expect(!e.getError()).to.be.true;
                    e.setVariableValue('x', x);
                    e.setVariableValue('y', y);
                    const r = e.getValue();
                    !(Number.isNaN(r)&&Number.isNaN(t.r)) && expect(r).to.be.equal(t.r);
                });
            }
        }
    });


    it('valid expression (operators)', function () {
        const x= 0.5, y=-2.2;
        const expressionGood = new Expression('2*(sin(pi-acos(x))+abs(y)^3)/x', ['x','y']);
        expect(expressionGood.isValid()).to.be.true;
        expect(!expressionGood.getError()).to.be.true;
        expressionGood.setVariableValue('x', x);
        expressionGood.setVariableValue('y', y);
        expect(expressionGood.getValue()).to.be.equal(2*(Math.sin(Math.PI-Math.acos(x))+Math.pow(Math.abs(y),3))/x);
    });



     it('invalid expression', function () {
         const expressionBad = new Expression('2*sin(x)+y/z', ['x','y']);
         expect(expressionBad.isValid()).to.be.false;
         const se = expressionBad.getError();
         expect(se).property('error');
         expect(se).property('details');
         expect(se).property('where');
         expect(se).property('why');
         console.log(se);
     });
});
