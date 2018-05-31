/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.callstackanomaly;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.internal.lttng2.ust.core.callstackanomaly.CallStackAnomalyAnalysisParameters;
import org.eclipse.tracecompass.internal.lttng2.ust.core.callstackanomaly.CallStackAnomalyAnalysis.AnalysisType;
import org.eclipse.tracecompass.tmf.ui.dialog.TmfFileDialogFactory;

/**
 * Dialog for callstack anomaly analysis parameter configuration
 *
 * @author Christophe Bedard
 * @since 4.1
 */
public class CallStackAnomalyAnalysisConfigDialog extends TitleAreaDialog {

    private static final Logger LOGGER = TraceCompassLog.getLogger(CallStackAnomalyAnalysisConfigDialog.class);

    /** The analyses folder */
    private CTabFolder fTabFolder;
    /** The button for array primitive serialization */
    private Button fPrimitiveArraysButton;
    /** The spinner for target depth selection */
    private Spinner fTargetDepthSpinner;
    /** The spinner for N value selection */
    private Spinner fNValueSpinner;
    /** The spinner for learning rate */
    private Spinner fLearningRateSpinner;
    /** The spinner for number of epochs selection */
    private Spinner fNumEpochsSpinner;
    /** The spinner for batch size selection */
    private Spinner fBatchSizeSpinner;
    /** The spinner for anomaly threshold */
    private Spinner fAnomalyThresholdSpinner;

    /** The array primitive serialization parameter */
    private boolean fIsPrimitiveArrays = false;
    /** The target depth */
    private int fTargetDepth = 3;
    /** The N value */
    private int fNValue = 1;
    /** The file/location of the model (to train or apply) */
    private String fModelFilePath = null;
    /** The learning rate */
    private int fLearningRate = 50;
    /** The number of epochs */
    private int fNumEpochs = 1;
    /** The batch size */
    private int fBatchSize = 50;
    /** The anomaly threshold */
    private int fAnomalyThreshold = 10;

    /** The default folder tab */
    private static final int DEFAULT_FOLDER_TAB = 0;

    /** The default array primitive serialization choice */
    private static final boolean SERIAL_ARRAYS_PRIMITIVES_DEFAULT = false;
    /** The minimum target depth */
    private static final int TARGET_DEPTH_MIN = 1;
    /** The maximum target depth */
    private static final int TARGET_DEPTH_MAX = 100;
    /** The minimum N value */
    private static final int N_VALUE_MIN = 0;
    /** The maximum N value */
    private static final int N_VALUE_MAX = 100;
    /** The number of digits for learning rate selection */
    private static final int LEARNING_RATE_DIGITS = 3;
    /** The minimum learning rate */
    private static final int LEARNING_RATE_MIN = 1;
    /** The maximum learning rate */
    private static final int LEARNING_RATE_MAX = 1000000;
    /** The learning rate increment */
    private static final int LEARNING_RATE_INCREMENT = 1;
    /** The minimum number of epochs */
    private static final int NUM_EPOCHS_MIN = 1;
    /** The maximum number of epochs */
    private static final int NUM_EPOCHS_MAX = 100;
    /** The minimum batch size */
    private static final int BATCH_SIZE_MIN = 1;
    /** The maximum batch size */
    private static final int BATCH_SIZE_MAX = 1000;
    /** The batch size increment */
    private static final int BATCH_SIZE_INCREMENT = 10;
    /** The number of digits for anomaly threshold selection */
    private static final int ANOMALY_THRESHOLD_DIGITS = 2;
    /** The minimum anomaly threshold */
    private static final int ANOMALY_THRESHOLD_MIN = 0;
    /** The maximum anomaly threshold */
    private static final int ANOMALY_THRESHOLD_MAX = 100;
    /** The anomaly threshold increment */
    private static final int ANOMALY_THRESHOLD_INCREMENT = 1;

