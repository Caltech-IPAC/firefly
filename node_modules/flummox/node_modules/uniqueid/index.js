'use strict';


var count = 0;

/**
 * Generate a unique ID.
 *
 * Optionally pass a prefix to prepend, a suffix to append, or a
 * multiplier to use on the ID.
 *
 * ```js
 * uniqueId(); //=> '25'
 *
 * uniqueId({prefix: 'apple_'});
 * //=> 'apple_10'
 *
 * uniqueId({suffix: '_orange'});
 * //=> '10_orange'
 *
 * uniqueId({multiplier: 5});
 * //=> 5, 10, 15, 20...
 * ```
 *
 * To reset the `id` to zero, do `id.reset()`.
 *
 * @param  {Object} `options` Optionally pass a `prefix`, `suffix` and/or `multiplier.
 * @return {String} The unique id.
 * @api public
 */

var id = module.exports = function (options) {
  options = options || {};

  var prefix = options.prefix;
  var suffix = options.suffix;

  var id = ++count * (options.multiplier || 1);

  if (prefix == null) {
    prefix = '';
  }

  if (suffix == null) {
    suffix = '';
  }

  return String(prefix) + id + String(suffix);
};


id.reset = function() {
  return count = 0;
};