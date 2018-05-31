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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.nd4j.linalg.dataset.DataSet;

/**
 * Identify anomalies using a trained autoencoder.
 * <p>
 * Both offsets and selftimes are fed into the neural network. Individual
 * callstack arrays are the inputs. The actual result used is a score between 0
 * and 1 (0 being more normal, and 1 being more abnormal).
 *
 * @author Christophe Bedard
 */
public class DeepAnomalyDetector extends CallStackAnomalyDetector {

    /** The path to the serialized model */
    private final String fModelFilePath;
    /** The anomaly threshold */
    private final double fAnomalyThreshold;

    /**
     * Constructor
     *
     * @param arrays
     *            the callstack arrays container
     * @param ssb
     *            the state system builder to use for results
     * @param modelFilePath
     *            the path to the serialized model
     * @param anomalyThreshold
     *            the anomaly threshold
     */
    public DeepAnomalyDetector(CallStackArrays arrays, ITmfStateSystemBuilder ssb, String modelFilePath, double anomalyThreshold) {
        super(arrays, ssb);
        fModelFilePath = modelFilePath;
        fAnomalyThreshold = anomalyThreshold;
    }

    @Override
    protected void detectAnomalies() throws IOException {
        long checkpoint = System.currentTimeMillis();

        // Get model from file
        MultiLayerNetwork net = ModelSerializer.restoreMultiLayerNetwork(new File(fModelFilePath));

        // Evaluate one by one, sequentially
        CallStackDataSetIterator iter = new CallStackDataSetIterator(fArrays, 1);

        // Keep minimum and maximum score
        double minScore = Double.MAX_VALUE;
        double maxScore = 0.0;

        // For each array
        while (iter.hasNext()) {
            // Get from file
            Pair<DataSet, CallStackArray> next = iter.nextWithInfo();
            CallStackArray array = next.getSecond();

            // Get score with NN
            double score = net.score(next.getFirst());

            // Add this score to state system
            addResultToStateSystem(array.getTimestamp(), array.getDuration(), array.getDepth(), score);

            // Update min/max score
            maxScore = Double.max(score, maxScore);
            minScore = Double.min(score, minScore);
        }

        iter.dispose();

        // Add min/max scores to state system
        addInfoToStateSystem(minScore, maxScore, fAnomalyThreshold);

        CallStackAnomalyAnalysis.debug.println("total eval=" + (System.currentTimeMillis() - checkpoint)); //$NON-NLS-1$
        CallStackAnomalyAnalysis.debug.println("end time=" + new SimpleDateFormat(CallStackAnomalyAnalysis.DATE_FORMAT).format(new Date())); //$NON-NLS-1$
    }
}
