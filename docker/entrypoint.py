#!/usr/bin/env python3

import sys
import os
import random
import base64
import subprocess
import shlex
import shutil
from pathlib import Path
from zipfile import ZipFile
from typing import List

SLIDES: str = "\n" + ("=" * 40) + "\n"


def extract(source: Path, destination: Path, prefix: str = "/"):
    """Extract a file to a destination directory.

    Args:
        source (Path): Source directory to extract from
        destination (Path): Destination directory to extract to
        prefix (str, optional): Prefix extracted filepaths. Default is "/".

    Raises:
        FileNotFoundError: If source directory does not exist.
        FileExistsError: If destination directory already exists.
    """
    extractions: dict[str, Path] = {}
    # Default file extension to extract
    kind: str = "war"
    print(f"{SLIDES} Extracting Files {SLIDES}")
    print(f"Source     : {source}")
    print(f"Destination: {destination}")
    print(f"Prefix     : {prefix}")
    print("\n")

    if not source.exists() or not source.is_dir():
        raise FileNotFoundError(f"Source directory does not exist: {source}")

    for item in source.iterdir():
        # Only extract specified file extensions
        if item.is_file() and item.suffix == f".{kind}":
            filename = item.stem
            # Configure prefix for extracted files
            landing = pathfix(destination, prefix, filename)

            # Extract archive to landing directory
            if expand(item, landing):
                extractions[filename] = landing

            # Modify log4j to send logs to stdout
            logfix(landing)

    # Copy secondary files for extracted archives to landing directory
    copy(source, extractions)
    print(f"{SLIDES} Extractions Done {SLIDES}")


def pathfix(destination: Path, prefix: str, filename: str) -> Path:
    """Fix the path for extracted files to include a prefix.

    Args:
        destination (Path): Top-level directory for extracted files.
        prefix (str): URL prefix for extracted files.
        filename (str): Name of the extracted file.

    Returns:
        Path: Path to extracted file with prefix.
    """
    prefix = prefix.replace("/", "#").strip("#")
    if prefix:
        landing = destination / f"{prefix}#{filename}"
    else:
        landing = destination / filename
    return landing


def copy(source: Path, extractions: dict[str, Path]):
    """Copy project files from source to landing directory.

    Args:
        source (Path): Source directory to copy from
        extractions (dict[str, Path]): Extracted files to copy to landing directory.
    """
    for filename, landing in extractions.items():
        origin = source / f"{filename}"
        if origin.exists() and origin.is_dir():
            for item in origin.iterdir():
                if item.is_dir():
                    print(f"    Copying tree {item} -> {landing}")
                    shutil.copytree(item, landing / item.name, dirs_exist_ok=True)
                else:
                    print(f"    Copying file {item} -> {landing}")
                    shutil.copy2(item, landing / item.name)


def expand(archive: Path, landing: Path) -> bool:
    """Extract a WAR archive to a landing directory.

    Args:
        archive (Path): WAR archive to extract
        landing (Path): Destination directory to extract to

    Raises:
        error: Unknown error during extraction

    Returns:
        bool: True if extraction is successful.
    """
    try:
        print(f"Extracting {archive}")
        # Create landing directory if it does not exist
        landing.mkdir(parents=True, exist_ok=False)
        print(f"    Created DIR -> {landing}")
        # Extract archive to landing directory
        with ZipFile(archive, "r") as source:
            source.extractall(landing)
        print(f"    Extracted WAR {archive} -> {landing}")
    except FileExistsError as error:
        print(f"    Destination exists: {error}")
        print(f"    Skipping {archive} extraction")
    except Exception as error:
        print(f"    Unknown extracting {archive}: {error}")
        raise error
    return True


def logfix(landing: Path):
    """Modify log4j2.properties to send logs to stdout.

    Args:
        landing (Path): Base path for extracted files.
    """
    log4j = landing / "WEB-INF/classes/log4j2.properties"
    if log4j.exists():
        # Create backup as log4j.properties.bak
        print(f"    Modifying {log4j}")
        shutil.copy(log4j, log4j.with_name(log4j.name + ".bak"))
        with open(log4j, "r+", encoding="utf-8") as file:
            content = file.read()
            content = content.replace("##out--", "")
            file.seek(0)
            file.write(content)
            file.truncate()


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


