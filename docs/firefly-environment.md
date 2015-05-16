## Changing the Firefly runtime environment



### Changing Tomcat environment

#### Tomcat setenv.sh

If you need to make changes to Tomcat's startup variables, do not set or change the variables in `catalina.sh`.
Instead put them into a script setenv.sh in `CATALINA_BASE/bin` to keep your customizations separate.
Create it if it's not there already.


#### Setup Tomcat memory
To change Tomcat maximum memory, add this line to the `setenv.sh`
In this example, we set it up 4 GB.  We also recommend setting the PermSize to 256m.

    CATALINA_OPTS = "-Xmx4g -XX:PermSize=256m"


#### Setup Tomcat Manager Application Access
Tomcat comes with a Manager webapp.  However, you need to setup user with access to use it.
Add this line to `$CATALINA_BASE/conf/tomcat-users.xml`.  Of course 'admin' and 'secret' are only an example.
You need to restart Tomcat for it to take effect.

    <user username="admin" password="secret" roles="manager-gui,manager-status"" />


### Firefly Configuration
Firefly relies heavily on its configuration to provide customizable behaviors for different environments.
You can change the default configuration both during build time and during runtime.

#### Runtime
Using Firefly Viewer with a context name of `fftools` as an example.
When deployed, the configuration files reside in CATALINA_BASE/webapps/fftools/WEB-INF/config/.
Although, you can make direct changes to those files, it is not recommended.
It should only be use to temporarily test a new configuration.
If you wish to override a property in one of the `.prop` files, you can drop your own `.prop` file into
the `${server_config_dir}` directory.  To setup the `${server_config_dir}` variable, add this line to the `setenv.sh` file.
In this example, if you set it to `/my_app_config`, it will look for `.prop` files in `/my_app_config/fftools/`.

    JAVA_OPTS="-Dserver_config_dir=/my_app_config"


#### Build Time

You can change the configuration of a build in a several ways.  First, by specifying which environment this build is for.
To do that, you add the `-Penv=<type>` to your gradle command.  At the moment, there are 4 types of env: local, dev, test, and ops with local being the default.
Here is an example of building Firefly Viewer for the ops envronment.

    $ gradle -Penv=ops :fftools:war

`firefly/config/app.config` contains a list of properties used during the build.  The `environments{}` block define properties specific for a particular environment.
In any cases, if a property is not found for a given env, it will use the one defined outside of the `environments{}` block.
Depending on your needs, you may edit `app.config` before you build.  This is effective, but not permanent.
`app.config` is under versioned control and may wipe out your changes if you pull in new changes.

Alternatively, you can create a `~/.gradle/build.config` with the same format as `app.config`