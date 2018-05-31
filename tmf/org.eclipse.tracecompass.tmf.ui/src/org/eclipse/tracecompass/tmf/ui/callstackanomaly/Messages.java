/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.callstackanomaly;

import org.eclipse.osgi.util.NLS;

/**
 * Message strings for callstack anomaly analysis config dialog.
 *
 * @author Christophe Bedard
 * @since 4.1
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.tmf.ui.callstackanomaly.messages"; //$NON-NLS-1$

    /** The config dialog shell text */
    public static String CallStackAnomalyAnalysis_DialogShellText;
    /** The config dialog title */
    public static String CallStackAnomalyAnalysis_DialogTitle;

    /** The default config dialog message */
    public static String CallStackAnomalyAnalysis_DialogMessage_Default;
    /** The specific config dialog message for statistical analysis */
    public static String CallStackAnomalyAnalysis_DialogMessage_Stat;
    /** The specific config dialog message for NN training */
    public static String CallStackAnomalyAnalysis_DialogMessage_NNTrain;
    /** The specific config dialog message for NN application */
    public static String CallStackAnomalyAnalysis_DialogMessage_NNApply;

    /** The tab text for statistical analysis */
    public static String CallStackAnomalyAnalysis_Tab_Stat;
    /** The tab text for NN training */
    public static String CallStackAnomalyAnalysis_Tab_NNTrain;
    /** The tab text for NN application */
    public static String CallStackAnomalyAnalysis_Tab_NNApply;

    /** The label for target depth selection */
    public static String CallStackAnomalyAnalysis_SelectionLabelTargetDepth;
    /** The info label for target depth selection */
    public static String CallStackAnomalyAnalysis_SelectionInfoLabelTargetDepth;
    /** The label for primitive arrays serialization option */
    public static String CallStackAnomalyAnalysis_SelectionLabelPrimitiveArrays;
    /** The info label for primitive arrays serialization option */
    public static String CallStackAnomalyAnalysis_SelectionInfoLabelPrimitiveArrays;

    /** The label for N value selection */
    public static String CallStackAnomalyAnalysis_SelectionLabelNValue;
    /** The info label for N value selection */
    public static String CallStackAnomalyAnalysis_SelectionInfoLabelNValue;
    /** The label for learning rate selection */
    public static String CallStackAnomalyAnalysis_SelectionLabelLearningRate;
    /** The info label for learning rate selection */
    public static String CallStackAnomalyAnalysis_SelectionInfoLabelLearningRate;
    /** The label for num epochs selection */
    public static String CallStackAnomalyAnalysis_SelectionLabelNumEpochs;
    /** The info label for num epochs selection */
    public static String CallStackAnomalyAnalysis_SelectionInfoLabelNumEpochs;
    /** The label for batch size selection */
    public static String CallStackAnomalyAnalysis_SelectionLabelBatchSize;
    /** The info label for batch size selection */
    public static String CallStackAnomalyAnalysis_SelectionInfoLabelBatchSize;
    /** The label for anomaly threshold selection */
    public static String CallStackAnomalyAnalysis_SelectionLabelAnomalyThreshold;
    /** The info label for anomaly threshold selection */
    public static String CallStackAnomalyAnalysis_SelectionInfoLabelAnomalyThreshold;

    /** The label for NN model export file */
    public static String CallStackAnomalyAnalysis_ExportFileLabel;
    /** The export file selection dialog text */
    public static String CallStackAnomalyAnalysis_ExportFile_DialogText;

    /** The label for NN model import file */
    public static String CallStackAnomalyAnalysis_ImportFileLabel;
    /** The import file selection dialog text */
    public static String CallStackAnomalyAnalysis_ImportFile_DialogText;

    /** The label for the model file import button */
    public static String CallStackAnomalyAnalysis_ImportFile;
    /** The label for the model file export button */
    public static String CallStackAnomalyAnalysis_ExportFile;

    static {
        // Initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
