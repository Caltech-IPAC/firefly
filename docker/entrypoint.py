#!/usr/bin/env python3

import sys
import os
import random
import base64
import subprocess
import shlex


def extract_war_files(webapps_dir, webapps_ref, path_prefix):
    """
    Prepare webapps on first-time startup:
    - Extract WAR files from webapps-ref to webapps.
    - Modify log4j to send logs to stdout.
    - Modify context path (pathPrefix) if given.

    :param webapps_dir: Path to the webapps directory.
    :param webapps_ref: Path to the webapps reference directory.
    :param path_prefix: Prefix for the context path.
    """
    if not os.listdir(webapps_dir):
        for war in os.listdir(webapps_ref):
            if war.endswith(".war"):
                fn = os.path.splitext(war)[0]
                prefix = path_prefix.replace("/", "#").strip("#")
                war_dir = os.path.join(webapps_dir, f"{prefix}#{fn}" if prefix else fn)
                os.makedirs(war_dir, exist_ok=True)
                subprocess.call(["unzip", "-oqd", war_dir, os.path.join(webapps_ref, war)])
                subprocess.call(["sed", "-E", "-i.bak", "s/##out--//", os.path.join(war_dir, "WEB-INF/classes/log4j2.properties")])


def add_multi_props_env_var():
    """
    Process environment variable `PROPS`, where:
    - Key-value pairs are separated by `;`
    - Use double semicolons (`;;`) to escape semicolon `;`

    Returns:
        str: JVM `-Dkey=value` options.
    """
    props_opts = ""
    props_env = os.getenv("PROPS", "")

    if props_env:
        placeholder = "__SEMICOLON__"
        props_env = props_env.replace(";;", placeholder)

        for prop in props_env.split(";"):
            prop = prop.replace(placeholder, ";")
            if "=" in prop:
                key, value = prop.split("=", 1)
                key = key.strip()
                value = shlex.quote(value.strip())
                props_opts += f" -D{key}={value}"

    return props_opts


def add_single_prop_env_vars():
    """
    Process environment variables that match `PROPS_*`, replacing:
    - `__` in variable names with `.`
    - Supports secrets and hard-to-escape characters.

    Returns:
        str: JVM `-Dkey=value` options.
    """
    props_opts = ""

    for key, value in os.environ.items():
        if key.startswith("PROPS_"):
            prop_key = key.replace("PROPS_", "").replace("__", ".").strip()
            value = shlex.quote(value.strip())
            props_opts += f" -D{prop_key}={value}"

    return props_opts


def log_env_info(path_prefix, visualize_fits_search_path):
    """Log environment variables and configuration information."""
    print(f"""
    ========== Information: You can set environment variables using -e on the docker run line =====

    Environment Variables:
            Description                      Name                          Value
            -----------                      --------                      -----
            Admin username                   ADMIN_USER                    {os.getenv('ADMIN_USER')}
            Admin password                   ADMIN_PASSWORD                {os.getenv('ADMIN_PASSWORD')}
            Additional data path             VISUALIZE_FITS_SEARCH_PATH    {visualize_fits_search_path}
            Clean internal (e.g., 720m, 5h)  CLEANUP_INTERVAL              {os.getenv('CLEANUP_INTERVAL', '')}
            Context path prefix              PATH_PREFIX                   {path_prefix}

    Advanced environment variables:
            Run tomcat with debug            DEBUG                         {os.getenv('DEBUG', '')}
            Extra firefly properties (*)     PROPS                         {os.getenv('PROPS', '')}
            Redis host                       PROPS_redis__host             {os.getenv('PROPS_redis__host', '')}
            SSO host                         PROPS_sso__req__auth__hosts   {os.getenv('PROPS_sso__req__auth__hosts', '')}
            firefly.options (JSON string)    PROPS_FIREFLY_OPTIONS         {os.getenv('PROPS_FIREFLY_OPTIONS', '')}
     (*) key=value pairs separated by semicolon; use double semicolons to escape semicolon

    Ports:
            8080 - http
            5050 - debug

    Volume Mount Points:
        /firefly/logs             : logs directory
        /firefly/workarea         : work area for temporary files
        /firefly/shared-workarea  : work area for files shared between multiple instances of the application
        /external                 : default external data directory visible to Firefly

      Less used:
        /firefly/config           : used to override application properties
        /firefly/logs/statistics  : directory for statistics logs
        /firefly/alerts           : alerts monitor will watch this directory for application alerts

    Command line options:
            --help  : show help message, examples, stop
            --debug : start in debug mode
    """)


def show_help(name):
    """Show help message and exit."""
    with open("./start-examples.txt") as f:
        print(f.read().replace("ipac/firefly", name))

    war_files = [f for f in os.listdir(webapps_ref) if f.endswith(".war")]
    if war_files and war_files[0] == "firefly.war":
        with open("./customize-firefly.txt") as f:
            print(f.read())
    sys.exit(0)


