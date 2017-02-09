package com.polidea.rxandroidble.internal.cache;

import android.support.annotation.Nullable;
import com.polidea.rxandroidble.RxBleDevice;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import rx.Observable;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;

public class RxBleDeviceCache implements Map<String, RxBleDevice> {

    private final HashMap<String, DeviceWeakReference> cache = new HashMap<>();

    private final DeviceWeakReference.Provider deviceReferenceProvider;

    public RxBleDeviceCache() {
        this(new DeviceWeakReference.Provider() {
            @Override
            public DeviceWeakReference provide(RxBleDevice device) {
                return new DeviceWeakReference(device);
            }
        });
    }

    RxBleDeviceCache(DeviceWeakReference.Provider provider) {
        deviceReferenceProvider = provider;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey(key) && get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        final Collection<DeviceWeakReference> values = cache.values();
        for (DeviceWeakReference weakReference : values) {
            if (weakReference.contains(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Entry<String, RxBleDevice>> entrySet() {
        return Observable.from(cache.entrySet())
                .filter(new Func1<Entry<String, DeviceWeakReference>, Boolean>() {
                    @Override
                    public Boolean call(Entry<String, DeviceWeakReference> stringDeviceWeakReferenceEntry) {
                        return !stringDeviceWeakReferenceEntry.getValue().isEmpty();
                    }
                })
                .map(new Func1<Entry<String, DeviceWeakReference>, CacheEntry>() {
                    @Override
                    public CacheEntry call(Entry<String, DeviceWeakReference> stringDeviceWeakReferenceEntry) {
                        return new CacheEntry(
                                stringDeviceWeakReferenceEntry.getKey(),
                                deviceReferenceProvider.provide(stringDeviceWeakReferenceEntry.getValue().get())
                        );
                    }
                })
                .collect(
                        new Func0<HashSet<Entry<String, RxBleDevice>>>() {
                            @Override
                            public HashSet<Entry<String, RxBleDevice>> call() {
                                return new HashSet<>();
                            }
                        },
                        new Action2<HashSet<Entry<String, RxBleDevice>>, CacheEntry>() {
                            @Override
                            public void call(HashSet<Entry<String, RxBleDevice>> entries, CacheEntry e) {
                                entries.add(e);
                            }
                        }
                )
                .toBlocking().first();
    }

    @Nullable
    @Override
    public RxBleDevice get(Object key) {
        final DeviceWeakReference deviceWeakReference = cache.get(key);
        return deviceWeakReference != null ? deviceWeakReference.get() : null;
    }

    @Override
    public boolean isEmpty() {
        evictEmptyReferences();
        return cache.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return cache.keySet();
    }

    @Override
    public RxBleDevice put(String key, RxBleDevice value) {
        cache.put(key, deviceReferenceProvider.provide(value));
        evictEmptyReferences();
        return value;
    }

    @Override
    public void putAll(Map<? extends String, ? extends RxBleDevice> map) {
        for (Entry<? extends String, ? extends RxBleDevice> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public RxBleDevice remove(Object key) {
        final DeviceWeakReference deviceWeakReference = cache.remove(key);
        evictEmptyReferences();
        return deviceWeakReference != null ? deviceWeakReference.get() : null;
    }

    @Override
    public int size() {
        evictEmptyReferences();
        return cache.size();
    }

    @Override
    public Collection<RxBleDevice> values() {
        return Observable.from(cache.values())
                .filter(new Func1<DeviceWeakReference, Boolean>() {
                    @Override
                    public Boolean call(DeviceWeakReference deviceWeakReference) {
                        return !deviceWeakReference.isEmpty();
                    }
                })
                .map(new Func1<DeviceWeakReference, RxBleDevice>() {
                    @Override
                    public RxBleDevice call(DeviceWeakReference deviceWeakReference) {
                        return deviceWeakReference.get();
                    }
                })
                .toList()
                .toBlocking()
                .first();
    }

    private void evictEmptyReferences() {

        for (Iterator<Entry<String, DeviceWeakReference>> iterator = cache.entrySet().iterator(); iterator.hasNext();) {
            final Entry<String, DeviceWeakReference> next = iterator.next();

            if (next.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }
}