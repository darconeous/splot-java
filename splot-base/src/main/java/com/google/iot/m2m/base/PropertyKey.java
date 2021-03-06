/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.iot.m2m.base;

import com.google.common.base.Preconditions;

/**
 * A generic class for facilitating type-safety on individual properties while remaining convenient
 * to use.
 *
 * @see com.google.iot.m2m.annotation.Method
 */
public final class PropertyKey<T> extends TypedKey<T> {
    /** "Directory" name for state section. */
    public static final String SECTION_STATE = "s";

    /** "Directory" name for config section. */
    public static final String SECTION_CONFIG = "c";

    /** "Directory" name for metadata section. */
    public static final String SECTION_METADATA = "m";

    /**
     * Method for determining if the given property name is in the state section.
     *
     * @param name the name of the property
     * @return true if the named property is in the state section, false otherwise.
     * @see #isSectionState()
     */
    public static boolean isSectionState(String name) {
        return name.startsWith(SECTION_STATE + "/");
    }

    /**
     * Method for determining if the given property name is in the config section.
     *
     * @param name the name of the property
     * @return true if the named property is in the config section, false otherwise.
     * @see #isSectionConfig()
     */
    public static boolean isSectionConfig(String name) {
        return name.startsWith(SECTION_CONFIG + "/");
    }

    /**
     * Method for determining if the given property name is in the metadata section.
     *
     * @param name the name of the property
     * @return true if the named property is in the metadata section, false otherwise.
     * @see #isSectionMetadata()
     */
    public static boolean isSectionMetadata(String name) {
        return name.startsWith(SECTION_METADATA + "/");
    }

    private final String mName;
    private final Class<T> mType;

    /**
     * Constructs a property key object. Note that {@link #PropertyKey(String, String, String,
     * Class)} is the preferred constructor to use.
     *
     * @param fullName the full name of the property, in the form <code>
     *     &lt;SECTION&gt;/&lt;TRAIT-SHORT-ID&gt;/&lt;PROP-SHORT-ID&gt;</code>.
     * @param type the class for the value that will be associated with this property.
     */
    public PropertyKey(String fullName, Class<T> type) {
        Preconditions.checkNotNull(fullName, "fullName cannot be null");
        Preconditions.checkNotNull(type, "type cannot be null");

        mName = fullName;
        mType = type;
    }

    /**
     * Preferred constructor for PropertyKey objects.
     *
     * @param section the short name of the section this property is in. Can be one of {@link
     *     #SECTION_STATE}, {@link #SECTION_CONFIG}, or {@link #SECTION_METADATA}.
     * @param trait the short name of the trait that owns this property
     * @param shortName the short name of the property
     * @param type the class for the value that will be associated with this property.
     */
    public PropertyKey(String section, String trait, String shortName, Class<T> type) {
        this(section + "/" + trait + "/" + shortName, type);
    }

    /**
     * Returns the name of this property, in the form <code>
     * &lt;SECTION&gt;/&lt;TRAIT-ID&gt;/&lt;PROP-ID&gt;</code>.
     *
     * <ul>
     *   <li>{@code SECTION}: either {@link #SECTION_STATE}, {@link #SECTION_CONFIG}, or {@link
     *       #SECTION_METADATA}
     *   <li>{@code TRAIT-ID}: the short identifier of the trait that this property belongs to
     *   <li>{@code PROP-ID}: the short identifier of the property
     * </ul>
     */
    @Override
    public final String getName() {
        return mName;
    }

    /** Returns the class object for this property's values. */
    @Override
    public Class<T> getType() {
        return mType;
    }

    /**
     * Method for determining if this property is in the state section.
     *
     * @return true if the property is in the state section, false otherwise.
     * @see #isSectionState(String)
     */
    public boolean isSectionState() {
        return isSectionState(mName);
    }

    /**
     * Method for determining if this property is in the config section.
     *
     * @return true if the property is in the config section, false otherwise.
     * @see #isSectionConfig(String)
     */
    public boolean isSectionConfig() {
        return isSectionConfig(mName);
    }

    /**
     * Method for determining if this property is in the metadata section.
     *
     * @return true if the property is in the metadata section, false otherwise.
     * @see #isSectionMetadata(String)
     */
    public boolean isSectionMetadata() {
        return isSectionMetadata(mName);
    }
}
