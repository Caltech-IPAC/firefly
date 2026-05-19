

# Installing a personal Firefly instance

Firefly can be installed directly on your macOS or Linux desktop machine. 
This is a full-featured installation that performs very well when working with local files.

                                
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

