package com.engineersbox.quanta.device;

public interface Bindable {

    void bind();

    void unbind();

    default void validate() {}

    void destroy();

}
