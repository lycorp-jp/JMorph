// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DictionaryImpl implements Dictionary, UserDicInRequestHandler {
    private final List<MorphDic> morphDicList;
    private final CostMatrix costMatrix;
    private final UnknownDic unknownDic;
    private final UserDicInRequestProvider userDicInRequestProvider;
    private MorphDic userDicInRequestDic;

    private final Boolean outputFeatures;

    public DictionaryImpl() {
        this.morphDicList = null;
        this.costMatrix = null;
        this.unknownDic = null;
        this.userDicInRequestProvider = null;
        this.userDicInRequestDic = null;
        this.outputFeatures = true;
    }

    public DictionaryImpl(List<MorphDic> morphDicList, CostMatrix costMatrix, UnknownDic unknownDic,
            UserDicInRequestProvider userDicInRequestProvider, boolean outputFeatures) {
        this.morphDicList = new ArrayList<>();
        this.morphDicList.addAll(morphDicList);
        this.costMatrix = costMatrix;
        this.unknownDic = unknownDic;
        this.userDicInRequestProvider = userDicInRequestProvider;
        this.userDicInRequestDic = null;
        this.outputFeatures = outputFeatures;
    }

    public CostMatrix getCostMatrix() {
        return costMatrix;
    }

    public List<NodeImpl> lookup(byte[] sentence, int pos) {
        return lookup(sentence, pos, TimeoutGuard.disabled());
    }

    public List<NodeImpl> lookup(byte[] sentence, int pos, TimeoutGuard timeout) {
        List<NodeImpl> nodeList = new ArrayList<>();
        for (int i = 0; i < morphDicList.size(); ++i) {
            timeout.checkPeriodically();
            nodeList.addAll(morphDicList.get(i).lookup(sentence, pos, i, timeout));
        }
        if (nodeList.isEmpty()) {
            nodeList.add(unknownDic.unkNode(sentence, pos));
        }
        return nodeList;
    }

    public List<NodeImpl> concat(byte[] sentence, List<NodeImpl> nodeList) {
        return this.unknownDic.concat(sentence, nodeList);
    }

    public List<byte[][]> getAllFeatures(byte[] sentence, NodeImpl node) {
        List<byte[][]> morphList = new ArrayList<>();
        if (node.dictId >= 0) {
            MorphDic morphDic = morphDicList.get(node.dictId);
            if (morphDic.isPhrase(node)) {
                List<NodeImpl> phrase = morphDic.getPhrase(node);
                if (phrase != null && !phrase.isEmpty()) {
                    for (NodeImpl phraseNode : phrase) {
                        morphList.add(getMorph(sentence, phraseNode));
                    }
                    return morphList;
                }
            }
        }
        morphList.add(getMorph(sentence, node));
        return morphList;
    }

    public byte[][] getEos(NodeImpl eos) {
        ByteArrayOutputStream os = new ByteArrayOutputStream(128);
        byte[] surface = "EOS".getBytes(StandardCharsets.UTF_8);
        os.write(surface, 0, surface.length);
        List<byte[]> list = new ArrayList<>();
        list.add(os.toByteArray());
        return list.toArray(new byte[list.size()][]);
    }

    private byte[][] getMorph(byte[] sentence, NodeImpl node) {
        byte[] surface = Arrays.copyOfRange(
                sentence, node.loc, node.loc + node.length);
        List<byte[]> morph = new ArrayList<>(Collections.singletonList(surface));
        if (outputFeatures) {
            byte[][] features;
            features = (node.dictId < 0) ? unknownDic.getFeaturesByConfig(ByteUtils.toString(surface), null)
                    : morphDicList.get(node.dictId).getFeatures(node);
            morph.addAll(Arrays.asList(features));
        }
        return morph.toArray(new byte[morph.size()][]);
    }

    public void addUserDicInRequest(List<? extends UserDicEntry> userDicEntries) throws IOException {
        addUserDicInRequest(userDicEntries, TimeoutGuard.disabled());
    }

    public void addUserDicInRequest(
            List<? extends UserDicEntry> userDicEntries, TimeoutGuard timeout) throws IOException {
        if (userDicInRequestDic != null) {
            throw new IllegalStateException("Request-time user dictionary is already set");
        }
        MorphDic dictionary = userDicInRequestProvider.create(userDicEntries, timeout);
        userDicInRequestDic = dictionary;
        morphDicList.add(dictionary);
    }

    public void removeUserDicInRequest() {
        if (userDicInRequestDic == null) {
            throw new IllegalStateException("Request-time user dictionary is not set");
        }
        morphDicList.remove(userDicInRequestDic);
        userDicInRequestDic = null;
    }
}
