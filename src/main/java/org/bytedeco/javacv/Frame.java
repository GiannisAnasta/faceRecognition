package org.bytedeco.javacv;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.javacpp.ShortPointer;
import org.bytedeco.javacpp.indexer.ByteIndexer;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.Indexable;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.javacpp.indexer.LongIndexer;
import org.bytedeco.javacpp.indexer.ShortIndexer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacpp.indexer.UShortIndexer;

public class Frame implements Indexable {

    public boolean keyFrame;

    public static final int DEPTH_BYTE = -8,
            DEPTH_UBYTE = 8,
            DEPTH_SHORT = -16,
            DEPTH_USHORT = 16,
            DEPTH_INT = -32,
            DEPTH_LONG = -64,
            DEPTH_FLOAT = 32,
            DEPTH_DOUBLE = 64;

    public int imageWidth, imageHeight, imageDepth, imageChannels, imageStride;

    public Buffer[] image;

    public int sampleRate, audioChannels;

    public Buffer[] samples;

    public Object opaque;

    public long timestamp;

    public Frame() {
    }

    public Frame(int width, int height, int depth, int channels) {
        int pixelSize = Math.abs(depth) / 8;
        this.imageWidth = width;
        this.imageHeight = height;
        this.imageDepth = depth;
        this.imageChannels = channels;
        this.imageStride = ((imageWidth * imageChannels * pixelSize + 7) & ~7) / pixelSize; // 8-byte aligned
        this.image = new Buffer[1];

        ByteBuffer buffer = ByteBuffer.allocateDirect(imageHeight * imageStride * pixelSize).order(ByteOrder.nativeOrder());
        switch (imageDepth) {
            case DEPTH_BYTE:
            case DEPTH_UBYTE:
                image[0] = buffer;
                break;
            case DEPTH_SHORT:
            case DEPTH_USHORT:
                image[0] = buffer.asShortBuffer();
                break;
            case DEPTH_INT:
                image[0] = buffer.asIntBuffer();
                break;
            case DEPTH_LONG:
                image[0] = buffer.asLongBuffer();
                break;
            case DEPTH_FLOAT:
                image[0] = buffer.asFloatBuffer();
                break;
            case DEPTH_DOUBLE:
                image[0] = buffer.asDoubleBuffer();
                break;
            default:
                throw new UnsupportedOperationException("Unsupported depth value: " + imageDepth);
        }
    }

    /**
     * Returns {@code createIndexer(true, 0)}.
     */
    public <I extends Indexer> I createIndexer() {
        return (I) createIndexer(true, 0);
    }

    @Override
    public <I extends Indexer> I createIndexer(boolean direct) {
        return (I) createIndexer(direct, 0);
    }

    /**
     * Returns an {@link Indexer} for the <i>i</i>th image plane.
     */
    public <I extends Indexer> I createIndexer(boolean direct, int i) {
        long[] sizes = {imageHeight, imageWidth, imageChannels};
        long[] strides = {imageStride, imageChannels, 1};
        Buffer buffer = image[i];
        Object array = buffer.hasArray() ? buffer.array() : null;
        switch (imageDepth) {
            case DEPTH_UBYTE:
                return array != null ? (I) UByteIndexer.create((byte[]) array, sizes, strides).indexable(this)
                        : direct ? (I) UByteIndexer.create((ByteBuffer) buffer, sizes, strides).indexable(this)
                                : (I) UByteIndexer.create(new BytePointer((ByteBuffer) buffer), sizes, strides, false).indexable(this);
            case DEPTH_BYTE:
                return array != null ? (I) ByteIndexer.create((byte[]) array, sizes, strides).indexable(this)
                        : direct ? (I) ByteIndexer.create((ByteBuffer) buffer, sizes, strides).indexable(this)
                                : (I) ByteIndexer.create(new BytePointer((ByteBuffer) buffer), sizes, strides, false).indexable(this);
            case DEPTH_USHORT:
                return array != null ? (I) UShortIndexer.create((short[]) array, sizes, strides).indexable(this)
                        : direct ? (I) UShortIndexer.create((ShortBuffer) buffer, sizes, strides).indexable(this)
                                : (I) UShortIndexer.create(new ShortPointer((ShortBuffer) buffer), sizes, strides, false).indexable(this);
            case DEPTH_SHORT:
                return array != null ? (I) ShortIndexer.create((short[]) array, sizes, strides).indexable(this)
                        : direct ? (I) ShortIndexer.create((ShortBuffer) buffer, sizes, strides).indexable(this)
                                : (I) ShortIndexer.create(new ShortPointer((ShortBuffer) buffer), sizes, strides, false).indexable(this);
            case DEPTH_INT:
                return array != null ? (I) IntIndexer.create((int[]) array, sizes, strides).indexable(this)
                        : direct ? (I) IntIndexer.create((IntBuffer) buffer, sizes, strides).indexable(this)
                                : (I) IntIndexer.create(new IntPointer((IntBuffer) buffer), sizes, strides, false).indexable(this);
            case DEPTH_LONG:
                return array != null ? (I) LongIndexer.create((long[]) array, sizes, strides).indexable(this)
                        : direct ? (I) LongIndexer.create((LongBuffer) buffer, sizes, strides).indexable(this)
                                : (I) LongIndexer.create(new LongPointer((LongBuffer) buffer), sizes, strides, false).indexable(this);
            case DEPTH_FLOAT:
                return array != null ? (I) FloatIndexer.create((float[]) array, sizes, strides).indexable(this)
                        : direct ? (I) FloatIndexer.create((FloatBuffer) buffer, sizes, strides).indexable(this)
                                : (I) FloatIndexer.create(new FloatPointer((FloatBuffer) buffer), sizes, strides, false).indexable(this);
            case DEPTH_DOUBLE:
                return array != null ? (I) DoubleIndexer.create((double[]) array, sizes, strides).indexable(this)
                        : direct ? (I) DoubleIndexer.create((DoubleBuffer) buffer, sizes, strides).indexable(this)
                                : (I) DoubleIndexer.create(new DoublePointer((DoubleBuffer) buffer), sizes, strides, false).indexable(this);
            default:
                assert false;
        }
        return null;
    }

