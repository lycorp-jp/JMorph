// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class JMorphAnalyzer implements Tokenizer, UserDicInRequestHandler {

    private final AnalyzerConfig config;
    private final Dictionary dictionary;
    private Lattice lattice;

    /**
     * Creates an analyzer with the given configuration.
     *
     * @param config analyzer configuration
     * @throws IOException if analyzer initialization fails
     */
    public JMorphAnalyzer(JMorphConfig config) throws IOException {
        this(config, new JMorphResourceLoaderImpl().load(config));
    }

    /**
     * Creates an analyzer with the given configuration and resource.
     *
     * @param config   analyzer configuration
     * @param resource analyzer resource
     * @throws IOException if analyzer initialization fails
     */
    public JMorphAnalyzer(JMorphConfig config, JMorphResource resource) throws IOException {
        this.config = config.getAnalyzerConfig();
        boolean outputFeatures = (this.config.getOutputMode() == OutputMode.ALL);
        this.dictionary = new DictionaryImpl(resource.getMorphDicList(), resource.getCostMatrix(),
                resource.getUnknownDic(), resource.getUserDicInRequestProvider(), outputFeatures);
        this.lattice = null;
    }

    /**
     * Analyzes a sentence as byte array.
     *
     * @param sentence input sentence bytes
     * @return analysis results
     * @throws TimeoutExceededException if the configured timeout is exceeded
     */
    public List<TokenInfo> analyze(byte[] sentence) {
        if (sentence.length == 0) {
            lattice = null;
            return Collections.emptyList();
        }

        TimeoutGuard timeout = TimeoutGuard.start(config.getTimeoutMillis());
        try {
            // build Lattice
            lattice = new Lattice(dictionary.getCostMatrix(), sentence);
            for (int i = 0; i < sentence.length; ++i) {
                timeout.checkPeriodically();
                if (lattice.get(i) != null) {
                    List<NodeImpl> nodes = dictionary.lookup(sentence, i, timeout);
                    timeout.checkPeriodically();
                    lattice.connect(nodes, timeout);
                }
            }
            lattice.finish(timeout);

            // build TokenInfo
            List<NodeImpl> nodeList = lattice.getBestPath(this.config.getNbestNum(), timeout);
            nodeList = dictionary.concat(lattice.getSentence(), nodeList);
            timeout.check();
            List<TokenInfo> resultList = new LinkedList<>();
            for (NodeImpl node : nodeList) {
                timeout.checkPeriodically();
                for (byte[][] allFeatures : dictionary.getAllFeatures(lattice.getSentence(), node)) {
                    timeout.checkPeriodically();
                    resultList.add(new TokenInfoImpl(allFeatures));
                }
            }
            resultList.add(new TokenInfoImpl(dictionary.getEos(lattice.getEos())));
            timeout.check();
            return resultList;
        } catch (TimeoutExceededException e) {
            lattice = null;
            throw e;
        }
    }

    /**
     * Analyzes a sentence as a String.
     *
     * @param sentence input sentence
     * @return analysis results
     * @throws TimeoutExceededException if the configured timeout is exceeded
     */
    public List<TokenInfo> analyze(String sentence) {
        return analyze(ByteUtils.toBytes(sentence));
    }

    /**
     * Returns the next-best analysis results.
     *
     * @return analysis results or empty if none
     * @throws TimeoutExceededException if the configured timeout is exceeded
     */
    public List<TokenInfo> next() {
        if (lattice == null) {
            return Collections.emptyList();
        }
        TimeoutGuard timeout = TimeoutGuard.start(config.getTimeoutMillis());
        try {
            List<NodeImpl> nodeList = lattice.getNextPath(timeout);
            timeout.check();
            if (nodeList.isEmpty()) {
                return Collections.emptyList();
            }
            nodeList = dictionary.concat(lattice.getSentence(), nodeList);
            timeout.check();
            List<TokenInfo> resultList = new LinkedList<>();
            for (NodeImpl node : nodeList) {
                timeout.checkPeriodically();
                for (byte[][] allFeatures : dictionary.getAllFeatures(lattice.getSentence(), node)) {
                    timeout.checkPeriodically();
                    resultList.add(new TokenInfoImpl(allFeatures));
                }
            }
            resultList.add(new TokenInfoImpl(dictionary.getEos(lattice.getEos())));
            timeout.check();
            return resultList;
        } catch (TimeoutExceededException e) {
            lattice = null;
            throw e;
        }
    }

    /**
     * Adds user dictionary entries for the current request.
     *
     * @param userDicEntries entries to add
     * @throws IOException if the entries cannot be loaded
     */
    public void addUserDicInRequest(List<? extends UserDicEntry> userDicEntries) throws IOException {
        TimeoutGuard timeout = TimeoutGuard.start(config.getTimeoutMillis());
        dictionary.addUserDicInRequest(userDicEntries, timeout);
    }

    /**
     * Removes user dictionary entries for the current request.
     */
    public void removeUserDicInRequest() {
        dictionary.removeUserDicInRequest();
    }
}
