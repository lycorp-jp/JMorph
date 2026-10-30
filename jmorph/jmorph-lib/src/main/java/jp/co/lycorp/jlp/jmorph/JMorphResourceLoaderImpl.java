// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.IOException;
import java.util.ArrayList;

public class JMorphResourceLoaderImpl implements JMorphResourceLoader {

    public JMorphResource load(JMorphConfig config) throws IOException {
        boolean outputFeatures = config.getAnalyzerConfig().getOutputMode() == OutputMode.ALL;
        String baseLocation = null;
        ArrayList<MorphDic> morphDicList;
        CostMatrix costMatrix;
        UnknownDic unknownDic;
        UserDicInRequestProvider userDicInRequestProvider;
        String dumpUnknownLocation;

        DictionaryConfig dictionaryConfig = config.getAnalyzerConfig().getDictionary();
        // baseDic
        if (dictionaryConfig.baseDic() instanceof MorphDicMecabConfig) {
            MorphDicMecabConfig c = (MorphDicMecabConfig) dictionaryConfig.baseDic();
            baseLocation = c.getLocation();

            String matrixLocation = FileUtils.resolve(c.getLocation(), "matrix.bin");
            costMatrix = new CostMatrixMecab(matrixLocation);

            morphDicList = new ArrayList<>();
            morphDicList.add(new MorphDicMecab(c.getLocation(), outputFeatures, costMatrix));
        } else {
            throw new RuntimeException("Unable to resolve baseDic type");
        }

        if (baseLocation == null) {
            throw new RuntimeException("Unable to resolve location");
        }

        // additionalDic
        for (MorphDicConfig additionalDicConfig : dictionaryConfig.additionalDic()) {
            if (additionalDicConfig instanceof MorphDicMecabConfig) {
                MorphDicMecabConfig c = (MorphDicMecabConfig) additionalDicConfig;
                morphDicList.add(new MorphDicMecab(c.getLocation(), outputFeatures, costMatrix));
            } else {
                throw new RuntimeException("Unable to resolve additionalDic type");
            }
        }

        // unknownDic
        if (dictionaryConfig.unknownDic() instanceof UnknownDicBasicConfig) {
            UnknownDicBasicConfig c = (UnknownDicBasicConfig) dictionaryConfig.unknownDic();
            String location = (c.getLocation() != null) ? c.getLocation() : baseLocation;
            String unkCsvLocation = FileUtils.resolve(location, "unk.csv");
            dumpUnknownLocation = FileUtils.resolve(location, "dump.unknown");
            unknownDic = new UnknownDicBasic(unkCsvLocation, dumpUnknownLocation, outputFeatures, c);
        } else {
            throw new RuntimeException("Unable to resolve unknownDic type");
        }

        // userDicInRequestProvider
        if (dictionaryConfig.userDicInRequestProvider() instanceof UserDicInRequestProviderBasicConfig) {
            UserDicInRequestProviderBasicConfig c = (UserDicInRequestProviderBasicConfig) dictionaryConfig
                    .userDicInRequestProvider();
            String location = (c.getLocation() != null) ? c.getLocation() : baseLocation;
            String delim = (c.getDelim() != null) ? c.getDelim() : " ";
            String dumpUserDicLocation = FileUtils.resolve(location, "dump.userdic");
            userDicInRequestProvider = new UserDicInRequestProviderBasic(
                    dumpUserDicLocation, dumpUnknownLocation, outputFeatures, delim,
                    costMatrix.getUserDicEntriesIndex(), morphDicList.get(0), unknownDic);
        } else {
            throw new RuntimeException("Unable to resolve userDicInRequestProvider type");
        }

        return new JMorphResource(morphDicList, costMatrix, unknownDic, userDicInRequestProvider);
    }
}
