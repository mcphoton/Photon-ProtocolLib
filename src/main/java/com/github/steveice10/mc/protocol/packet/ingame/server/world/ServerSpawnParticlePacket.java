package com.github.steveice10.mc.protocol.packet.ingame.server.world;

import com.github.steveice10.mc.protocol.data.game.world.Particle;
import com.github.steveice10.mc.protocol.util.ReflectionToString;
import com.github.steveice10.mc.protocol.data.MagicValues;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.packet.Packet;

import java.io.IOException;
import org.mcphoton.utils.Vector;

public class ServerSpawnParticlePacket implements Packet {
    private Particle particle;
    private boolean longDistance;
    private Vector position, offset;
    private float velocityOffset;
    private int amount;
    private int data[];

    @SuppressWarnings("unused")
    private ServerSpawnParticlePacket() {}

    public ServerSpawnParticlePacket(Particle particle, boolean longDistance, Vector position,
                                     Vector offset, float velocityOffset, int amount, int... data) {
        this.particle = particle;
        this.longDistance = longDistance;
        this.position = position.clone();
        this.offset = offset.clone();
        this.velocityOffset = velocityOffset;
        this.amount = amount;
        this.data = data;
        if (this.data.length != particle.getDataLength()) {
            throw new IllegalArgumentException(
                    "Data array length must be equal to particle's data length.");
        }
    }

    public Particle getParticle() {
        return this.particle;
    }

    public boolean isLongDistance() {
        return this.longDistance;
    }

    public Vector getPosition() {
        return position;
    }

    public Vector getOffset() {
        return offset;
    }

    public float getVelocityOffset() {
        return this.velocityOffset;
    }

    public int getAmount() {
        return this.amount;
    }

    public int[] getData() {
        return this.data;
    }

    @Override
    public void read(NetInput in) throws IOException {
        this.particle = MagicValues.key(Particle.class, in.readInt());
        this.longDistance = in.readBoolean();
        double x = in.readFloat();
        double y = in.readFloat();
        double z = in.readFloat();
        position = new Vector(x, y, z);
        double offsetX = in.readFloat();
        double offsetY = in.readFloat();
        double offsetZ = in.readFloat();
        offset = new Vector(offsetX, offsetY, offsetZ);
        this.velocityOffset = in.readFloat();
        this.amount = in.readInt();
        this.data = new int[this.particle.getDataLength()];
        for (int index = 0; index < this.data.length; index++) {
            this.data[index] = in.readVarInt();
        }
    }

    @Override
    public void write(NetOutput out) throws IOException {
        out.writeInt(MagicValues.value(Integer.class, this.particle));
        out.writeBoolean(this.longDistance);
        out.writeFloat((float)position.getX());
        out.writeFloat((float)position.getY());
        out.writeFloat((float)position.getZ());
        out.writeFloat((float)offset.getX());
        out.writeFloat((float)offset.getY());
        out.writeFloat((float)offset.getZ());
        out.writeFloat(this.velocityOffset);
        out.writeInt(this.amount);
        for (int index = 0; index < this.particle.getDataLength(); index++) {
            out.writeVarInt(this.data[index]);
        }
    }

    @Override
    public boolean isPriority() {
        return false;
    }

    @Override
    public String toString() {
        return ReflectionToString.toString(this);
    }
}