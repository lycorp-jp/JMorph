# JMorph

**JMorph** is a Java morphological analysis library. It provides a Java API, a JSON-RPC interface, and an MCP server for tokenizing Japanese text.

## Features

- Java API for morphological analysis
- UTF-8 MeCab-format binary dictionaries
- Configurable token concatenation by character type
- Request-time user dictionary
- JSON-RPC 2.0 interface
- MCP server for AI clients

## Background

JMorph originated as part of an experimental project developed internally at LY Corporation. This repository makes part of the work produced by that project available as open source.

## Requirements

- Java 11 or later
- A UTF-8 MeCab-format binary dictionary
- JMorph-specific dictionary configuration files (`unk.csv` is required; `dump.unknown` is required with `OutputMode.ALL`; `dump.userdic` is optional)

## Installation

### Build from source

```console
git clone https://github.com/lycorp-jp/JMorph
cd JMorph

# Build all modules
./gradlew clean build
```

### Use as a dependency

JMorph is not yet available from Maven Central. Once it is published, use the following Maven coordinates:

```text
jp.co.lycorp.jlp.jmorph:jmorph-lib:<version>
```

Gradle:

```gradle
dependencies {
    implementation 'jp.co.lycorp.jlp.jmorph:jmorph-lib:<version>'
}
```

Maven:

```xml
<dependency>
    <groupId>jp.co.lycorp.jlp.jmorph</groupId>
    <artifactId>jmorph-lib</artifactId>
    <version>VERSION</version>
</dependency>
```

## Dictionary setup

### Preparing your own dictionary

JMorph currently supports analysis using UTF-8 MeCab-format binary dictionaries (`sys.dic` and `matrix.bin`).
JMorph also requires `unk.csv`. `dump.unknown` is required with `OutputMode.ALL`, while `dump.userdic` is optional.
See the [dictionary documentation](docs/dictionary.md) for detailed specifications.

```text
dictionary/
├── sys.dic
├── matrix.bin
├── unk.csv
├── dump.unknown   # Required with OutputMode.ALL
└── dump.userdic   # Optional
```

The dictionary and its accompanying files may have their own licenses. Confirm that you have the right to use and redistribute them.

#### Example: UniDic

This section describes how to use UniDic with JMorph.

Download a UTF-8 MeCab-format binary dictionary from https://clrd.ninjal.ac.jp/unidic/ and place `sys.dic` and `matrix.bin` in `models/mecab-unidic/data/`.

The `models/mecab-unidic/data/` directory already contains the additional JMorph configuration files for UniDic.

```text
models/mecab-unidic/data/
├── sys.dic        # Downloaded separately
├── matrix.bin     # Downloaded separately
├── unk.csv
├── dump.unknown   # Required with OutputMode.ALL
└── dump.userdic   # Optional
```

## Configuration

Create a JSON configuration file such as `config.json`. The examples below assume that this file is in the repository root, but configuration can also be loaded from another file system location, a JAR or classpath resource, an `InputStream`, or a JSON string. In the configuration, set `baseDic.location` to the dictionary directory.
See the [configuration documentation](docs/configuration.md) for detailed specifications.

For the UniDic example above, the configuration looks like this:

```json
{
  "description": "jmorph",
  "analyzer": {
    "dictionary": {
      "baseDic": {
        "type": "mecab",
        "location": "./models/mecab-unidic/data/"
      },
      "additionalDic": [],
      "unknownDic": {
        "type": "basic",
        "concatMode": "ALL"
      },
      "userDicInRequestProvider": {
        "type": "basic"
      }
    },
    "nbestNum": 1,
    "outputMode": "ALL",
    "timeoutMillis": 10000
  },
  "jsonrpc": {
    "method": "jmorph.tokenize",
    "maxObject": 16
  }
}
```

## Usage

### Java API

```java
import java.io.IOException;
import java.util.List;

import jp.co.lycorp.jlp.jmorph.JMorphAnalyzer;
import jp.co.lycorp.jlp.jmorph.JMorphConfig;
import jp.co.lycorp.jlp.jmorph.TokenInfo;

try {
    JMorphConfig config = JMorphConfig.load("config.json");
    JMorphAnalyzer analyzer = new JMorphAnalyzer(config);

    List<TokenInfo> results = analyzer.analyze("東京特許許可局");
    for (TokenInfo tokenInfo : results) {
        System.out.println(String.join("\t", tokenInfo.get()));
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

### JSON-RPC

`JMorphAnalyzerJsonrpc` accepts a JSON-RPC 2.0 request as a string. The request method must match `jsonrpc.method` in the configuration.

```java
import java.io.IOException;

