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
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackAnalysis;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.ScopeLog;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph.CallGraphAnalysis;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph.CalledFunction;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.internal.dl4tc.dl4j.Dl4jUtils;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

/**
 * Analysis module for callstack anomaly detection
 *
 * @author Christophe Bedard
 */
public class CallStackAnomalyAnalysisProvider extends AbstractTmfStateProvider {

    /** The parameters */
    private final CallStackAnomalyAnalysisParameters fParameters;

    /** The callstack arrays container and serializer */
    private CallStackArrays fArrays;

    private @NonNull IProgressMonitor fMonitor;
    private boolean fHandled = false;
    private ITmfStateSystemBuilder ss;

    /** Attribute for information list */
    public static final String ATTRIBUTE_INFO = "Info"; //$NON-NLS-1$
    /** Attribute for min score */
    public static final String ATTRIBUTE_MIN = "min"; //$NON-NLS-1$
    /** Attribute for max score */
    public static final String ATTRIBUTE_MAX = "max"; //$NON-NLS-1$
    /** Attribute for threshold */
    public static final String ATTRIBUTE_THRESHOLD = "threshold"; //$NON-NLS-1$
    /** Attribute for results */
    public static final String ATTRIBUTE_RESULTS = "Results"; //$NON-NLS-1$

    private static final int WORK_DL4J_LOAD = 1;
    private static final int WORK_METADATA = 1;
    private static final int WORK_ARRAYS = 2;
    private static final int WORK_ANALYSIS = 3;
    private static final int WORK_TOTAL = WORK_DL4J_LOAD + WORK_METADATA + WORK_ARRAYS + WORK_ANALYSIS + 1;

    /**
     * Constructor
     * <p>
     * Set the analysis type, pass the needed parameters, and set the rest to null.
     *
     * @param trace
     *            the trace
     * @param parameters
     *            the analysis parameters
     * @param monitor
     *            the monitor (from the analysis)
     */
    public CallStackAnomalyAnalysisProvider(@NonNull ITmfTrace trace, CallStackAnomalyAnalysisParameters parameters, @NonNull IProgressMonitor monitor) {
        super(trace, CallStackAnomalyAnalysis.ID);
        fMonitor = monitor;
        fParameters = parameters;
        logParameters();

        // Delete state system supplementary file if it exists, otherwise the analysis
        // cannot be executed again
        File supFile = new File(getSupplementaryFilesPath() + CallStackAnomalyAnalysis.class.getName() + ".ht"); //$NON-NLS-1$
        if (supFile.exists()) {
            supFile.delete();
        }
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        if (fHandled) {
            return;
        }

        ss = getStateSystemBuilder();
        if (ss == null) {
            return;
        }

        executeAnalysis();
        fHandled = true;
    }

    /**
     * Execute analysis tasks
     *
     * @return true if successful, false otherwise
     */
    private boolean executeAnalysis() {
        try (ScopeLog sl = new ScopeLog(CallStackAnomalyAnalysis.LOGGER, Level.CONFIG, "callstack anomaly analysis")) { //$NON-NLS-1$
            CallStackAnomalyAnalysis.debug.println("begin analysis time=" + new SimpleDateFormat(CallStackAnomalyAnalysis.DATE_FORMAT).format(new Date())); //$NON-NLS-1$

            fMonitor.beginTask(CallStackAnomalyAnalysis.ANALYSIS_DESCRIPTION, WORK_TOTAL);

            loadDl4j();

            if (fMonitor.isCanceled()) {
                return false;
            }

            // Initialize callstack arrays container
            fArrays = new CallStackArrays(getSupplementaryFilesPath());

            // Generate the arrays if necessary
            if (!fArrays.exists()) {
                boolean isArraysGenSuccessful = generateArrays();
                if (!isArraysGenSuccessful) {
                    // Abort and cleanup
                    fArrays.dispose();
                    return false;
                }
            } else {
                fMonitor.worked(WORK_METADATA + WORK_ARRAYS);
            }

            if (fMonitor.isCanceled()) {
                return false;
            }

            // Perform given analysis type
            performAnalysis();

            CallStackAnomalyAnalysis.debug.println("end analysis time=" + new SimpleDateFormat(CallStackAnomalyAnalysis.DATE_FORMAT).format(new Date())); //$NON-NLS-1$
            CallStackAnomalyAnalysis.debug.println();
        }

        return true;
    }

