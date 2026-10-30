# Java API

This document describes how to perform morphological analysis by using `JMorphAnalyzer` directly. For the JSON-RPC interface provided by `JMorphAnalyzerJsonrpc`, see [JSON-RPC](./jsonrpc.md).

## Loading Configuration

Configuration can be loaded from a JSON file, an `InputStream`, or a JSON string.

```java
JMorphConfig fromFile = JMorphConfig.load("config.json");
JMorphConfig fromPath = JMorphConfig.load(Path.of("config.json"));
JMorphConfig fromStream = JMorphConfig.load(inputStream);
JMorphConfig fromJson = JMorphConfig.fromJson(jsonString);
```

If reading the file or parsing the JSON fails, a `RuntimeException` is thrown. See [Configuration](./configuration.md) for configuration properties.

## Configuring with Builders

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

JMorphConfig config = JMorphConfig.builder()
        .description("JMorph sample")
        .analyzer(analyzerConfig)
        .build();
```

`baseDic` is required by `DictionaryConfig.Builder`, `dictionary` is required by `AnalyzerConfig.Builder`, and `analyzer` is required by `JMorphConfig.Builder`.

## Analyzing a String

```java
JMorphAnalyzer analyzer = new JMorphAnalyzer(config);
List<TokenInfo> results = analyzer.analyze("東京特許許可局");

for (TokenInfo tokenInfo : results) {
    String[] fields = tokenInfo.get();
    System.out.println(String.join("\t", fields));
}
```

`analyze(String)` converts the string to UTF-8 bytes and analyzes it. An `analyze(byte[])` overload is also available for passing UTF-8 bytes directly. Empty input returns an empty list; normal results include a trailing `EOS` element.

JMorph does not currently enforce a maximum input length. If input can be supplied by an application user, validate its length against an application-appropriate limit and reject oversized input before calling `analyze()`.

`TokenInfo.get()` returns a string array, while `TokenInfo.getBytes()` returns an array of UTF-8 byte arrays. `OutputMode.SURFACE` contains only the surface form. `OutputMode.ALL` contains the surface form followed by dictionary features.

## Analysis Timeout

Each call to `analyze()` and `next()` is limited by `AnalyzerConfig.timeoutMillis`. The default is 10,000 milliseconds. Set it to `0` to disable the timeout.

If the configured time is exceeded, `TimeoutExceededException` is thrown and any partially constructed lattice is discarded. A subsequent call to `next()` returns an empty list.

## Request-time User Dictionary

```java
List<UserDicEntry> entries = List.of(
        new UserDicEntry("架空ランドマーク"),
        new UserDicEntry("架空冒険譚", "かくうぼうけんたん")
);

analyzer.addUserDicInRequest(entries);
try {
    List<TokenInfo> results = analyzer.analyze(text);
    // Use results
} finally {
    analyzer.removeUserDicInRequest();
}
```

Added entries remain active until explicitly removed. Call `removeUserDicInRequest()` in a `finally` block to ensure that they are removed even if an exception occurs. Adding entries again to the same analyzer without first removing the previous entries causes an exception.

JMorph does not currently enforce a maximum number of request-time user dictionary entries or a maximum length for their surface forms and readings. These limits are separate from the analysis input length. If entries can be supplied by an application user, validate their count and lengths against application-appropriate limits before calling `addUserDicInRequest()`.

## Retrieving N-best Results

```java
List<TokenInfo> best = analyzer.analyze(text);
List<TokenInfo> second = analyzer.next();
List<TokenInfo> third = analyzer.next();
```

`next()` returns the next candidate for the input most recently passed to `analyze()`. It returns an empty list if called before analysis or if no further candidate exists. Set the maximum number of retained candidates with `nbestNum`.

## Using Multiple Threads

`JMorphAnalyzer` is not thread-safe. You can use `GenericObjectPool` from Apache Commons Pool 2 with `JMorphAnalyzerPoolFactory`.

```java
try (GenericObjectPool<JMorphAnalyzer> pool =
        new GenericObjectPool<>(new JMorphAnalyzerPoolFactory(config))) {
    JMorphAnalyzer analyzer = pool.borrowObject();
    try {
        List<TokenInfo> results = analyzer.analyze(text);
    } finally {
        pool.returnObject(analyzer);
    }
}
```

If you add a request-time user dictionary, always remove it before returning the analyzer to the pool. If you need the JSON-RPC format, you can also use `JMorphAnalyzerJsonrpc`, which manages an analyzer pool internally.
