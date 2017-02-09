package com.polidea.rxandroidble.internal.cache

import com.polidea.rxandroidble.RxBleDevice

class MockDeviceReferenceProvider implements DeviceWeakReference.Provider {


    private final HashMap<RxBleDevice, List<MockDeviceWeakReference>> devices = new HashMap<>()

    class MockDeviceWeakReference extends DeviceWeakReference {

        MockDeviceWeakReference(RxBleDevice device) {
            super(device)
        }

        public release() {
            clear()
        }

        @Override
        boolean isEmpty() {
            return super.isEmpty()
        }
    }

    @Override
    DeviceWeakReference provide(RxBleDevice rxBleDevice) {
        def reference = new MockDeviceWeakReference(rxBleDevice)
        storeReference(rxBleDevice, reference)
        return reference
    }

    public releaseReferenceFor(RxBleDevice rxBleDevice) {
        devices.get(rxBleDevice)?.each { it.release() }
    }

    private storeReference(RxBleDevice rxBleDevice, MockDeviceWeakReference reference) {

        if (devices.containsKey(rxBleDevice)) {
            devices.get(rxBleDevice).add(reference)
        } else {
            devices.put(rxBleDevice, [reference])
        }
    }
}