    @Override
    public Frame clone() throws CloneNotSupportedException {
        Frame newFrame = new Frame();

        // Video part
        newFrame.imageWidth = imageWidth;
        newFrame.imageHeight = imageHeight;
        newFrame.imageDepth = imageDepth;
        newFrame.imageChannels = imageChannels;
        newFrame.imageStride = imageStride;
        newFrame.keyFrame = keyFrame;
        newFrame.opaque = opaque;
        newFrame.image = cloneBufferArray(image);

        // Audio part
        newFrame.audioChannels = audioChannels;
        newFrame.sampleRate = sampleRate;
        newFrame.samples = cloneBufferArray(samples);

        // Add timestamp
        newFrame.timestamp = timestamp;

        return newFrame;

    }

    private static Buffer[] cloneBufferArray(Buffer[] srcBuffers) {

        Buffer[] clonedBuffers = null;
        int i;
        short dataSize;

        if (srcBuffers != null) {
            clonedBuffers = new Buffer[srcBuffers.length];

            for (i = 0; i < srcBuffers.length; i++) {
                srcBuffers[i].rewind();
            }

            /*
             * In order to optimize the transfer we need a type check.
             *
             * Most CPUs support hardware memory transfer for different data
             * types, so it's faster to copy more bytes at once rather
             * than one byte per iteration as in case of ByteBuffer.
             *
             * For example, Intel CPUs support MOVSB (byte transfer), MOVSW
             * (word transfer), MOVSD (double word transfer), MOVSS (32 bit
             * scalar single precision floating point), MOVSQ (quad word
             * transfer) and so on...
             *
             * Type checking may be improved by changing the order in
             * which a buffer is checked against. If it's likely that the
             * expected buffer is of type "ShortBuffer", then it should be
             * checked at first place.
             *
             */
            if (srcBuffers[0] instanceof ByteBuffer) // dataSize is 1
            {
                for (i = 0; i < srcBuffers.length; i++) {
                    clonedBuffers[i] = ByteBuffer.allocateDirect(srcBuffers[i].capacity())
                            .put((ByteBuffer) srcBuffers[i]).rewind();
                }
            } else if (srcBuffers[0] instanceof ShortBuffer) {
                dataSize = Short.SIZE >> 3; // dataSize is 2
                for (i = 0; i < srcBuffers.length; i++) {
                    clonedBuffers[i] = ByteBuffer.allocateDirect(srcBuffers[i].capacity() * dataSize)
                            .order(ByteOrder.nativeOrder()).asShortBuffer().put((ShortBuffer) srcBuffers[i]).rewind();
                }
            } else if (srcBuffers[0] instanceof IntBuffer) {
                dataSize = Integer.SIZE >> 3; // dataSize is 4
                for (i = 0; i < srcBuffers.length; i++) {
                    clonedBuffers[i] = ByteBuffer.allocateDirect(srcBuffers[i].capacity() * dataSize)
                            .order(ByteOrder.nativeOrder()).asIntBuffer().put((IntBuffer) srcBuffers[i]).rewind();
                }
            } else if (srcBuffers[0] instanceof LongBuffer) {
                dataSize = Long.SIZE >> 3; // dataSize is 8
                for (i = 0; i < srcBuffers.length; i++) {
                    clonedBuffers[i] = ByteBuffer.allocateDirect(srcBuffers[i].capacity() * dataSize)
                            .order(ByteOrder.nativeOrder()).asLongBuffer().put((LongBuffer) srcBuffers[i]).rewind();
                }
            } else if (srcBuffers[0] instanceof FloatBuffer) {
                dataSize = Float.SIZE >> 3; // dataSize is 4
                for (i = 0; i < srcBuffers.length; i++) {
                    clonedBuffers[i] = ByteBuffer.allocateDirect(srcBuffers[i].capacity() * dataSize)
                            .order(ByteOrder.nativeOrder()).asFloatBuffer().put((FloatBuffer) srcBuffers[i]).rewind();
                }
            } else if (srcBuffers[0] instanceof DoubleBuffer) {
                dataSize = Double.SIZE >> 3; // dataSize is 8
                for (i = 0; i < srcBuffers.length; i++) {
                    clonedBuffers[i] = ByteBuffer.allocateDirect(srcBuffers[i].capacity() * dataSize)
                            .order(ByteOrder.nativeOrder()).asDoubleBuffer().put((DoubleBuffer) srcBuffers[i]).rewind();
                }
            }

            for (i = 0; i < srcBuffers.length; i++) {
                srcBuffers[i].rewind();
            }

        }

        return clonedBuffers;

    }

}
