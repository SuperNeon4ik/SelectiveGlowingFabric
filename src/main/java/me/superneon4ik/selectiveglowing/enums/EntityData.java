package me.superneon4ik.selectiveglowing.enums;

public enum EntityData {

    ON_FIRE((byte) 0x01),
    CROUCHING((byte) 0x02),
    SPRINTING((byte) 0x08),
    SWIMMING((byte) 0x10),
    INVISIBLE((byte) 0x20),
    GLOWING((byte) 0x40),
    ELYTRA_FLY((byte) 0x80);

    final byte bitMask;

    EntityData(byte bitMask) {
        this.bitMask = bitMask;
    }

    public byte getBitMask() {
        return bitMask;
    }

    public boolean isPresent(byte bits) {
        return (this.bitMask & bits) == this.bitMask;
    }

    public byte setBit(byte bits) {
        return (byte) (bits | this.bitMask);
    }

    public byte unsetBit(byte bits) {
        return (byte) (bits & ~this.bitMask);
    }
}