def log_env_info(path_prefix: str, visualize_fits_search_path: str):
    """Log environment variables and configuration information."""
    print(
        f"""
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
    """
    )


def show_help(name: str, webapps_ref: Path):
    """Show help message and examples.

    Args:
        name (str): Application name
        webapps_ref (Path): Reference webapps directory
    """
    with open("./start-examples.txt", encoding="utf-8") as f:
        print(f.read().replace("ipac/firefly", name))

    for item in webapps_ref.iterdir():
        if item.is_file() and item.stem == "firefly" and item.suffix == ".war":
            with open("./customize-firefly.txt", encoding="utf-8") as f:
                print(f.read())
    sys.exit(0)


def dry_run(cmd: List[str], webapps: Path):
    """Display a dry run of the command and environment setup.

    Args:
        cmd (List[str]): Command to run
        webapps (Path): Webapps directory
    """
    print(f"\n{SLIDES} DRY RUN {SLIDES}")
    print(f"COMMAND      : {' '.join(cmd)}")
    print(f"CATALINA_OPTS: {os.getenv('CATALINA_OPTS')}\n")
    print(f"WEBAPPS      : {webapps}':")
    for item in webapps.iterdir():
        if item.is_dir():
            print(f" [DIR]  {item.name}")
        else:
            print(f" [FILE] {item.name}")
    print(f"{SLIDES} DRY RUN COMPLETE {SLIDES}")
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
    admin_password = os.getenv(
        "ADMIN_PASSWORD",
        base64.b64encode(str(random.randint(100000, 999999)).encode()).decode()[:8],
    )
    use_admin_auth = os.getenv("USE_ADMIN_AUTH", "true").lower()
    path_prefix = (
        os.getenv("PATH_PREFIX") or os.getenv("baseURL") or ""
    )  # use PATH_PREFIX; baseURL is for backward compatibility

    vis_path = (
        "/external"
        if not visualize_fits_search_path
        else f"/external:{visualize_fits_search_path}"
    )

    catalina_opts = " ".join(
        [
            f"-XX:InitialRAMPercentage={os.getenv('INIT_RAM_PERCENT', '10')}",
            f"-XX:MaxRAMPercentage={os.getenv('MAX_RAM_PERCENT', '80')}",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:TrimNativeHeapInterval=30000",
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
            f"-Dvisualize.fits.search.path={vis_path}",
        ]
    )

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
    debug_mode = os.getenv("DEBUG", "false").lower() == "true" or (
        len(sys.argv) > 1 and sys.argv[1] == "--debug"
    )
    cmd = [f"{catalina_home}/bin/catalina.sh"]
    if debug_mode:
        cmd.append("jpda")
    cmd.append(start_mode)

    # log environment information
    log_env_info(path_prefix, visualize_fits_search_path)

    # Setup examples
    subprocess.call("./setupFireflyExample.sh", shell=True)

    catalina: Path = Path(catalina_home)
    webapps_ref: Path = catalina / "webapps-ref"
    webapps: Path = catalina / "webapps"
    extract(source=webapps_ref, destination=webapps, prefix=path_prefix)

    # check for no-ops flags
    if len(sys.argv) > 1:
        arg = sys.argv[1]
        if arg in ["--help", "-help", "-h"]:
            show_help(name, webapps_ref)
        elif arg == "--dry-run":
            dry_run(cmd, webapps)

    # Start background cleanup
    subprocess.Popen(
        [f"{catalina_home}/cleanup.sh", "/firefly/workarea", "/firefly/shared-workarea"]
    )

    # Start Tomcat; Replace the current process with Tomcat
    print("Starting Tomcat...")
    os.execvp(cmd[0], cmd)


if __name__ == "__main__":
    main()
