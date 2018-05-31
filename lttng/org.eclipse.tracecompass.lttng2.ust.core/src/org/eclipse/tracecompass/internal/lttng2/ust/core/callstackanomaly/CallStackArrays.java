/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.ust.core.callstackanomaly;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.lttng2.ust.core.Activator;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * Container and serializer for callstack arrays
 * <p>
 * Provide the path to the supplementary files folder. Call {@link #initRead()}
 * or {@link #initWrite(int, boolean)} to initialize, then call {@link #read()}
 * or {@link #write(CallStackArray)}. Finally, call {@link #closeRead()} or
 * {@link #closeWrite()}. The init/reset methods can be called as many times as
 * needed. Call {@link #dispose()} to free all contained resources.
 *
 * @author Christophe Bedard
 */
public class CallStackArrays {

    /** Extension for arrays file */
    public static final @NonNull String ARRAYS_FILE_EXTENSION = ".zip.dat"; //$NON-NLS-1$
    /** Extension for metadata file */
    public static final @NonNull String METADATA_FILE_EXTENSION = ".dat"; //$NON-NLS-1$

    private String fPath = null;
    private File fArraysFile = null;
    private File fMetadataFile = null;

    private @Nullable ObjectInputStream fInputStream = null;
    private @Nullable ObjectOutputStream fOutputStream = null;

    /**
     * Whether the arrays are serialized as primitives (true) or not (false)
     */
    private boolean fIsPrimitive = false;
    /**
     * The number of arrays. When writing arrays to the file, it represents the
     * number of arrays that have been written. When reading arrays from file, it
     * represents the number of arrays left to be read.
     */
    private long fArraysCount = 0;
    /**
     * The size of the global concatenated and flattened array
     */
    private int fArraysSize = 0;

    /**
     * Constructor
     *
     * @param path
     *            the path to the directory in which to store files
     */
    public CallStackArrays(@NonNull String path) {
        setPath(path);
    }

    /**
     * Set files path
     *
     * @param path
     *            the path to the directory in which to store files
     */
    private void setPath(@NonNull String path) {
        fPath = path;
        fArraysFile = new File(fPath + getFileName());
        fMetadataFile = new File(fPath + getMetadataFileName());
    }

    /**
     * Check that the files exist (i.e. arrays have been written)
     * <p>
     * The path has to be set first.
     *
     * @return true if the files exist, false otherwise
     */
    public boolean exists() {
        if (fArraysFile == null || fMetadataFile == null) {
            return false;
        }
        return fArraysFile.exists() && fMetadataFile.exists();
    }

    /**
     * Properly dispose (close and delete files)
     */
    public void dispose() {
        closeWrite();
        closeRead();
        deleteFiles();
    }

    /**
     * @return the arrays count (useful for reading all arrays)
     */
    public long getArraysCount() {
        return fArraysCount;
    }

    /**
     * @return the total size of the callstack array
     */
    public int getArraysSize() {
        return fArraysSize;
    }

    /**
     * Check that there is at least one array left in the file to read
     * <p>
     * If this returns <code>false</code>, then calling {@link #read()} will most
     * likely return <code>null</code>.
     *
     * @return true if there is another array to read, false otherwise
     */
    public boolean hasNextArray() {
        return fArraysCount > 0;
    }

    /**
     * Reset current state.
     * <p>
     * e.g. if the arrays were being read, this will start reading at the beginning
     */
    public void reset() {
        if (fInputStream != null) {
            closeRead();
            initRead();
        }
        if (fOutputStream != null) {
            closeWrite();
            initWrite(fArraysSize, fIsPrimitive);
        }
    }

    /**
     * Initialize input stream and read metadata
     */
    public void initRead() {
        // Read metadata
        try (ObjectInputStream metadataInputStream = new ObjectInputStream(new FileInputStream(fMetadataFile))) {
            fArraysCount = metadataInputStream.readLong();
            fArraysSize = metadataInputStream.readInt();
            fIsPrimitive = metadataInputStream.readBoolean();
        } catch (IOException e) {
            Activator.getDefault().logError("Problem reading callstack arrays metadata", e); //$NON-NLS-1$
        }

        // Make sure that the file was set
        // and that the stream has not already been initialized
        if (fArraysFile != null && fInputStream == null) {
            try {
                fInputStream = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(fArraysFile))));
            } catch (IOException e) {
                fInputStream = null;
                Activator.getDefault().logError("Problem initializing callstack arrays reading", e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Initialize output stream for writing
     *
     * @param arraysSize
     *            the size of the arrays that will be written to the file
     * @param isPrimitive
     *            the choice to serialize as primitive arrays (true) or not (false)
     */
    public void initWrite(int arraysSize, boolean isPrimitive) {
        fIsPrimitive = isPrimitive;
        fArraysSize = arraysSize;
        // Reset count
        fArraysCount = 0;

        // Make sure that the file was set
        // and that the stream has not already been initialized
        if (fArraysFile != null && fOutputStream == null) {
            try {
                fOutputStream = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(fArraysFile))));
            } catch (IOException e) {
                fOutputStream = null;
                Activator.getDefault().logError("Problem initializing callstack arrays writing", e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Close input stream
     */
    public void closeRead() {
        try {
            if (fInputStream != null) {
                fInputStream.close();
                fInputStream = null;
            }
        } catch (IOException e) {
            Activator.getDefault().logError("Problem closing callstack arrays reading", e); //$NON-NLS-1$
        }
    }

    /**
     * Close output stream and write metadata
     */
    public void closeWrite() {
        try {
            if (fOutputStream != null) {
                fOutputStream.close();
                fOutputStream = null;
            }
        } catch (IOException e) {
            Activator.getDefault().logError("Problem closing callstack arrays writing", e); //$NON-NLS-1$
        }

        // Write metadata to file
        try (ObjectOutputStream metadataOutputStream = new ObjectOutputStream(new FileOutputStream(fMetadataFile))) {
            metadataOutputStream.writeLong(fArraysCount);
            metadataOutputStream.writeInt(fArraysSize);
            metadataOutputStream.writeBoolean(fIsPrimitive);
        } catch (IOException e) {
            Activator.getDefault().logError("Problem writing callstack arrays metadata", e); //$NON-NLS-1$
        }
    }

    /**
     * Write an array to the file
     *
     * @param callstackArray
     *            the array to write
     */
    public void write(CallStackArray callstackArray) {
        try {
            if (fOutputStream != null) {
                fOutputStream.writeObject(callstackArray);
                fArraysCount++;
            } else {
                Activator.getDefault().logWarning("Problem writing callstack array; initWrite() may not have been called!"); //$NON-NLS-1$
            }
        } catch (IOException e) {
            Activator.getDefault().logError("Problem writing callstack array to file", e); //$NON-NLS-1$
        }
    }

    /**
     * Read the mext array from the file
     * <p>
     * {@link CallStackArray} will convert the serialized arrays to {@link INDArray}
     * if necessary
     *
     * @return the array
     * @throws IOException
     *             I/O
     * @throws ClassNotFoundException
     *             cast to CallStackArray
     */
    public CallStackArray read() throws ClassNotFoundException, IOException {
        if (fInputStream != null) {
            CallStackArray callstackArray = (CallStackArray) fInputStream.readObject();
            fArraysCount--;
            return callstackArray;
        }
        return null;
    }

    /**
     * Delete files (metadata and arrays)
     */
    private void deleteFiles() {
        fArraysFile.delete();
        fMetadataFile.delete();
    }

    /**
     * @return the name of the file which stores the arrays
     */
    public static String getFileName() {
        return CallStackArrays.class.getName() + ARRAYS_FILE_EXTENSION;
    }

    /**
     * @return the name of the file which stores the metadata
     */
    public static String getMetadataFileName() {
        return CallStackArrays.class.getName() + ".metadata" + METADATA_FILE_EXTENSION; //$NON-NLS-1$
    }

    /**
     * Debug
     * <p>
     * Print all arrays in file
     */
    public void printArrays() {
        reset();
        initRead();
        int arraysCount = 0;
        try {
            while (hasNextArray()) {
                System.out.println(read());
                arraysCount++;
            }
        } catch (Exception e) {
            System.out.println("Problem reading arrays"); //$NON-NLS-1$
        }
        System.out.println("done; " + arraysCount + " arrays"); //$NON-NLS-1$ //$NON-NLS-2$
        closeRead();
    }
}
