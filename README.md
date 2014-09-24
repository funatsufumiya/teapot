# JarGet

Java Library Installer *using Maven Central* (like rubygems)

## Features

- Standalone
- Search jar-files in Maven Central
- List versions of the artifacts
- Install jar-files and the dependencies

## Downloads

### Latest

- [jarget-0.9.12b.jar](http://atmarksharp.github.io/jarget/jarget-0.9.12b.jar)

### Archives

- [jarget-0.9.10b.jar](http://atmarksharp.github.io/jarget/jarget-0.9.10b.jar)

## Usage

<pre>
usage: jarget [options] [args...]

options:

    help
                --- show help

    search [query]
                --- search jar with query

    versions [group-id] [artifact]
                --- search versions of [group-id].[artifact]

    install [group-id] [artifact] [version] (-d [directory])
                --- install [version] of [group-id].[artifact]

    pom [group-id] [artifact] [version] (-d [directory])
                --- download pom-file [version] of [group-id].[artifact]
</pre>

## Example

```bash
alias jarget="java -jar jarget-xxx.jar"

jarget search commons

# Search Results:

# commons (net.dongliu.commons)
# commons (com.watchrabbit)
# commons (com.cisco.oss.foundation)
# commons (org.opencb.commons)
# commons (org.apache.commons)
# ...

jarget versions commons-io commons-io

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

jarget install commons-io commons-io 2.4 -d libs

# Installing commons-io:commons-io (2.4)...

# [Phase 1/4] Downloading Pom...
# [Phase 2/4] Check Dependencies...
# [Phase 3/4] Installing junit:junit (4.10)...
# [Phase 4/4] Installing commons-io:commons-io (2.4)...
# Complete!

```

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

### Open Source Licenses

- [Apache Commons IO 2.4](http://commons.apache.org/proper/commons-io/) (Apache License, Version 2.0)
- [JSONIC 1.3](http://jsonic.sourceforge.jp/) (Apache License, Version 2.0)