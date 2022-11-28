package com.engineersbox.quanta.device.gpu;

import com.engineersbox.quanta.device.Bindable;

public abstract class GPUResource implements Bindable {

    protected static final int UNINITIALIZED_ID = -1;

    protected int id = UNINITIALIZED_ID;
    protected boolean bound = false;

    public int getId() {
        return this.id;
    }

    @Override
    public void unbind() {
        if (!this.bound) {
            throw new IllegalStateException(String.format(
                    "Resource %s is not bound",
                    getClass().getSimpleName()
            ));
        } else if (this.id == GPUResource.UNINITIALIZED_ID) {
            throw new IllegalStateException(String.format(
                    "Cannot unbind uninitialised resource %s",
                    getClass().getSimpleName()
            ));
        }
    }

    @Override
    public void bind() {
        if (this.bound) {
            throw new IllegalStateException(String.format(
                    "Resource %s is already bound",
                    getClass().getSimpleName()
            ));
        } else if (this.id == GPUResource.UNINITIALIZED_ID) {
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
