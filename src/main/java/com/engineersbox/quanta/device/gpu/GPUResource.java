package com.engineersbox.quanta.device.gpu;

import com.engineersbox.quanta.device.Bindable;

public abstract class GPUResource implements Bindable {

    protected static final int UNINITIALIZED_ID = -1;

    protected int id = UNINITIALIZED_ID;

    public int getId() {
        return this.id;
    }

    @Override
    public void bind() {
        if (this.id == GPUResource.UNINITIALIZED_ID) {
            throw new IllegalStateException(String.format(
                    "Cannot bind uninitialised resource %s",
                    getClass().getSimpleName()
            ));
        }
    }

    @Override
    public void destroy() {
        if (this.id == GPUResource.UNINITIALIZED_ID) {
            throw new IllegalStateException(String.format(
                    "Cannot destroy uninitialised resource %s",
                    getClass().getSimpleName()
            ));
        }
    }

}
