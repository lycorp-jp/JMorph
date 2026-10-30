# JSON-RPC

`JMorphAnalyzerJsonrpc` is a Java API that accepts a JSON-RPC 2.0 string and returns a string in the same format.

This format is compatible with the [Yahoo! JAPAN Developer Network Japanese Morphological Analysis API](https://developer.yahoo.co.jp/webapi/jlp/ma/v2/parse.html).
For information about the interface standardization initiative, see https://techblog.yahoo.co.jp/entry/2020091730016720/.

It does not start an HTTP or standard I/O server. Combine it with the transport required by your application.

## Initialization

```java
JMorphConfig config = JMorphConfig.load("config.json");
JMorphAnalyzerJsonrpc analyzer = new JMorphAnalyzerJsonrpc(config);
```

The `jsonrpc` configuration is optional. If omitted, the accepted method name is `jmorph.tokenize` and the maximum number of analyzers in the internal pool is 16. Set `jsonrpc.method` or `jsonrpc.maxObject` only when overriding these defaults.

When configuring JMorph with builders, include a `JsonrpcConfig` to override the defaults:

```java
DictionaryConfig dictionary = DictionaryConfig.builder()
        .baseDic(new MorphDicMecabConfig("./dictionary"))
        .additionalDic(new MorphDicMecabConfig("./additional-dictionary"))
        .unknownDic(new UnknownDicBasicConfig(ConcatMode.ALL))
        .userDicInRequestProvider(new UserDicInRequestProviderBasicConfig())
        .build();

AnalyzerConfig analyzerConfig = AnalyzerConfig.builder()
        .dictionary(dictionary)
        .nbestNum(1)
        .outputMode(OutputMode.ALL)
        .timeoutMillis(10000)
        .build();

JsonrpcConfig jsonrpcConfig = JsonrpcConfig.builder()
        .method("jmorph.tokenize")
        .maxObject(16)
        .build();

JMorphConfig config = JMorphConfig.builder()
        .description("JMorph JSON-RPC sample")
        .analyzer(analyzerConfig)
        .jsonrpc(jsonrpcConfig)
        .build();

JMorphAnalyzerJsonrpc analyzer = new JMorphAnalyzerJsonrpc(config);
```

## Request

```json
{
  "id": "1",
  "jsonrpc": "2.0",
  "method": "jmorph.tokenize",
  "params": {
    "q": "東京特許許可局"
  }
}
```

| Property | Required | Description |
|--|--|--|
| `id` | Yes | A string, number, or `null`. Notification requests that omit this property are not supported |
| `jsonrpc` | Yes | Must be `"2.0"` |
| `method` | Yes | A string matching the configured `jsonrpc.method` |
| `params` | Yes | An object |
| `params.q` | Yes | The string to analyze. An empty string is allowed |
| `params.context` | No | An object containing a request-time user dictionary |

JMorph does not currently enforce a maximum request length. If requests can be supplied by an application user, validate the overall request size, including `params.q` and `params.context.entries`, against an application-appropriate limit and reject oversized requests before passing them to JMorph.

Pass a JSON-RPC request body to `analyze(String)`. To read a request from a regular file or another location supported by `FileUtils`, use `analyzeFile(String)` instead:

```java
String response = analyzer.analyze(requestJson);
String responseFromFile = analyzer.analyzeFile("request.json");
```

## Response

```json
{
  "id": "1",
  "jsonrpc": "2.0",
  "result": {
    "tokens": [
      ["東京", "名詞", "固有名詞"],
      ["特許", "名詞", "普通名詞"],
      ["許可", "名詞", "普通名詞"],
      ["局", "名詞", "普通名詞"]
    ]
  }
}
```

Each array in `tokens` corresponds to a Java API `TokenInfo`. The first element is the surface form, followed by dictionary features. The exact number and meaning of columns depend on the dictionary. With `OutputMode.SURFACE`, only the surface form is returned. The `EOS` included in Java API results is omitted from JSON-RPC responses. The result for an empty string is `"tokens": []`.

## Request-time User Dictionary

Specify an array of CSV strings in `context.entries`. Each string must have the format `surface form` or `surface form,reading`.

```json
{
  "id": "2",
  "jsonrpc": "2.0",
  "method": "jmorph.tokenize",
  "params": {
    "q": "架空ランドマークに行く",
    "context": {
      "entries": [
        "架空ランドマーク,かくうらんどまーく"
      ]
    }
  }
}
```

Entries apply only to the current request. `JMorphAnalyzerJsonrpc` removes them internally before returning the analyzer to the pool.

The number and individual lengths of entries are not limited separately by JMorph; they are part of the overall JSON-RPC request size described above.

## Thread Safety and Pooling

`JMorphAnalyzerJsonrpc` manages concurrent analysis through an internal pool of `JMorphAnalyzer` instances. Each call to `analyze()` borrows an analyzer from the pool and returns it when processing finishes. Callers do not need to create or manage their own analyzer pool.

`jsonrpc.maxObject` sets the maximum number of analyzers that the pool can use concurrently. If a request-time user dictionary was added, `JMorphAnalyzerJsonrpc` removes it before returning the analyzer to the pool so that its entries do not affect later requests.

## Errors

```json
{
  "id": "1",
  "jsonrpc": "2.0",
  "error": {
    "code": -32602,
    "message": "Invalid params"
  }
}
```

| Code | Message | Common cause |
|--|--|--|
| `-32700` | `Parse error` | The JSON cannot be parsed |
| `-32600` | `Invalid request` | A required property, the JSON-RPC version, or the `id` type is invalid |
| `-32601` | `Method not found` | `method` does not match the configured value |
| `-32602` | `Invalid params` | The type of `params.q` or `context.entries` is invalid |
| `-32603` | `Internal error` | Acquiring an analyzer, applying the dictionary, exceeding the configured analysis timeout, or another internal operation failed |

## Limitations

- JSON-RPC batch requests are not supported.
- Notification requests that omit `id` are not supported.
- N-best results cannot be retrieved through JSON-RPC.
