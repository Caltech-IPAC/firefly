

# Installing a personal Firefly instance

Firefly can be installed directly on your macOS or Linux desktop machine. 
This is a full-featured installation that performs very well when working with local files.

### Requirements

- `curl` and `unzip` must be available on your system; the installer checks for these up front and stops with a clear message if either is missing.
- Java is not required beforehand — the installer downloads a compatible Java runtime automatically unless you configure your own (see [Advanced Configuration](#advanced-configuration)).
- See [Confirming firefly will run on your OS](#confirming-firefly-will-run-on-your-os) for OS-specific requirements.

## Installing Firefly

### Quick install

```bash
curl -L https://raw.githubusercontent.com/Caltech-IPAC/firefly/refs/heads/dev/bin/get-firefly | bash
```
#### Usage
1. Change to the directory where you want to install Firefly.
1. Run the command above.
1. The installer will create a firefly directory containing the application and supporting files.


### Advanced Install

#### 1. Download the installer

```bash
curl -L https://raw.githubusercontent.com/Caltech-IPAC/firefly/refs/heads/dev/bin/install.sh -o install.sh
```

#### 2. Run the installer

```bash
chmod +x install.sh
./install.sh
```

#### 3. Choose installation options

The installer will prompt you for a destination directory.

Use the following command to see all available options:

```bash
./install.sh -h
```


---

## Starting Firefly

After installation, Firefly provides instructions for starting the server.

The Firefly server is managed using the `firefly/bin/ff` script.

To see all available commands and options:

```bash
firefly/bin/ff --help
```

When Firefly starts, it automatically opens in your default web browser.

The `ff` script uses commands as its primary argument.

### Start Firefly

```bash
firefly/bin/ff start
```

### Start Firefly in the Background

```bash
firefly/bin/ff start --background
```

### Stop Firefly

(Only needed when running in background mode.)

```bash
firefly/bin/ff stop
```

### Tail Log Files

```bash
firefly/bin/ff logs -f
```

### Check Server Status

```bash
firefly/bin/ff status
```

### Uninstall Firefly

```bash
firefly/bin/ff uninstall
```

---

## macOS UI Integration

On macOS, Firefly creates a menu bar icon on the right side of the system menu bar.

Use the drop-down menu to control and monitor the Firefly server.

## Advanced Configuration

Firefly can be configured using the JSON file:

```text
~/.firefly/config.json
```

Edit this file to change the ports Firefly uses or to specify your own Java installation.

Firefly requires Java 21 or later. By default, Firefly uses `"auto"` to automatically select a compatible Java runtime.

If you want to use a Java installation already available on your system, replace the `java` entry in `config.json` with the path to your Java executable.

### Default Configuration

```json
{
  "ports": {
    "firefly": 10233,
    "redis": 10234
  },
  "java": "auto"
}
```

### Example Custom Configuration

This example changes the Firefly port and uses a local Java installation:

```json
{
  "ports": {
    "firefly": 7777,
    "redis": 102346
  },
  "java": "/usr/bin/java"
}
```

### Confirming firefly will run on your OS

#### OSX
Firefly requires macOS 15 or greater

#### Linux

Firefly requires that `libssl.so.3` is on your linux system.
Check with the following command
```bash
 /sbin/ldconfig -p | grep libssl.so.3
```
#### Linux version with `libssl.so.3`
- Debian 12 or later
- Red Hat 9 or later
- Ubuntu 22.04 LTS or later
- Fedora all recent releases

#### Windows
Standalone Firefly is not supported on Windows

---

## Troubleshooting

- **The installer stops with a missing command error**: install the missing tool (`curl` and/or `unzip`) with your system's package manager and re-run the install.
- **"Error contacting the GitHub API" or "No package defined to download"**: this usually means a network issue or that GitHub's API rate limit was hit. Wait a few minutes and try again, or pass a direct URL/path to a `standalone.zip` with `./install.sh -url <path-or-url>`.
- **The installer reports a failed download or a failure expanding a package**: re-run the install; if it persists, check your network connection and firewall, or download the release manually from the [Firefly releases page](https://github.com/Caltech-IPAC/firefly/releases) and reinstall with `./install.sh -url <path-to-standalone.zip>`.
- **`ff start` reports the port is in use**: another application is using the configured port. Change the port in `~/.firefly/config.json` or start with `firefly/bin/ff start --port <port>`.
- **Java fails to install automatically**: install Java 21+ yourself and set its path in the `java` field of `~/.firefly/config.json`, replacing `"auto"`.

