package org.aiwolf.liuchang;

import java.io.IOException;
import java.util.Map;
import java.util.Arrays;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;
import ai.onnxruntime.OrtSession.SessionOptions;
import ai.onnxruntime.OrtSession.SessionOptions.OptLevel;
import ai.onnxruntime.OnnxValue;

public class ONNX {
    public static void main(String args[]) throws OrtException, IOException {
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        OrtSession session = env.createSession("../../../../CNNLSTM_0625170355.onnx",new OrtSession.SessionOptions());
        
        float[][][][][] sourceArray = new float[1][14][8][15][15];
        // System.out.println(sourceArray[0][0][0][0][1]);
        OnnxTensor tensorFromArray = OnnxTensor.createTensor(env,sourceArray);

        long startTime = System.currentTimeMillis();

        Map<String, OnnxTensor> inputs = Map.of("modelInput", tensorFromArray);
        try (var results = session.run(inputs)) {
            float[][][] res = (float[][][]) results.get(0).getValue();
            System.out.println(Arrays.deepToString(res[0]));
            // System.out.println(res.getValue().getClass().getName());
        }

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        System.out.println(duration);
    }
}
