package com.bighero2.comovies.util;

import java.nio.ByteBuffer;

/**
 * Created by tuan on 06/04/2016.
 */
public class PieceObject {
    public ByteBuffer buffer;
    public int size;
    public long time;

    public PieceObject(ByteBuffer buffer, int size, long time) {
        this.buffer = clone(buffer);
        this.size = size;
        this.time = time;
    }

    private ByteBuffer clone(final ByteBuffer buffer) {
        assert buffer != null;

        if (buffer.remaining() == 0)
            return null;

        ByteBuffer clone = ByteBuffer.allocate(buffer.remaining());

        if (buffer.hasArray())
        {
            System.arraycopy(buffer.array(), buffer.arrayOffset() + buffer.position(), clone.array(), 0, buffer.remaining());
        }
        else
        {
            clone.put(buffer.duplicate());
            clone.flip();
        }

        return clone;
    }
}
