#!/bin/bash
sed -n '1h;1!H;${;g;s/function isBodyLoaded.*readyState)\;/function isBodyLoaded() {return true\;/g;p;}' $1.js > tmp-out.js
#sed 's#if(typeof p.readyState==W){return typeof p.body!=W&&p.body!=null}return /loaded|complete/.test(p.readyState)#return true#' tmp-out.js > tmp-out2.js
sed 's#if(typeof ..readyState==W){return typeof ..body!=W&&..body!=null}return /loaded|complete/.test(..readyState)#return true#' tmp-out.js > tmp-out2.js
mv tmp-out2.js $1.js
rm tmp-out.js
