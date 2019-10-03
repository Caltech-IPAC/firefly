
import {sprintf} from '../sprintf.js';


describe('sprintf: ', () => {

    test('standard:', () => {
        expect(sprintf('%%'))       .toBe('%');
        expect(sprintf('%b', 2))    .toBe('10');
        expect(sprintf('%c', 65))	 .toBe('A');
        expect(sprintf('%d', 2))    .toBe('2');
        expect(sprintf('%i', 2))    .toBe('2');
        expect(sprintf('%d', '2'))  .toBe('2');
        expect(sprintf('%i', '2'))  .toBe('2');

        expect(sprintf('%j', {foo: 'bar'}))	.toBe('{"foo":"bar"}');
        expect(sprintf('%j', ['foo', 'bar']))	.toBe('["foo","bar"]');

        expect(sprintf('%e', 2))    .toBe('2.000000e+0');
        expect(sprintf('%.6e', 2))    .toBe('2.000000e+0');
        expect(sprintf('%u', 2))    .toBe('2');
        expect(sprintf('%u', -2))   .toBe('4294967294');
        expect(sprintf('%f', 2.2))  .toBe('2.200000');

        expect(sprintf('%o', 8))    .toBe('10');
        expect(sprintf('%o', -8))   .toBe('37777777770');

        expect(sprintf('%s', '%s')) .toBe('%s');

        expect(sprintf('%x', 255))  .toBe('ff');
        expect(sprintf('%x', -255)) .toBe('ffffff01');
        expect(sprintf('%X', 255))  .toBe('FF');
        expect(sprintf('%X', -255)) .toBe('FFFFFF01');

        expect(sprintf('%2$s %3$s a %1$s', 'cracker', 'Polly', 'wants')).toBe('Polly wants a cracker');
        expect(sprintf('Hello %(who)s!', {who: 'world'})).toBe('Hello world!');

        expect(sprintf('%t', true))     .toBe('true');
        expect(sprintf('%.1t', true))   .toBe('t');
        expect(sprintf('%t', 'true'))   .toBe('true');
        expect(sprintf('%t', 1))        .toBe('true');
        expect(sprintf('%t', false))    .toBe('false');
        expect(sprintf('%.1t', false))  .toBe('f');
        expect(sprintf('%t', ''))       .toBe('false');
        expect(sprintf('%t', 0))		 .toBe('false');

        expect(sprintf('%T', undefined))    .toBe('undefined');
        expect(sprintf('%T', null))		 .toBe('null');
        expect(sprintf('%T', true))		 .toBe('boolean');
        expect(sprintf('%T', 42))           .toBe('number');
        expect(sprintf('%T', 'This is a string')).toBe('string');
        expect(sprintf('%T', Math.log))     .toBe('function');
        expect(sprintf('%T', [1, 2, 3]))    .toBe('array');
        expect(sprintf('%T', {foo: 'bar'})) .toBe('object');
        expect(sprintf('%T', /<('[^']*'|'[^']*'|[^''>])*>/)).toBe('regexp');

        expect(sprintf('%v', true))         .toBe('true');
        expect(sprintf('%v', 42))           .toBe('42');
        expect(sprintf('%v', [1, 2, 3]))    .toBe('1,2,3');
        expect(sprintf('%v', 'This is a string'))   .toBe('This is a string');
        expect(sprintf('%v', {foo: 'bar'}))         .toBe('[object Object]');
        expect(sprintf('%v', /<("[^"]*"|'[^']*'|[^'">])*>/)).toBe('/<("[^"]*"|\'[^\']*\'|[^\'">])*>/');
    });

    test('g format:', () => {
        //If value is greater than or equal to 10^-4 but less than 10^precision then it is represented in decimal format, otherwise scientific
        //precision is default to 6 if not given.
        expect(sprintf('%g', 123456))   .toBe('123456');      // less than 10^6 (default)
        expect(sprintf('%g', 1.234567)) .toBe('1.23457');
        expect(sprintf('%.4g', 123456)) .toBe('1.235e+5');    // less than 10^4.. p=4

        expect(sprintf('%g', 0.0000123456))     .toBe('1.23456e-5');  // less than 10^-4, p=default
        expect(sprintf('%.4g', 0.0000123456))   .toBe('1.235e-5');    // less than 10^-4, p=4
    });

    test('width with multiple args:', () => {
        expect(sprintf('|%5s|%5s|%5s|', 'a', 'ab', 'abc'))   .toBe('|    a|   ab|  abc|');
    });


    test('embedded:', () => {
        expect(sprintf('Total cost: $%7.2f', 12.1)) .toBe('Total cost: $  12.10');
        expect(sprintf('Total cost: $%7.3f', 12))   .toBe('Total cost: $ 12.000');
    });

});