def dry_run(cmd, webapps_dir):
    """Display a dry run of the command and environment setup."""
    print("\n\n----------------------")
    print(f"Command: {' '.join(cmd)}")
    print(f"CATALINA_OPTS: {os.getenv('CATALINA_OPTS')}\n")

    print(f"Contents of '{webapps_dir}':")
    for item in os.listdir(webapps_dir):
        item_path = os.path.join(webapps_dir, item)
        if os.path.isdir(item_path):
            print(f" [DIR]  {item}")
        else:
            print(f" [FILE] {item}")
    print()
    sys.exit(0)


# ================================ Main Execution =============================
def main():
    """Main execution function for the entrypoint script."""

    os.environ["JPDA_ADDRESS"] = "*:5050"
    catalina_home = os.getenv("CATALINA_HOME", "/usr/local/tomcat")
    visualize_fits_search_path = os.getenv("VISUALIZE_FITS_SEARCH_PATH", "")
    start_mode = os.getenv("START_MODE", "run")
    name = os.getenv("BUILD_TIME_NAME", "ipac/firefly")
    admin_user = os.getenv("ADMIN_USER", "admin")
    admin_password = os.getenv("ADMIN_PASSWORD", base64.b64encode(str(random.randint(100000, 999999)).encode()).decode()[:8])
    use_admin_auth = os.getenv("USE_ADMIN_AUTH", "true").lower()
    path_prefix = os.getenv("PATH_PREFIX") or os.getenv("baseURL") or ""      # use PATH_PREFIX; baseURL is for backward compatibility

    vis_path = "/external" if not visualize_fits_search_path \
        else f"/external:{visualize_fits_search_path}"

    catalina_opts = " ".join([
        f"-XX:InitialRAMPercentage={os.getenv('INIT_RAM_PERCENT', '10')}",
        f"-XX:MaxRAMPercentage={os.getenv('MAX_RAM_PERCENT', '80')}",
        "-XX:+UnlockExperimentalVMOptions", "-XX:TrimNativeHeapInterval=30000",
        f"-DADMIN_USER={admin_user}",
        f"-DADMIN_PASSWORD={admin_password}",
        f"-Dhost.name={os.getenv('HOSTNAME', '')}",
        f"-Dserver.cores={os.getenv('JVM_CORES', '')}",
        "-Djava.net.preferIPv4Stack=true",
        "-Dwork.directory=/firefly/workarea",
        "-Dshared.work.directory=/firefly/shared-workarea",
        "-Dserver_config_dir=/firefly/config",
        "-Dstats.log.dir=/firefly/logs/statistics",
        "-Dalerts.dir=/firefly/alerts",
        f"-Dvisualize.fits.search.path={vis_path}"
    ])

    # Remove admin protection if disabled
    if use_admin_auth == "false":
        catalina_opts += " -DADMIN_PROTECTED="

    # extract and add properties defined as environment variables
    catalina_opts += add_multi_props_env_var()
    catalina_opts += add_single_prop_env_vars()

    # Set environment variables so they persist in Tomcat
    os.environ["CATALINA_PID"] = os.path.join(catalina_home, "bin", "catalina.pid")
    os.environ["CATALINA_OPTS"] = catalina_opts
    os.environ["ADMIN_USER"] = admin_user
    os.environ["ADMIN_PASSWORD"] = admin_password
    debug_mode = os.getenv("DEBUG", "false").lower() == "true" or (len(sys.argv) > 1 and sys.argv[1] == "--debug")
    cmd = [f"{catalina_home}/bin/catalina.sh"]
    if debug_mode:
        cmd.append("jpda")
    cmd.append(start_mode)

    # log environment information
    log_env_info(path_prefix, visualize_fits_search_path)

    # Setup examples
    subprocess.call("./setupFireflyExample.sh", shell=True)

    # Prepare webapps on first-time startup
    webapps_dir = os.path.join(catalina_home, "webapps")
    webapps_ref = os.path.join(catalina_home, "webapps-ref")
    extract_war_files(webapps_dir, webapps_ref, path_prefix)

    # check for no-ops flags
    if len(sys.argv) > 1:
        arg = sys.argv[1]
        if arg in ["--help", "-help", "-h"]:
            show_help(name)
        elif arg == "--dry-run":
            dry_run(cmd, webapps_dir)

    # Start background cleanup
    subprocess.Popen([f"{catalina_home}/cleanup.sh", "/firefly/workarea", "/firefly/shared-workarea"])

    # Start Tomcat; Replace the current process with Tomcat
    print("Starting Tomcat...")
    os.execvp(cmd[0], cmd)


if __name__ == "__main__":
    main()
