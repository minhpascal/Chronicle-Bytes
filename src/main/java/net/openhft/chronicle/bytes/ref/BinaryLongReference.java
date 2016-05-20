/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class BinaryLongReference implements LongReference {
    private BytesStore bytes;
    private long offset;

    private static Set<WeakReference<BinaryLongReference>> binaryLongReferences;

    static {
        if (Jvm.isDebug()) {
            binaryLongReferences = Collections.newSetFromMap(new IdentityHashMap<>());
        }
    }

    /**
     * only used for testing
     */
    public static void forceAllToNotCompleteState() {
        binaryLongReferences.forEach(x -> {
            BinaryLongReference binaryLongReference = x.get();
            if (binaryLongReference != null) {
                binaryLongReference.setValue(1 << 31 | binaryLongReference.getValue());
            }
        });
    }


    @Override
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) {
        if (length != maxSize())
            throw new IllegalArgumentException();

        this.bytes = bytes.bytesStore();
        this.offset = offset;
    }

    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public long maxSize() {
        return 8;
    }

    @NotNull
    public String toString() {
        return bytes == null ? "bytes is null" : "value: " + getValue();
    }

    @Override
    public long getValue() {
        return bytes.readLong(offset);
    }

    @Override
    public void setValue(long value) {
        bytes.writeLong(offset, value);
    }

    @Override
    public long getVolatileValue() {
        return bytes.readVolatileLong(offset);
    }

    @Override
    public void setOrderedValue(long value) {
        bytes.writeOrderedLong(offset, value);
    }

    @Override
    public long addValue(long delta) {
        return bytes.addAndGetLong(offset, delta);
    }

    @Override
    public long addAtomicValue(long delta) {
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(long expected, long value) {
        if (Jvm.isDebug() && (value & 1 << 31) != 0)
            binaryLongReferences.add(new WeakReference<BinaryLongReference>(this));

        return bytes.compareAndSwapLong(offset, expected, value);
    }
}
