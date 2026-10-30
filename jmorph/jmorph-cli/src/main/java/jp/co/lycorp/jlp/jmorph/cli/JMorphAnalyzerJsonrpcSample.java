// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import jp.co.lycorp.jlp.jmorph.JMorphAnalyzerJsonrpc;
import jp.co.lycorp.jlp.jmorph.AnalyzerConfig;
import jp.co.lycorp.jlp.jmorph.ConcatMode;
import jp.co.lycorp.jlp.jmorph.DictionaryConfig;
import jp.co.lycorp.jlp.jmorph.JsonrpcConfig;
import jp.co.lycorp.jlp.jmorph.JMorphConfig;
import jp.co.lycorp.jlp.jmorph.MorphDicMecabConfig;
import jp.co.lycorp.jlp.jmorph.OutputMode;
import jp.co.lycorp.jlp.jmorph.UnknownDicBasicConfig;
import jp.co.lycorp.jlp.jmorph.UserDicInRequestProviderBasicConfig;

public class JMorphAnalyzerJsonrpcSample {
    private static final Logger LOGGER = Logger.getLogger(JMorphAnalyzerJsonrpcSample.class.getName());

    private static JMorphConfig makeConfig() {
        DictionaryConfig dictionaryConfig = DictionaryConfig.builder()
                // ベース辞書の指定
                .baseDic(new MorphDicMecabConfig("./models/mecab-unidic/data/"))
                // 追加辞書の指定
                // .additionalDic(new MorphDicMecabConfig(""))
                // .additionalDic(new MorphDicMecabConfig(""))
                .unknownDic(new UnknownDicBasicConfig(ConcatMode.ALL)) // 未知語辞書・連結処理に関する設定
                .userDicInRequestProvider(new UserDicInRequestProviderBasicConfig()) // リクエスト単位ユーザ辞書に関する設定
                .build();
        AnalyzerConfig analyzerConfig = AnalyzerConfig.builder()
                .dictionary(dictionaryConfig) // 辞書に関する設定
                .nbestNum(1) // Nbest解のNの指定
                .outputMode(OutputMode.ALL) // 出力に関する設定
                .build();
        JsonrpcConfig jsonrpcConfig = JsonrpcConfig.builder()
                .method("jmorph.tokenize") // method名の指定
                .maxObject(16) // マルチスレッド向けpoolでの最大オブジェクト数の指定
                .build();
        return JMorphConfig.builder()
                .description("")
                .analyzer(analyzerConfig)
                .jsonrpc(jsonrpcConfig)
                .build();
    }

    private static JMorphConfig makeConfigFromJson() {
        String json = "{"
                + "  \"description\":\"\","
                + "  \"analyzer\":{"
                + "    \"dictionary\":{"
                + "      \"baseDic\":{\"type\":\"mecab\",\"location\":\"./models/mecab-unidic/data/\"},"
                + "      \"additionalDic\":[],"
                + "      \"unknownDic\":{\"type\":\"basic\",\"concatMode\":\"ALL\"},"
                + "      \"userDicInRequestProvider\":{\"type\":\"basic\"}"
                + "    },"
                + "    \"nbestNum\":1,"
                + "    \"outputMode\":\"ALL\""
                + "  },"
                + "  \"jsonrpc\":{"
                + "    \"method\":\"jmorph.tokenize\","
                + "    \"maxObject\":16"
                + "  }"
                + "}";
        return JMorphConfig.fromJson(json);
    }

    public static void main(String[] args) {
        try {
            // 設定ファイルを読み込む場合
            JMorphConfig config = JMorphConfig.load(Path.of("config.json"));
            // メソッドで設定する場合
            // JMorphConfig config = makeConfig();
            // JSON文字列で設定する場合
            // JMorphConfig config = makeConfigFromJson();

            LOGGER.info(config.toString());

            // 解析例
            JMorphAnalyzerJsonrpc analyzer = new JMorphAnalyzerJsonrpc(config);

            File f = new File(args[0]); // 引数にjsonlファイルを指定
            BufferedReader br = new BufferedReader(new FileReader(f));

            String line = br.readLine();
            while (line != null) {
                // System.out.println(line);
                String result = analyzer.analyze(line);
                System.out.println(result);
                line = br.readLine();
            }
            br.close();
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Failed to run JMorphAnalyzerJsonRPCSample", e);
        }
    }
}
