# Troubleshooting

## Configuration File Cannot Be Loaded

- Relative paths are resolved against the current working directory of the Java process.
- Specify an absolute path when using `file:`.
- JSON `type` values are case-sensitive.
- `JMorphConfig.load()` reports file-reading and JSON-parsing failures with a `RuntimeException`. Check the cause exception as well.

## Dictionary Initialization Fails

- Confirm that `sys.dic` and `matrix.bin` exist at `baseDic.location`.
- If `unknownDic.location` is omitted, `unk.csv` is loaded from `baseDic.location`. With `outputMode: ALL`, `dump.unknown` is also loaded from there.
- Confirm that the dictionary is a UTF-8 MeCab-format binary dictionary.
- Confirm that the left and right context IDs in `baseDic`'s `sys.dic` are within the ranges defined by `matrix.bin`.
- For each `additionalDic`, confirm that `location` is a directory containing `sys.dic` and that its context IDs are compatible with `baseDic`'s `matrix.bin`.
- Confirm that the context IDs in `unk.csv` are within the ranges defined by `matrix.bin`.
  - Also confirm that the costs are within the supported range.
- When `outputMode` is `ALL`, `dump.unknown` must contain at least an `o` definition.

## Feature Columns or Values Differ from Expectations

The feature format is determined by the system dictionary, not by JMorph. Match `dump.unknown` and `dump.userdic` to the system dictionary's feature format. If you need only surface forms, set `outputMode` to `SURFACE`.

## Tokens Are Not Concatenated as Expected

Check `concatMode` and the character types. `ALL` concatenates letters, Arabic numerals, kanji numerals, and katakana. It does not concatenate hiragana, kanji, or symbols. The katakana middle dot `・` is not treated as katakana.

## Request-time User Dictionary Persists

With the Java API, added entries remain active until `removeUserDicInRequest()` is called. Put the analysis in a `try` block and the removal in a `finally` block. When using a pool, remove the entries before returning the analyzer.

`JMorphAnalyzerJsonrpc` removes them internally at the end of the request.

## `next()` Returns an Empty List

- First call `analyze()` on the same `JMorphAnalyzer`.
- Set `analyzer.nbestNum` to at least 2.
- The requested number of candidates may not exist for some inputs.

## JSON-RPC Returns an Error

Check the property types and error codes in [JSON-RPC](./jsonrpc.md). In particular:

- `id` cannot be omitted.
- `jsonrpc` must be the string `"2.0"`.
- `method` must exactly match the configured value.
- `params.q` must be a string.
- `context.entries` must be an array of strings.

## Results Are Mixed Between Threads

`JMorphAnalyzer` is not thread-safe and must not be shared between threads. Create an instance for each thread, or use `JMorphAnalyzerPoolFactory` or `JMorphAnalyzerJsonrpc`.
