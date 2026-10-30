# Core Concepts

This document explains the core concepts needed to understand JMorph configuration and APIs.

## Analysis Flow

JMorph is a dictionary-based morphological analyzer that returns analysis results using the least-cost method.
The analysis flow is as follows:

```text
Input string
  ↓
Generate candidates from the base and additional dictionaries, request-time user dictionary, and unknown words
  ↓
Find the optimal path through the lattice
  ↓
Concatenate tokens by character type and generate features
  ↓
TokenInfo list or JSON-RPC response
```

## API Differences

JMorph provides two APIs with different state-management and output behavior. See [Java API](./java_api.md) and [JSON-RPC](./jsonrpc.md) for detailed usage.

| Aspect | `JMorphAnalyzer` | `JMorphAnalyzerJsonrpc` |
|--|--|--|
| Request-time User Dictionary | Entries remain active until the caller invokes `removeUserDicInRequest()` | Entries apply only to the current request and are removed internally |
| N-best | Available by calling `next()` after `analyze()` | Not supported |
| Thread safety and pooling | Not thread-safe; the caller must use separate instances or manage a pool | Manages concurrent analysis through an internal analyzer pool |
| `EOS` | Included at the end of normal analysis results | Omitted from responses |

## Base Dictionary (`baseDic`)

The base dictionary serves as the foundation for analysis. JMorph currently supports UTF-8 MeCab-format binary dictionaries and uses `sys.dic` and `matrix.bin`.
Because part-of-speech systems, feature strings, cost scales, and other details vary between dictionaries, user dictionaries and configuration files must be prepared for the selected base dictionary.

Optional additional dictionaries add known-word candidates without replacing the base dictionary. They share the base dictionary's connection matrix, so their context IDs must be compatible with it.

## Unknown Words

When no matching word exists in the base dictionary, additional dictionaries, or request-time user dictionary, JMorph dynamically generates morphemes through unknown-word processing.
The currently implemented unknown-word processor generates candidates one character at a time. Unknown-word costs and connection information are defined in `unk.csv`, while output features are defined in `dump.unknown`.
Note that this behavior differs from MeCab's processing.

JMorph can also concatenate character sequences by character type after analysis. When `concatMode` is enabled, adjacent tokens of the same supported character type are concatenated whether they are known words or unknown words.
The features for the resulting morphemes are taken from `dump.unknown`.

| Value | Behavior |
|--|--|
| `NONE` | Do not concatenate tokens by character type |
| `ALL` | Concatenate letters, Arabic numerals, kanji numerals, and katakana by character type |
| `ALL_EXCEPT_KATAKANA` | Concatenate the character types covered by `ALL`, except katakana |

Hiragana, kanji, symbols, and other character types are not concatenated even with `ALL`. See [Dictionaries](./dictionary.md) for details about character types.

## Request-time User Dictionary

This feature is equivalent to the one provided at https://developer.yahoo.co.jp/webapi/jlp/ma/v2/user-dictionary.html.

It dynamically adds spellings that should be preferred for an analysis without rebuilding the dictionary when the analyzer is created. Each entry specifies a surface form and, optionally, a reading.

With the Java API, added entries remain active until `removeUserDicInRequest()` is called. With JSON-RPC, entries are specified in `params.context.entries` and apply only to that request.
With `OutputMode.ALL`, features are generated from `dump.userdic`. If that file does not exist, JMorph falls back to the unknown-word configuration.

## N-best

This feature returns not only the least-cost analysis result, but also alternative paths in ascending order of cost. Specify the number of candidates to retain with `analyzer.nbestNum`, then call `next()` after `analyze()` in the Java API.

N-best results are path candidates, so the requested number of distinct results may not always be available. The JSON-RPC API does not support returning N-best results.

## Output Modes

| Value | Contents of `TokenInfo` |
|--|--|
| `ALL` | Surface form and dictionary-derived features |
| `SURFACE` | Surface form only |

The number and meaning of features in `ALL` mode depend on the dictionary.

Java API analysis results contain a trailing `EOS` element, whereas JSON-RPC responses omit `EOS`.

## Thread Safety

`JMorphAnalyzer` cannot be shared between threads because it retains the previous lattice and the state of the request-time user dictionary. Create an instance for each thread, or use `JMorphAnalyzerPoolFactory` with Apache Commons Pool 2. `JMorphAnalyzerJsonrpc` maintains an internal analyzer pool.
