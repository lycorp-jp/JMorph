# Configuration

JMorph behavior is configured through `JMorphConfig`. This document describes the structure and properties of the JSON configuration file.

## Example Configuration

```json
{
  "description": "JMorph sample configuration",
  "analyzer": {
    "dictionary": {
      "baseDic": {
        "type": "mecab",
        "location": "./dictionary"
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

You can omit `jsonrpc` to use the default method name `jmorph.tokenize` and the default pool size of 16.

## Loading Configuration

Load a JSON file with `JMorphConfig.load()`.

```java
JMorphConfig config = JMorphConfig.load("config.json");
JMorphAnalyzer analyzer = new JMorphAnalyzer(config);
```

Configuration can also be constructed from an `InputStream` or a JSON string instead of a path. See [Java API](./java_api.md#loading-configuration) for all methods, including builders.

## Top Level

| Property | Required | Description |
|--|--|--|
| `description` | No | Optional description used to identify the configuration. It does not affect analysis behavior |
| `analyzer` | Yes | Analyzer configuration |
| `jsonrpc` | No | Configuration for `JMorphAnalyzerJsonrpc`. If omitted, all JSON-RPC defaults apply |

## `analyzer`

| Property | Required | Default | Description |
|--|--|--|--|
| `dictionary` | Yes | None | Dictionary used for analysis |
| `nbestNum` | No | `1` | Maximum number of N-best candidates to retain |
| `outputMode` | No | `ALL` | Output format for analysis results |
| `timeoutMillis` | No | `10000` | Maximum elapsed time in milliseconds for each analysis operation. Set to `0` to disable |

### `analyzer.dictionary`

| Property | Required | Default | Description |
|--|--|--|--|
| `baseDic` | Yes | None | System dictionary used as the basis for analysis |
| `additionalDic` | No | `[]` | Additional system dictionaries searched together with `baseDic` |
| `unknownDic` | No | `basic` | Unknown-word processing |
| `userDicInRequestProvider` | No | `basic` | Request-time user dictionary |

### `analyzer.dictionary.baseDic`

JMorph currently supports UTF-8 MeCab-format binary dictionaries.

```json
{
  "type": "mecab",
  "location": "./dictionary"
}
```

| Property | Required | Value | Description |
|--|--|--|--|
| `type` | Yes | `mecab` | Dictionary type |
| `location` | Yes | Path or URI-like format | Directory containing `sys.dic` and `matrix.bin`. It is also the default location for JMorph supplemental files |

See [Dictionaries](./dictionary.md) for the required files and formats.

#### `location`

You can specify a relative path, an absolute path, or one of the following URI-like formats:

```text
# Relative path
./path/to/dictionary

# Absolute path
/path/to/dictionary

# Absolute path in file: format
file:/path/to/dictionary

# Directory inside a JAR
jar:file:/path/to/dictionary.jar!/path/to/dictionary

