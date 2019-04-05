package com.google.iot.m2m.local;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.iot.m2m.base.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Executor;

class PropertyResourceLink<T> extends AbstractResourceLink<T> implements PropertyListener<T> {
    final FunctionalEndpoint mFe;
    final PropertyKey<T> mKey;
    final URI mUri;
    final Modifier[] mModifiers;

    PropertyResourceLink(FunctionalEndpoint fe, PropertyKey<T> key, URI uri, Modifier ... modifiers) {
        mFe = fe;
        mKey = key;
        mUri = uri;
        mModifiers = modifiers;
    }

    public URI getUri() {
        return mUri;
    }

    @Override
    public ListenableFuture<?> invoke(@Nullable T value) {
        return mFe.setProperty(mKey, value, mModifiers);
    }

    public static <T> ResourceLink<T> create(FunctionalEndpoint fe, PropertyKey<T> key, URI uri, Modifier ... modifiers) {
        return new PropertyResourceLink<>(fe, key, uri, modifiers);
    }

    public static <T extends Number> ResourceLink<T> createIncrement(FunctionalEndpoint fe, PropertyKey<T> key, URI uri, Modifier ... modifiers) {
        List<Modifier> withIncrement = new ArrayList<>();
        withIncrement.add(Modifier.increment());
        withIncrement.addAll(Arrays.asList(modifiers));

        return new PropertyResourceLink<T>(fe, key, uri, withIncrement.toArray(Modifier.EMPTY_LIST)) {
            @Override
            public ListenableFuture<?> invoke(@Nullable T value) {
                if (value == null) {
                    try {
                        // Default to incrementing by one.
                        value = mKey.coerce(1);

                    } catch (InvalidValueException e) {
                        // Should never happen.
                        throw new AssertionError("Unable to coerce '1' to " + mKey.getType(), e);
                    }

                    if (value == null) {
                        // Should never happen, checking only to silence warnings.
                        throw new AssertionError("Unable to coerce '1' to " + mKey.getType());
                    }
                }

                return mFe.incrementProperty(mKey, value, modifiers);
            }
        };
    }

    public static ResourceLink<Boolean> createToggle(FunctionalEndpoint fe, PropertyKey<Boolean> key, URI uri, Modifier ... modifiers) {
        List<Modifier> withMutator = new ArrayList<>();
        withMutator.add(Modifier.toggle());
        withMutator.addAll(Arrays.asList(modifiers));

        return new PropertyResourceLink<Boolean>(fe, key, uri, withMutator.toArray(Modifier.EMPTY_LIST)) {
            @Override
            public ListenableFuture<?> invoke(@Nullable Boolean value) {
                return mFe.toggleProperty(mKey, modifiers);
            }
        };
    }

    public static <T> ResourceLink<T> createInsert(FunctionalEndpoint fe, PropertyKey<T[]> key, URI uri, Modifier ... modifiers) {
        List<Modifier> withMutator = new ArrayList<>();
        withMutator.add(Modifier.insert());
        withMutator.addAll(Arrays.asList(modifiers));

        return new ResourceLink<T>() {
            @Override
            public ListenableFuture<T> fetchValue() {
                // We can't fetch from insert/remove operations because of
                // the type mismatch.
                return Futures.immediateFailedFuture(new PropertyOperationUnsupportedException());
            }

            @Override
            public ListenableFuture<?> invoke(@Nullable T value) {
                if (value == null) {
                    return Futures.immediateFailedFuture(
                            new InvalidPropertyValueException("Can't add null to property value"));
                } else {
                    return fe.addValueToProperty(key, value, modifiers);
                }
            }

            public URI getUri() {
                return uri;
            }

            @Override
            public void registerListener(Executor executor, Listener listener) {
                /* Does nothing, because we don't implement fetchValue(). */
            }

            @Override
            public void unregisterListener(Listener listener) {
                /* Does nothing, because we don't implement fetchValue(). */
            }
        };
    }

    public static <T> ResourceLink<T> createRemove(FunctionalEndpoint fe, PropertyKey<T[]> key, URI uri, Modifier ... modifiers) {
        List<Modifier> withMutator = new ArrayList<>();
        withMutator.add(Modifier.insert());
        withMutator.addAll(Arrays.asList(modifiers));

        return new ResourceLink<T>() {
            @Override
            public ListenableFuture<T> fetchValue() {
                // We can't fetch from insert/remove operations because of
                // the type mismatch.
                return Futures.immediateFailedFuture(new PropertyOperationUnsupportedException());
            }

            @Override
            public ListenableFuture<?> invoke(@Nullable T value) {
                if (value == null) {
                    return Futures.immediateFailedFuture(new InvalidPropertyValueException(
                            "Can't remove null from property value"));
                } else {
                    return fe.removeValueFromProperty(key, value, modifiers);
                }
            }

            public URI getUri() {
                return uri;
            }

            @Override
            public void registerListener(Executor executor, Listener listener) {
                /* Does nothing, because we don't implement fetchValue(). */
            }

            @Override
            public void unregisterListener(Listener listener) {
                /* Does nothing, because we don't implement fetchValue(). */
            }
        };
    }

    @Override
    public ListenableFuture<T> fetchValue() {
        return mFe.fetchProperty(mKey, mModifiers);
    }

    @Override
    protected void onListenerCountChanged(int listeners) {
        if (listeners == 0) {
            mFe.unregisterPropertyListener(mKey, this);
        } else if (listeners == 1) {
            mFe.registerPropertyListener(Runnable::run, mKey, this);
        }
    }

    @Override
    public void onPropertyChanged(FunctionalEndpoint fe, PropertyKey<T> key, @Nullable T value) {
        didChangeValue(value);
    }

    @Override
    public String toString() {
        return "<PropertyResourceLink " + mKey + " " + mFe + ">";
    }
}
