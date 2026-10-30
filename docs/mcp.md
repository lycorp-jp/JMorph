# JMorph MCP Server

JMorph provides a stdio-based Model Context Protocol (MCP) server through `JMorphAnalyzerMCP`. After registering it with an MCP client, the client can use JMorph for Japanese morphological analysis.

The MCP client discovers the tool and its input and output schemas automatically. You do not need to construct MCP or JSON-RPC requests manually.

## Preparation

Build the `jmorph-cli` JAR and the `jmorph-lib` fat JAR:

```console
./gradlew :jmorph:jmorph-cli:jar :jmorph:jmorph-lib:shadowJar
```

Prepare a JMorph configuration JSON file for the dictionary and analysis settings. See [Configuration](./configuration.md) for the available properties and [Dictionaries](./dictionary.md) for the required dictionary files.

The MCP tool name is `jmorph.tokenize`. Omit `jsonrpc.method` from the configuration to use this default, or set it explicitly to `jmorph.tokenize`.

## Client Configuration

Configure your MCP client to start `JMorphAnalyzerMCP`. For example, the following configuration can be used as a template for Codex; replace the placeholders with local paths:

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

The classpath separator is `:` on macOS and Linux and `;` on Windows. `<path-to-config-json>` must point to the JMorph configuration JSON file prepared above.

Restart or reload the MCP client after changing its configuration. The client should then discover the `jmorph.tokenize` tool automatically.

## Usage

Ask the MCP client to analyze Japanese text with JMorph. The client selects the tool and supplies its arguments, so users do not need to know the MCP message format.

Each analysis is limited to 10 seconds by default. Configure the limit with `analyzer.timeoutMillis` in the JMorph configuration file, or set it to `0` to disable the timeout. See [Configuration](./configuration.md#analyzertimeoutmillis) for details.

The server limits each line-delimited MCP message to 1 MiB (1,048,576 bytes) by default. A message that exceeds the limit is rejected and the server stops reading input, preventing an unterminated message from consuming memory without bound.

Set a limit appropriate for the available memory and expected analysis input with the `JMORPH_MCP_MAX_MESSAGE_BYTES` environment variable:

```bash
JMORPH_MCP_MAX_MESSAGE_BYTES=2097152
```

Alternatively, set the `jmorph.mcp.maxMessageBytes` Java system property. The system property takes precedence over the environment variable:

```bash
java -Djmorph.mcp.maxMessageBytes=2097152 ...
```

The value is the maximum encoded message size in bytes and must be a positive integer. Keep application-level input validation in place when input can be supplied by an application user.

JMorph also supports request-time user dictionary entries through the MCP tool. See [Request-time User Dictionary](./jsonrpc.md#request-time-user-dictionary) for the entry format and behavior.

## Debugging

The server writes startup messages and errors to standard error. Enable additional debug logs with the `JMORPH_MCP_DEBUG` environment variable:

```bash
JMORPH_MCP_DEBUG=true
```

Alternatively, set the `jmorph.mcp.debug` Java system property:

```bash
java -Djmorph.mcp.debug=true ...
```

Debug logs are written to standard error and do not interfere with MCP messages on standard output. Logged messages longer than 2,000 characters are truncated.