    private void performAnalysis() {
        switch (fParameters.getSelectedAnalysisType()) {
        default:
        case STATISTICAL:
            try (ScopeLog sl = new ScopeLog(CallStackAnomalyAnalysis.LOGGER, Level.CONFIG, "statistical analysis")) { //$NON-NLS-1$
                performStatisticalAnalysis();
            }
            break;
        case NN_TRAIN:
            try (ScopeLog sl = new ScopeLog(CallStackAnomalyAnalysis.LOGGER, Level.CONFIG, "NN training")) { //$NON-NLS-1$
                performNnTraining();
            }
            break;
        case NN_APPLY:
            try (ScopeLog sl = new ScopeLog(CallStackAnomalyAnalysis.LOGGER, Level.CONFIG, "NN detection")) { //$NON-NLS-1$
                performNnDetection();
            }
            break;
        }
        fMonitor.worked(WORK_ANALYSIS);
    }

    private void performStatisticalAnalysis() {
        fMonitor.subTask("applying statistical anomaly detection"); //$NON-NLS-1$
        CallStackAnomalyDetector anomalyDetector = new StatisticalAnomalyDetector(
                fArrays,
                ss,
                fParameters.getNValue());
        anomalyDetector.apply();
    }

    private void performNnTraining() {
        fMonitor.subTask("training NN model"); //$NON-NLS-1$
        CallStackNnTrainer.train(
                fArrays,
                fParameters.getModelFilePath(),
                fParameters.getLearningRate(),
                fParameters.getNumEpochs(),
                fParameters.getBatchSize());
    }

    private void performNnDetection() {
        fMonitor.subTask("applying NN anomaly detection"); //$NON-NLS-1$
        CallStackAnomalyDetector anomalyDetector = new DeepAnomalyDetector(
                fArrays,
                ss,
                fParameters.getModelFilePath(),
                fParameters.getAnomalyThreshold());
        anomalyDetector.apply();
    }

    /**
     * Get callstack segment store and generate arrays needed for actual analysis
     *
     * @return true if successful, false otherwise
     */
    private boolean generateArrays() {
        try (ScopeLog sl = new ScopeLog(CallStackAnomalyAnalysis.LOGGER, Level.CONFIG, "arrays generation")) { //$NON-NLS-1$
            // Get callstack analysis
            fMonitor.subTask("acquiring callstack analysis results"); //$NON-NLS-1$
            ITmfTrace trace = getTrace();
            Iterable<CallStackAnalysis> callstackAnalyses = TmfTraceUtils.getAnalysisModulesOfClass(trace, CallStackAnalysis.class);
            @Nullable
            CallStackAnalysis callstackAnalysis = Iterables.getFirst(callstackAnalyses, null);
            if (callstackAnalysis == null) {
                return false;
            }

            // Make sure it is done
            callstackAnalysis.schedule();
            callstackAnalysis.waitForCompletion();
            // Seems necessary, otherwise the returned segment store is null
            ((CallGraphAnalysis) callstackAnalysis.getCallGraph()).waitForCompletion();

            ISegmentStore<@NonNull ISegment> callstack = callstackAnalysis.getSegmentStore();
            if (callstack == null) {
                return false;
            }

            generateArraysFile(callstack);
        }
        return true;
    }

    // ------------------------------------------------------------------------
    // Callstack arrays generation methods
    // ------------------------------------------------------------------------