    private static final String[] FILTER_EXTENSIONS = new String[] { "*.zip", "*" }; //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Constructor
     *
     * @param parentShell
     *            the parent shell
     */
    public CallStackAnomalyAnalysisConfigDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    public void create() {
        super.create();
        getShell().setText(Messages.CallStackAnomalyAnalysis_DialogShellText);
        setTitle(Messages.CallStackAnomalyAnalysis_DialogTitle);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite mainComposite = (Composite) super.createDialogArea(parent);
        mainComposite.setLayout(new GridLayout());

        // Common parameters
        Composite commonParameters = new Composite(mainComposite, SWT.NONE);
        commonParameters.setLayout(new GridLayout(3, false));
        Label targetDepthLabel = new Label(commonParameters, SWT.NONE);
        targetDepthLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        targetDepthLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionLabelTargetDepth);
        fTargetDepthSpinner = new Spinner(commonParameters, SWT.NONE);
        fTargetDepthSpinner.setMinimum(TARGET_DEPTH_MIN);
        fTargetDepthSpinner.setMaximum(TARGET_DEPTH_MAX);
        fTargetDepthSpinner.setSelection(fTargetDepth);
        Label targetDepthInfoLabel = new Label(commonParameters, SWT.NONE);
        targetDepthInfoLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        targetDepthInfoLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionInfoLabelTargetDepth);
        Label primitiveArraysLabel = new Label(commonParameters, SWT.NONE);
        primitiveArraysLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        primitiveArraysLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionLabelPrimitiveArrays);
        fPrimitiveArraysButton = new Button(commonParameters, SWT.CHECK);
        fPrimitiveArraysButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        fPrimitiveArraysButton.setSelection(SERIAL_ARRAYS_PRIMITIVES_DEFAULT);
        Label primitiveArraysInfoLabel = new Label(commonParameters, SWT.NONE);
        primitiveArraysInfoLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        primitiveArraysInfoLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionInfoLabelPrimitiveArrays);

        // Analyses folder
        Group analysesGroup = new Group(mainComposite, SWT.NONE);
        analysesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        analysesGroup.setLayout(new GridLayout());
        fTabFolder = new CTabFolder(analysesGroup, SWT.NONE);
        fTabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
        fTabFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                switch (getSelectedAnalysisType()) {
                case STATISTICAL:
                    setMessage(Messages.CallStackAnomalyAnalysis_DialogMessage_Stat);
                    break;
                case NN_TRAIN:
                    setMessage(Messages.CallStackAnomalyAnalysis_DialogMessage_NNTrain);
                    break;
                case NN_APPLY:
                    setMessage(Messages.CallStackAnomalyAnalysis_DialogMessage_NNApply);
                    break;
                default:
                    setMessage(Messages.CallStackAnomalyAnalysis_DialogMessage_Default);
                    break;
                }
            }
        });

        // Tab: statistical
        CTabItem statsTab = new CTabItem(fTabFolder, SWT.NONE);
        statsTab.setText(Messages.CallStackAnomalyAnalysis_Tab_Stat);
        Composite statsTabComposite = new Composite(fTabFolder, SWT.NONE);
        statsTabComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true));
        statsTabComposite.setLayout(new GridLayout(3, false));
        statsTab.setControl(statsTabComposite);
        Label nValueLabel = new Label(statsTabComposite, SWT.NONE);
        nValueLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        nValueLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionLabelNValue);
        fNValueSpinner = new Spinner(statsTabComposite, SWT.NONE);
        fNValueSpinner.setMinimum(N_VALUE_MIN);
        fNValueSpinner.setMaximum(N_VALUE_MAX);
        fNValueSpinner.setSelection(fNValue);
        Label nValueInfoLabel = new Label(statsTabComposite, SWT.NONE);
        nValueInfoLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        nValueInfoLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionInfoLabelNValue);

        // Tab: NN train
        CTabItem nnTrainTab = new CTabItem(fTabFolder, SWT.NONE);
        nnTrainTab.setText(Messages.CallStackAnomalyAnalysis_Tab_NNTrain);
        Composite trainTabComposite = new Composite(fTabFolder, SWT.NONE);
        trainTabComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true));
        trainTabComposite.setLayout(new GridLayout(3, false));
        nnTrainTab.setControl(trainTabComposite);
        Label learningRateLabel = new Label(trainTabComposite, SWT.NONE);
        learningRateLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        learningRateLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionLabelLearningRate);
        fLearningRateSpinner = new Spinner(trainTabComposite, SWT.NONE);
        fLearningRateSpinner.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        fLearningRateSpinner.setDigits(LEARNING_RATE_DIGITS);
        fLearningRateSpinner.setMinimum(LEARNING_RATE_MIN);
        fLearningRateSpinner.setMaximum(LEARNING_RATE_MAX);
        fLearningRateSpinner.setSelection(fLearningRate);
        fLearningRateSpinner.setIncrement(LEARNING_RATE_INCREMENT);
        Label learningRateInfoLabel = new Label(trainTabComposite, SWT.NONE);
        learningRateInfoLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        learningRateInfoLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionInfoLabelLearningRate);
        Label nEpochsLabel = new Label(trainTabComposite, SWT.NONE);
        nEpochsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        nEpochsLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionLabelNumEpochs);
        fNumEpochsSpinner = new Spinner(trainTabComposite, SWT.NONE);
        fNumEpochsSpinner.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        fNumEpochsSpinner.setMinimum(NUM_EPOCHS_MIN);
        fNumEpochsSpinner.setMaximum(NUM_EPOCHS_MAX);
        fNumEpochsSpinner.setSelection(fNumEpochs);
        Label nEpochsInfoLabel = new Label(trainTabComposite, SWT.NONE);
        nEpochsInfoLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        nEpochsInfoLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionInfoLabelNumEpochs);
        Label batchSizeLabel = new Label(trainTabComposite, SWT.NONE);
        batchSizeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        batchSizeLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionLabelBatchSize);
        fBatchSizeSpinner = new Spinner(trainTabComposite, SWT.NONE);
        fBatchSizeSpinner.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        fBatchSizeSpinner.setMinimum(BATCH_SIZE_MIN);
        fBatchSizeSpinner.setMaximum(BATCH_SIZE_MAX);
        fBatchSizeSpinner.setSelection(fBatchSize);
        fBatchSizeSpinner.setIncrement(BATCH_SIZE_INCREMENT);
        Label batchSizeInfoLabel = new Label(trainTabComposite, SWT.NONE);
        batchSizeInfoLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        batchSizeInfoLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionInfoLabelBatchSize);
        Label trainFileLabel = new Label(trainTabComposite, SWT.NONE);
        trainFileLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        trainFileLabel.setText(Messages.CallStackAnomalyAnalysis_ExportFileLabel);
        Button trainFileButton = new Button(trainTabComposite, SWT.PUSH);
        trainFileButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        trainFileButton.setText(Messages.CallStackAnomalyAnalysis_ExportFile);
        Combo trainFileLocation = new Combo(trainTabComposite, SWT.SINGLE | SWT.BORDER);
        trainFileLocation.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
        trainFileLocation.setEnabled(false);
        trainFileButton.addSelectionListener(getButtonFileSelectionAdapter(Messages.CallStackAnomalyAnalysis_ExportFile_DialogText, trainFileLocation, trainTabComposite, SWT.SAVE));

        // Tab: NN apply
        CTabItem nnApplyTab = new CTabItem(fTabFolder, SWT.NONE);
        nnApplyTab.setText(Messages.CallStackAnomalyAnalysis_Tab_NNApply);
        Composite applyTabComposite = new Composite(fTabFolder, SWT.NONE);
        applyTabComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true));
        applyTabComposite.setLayout(new GridLayout(3, false));
        nnApplyTab.setControl(applyTabComposite);
        Label anomalyThresholdLabel = new Label(applyTabComposite, SWT.NONE);
        anomalyThresholdLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        anomalyThresholdLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionLabelAnomalyThreshold);
        fAnomalyThresholdSpinner = new Spinner(applyTabComposite, SWT.NONE);
        fAnomalyThresholdSpinner.setDigits(ANOMALY_THRESHOLD_DIGITS);
        fAnomalyThresholdSpinner.setMinimum(ANOMALY_THRESHOLD_MIN);
        fAnomalyThresholdSpinner.setMaximum(ANOMALY_THRESHOLD_MAX);
        fAnomalyThresholdSpinner.setSelection(fAnomalyThreshold);
        fAnomalyThresholdSpinner.setIncrement(ANOMALY_THRESHOLD_INCREMENT);
        Label anomalyThresholdInfoLabel = new Label(applyTabComposite, SWT.NONE);
        anomalyThresholdInfoLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        anomalyThresholdInfoLabel.setText(Messages.CallStackAnomalyAnalysis_SelectionInfoLabelAnomalyThreshold);
        Label applyFileLabel = new Label(applyTabComposite, SWT.NONE);
        applyFileLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        applyFileLabel.setText(Messages.CallStackAnomalyAnalysis_ImportFileLabel);
        Button applyFileButton = new Button(applyTabComposite, SWT.PUSH);
        applyFileButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        applyFileButton.setText(Messages.CallStackAnomalyAnalysis_ImportFile);
        Combo applyFileLocation = new Combo(applyTabComposite, SWT.SINGLE | SWT.BORDER);
        applyFileLocation.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
        applyFileLocation.setEnabled(false);
        applyFileButton.addSelectionListener(getButtonFileSelectionAdapter(Messages.CallStackAnomalyAnalysis_ImportFile_DialogText, applyFileLocation, applyTabComposite, SWT.OPEN));

        // Default tab selection
        fTabFolder.setSelection(DEFAULT_FOLDER_TAB);
        fTabFolder.notifyListeners(SWT.Selection, new Event());

        return mainComposite;
    }

    private SelectionAdapter getButtonFileSelectionAdapter(String dialogText, Combo combo, Composite composite, int style) {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = TmfFileDialogFactory.create(Display.getCurrent().getActiveShell(), style);
                dialog.setText(dialogText);
                dialog.setFilterExtensions(FILTER_EXTENSIONS);
                fModelFilePath = dialog.open();
                if (fModelFilePath != null) {
                    combo.setText(fModelFilePath);
                    composite.layout();
                }
            }
        };
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    public boolean isHelpAvailable() {
        return false;
    }

    @Override
    protected void okPressed() {
        saveParameters();
        // Quick and easy way to validate parameters;
        // forces cancelPressed() instead if not valid
        if (isValidParameters()) {
            super.okPressed();
        } else {
            LOGGER.log(Level.WARNING, "Invalid callstack anomaly analysis parameters; cancelling"); //$NON-NLS-1$
            super.cancelPressed();
        }
    }

    /**
     * @return true if the parameters are valid, given the selected analysis
     */
    private boolean isValidParameters() {
        boolean isValidParams = false;
        if (getSelectedAnalysisType() == AnalysisType.STATISTICAL) {
            // No checks needed
            isValidParams = true;
        } else {
            // Make sure a file was selected
            isValidParams = (fModelFilePath != null);
        }
        return isValidParams;
    }

    /**
     * Save the parameters by copying them from their corresponding spinners
     */
    private void saveParameters() {
        fIsPrimitiveArrays = fPrimitiveArraysButton.getSelection();
        fTargetDepth = fTargetDepthSpinner.getSelection();
        fNValue = fNValueSpinner.getSelection();
        fLearningRate = fLearningRateSpinner.getSelection();
        fNumEpochs = fNumEpochsSpinner.getSelection();
        fBatchSize = fBatchSizeSpinner.getSelection();
        fAnomalyThreshold = fAnomalyThresholdSpinner.getSelection();
    }

    /**
     * @return the parameters
     */
    public @NonNull CallStackAnomalyAnalysisParameters getParameters() {
        return new CallStackAnomalyAnalysisParameters(
                fIsPrimitiveArrays,
                getSelectedAnalysisType(),
                fTargetDepth,
                fNValue,
                fModelFilePath,
                fLearningRate * Math.pow(10, -LEARNING_RATE_DIGITS),
                fNumEpochs,
                fBatchSize,
                fAnomalyThreshold * Math.pow(10, -ANOMALY_THRESHOLD_DIGITS));
    }

    private AnalysisType getSelectedAnalysisType() {
        return AnalysisType.values()[fTabFolder.getSelectionIndex()];
    }
}
