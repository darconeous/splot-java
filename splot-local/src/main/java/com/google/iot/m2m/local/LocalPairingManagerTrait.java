/*
 * Copyright (C) 2019 Google Inc.
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
package com.google.iot.m2m.local;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.iot.m2m.base.*;
import com.google.iot.m2m.trait.AutomationPairingManagerTrait;
import com.google.iot.m2m.trait.AutomationPairingTrait;
import com.google.iot.m2m.trait.EnabledDisabledTrait;
import com.google.iot.m2m.util.NestedPersistentStateManager;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class LocalPairingManagerTrait extends AutomationPairingManagerTrait.AbstractLocalTrait implements PersistentStateInterface {
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(LocalPairingManagerTrait.class.getCanonicalName());

    private final Technology mTechnology;

    Map<String, LocalPairing> mPairingLookup = new HashMap<>();
    Map<FunctionalEndpoint, String> mPairingReverseLookup = new HashMap<>();
    int mNextChildId = 1;
    FunctionalEndpoint mParent;
    NestedPersistentStateManager mNestedPersistentStateManager = new NestedPersistentStateManager();

    public LocalPairingManagerTrait(Technology technology, FunctionalEndpoint parent) {
        mTechnology = technology;
        mParent = parent;
    }

    private String getNewChildId() {
        String ret;

        do {
            ret = "" + mNextChildId++;
        } while(mPairingLookup.containsKey(ret));

        return ret;
    }

    @Override
    public Set<FunctionalEndpoint> onCopyChildrenSet() {
        return new HashSet<>(mPairingReverseLookup.keySet());
    }

    @Override
    public @Nullable String onGetIdForChild(FunctionalEndpoint child) {
        return mPairingReverseLookup.get(child);
    }

    @Override
    public @Nullable FunctionalEndpoint onGetChild(String childId) {
        return mPairingLookup.get(childId);
    }

    @CanIgnoreReturnValue
    private boolean onDeleteChild(FunctionalEndpoint child) {
        String childId = mPairingReverseLookup.get(child);

        if (childId == null || !mPairingLookup.containsKey(childId)) {
            return false;
        }

        child.setProperty(EnabledDisabledTrait.STAT_VALUE, false);

        mPairingReverseLookup.remove(child);
        mPairingLookup.remove(childId);

        mNestedPersistentStateManager.stopManaging(childId);
        mNestedPersistentStateManager.reset(childId);

        didRemoveChild(child);
        return true;
    }

    @CanIgnoreReturnValue
    private synchronized LocalPairing newLocalPairing(String childId) {
        if (mPairingLookup.containsKey(childId)) {
            onDeleteChild(mPairingLookup.get(childId));
        }

        LocalPairing pairing = new LocalPairing(mTechnology) {
            @Override
            public ListenableFuture<Boolean> delete() {
                return Futures.immediateFuture(onDeleteChild(this));
            }

            @Override
            public @Nullable FunctionalEndpoint getParentFunctionalEndpoint() {
                return mParent;
            }
        };

        mPairingLookup.put(childId, pairing);
        mPairingReverseLookup.put(pairing, childId);
        mNestedPersistentStateManager.startManaging(childId, pairing);
        didAddChild(pairing);

        return pairing;
    }

    @Override
    public FunctionalEndpoint onInvokeCreate(Map<String, Object> args) throws InvalidMethodArgumentsException {
        LocalPairing ret = newLocalPairing(getNewChildId());
        String param = null;
        try {
            if (AutomationPairingManagerTrait.PARAM_PUSH.isInMap(args)) {
                param = AutomationPairingManagerTrait.PARAM_PUSH.getName();
                ret.setProperty(AutomationPairingTrait.CONF_PUSH,
                        AutomationPairingManagerTrait.PARAM_PUSH.getFromMap(args)).get();
            }

            if (AutomationPairingManagerTrait.PARAM_PULL.isInMap(args)) {
                param = AutomationPairingManagerTrait.PARAM_PULL.getName();
                ret.setProperty(AutomationPairingTrait.CONF_PULL,
                        AutomationPairingManagerTrait.PARAM_PULL.getFromMap(args)).get();
            }

            if (AutomationPairingManagerTrait.PARAM_SOURCE.isInMap(args)) {
                param = AutomationPairingManagerTrait.PARAM_SOURCE.getName();
                ret.setProperty(AutomationPairingTrait.CONF_SOURCE,
                        AutomationPairingManagerTrait.PARAM_SOURCE.coerceFromMap(args)).get();
            }

            if (AutomationPairingManagerTrait.PARAM_DESTINATION.isInMap(args)) {
                param = AutomationPairingManagerTrait.PARAM_DESTINATION.getName();
                ret.setProperty(AutomationPairingTrait.CONF_DESTINATION,
                        AutomationPairingManagerTrait.PARAM_DESTINATION.coerceFromMap(args)).get();
            }

            if (AutomationPairingManagerTrait.PARAM_FORWARD_TRANSFORM.isInMap(args)) {
                param = AutomationPairingManagerTrait.PARAM_FORWARD_TRANSFORM.getName();
                ret.setProperty(AutomationPairingTrait.CONF_FORWARD_TRANSFORM,
                        AutomationPairingManagerTrait.PARAM_FORWARD_TRANSFORM.coerceFromMap(args)).get();
            }

            if (AutomationPairingManagerTrait.PARAM_REVERSE_TRANSFORM.isInMap(args)) {
                param = AutomationPairingManagerTrait.PARAM_REVERSE_TRANSFORM.getName();
                ret.setProperty(AutomationPairingTrait.CONF_REVERSE_TRANSFORM,
                        AutomationPairingManagerTrait.PARAM_REVERSE_TRANSFORM.coerceFromMap(args)).get();
            }

            if (AutomationPairingManagerTrait.PARAM_ENABLED.isInMap(args)) {
                param = AutomationPairingManagerTrait.PARAM_ENABLED.getName();
                ret.setProperty(EnabledDisabledTrait.STAT_VALUE,
                        AutomationPairingManagerTrait.PARAM_ENABLED.getFromMap(args)).get();
            }
        } catch (InterruptedException e) {
            onDeleteChild(ret);
            Thread.currentThread().interrupt();
            throw new TechnologyRuntimeException(e);

        } catch (InvalidValueException e) {
            if (DEBUG) LOGGER.info("InvalidValueException: " + e);
            onDeleteChild(ret);
            throw new InvalidMethodArgumentsException("Bad value for \"" + param + "\" " + e);

        } catch (ExecutionException e) {
            if (DEBUG) LOGGER.info("ExecutionException: " + e);
            onDeleteChild(ret);
            throw new InvalidMethodArgumentsException("Bad value for \"" + param + "\" " + e.getCause().getMessage(), e.getCause());
        }

        return ret;
    }

    @Override
    public Map<String, Object> copyPersistentState() {
        return mNestedPersistentStateManager.copyPersistentState();
    }

    @Override
    public void initWithPersistentState(@Nullable Map<String, Object> persistentState) {
        if (persistentState == null) {
            return;
        }
        mNestedPersistentStateManager.initWithPersistentState(persistentState);

        for (Map.Entry<String, Object> entry : persistentState.entrySet()) {
            Object entryValue = entry.getValue();

            if (!(entryValue instanceof Map)) {
                continue;
            }

            newLocalPairing(entry.getKey());
        }
    }

    @Override
    public void setPersistentStateListener(@Nullable PersistentStateListener listener) {
        mNestedPersistentStateManager.setPersistentStateListener(listener);
    }
}