    /**
     * Process trace, generate callstack arrays, and write them to a file
     *
     * @param callstack
     *            the callstack, as a segment store
     */
    private void generateArraysFile(ISegmentStore<@NonNull ISegment> callstack) {
        long checkpoint;

        // Only use calls at a target depth as sub-root calls for the moment
        fMonitor.subTask("filtering"); //$NON-NLS-1$
        Iterable<@NonNull ISegment> rootCalls = Iterables.filter(callstack, (segment) -> {
            return segment instanceof ICalledFunction && ((ICalledFunction) segment).getDepth() == fParameters.getTargetDepth();
        });

        // Compute metadata
        fMonitor.subTask("gathering callstack information"); //$NON-NLS-1$
        checkpoint = System.currentTimeMillis();
        Pair<SortedMap<Long, Integer>, Integer> metadata = getMetadata(rootCalls);
        // Max number of calls for each address at any depth
        SortedMap<Long, Integer> maxNumberOfCalls = metadata.getFirst();
        // Number of rows
        int depthSize = metadata.getSecond();
        // Number of columns (sum of the maximum number of calls for each address)
        int addressSize = maxNumberOfCalls.values().stream().mapToInt(Integer::intValue).sum();
        CallStackAnomalyAnalysis.debug.println("total metadata=" + (System.currentTimeMillis() - checkpoint)); //$NON-NLS-1$
        fMonitor.worked(WORK_METADATA);

        // Generate and write arrays
        fMonitor.subTask("generating and writing arrays"); //$NON-NLS-1$
        fArrays.initWrite(2 * depthSize * addressSize, fParameters.getIsPrimitiveArrays());
        checkpoint = System.currentTimeMillis();
        generateAndWriteArrays(rootCalls, maxNumberOfCalls, depthSize, addressSize, fParameters.getIsPrimitiveArrays());
        fArrays.closeWrite();
        CallStackAnomalyAnalysis.debug.println("total arrays=" + (System.currentTimeMillis() - checkpoint)); //$NON-NLS-1$
        fMonitor.worked(WORK_ARRAYS);
    }

    /**
     * Get overall metadata
     *
     * @param rootCalls
     *            the root calls
     * @return the resulting metadata pair (maximums calls per function at any
     *         depth, maximum depth)
     */
    private Pair<SortedMap<Long, Integer>, Integer> getMetadata(Iterable<@NonNull ISegment> rootCalls) {
        // Initialize maximums
        SortedMap<Long, MutableInt> maximums = new TreeMap<>();
        MutableInt maxDepth = new MutableInt();

        // For each callstack
        rootCalls.forEach(segment -> {
            CalledFunction rootCall = (CalledFunction) segment;

            // Process and get map of number of calls
            Table<Long, Integer, MutableInt> counts = HashBasedTable.create();
            computeMetadata(rootCall, counts, maxDepth);

            // Compare to current maximums and update
            updateMaxCounts(maximums, counts);
        });

        return new Pair<>(Maps.transformEntries(maximums, (address, mutableInt) -> (mutableInt.get())), maxDepth.get());
    }

    /**
     * Compare the number of calls of each function per depth with the current
     * maximum. Update the latter if necessary.
     *
     * @param maximums
     *            the current maximum values
     * @param counts
     *            the counts from one individual callstack
     */
    private void updateMaxCounts(SortedMap<Long, MutableInt> maximums, Table<Long, Integer, MutableInt> counts) {
        // For each address
        for (Long address : counts.rowKeySet()) {
            // Get max from all depths
            int maxCalls = Collections.max(counts.row(address).values(), Comparator.comparingInt(MutableInt::get)).get();

            // Compare with current max and update if greater
            MutableInt currentMax = maximums.get(address);
            if (currentMax == null) {
                currentMax = new MutableInt(maxCalls);
                maximums.put(address, currentMax);
            } else if (maxCalls > currentMax.get()) {
                currentMax.set(maxCalls);
            }
        }
    }

    /**
     * Compute metadata for one callstack
     * <p>
     * Find the maximum depth and the number of calls of each function per depth.
     *
     * @param call
     *            the parent call to check
     * @param counts
     *            the current counts (to add to)
     * @param maxDepth
     *            the current maximum (to update)
     */
    private void computeMetadata(CalledFunction call, Table<Long, Integer, MutableInt> counts, MutableInt maxDepth) {
        long address = call.getSymbol();
        int depth = call.getDepth();

        // Process current calls
        if (depth > maxDepth.get()) {
            maxDepth.set(depth);
        }
        MutableInt countValue = counts.get(address, depth);
        if (countValue == null) {
            countValue = new MutableInt();
            counts.put(address, depth, countValue);
        }
        countValue.increment();

        // Process children
        call.getChildren().forEach(child -> computeMetadata((CalledFunction) child, counts, maxDepth));
    }

