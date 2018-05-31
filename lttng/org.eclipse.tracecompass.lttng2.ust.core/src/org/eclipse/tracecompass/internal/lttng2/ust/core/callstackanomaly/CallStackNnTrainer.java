/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.ust.core.callstackanomaly;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.AdaGrad;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Class for NN model training
 *
 * @author Christophe Bedard
 */
public class CallStackNnTrainer {

    private static final int RANDOM_SEED = 12345;

    /**
     * Train model
     *
     * @param arrays
     *            the callstack arrays container
     * @param modelFilePath
     *            the path and filename to save the model to
     * @param learningRate
     *            the learning rate
     * @param numEpochs
     *            the number of epochs
     * @param batchSize
     *            the batch size
     */
    public static void train(CallStackArrays arrays, String modelFilePath, double learningRate, int numEpochs, int batchSize) {
        // Get input size from arrays
        CallStackDataSetIterator iter = new CallStackDataSetIterator(arrays, batchSize);
        final int inputSize = arrays.getArraysSize();

        // Create model
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(RANDOM_SEED)
                .weightInit(WeightInit.XAVIER)
                .updater(new AdaGrad(learningRate))
                .activation(Activation.RELU)
                .l2(0.00001)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(inputSize).nOut(15)
                        .build())
                .layer(1, new DenseLayer.Builder().nIn(15).nOut(5)
                        .build())
                .layer(2, new DenseLayer.Builder().nIn(5).nOut(15)
                        .build())
                .layer(3, new OutputLayer.Builder().nIn(15).nOut(inputSize)
                        .lossFunction(LossFunctions.LossFunction.MSE)
                        .build())
                .pretrain(false).backprop(true)
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.setListeners(Collections.singletonList(new ScoreIterationListener(Integer.MAX_VALUE)));

        // Train
        long checkpoint = System.currentTimeMillis();
        for (int epoch = 0; epoch < numEpochs; epoch++) {
            while (iter.hasNext()) {
                net.fit(iter.next());
            }
            iter.reset();
            System.out.println("Epoch " + epoch + " complete"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        CallStackAnomalyAnalysis.debug.println("total training=" + (System.currentTimeMillis() - checkpoint)); //$NON-NLS-1$

        iter.dispose();

        // Write model to file
        try {
            net.save(new File(modelFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