import jp.co.lycorp.jlp.jmorph.JMorphAnalyzerJsonrpc;
import jp.co.lycorp.jlp.jmorph.JMorphConfig;

try {
    JMorphConfig config = JMorphConfig.load("config.json");
    JMorphAnalyzerJsonrpc analyzer = new JMorphAnalyzerJsonrpc(config);

    String request = "{\"id\": \"1\", \"jsonrpc\": \"2.0\", \"method\": \"jmorph.tokenize\", \"params\": { \"q\": \"東京特許許可局\" }}";

    String response = analyzer.analyze(request);
    System.out.println(response);

    String responseFromFile = analyzer.analyzeFile("request.json");
    System.out.println(responseFromFile);
} catch (IOException e) {
    e.printStackTrace();
}
```

### MCP Server

`JMorphAnalyzerMCP` is a stdio-based MCP server that allows AI clients to use JMorph. Build the `jmorph-cli` JAR and the `jmorph-lib` fat JAR, then register the server with your MCP client.

For example, the following configuration can be used as a template for Codex:

```toml
[mcp_servers.local_jmorph]
command = "java"
args = [
  "-classpath",
  "<path-to-jmorph-cli-jar>:<path-to-jmorph-lib-all-jar>",
  "jp.co.lycorp.jlp.jmorph.cli.JMorphAnalyzerMCP",
  "<path-to-config-json>"
]
```

After registration, the MCP client discovers the `jmorph.tokenize` tool and its schema automatically. See the [MCP Server documentation](docs/mcp.md) for build instructions, configuration details, and debugging options.

### Sample CLI

Samples are available in `jmorph/jmorph-cli` and can be run as follows.

Prepare the `config.json` file shown above and the following `input.jsonl` file.
```jsonl
{"id": "1", "jsonrpc": "2.0", "method": "jmorph.tokenize", "params": { "q": "美しい水車小屋の娘" }}
{"id": "2", "jsonrpc": "2.0", "method": "jmorph.tokenize", "params": { "q": "東京特許許可局" }}
```

```console
# Build the CLI JAR and library fat JAR
./gradlew :jmorph:jmorph-cli:jar :jmorph:jmorph-lib:shadowJar

# Java API sample (standard input/output)
# NOTICE: Replace <cli-version> and <lib-version> with the versions in the generated JAR file names.
java -classpath './jmorph/jmorph-cli/build/libs/jmorph-cli-<cli-version>.jar:./jmorph/jmorph-lib/build/libs/jmorph-lib-<lib-version>-all.jar' \
jp.co.lycorp.jlp.jmorph.cli.JMorphAnalyzerSample
<INPUT YOUR TEXT>

# JSON-RPC sample (input file)
# NOTICE: Replace <cli-version> and <lib-version> with the versions in the generated JAR file names.
java -classpath './jmorph/jmorph-cli/build/libs/jmorph-cli-<cli-version>.jar:./jmorph/jmorph-lib/build/libs/jmorph-lib-<lib-version>-all.jar' \
jp.co.lycorp.jlp.jmorph.cli.JMorphAnalyzerJsonrpcSample \
input.jsonl
```

## Documentation

Detailed documentation is available in [docs/](docs/README.md).
See the [changelog](docs/changelog.md) for notable changes in each release.

## Contributing

See the [contribution guidelines](docs/contributing.md).

## Acknowledgments

We thank everyone who contributed to the internal development of JMorph. See [Contributors](docs/contributors.md) for a list of contributors.

JMorph contains a Java implementation derived from MeCab's Darts (Double-Array Trie System). See [NOTICE](NOTICE) and [THIRD_PARTY_LICENSES](THIRD_PARTY_LICENSES/) for attribution and license details.

## License

This repository is licensed under Apache License 2.0, except for files that carry a different SPDX license identifier. Third-party derived files remain under their respective licenses. See [THIRD_PARTY_LICENSES](THIRD_PARTY_LICENSES/) for details.

See [LICENSE](LICENSE) and [NOTICE](NOTICE).
