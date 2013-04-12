/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.web.api;

import javax.annotation.Nullable;

/**
 *
 */
public interface ParameterSet {

    @Nullable
    public String getFirstValue(String key);

    public String getFirstValue(String key, String defaultValue);

    @Nullable
    public String[] getValuesArray(String key);

}
