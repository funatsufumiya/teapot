# Teapot

Java Library Installer *using Maven Central* (like rubygems)

## Features

- Standalone
- Search jar-files in Maven Central
- List versions of the artifacts
- Install jar-files and the dependencies *(simple or all)*

## Downloads

#### Latest

- [teapot-0.12.0b.jar](https://github.com/atmarksharp/teapot/releases/download/0.12.0b/teapot-0.12.0b.jar)

## Usage

<pre>
usage: teapot [command] [args...] [option...]

command:

    help
                --- show help

    search [query]
                --- search jar with query

    versions [group-id] [artifact]
                --- search versions of [group-id].[artifact]

    install [group-id] [artifact] [version]
                --- install jars and dependencies (without test scope)

    install-all [group-id] [artifact] [version]
                --- install jars and dependencies (include test scope and optional)

    jar [group-id] [artifact] [version]
                --- install jar only

    pom [group-id] [artifact] [version]
                --- download pom-file [version] of [group-id].[artifact]

option:

    -v | --version
                --- print version

    -h | --help
                --- show help

    -Dproperty=value
                --- define property. use this if the property is undefined

    -Dproperty=*
                --- define property as latest version (in bash, this will be -Dproperty=\*)

    -d [directory]
                --- set file output directory

    -l [number]
                --- set list count for display
</pre>

## Example

```bash
alias teapot="java -jar teapot-xxx.jar"

teapot search commons

# Search Results:

# commons (net.dongliu.commons)
# commons (com.watchrabbit)
# commons (com.cisco.oss.foundation)
# commons (org.opencb.commons)
# commons (org.apache.commons)
# ...

teapot versions commons-io commons-io

# Versions:

# 2.4
# 2.3
# 2.2
# 2.1
# 2.0.1
# 2.0
# 1.4
# 1.3.2
# 1.3.1
# ...

teapot install commons-io commons-io 2.4 -d libs

# Installing commons-io:commons-io (2.4)...

# [Phase 1/4] Downloading Pom...
# [Phase 2/4] Check Dependencies...
# [Phase 3/4] Installing Dependencies... OK
# [Phase 4/4] Installing commons-io:commons-io (2.4)...
# Complete!

```

## FAQ

#### [Error] parameter 'xxx' is undefined

On current version, teapot recognizes parametes only defined in `<parameters>...</parameters>`.

Please use `-Dparameter=value` option.

#### Can't read version like `[1.7.0,), (,1.8.0]`

Sorry. We will not support complicated version range.

Please use simple range like `[1.7.0,1.8.0]`

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

### Open Source Licenses

- [Apache Commons IO 2.4](http://commons.apache.org/proper/commons-io/) (Apache License, Version 2.0)
- [JSONIC 1.3](http://jsonic.sourceforge.jp/) (Apache License, Version 2.0)
