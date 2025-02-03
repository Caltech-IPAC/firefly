#!/usr/bin/env python3

import sys
import os
import random
import base64
import subprocess
import shlex

def extract_war_files(webapps_dir, webapps_ref, PATH_PREFIX):
    """
    Prepare webapps on first-time startup:
    - Extract WAR files from webapps-ref to webapps.
    - Modify log4j to send logs to stdout.
    - Modify context path (pathPrefix) if given.

    :param webapps_dir: Path to the webapps directory
    :param webapps_ref: Path to the webapps reference directory.
    """

    if not os.listdir(webapps_dir):
        for war in os.listdir(webapps_ref):
            if war.endswith(".war"):
                fn = os.path.splitext(war)[0]
                prefix = PATH_PREFIX.replace("/", "#").strip("#")
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
    PROPS = os.getenv("PROPS", "")

    if PROPS:
        placeholder = "__SEMICOLON__"
        PROPS = PROPS.replace(";;", placeholder)  # Handle escaped semicolons

        for prop in PROPS.split(";"):
            prop = prop.replace(placeholder, ";")  # Restore escaped semicolons

            if "=" in prop:
                key, value = prop.split("=", 1)
                key = key.strip()
                value = shlex.quote(value.strip())  # Safely quote values
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
            value = shlex.quote(value.strip())  # Ensure safe values
            props_opts += f" -D{prop_key}={value}"

    return props_opts

def log_env_info(PATH_PREFIX, VISUALIZE_FITS_SEARCH_PATH):
    print("========== Information:  you can set environment variable using -e on docker run line =====  \n")
    print("Environment Variables:")
    print("        Description                      Name                          Value")
    print("        -----------                      --------                      -----")
    print(f"        Admin username                   ADMIN_USER                    {os.getenv('ADMIN_USER')}")
    print(f"        Admin password                   ADMIN_PASSWORD                {os.getenv('ADMIN_PASSWORD')}")
    print(f"        Additional data path             VISUALIZE_FITS_SEARCH_PATH    {VISUALIZE_FITS_SEARCH_PATH}")
    print(f"        Clean internal(eg- 720m, 5h, 3d) CLEANUP_INTERVAL              {os.getenv('CLEANUP_INTERVAL', '')}")
    print(f"        Context path prefix              pathPrefix                    {PATH_PREFIX}")
    print()
    print("Advanced environment variables:")
    print(f"        Run tomcat with debug            DEBUG                         {os.getenv('DEBUG', '')}")
    print(f"        Extra firefly properties(*)      PROPS                         {os.getenv('PROPS', '')}")
    print(f"        Redis host                       PROPS_redis__host             {os.getenv('PROPS_redis__host', '')}")
    print(f"        SSO host                         PROPS_sso__req__auth__hosts   {os.getenv('PROPS_sso__req__auth__hosts', '')}")
    print(f"        firefly.options (JSON string)    PROPS_FIREFLY_OPTIONS         {os.getenv('PROPS_FIREFLY_OPTIONS', '')}")
    print(" (*) key=value pairs separated by semicolon, use double semicolons to escape semicolon")
    print()
    print("Ports: ")
    print("        8080 - http")
    print("        5050 - debug")
    print()
    print("Volume Mount Points: ")
    print("    /firefly/logs             : logs directory")
    print("    /firefly/workarea         : work area for temporary files")
    print("    /firefly/shared-workarea  : work area for files that are shared between multiple instances of the application")
    print("    /external                 : default external data directory visible to Firefly")
    print()
    print("  Less used:")
    print("    /firefly/config           : used to override application properties")
    print("    /firefly/logs/statistics  : directory for statistics logs")
    print("    /firefly/alerts           : alerts monitor will watch this directory for application alerts")
    print()
    print("Command line options: ")
    print("        --help  : show help message, examples, stop")
    print("        --debug : start in debug mode")
    print("\n")

def show_help(NAME):
    """Show help message and exit."""
    with open("./start-examples.txt") as f:
        print(f.read().replace("ipac/firefly", NAME))
    war_files = [f for f in os.listdir(webapps_ref) if f.endswith(".war")]
    if war_files and war_files[0] == "firefly.war":
        with open("./customize-firefly.txt") as f:
            print(f.read())
    sys.exit(0)

