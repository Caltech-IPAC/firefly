# EupsPkg config file. Sourced by 'eupspkg'

_ensure_exists()
{
	hash "$1" 2>/dev/null || die "need '$1' to install this product. please install it and try again."
}

prep()
{
	# check for system prerequisites
	_ensure_exists javac
	_ensure_exists unzip

	# make sure that JAVA_HOME is set
	if [[ -z "$JAVA_HOME" ]]; then
		# Try to autodetect it on OS X
		if [[ -x /usr/libexec/java_home ]]; then
			export JAVA_HOME=$(/usr/libexec/java_home)
		else
			die "Please set the JAVA_HOME environmental variable to the location of your JDK."
		fi
	fi

	default_prep
}

build()
{
	gradle :fftools:war
}

install()
{
	clean_old_install

	(
		#
		# FIXME: This should be a makefile (or gradle?) target
		# e.g., 'make install' should deploy this
		#
		TOMCAT="$PREFIX/tomcat"
		mkdir -p "$TOMCAT"

		# Copy and unpack the WAR file
		mkdir "$TOMCAT/webapps"
		cp ./build/libs/fftools.war "$TOMCAT/webapps"
		( cd "$TOMCAT/webapps" && mkdir fftools && cd fftools && unzip -q ../fftools.war )

		# Copy the config from CATALINA_HOME, but override
		# server.xml with our own that takes port.http and port.shutdown
		# JAVA_OPTS variables
		cp -r tomcat/conf "$TOMCAT"

		# Copy the binaries
		cp -r bin "$PREFIX"
	)

	install_ups
}
