/**********************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/
package org.eclipse.tracecompass.tmf.ui.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Utilities for default trace location preferences.
 *
 * @author Christophe Bedard
 * @since 5.2
 */
public class DefaultTraceLocation {

    /**
     * Preference store key for the default trace locations.
     */
    public static final String PREFERENCE_KEY = "default_trace_location"; //$NON-NLS-1$
    /**
     * Path separator for default trace locations.
     */
    public static final String SEPARATOR = ":"; //$NON-NLS-1$
    /**
     * The scope qualifier for the associated preference store.
     */
    public static final String PREFERENCE_SCOPE = "org.eclipse.tracecompass.tmf.ui"; //$NON-NLS-1$

    private static IPreferenceStore fStore = null;

    private DefaultTraceLocation() {
        // Do nothing
    }

    /**
     * @param location
     *            the new default trace location to add
     */
    public static void addLocation(String location) {
        IPreferenceStore store = getScopedPreferenceStore();
        String currentValue = store.getString(PREFERENCE_KEY);
        String newValue = currentValue + location + SEPARATOR;
        store.setValue(PREFERENCE_KEY, newValue);
    }

    private static IPreferenceStore getScopedPreferenceStore() {
        if (fStore == null) {
            fStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, PREFERENCE_SCOPE);
        }
        return fStore;
    }
}
