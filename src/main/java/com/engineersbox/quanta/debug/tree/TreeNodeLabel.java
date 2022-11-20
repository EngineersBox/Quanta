package com.engineersbox.quanta.debug.tree;

public record TreeNodeLabel(String value) {

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof TreeNodeLabel other)) {
            return false;
        }
        return this.value.equals(other.value);
    }

}