# Directory on the classpath
classpath:/path/to/dictionary
```

Relative paths are resolved against the current working directory of the Java process. If multiple resources with the same name are found with `classpath:`, the first resource is used. Loading from `jar:` and `classpath:` may take longer than loading from a regular file path.

### `analyzer.dictionary.additionalDic`

Specify zero or more additional UTF-8 MeCab-format binary dictionaries. Each entry has the same `type` and `location` properties as `baseDic`. Set `location` to a directory containing `sys.dic`, not to the `sys.dic` file itself.

```json
[
  {
    "type": "mecab",
    "location": "./additional-dictionary"
  }
]
```

JMorph loads `<location>/sys.dic` from each entry. The connection matrix and supplemental files continue to come from `baseDic` and the configured unknown-word and request-time user-dictionary locations. The additional dictionaries' left and right context IDs must therefore be valid for `baseDic`'s `matrix.bin`.

### `analyzer.dictionary.unknownDic`

This section configures unknown-word processing.
The `basic` implementation analyzes strings not found in the dictionaries one character at a time. After analysis, it concatenates adjacent tokens of the same configured character type, including tokens found in a dictionary.

```json
{
  "type": "basic",
  "concatMode": "ALL"
}
```

| Property | Required | Default | Description |
|--|--|--|--|
| `type` | When `unknownDic` is specified | None | Unknown-word processor type. Currently, only `basic` is supported. Omit the entire `unknownDic` section to use the `basic` defaults |
| `location` | No | `baseDic.location` | Directory containing `unk.csv` and, with `outputMode: ALL`, `dump.unknown` |
| `concatMode` | No | `ALL` | Concatenation mode |
| `delim` | No | ASCII space | Feature delimiter in `dump.unknown` |

`delim` must be exactly one character encoded as one byte in UTF-8.

#### `concatMode`

| Value | Description |
|--|--|
| `NONE` | Do not concatenate tokens by character type |
| `ALL` | Concatenate letters, Arabic numerals, kanji numerals, and katakana by character type |
| `ALL_EXCEPT_KATAKANA` | Exclude katakana from the character types covered by `ALL` |

See [Core Concepts](./concepts.md#unknown-words) for detailed behavior and [Dictionaries](./dictionary.md) for supplemental file formats.

### `analyzer.dictionary.userDicInRequestProvider`

This section configures the request-time user dictionary feature.
The `basic` implementation allows a user dictionary containing surface forms and readings to be registered dynamically at analysis time.

```json
{
  "type": "basic"
}
```

| Property | Required | Default | Description |
|--|--|--|--|
| `type` | When `userDicInRequestProvider` is specified | None | Implementation type. Currently, only `basic` is supported. Omit the entire `userDicInRequestProvider` section to use the `basic` defaults |
| `location` | No | `baseDic.location` | Directory containing the optional `dump.userdic` file |
| `delim` | No | ASCII space | Feature delimiter in `dump.userdic` |

`delim` must be exactly one character encoded as one byte in UTF-8. With `OutputMode.ALL`, if `dump.userdic` does not exist, features are generated using the `dump.unknown` file loaded for `unknownDic`.

With `JMorphAnalyzer`, registered entries remain active until `removeUserDicInRequest()` is called. With `JMorphAnalyzerJsonrpc`, entries apply only to the current request and are removed internally before the analyzer is returned to the pool. See [Java API](./java_api.md#request-time-user-dictionary) or [JSON-RPC](./jsonrpc.md#request-time-user-dictionary) for usage.

### `analyzer.nbestNum`

The maximum number of analysis candidates to retain. Retrieve the best result with `JMorphAnalyzer.analyze()` and subsequent candidates with `JMorphAnalyzer.next()`. Because fewer candidates may exist, the specified number of results is not always available.

Specify a positive integer. Configuration loading and builder construction reject zero and negative values.

JMorph does not currently enforce an upper bound on this value. If the value can be supplied by an application user, validate it against an application-appropriate upper bound before loading or constructing the configuration.

The JSON-RPC API does not support returning N-best results.

### `analyzer.outputMode`

| Value | Description |
|--|--|
| `ALL` | Output the surface form and dictionary-derived features |
| `SURFACE` | Output only the surface form |

The number and contents of feature columns depend on the system dictionary in use.

### `analyzer.timeoutMillis`

The maximum elapsed time in milliseconds for each call to `JMorphAnalyzer.analyze()` or `JMorphAnalyzer.next()`. When the time is exceeded, the operation stops and throws `TimeoutExceededException`. Each call has its own timeout period.

The default is 10,000 milliseconds. Specify `0` to disable the timeout. Negative values are rejected during configuration loading or builder construction.

## `jsonrpc`

This section configures the method name accepted by `JMorphAnalyzerJsonrpc` and its internal analyzer pool.

```json
{
  "method": "jmorph.tokenize",
  "maxObject": 16
}
```

| Property | Required | Description |
|--|--|--|
| `method` | No | JSON-RPC method name accepted in requests. The default is `jmorph.tokenize` |
| `maxObject` | No | Maximum number of analyzers the internal pool can hold concurrently. Specify a positive integer. The default is 16 |

JMorph does not currently enforce an upper bound on `maxObject`. If the value can be supplied by an application user, validate it against an application-appropriate upper bound before loading or constructing the configuration.

See [JSON-RPC](./jsonrpc.md) for request and response formats, errors, and limitations.

## Configuring with Builders

To configure JMorph without JSON, combine the builders for each configuration class.

```java
DictionaryConfig dictionary = DictionaryConfig.builder()
        .baseDic(new MorphDicMecabConfig("./dictionary"))
        .additionalDic(new MorphDicMecabConfig("./additional-dictionary"))
        .unknownDic(new UnknownDicBasicConfig(ConcatMode.ALL))
        .userDicInRequestProvider(new UserDicInRequestProviderBasicConfig())
        .build();

AnalyzerConfig analyzer = AnalyzerConfig.builder()
        .dictionary(dictionary)
        .nbestNum(1)
        .outputMode(OutputMode.ALL)
        .timeoutMillis(10000)
        .build();

JsonrpcConfig jsonrpc = JsonrpcConfig.builder()
        .method("jmorph.tokenize")
        .maxObject(16)
        .build();

JMorphConfig config = JMorphConfig.builder()
        .description("JMorph sample configuration")
        .analyzer(analyzer)
        .jsonrpc(jsonrpc)
        .build();
```

`baseDic` is required by `DictionaryConfig.Builder`, `dictionary` is required by `AnalyzerConfig.Builder`, and `analyzer` is required by `JMorphConfig.Builder`. The builder and JSON-loading paths apply the same defaults and validation.