    /**
     * Generate callstack arrays for given roots and write to file
     *
     * @param rootCalls
     *            the root calls to generate arrays for
     * @param maximums
     *            the maximum number of calls per address at any depth
     * @param numRows
     *            the number of rows
     * @param numCols
     *            the number of columns
     * @param isPrimitive
     *            the choice to serialize as primitive arrays (true) or not (false)
     */
    private void generateAndWriteArrays(Iterable<@NonNull ISegment> rootCalls, SortedMap<Long, Integer> maximums, int numRows, int numCols, boolean isPrimitive) {
        rootCalls.forEach(segment -> {
            CalledFunction rootCall = (CalledFunction) segment;
            StackModel stackModel = new StackModel(rootCall);
            handleCall(rootCall, rootCall, stackModel);
            CallStackArray callstackArray = new CallStackArray(stackModel, maximums, numRows, numCols, isPrimitive);
            fArrays.write(callstackArray);
        });
    }

    /**
     * Handle a call to add it to a {@link StackModel} recursively
     *
     * @param call
     *            the function call to add to the data
     * @param rootCall
     *            the root call of the sub-callstack the call belongs to
     * @param model
     *            the {@link StackModel} to insert data into
     */
    private static void handleCall(CalledFunction call, CalledFunction rootCall, StackModel model) {
        long address = call.getSymbol();
        int depth = call.getDepth();
        long offset = call.getStart() - rootCall.getStart();
        long selfTime = call.getSelfTime();

        model.addSelfTime(depth, address, offset, selfTime);

        call.getChildren().forEach(child -> handleCall((CalledFunction) child, rootCall, model));
    }

    // ------------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------------

    private @NonNull String getSupplementaryFilesPath() {
        return TmfTraceManager.getSupplementaryFileDir(getTrace());
    }

    private class MutableInt {
        private int fValue;

        public MutableInt() {
            this(0);
        }

        public MutableInt(int value) {
            fValue = value;
        }

        public void increment() {
            fValue++;
        }

        public void set(int newValue) {
            fValue = newValue;
        }

        public int get() {
            return fValue;
        }
    }

    @Override
    public void dispose() {
        CallStackAnomalyAnalysis.debug.flush();
        super.dispose();
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new CallStackAnomalyAnalysisProvider(getTrace(), fParameters, fMonitor);
    }

    /**
     * Force load dl4j
     */
    private void loadDl4j() {
        fMonitor.subTask("loading dl4j"); //$NON-NLS-1$
        long checkpoint = System.currentTimeMillis();
        Dl4jUtils.load();
        CallStackAnomalyAnalysis.debug.println("total lib=" + (System.currentTimeMillis() - checkpoint)); //$NON-NLS-1$
        fMonitor.worked(WORK_DL4J_LOAD);
    }

    /**
     * Debug
     */
    private void logParameters() {
        CallStackAnomalyAnalysis.debug.println("type=" + fParameters.getSelectedAnalysisType()); //$NON-NLS-1$
        CallStackAnomalyAnalysis.debug.println("target depth=" + fParameters.getTargetDepth()); //$NON-NLS-1$
        CallStackAnomalyAnalysis.debug.println("N value=" + fParameters.getNValue()); //$NON-NLS-1$
        CallStackAnomalyAnalysis.debug.println("learning rate=" + fParameters.getLearningRate()); //$NON-NLS-1$
        CallStackAnomalyAnalysis.debug.println("num epochs=" + fParameters.getNumEpochs()); //$NON-NLS-1$
        CallStackAnomalyAnalysis.debug.println("batch size=" + fParameters.getBatchSize()); //$NON-NLS-1$
        CallStackAnomalyAnalysis.debug.println("anomaly threshold=" + fParameters.getAnomalyThreshold()); //$NON-NLS-1$
    }
}
