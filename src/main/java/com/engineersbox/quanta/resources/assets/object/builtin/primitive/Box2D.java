package com.engineersbox.quanta.resources.assets.object.builtin.primitive;

public record Box2D(float x,
                    float y,
                    float width,
                    float height) {

    public boolean contains(final float xPos,
                            final float yPos) {
        return xPos >= this.x
                && yPos >= this.y
                && xPos < this.x + this.width
                && yPos < this.y + this.height;
    }
}
