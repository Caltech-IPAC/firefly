/*!
 * unique-id <https://github.com/jonschlinkert/unique-id>
 *
 * Copyright (c) 2014 Jon Schlinkert, contributors.
 * Licensed under the MIT License
 */

'use strict';

var assert = require('assert');
var uniqueId = require('./');

describe('uniqueId', function () {
  it('should generate a unique id', function () {
    for(var i = 1; i < 25; i++) {
      assert.equal(uniqueId(), i);
    }
  });

  it('should generate a unique id with a prefix:', function () {
    uniqueId.reset();

    for(var i = 1; i < 25; i++) {
      assert.equal(uniqueId({prefix: 'apple_'}), 'apple_' + i);
    }
  });

  it('should generate a unique id with a suffix:', function () {
    uniqueId.reset();

    for(var i = 1; i < 25; i++) {
      assert.equal(uniqueId({suffix: '_orange'}), i + '_orange');
    }
  });

  it('should generate a unique id using the given multiplier:', function () {
    uniqueId.reset();

    for(var i = 1; i < 25; i++) {
      assert.equal(uniqueId({multiplier: 3}), i * 3);
    }
  });
});