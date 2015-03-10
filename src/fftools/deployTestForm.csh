#webpack -d
cp html/rtest.html /hydra/server/tomcat/webapps/fftools
cp ../../build/gwt/fftools/out.{js,js.map} /hydra/server/tomcat/webapps/fftools
mkdir -p /hydra/server/tomcat/webapps/fftools/tmp-stuff
cp -r html/tmp-stuff /hydra/server/tomcat/webapps/fftools/tmp-stuff