def dry_run(cmd, webapps_dir):
    print("\n\n----------------------")
    print(f"Command: {' '.join(cmd)}")  # Fixed by using single quotes inside the f-string
    print(f"CATALINA_OPTS: {os.getenv('CATALINA_OPTS')}")

    print(f"\nContents of '{webapps_dir}':")
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

    # Load environment variables with fallback defaults
    os.environ["JPDA_ADDRESS"] = "*:5050"
    CATALINA_HOME = os.getenv("CATALINA_HOME", "/usr/local/tomcat")
    VISUALIZE_FITS_SEARCH_PATH = os.getenv("VISUALIZE_FITS_SEARCH_PATH", "")
    START_MODE = os.getenv("START_MODE", "run")
    NAME = os.getenv("BUILD_TIME_NAME", "ipac/firefly")
    ADMIN_USER = os.getenv("ADMIN_USER", "admin")
    ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD", base64.b64encode(str(random.randint(100000, 999999)).encode()).decode()[:8])
    USE_ADMIN_AUTH = os.getenv("USE_ADMIN_AUTH", "true").lower()
    PATH_PREFIX = os.getenv("pathPrefix") or os.getenv("baseURL") or ""      # use pathPrefix; baseURL is for backward compatibility

    # Set visualize path
    VIS_PATH = "/external" if not VISUALIZE_FITS_SEARCH_PATH else f"/external:{VISUALIZE_FITS_SEARCH_PATH}"

    # Set Catalina options
    CATALINA_OPTS = " ".join([
        f"-XX:InitialRAMPercentage={os.getenv('INIT_RAM_PERCENT', '10')}",
        f"-XX:MaxRAMPercentage={os.getenv('MAX_RAM_PERCENT', '80')}",
        "-XX:+UnlockExperimentalVMOptions", "-XX:TrimNativeHeapInterval=30000",
        f"-DADMIN_USER={ADMIN_USER}",
        f"-DADMIN_PASSWORD={ADMIN_PASSWORD}",
        f"-Dhost.name={os.getenv('HOSTNAME', '')}",
        f"-Dserver.cores={os.getenv('JVM_CORES', '')}",
        "-Djava.net.preferIPv4Stack=true",
        "-Dwork.directory=/firefly/workarea",
        "-Dshared.work.directory=/firefly/shared-workarea",
        "-Dserver_config_dir=/firefly/config",
        "-Dstats.log.dir=/firefly/logs/statistics",
        "-Dalerts.dir=/firefly/alerts",
        f"-Dvisualize.fits.search.path={VIS_PATH}"
    ])

    # Remove admin protection if disabled
    if USE_ADMIN_AUTH == "false":
        CATALINA_OPTS += " -DADMIN_PROTECTED="

    # extract and add properties defined as environment variables
    CATALINA_OPTS += add_multi_props_env_var()
    CATALINA_OPTS += add_single_prop_env_vars()

    # Set environment variables so they persist in Tomcat
    os.environ["CATALINA_PID"] = os.path.join(CATALINA_HOME, "bin", "catalina.pid")
    os.environ["CATALINA_OPTS"] = CATALINA_OPTS
    os.environ["ADMIN_USER"] = ADMIN_USER
    os.environ["ADMIN_PASSWORD"] = ADMIN_PASSWORD
    debug_mode = os.getenv("DEBUG", "false").lower() == "true" or (len(sys.argv) > 1 and sys.argv[1] == "--debug")
    cmd = [f"{CATALINA_HOME}/bin/catalina.sh"]
    if debug_mode:
        cmd.append("jpda")
    cmd.append(START_MODE)

    # log environment information
    log_env_info(PATH_PREFIX, VISUALIZE_FITS_SEARCH_PATH)

    # Setup examples
    subprocess.call("./setupFireflyExample.sh", shell=True)

    # Prepare webapps on first-time startup
    webapps_dir = os.path.join(CATALINA_HOME, "webapps")
    webapps_ref = os.path.join(CATALINA_HOME, "webapps-ref")
    extract_war_files(webapps_dir, webapps_ref, PATH_PREFIX)

    # check for no-ops flags
    if len(sys.argv) > 1:
        arg = sys.argv[1]
        if arg in ["--help", "-help", "-h"]:
            show_help(NAME)
        elif arg == "--dry-run":
            dry_run(cmd, webapps_dir)

    # Start background cleanup
    subprocess.Popen([f"{CATALINA_HOME}/cleanup.sh", "/firefly/workarea", "/firefly/shared-workarea"])

    # Start Tomcat; Replace the current process with Tomcat
    print("Starting Tomcat...")
    os.execvp(cmd[0], cmd)

if __name__ == "__main__":
    main()
