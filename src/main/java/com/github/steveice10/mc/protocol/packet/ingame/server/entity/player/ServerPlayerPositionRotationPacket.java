package com.github.steveice10.mc.protocol.packet.ingame.server.entity.player;

import com.github.steveice10.mc.protocol.data.MagicValues;
import com.github.steveice10.mc.protocol.data.game.entity.player.PositionElement;
import com.github.steveice10.mc.protocol.util.ReflectionToString;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.packet.Packet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.mcphoton.utils.Vector;

public class ServerPlayerPositionRotationPacket implements Packet {

    private Vector position;
    private float yaw;
    private float pitch;
    private List<PositionElement> relative;
    private int teleportId;

    @SuppressWarnings("unused")
    private ServerPlayerPositionRotationPacket() {
    }

    public ServerPlayerPositionRotationPacket(Vector position, float yaw, float pitch, int
            teleportId,
                                                PositionElement... relative) {
        this.position = position.clone();
        this.yaw = yaw;
        this.pitch = pitch;
        this.teleportId = teleportId;
        this.relative = Arrays.asList(relative != null ? relative : new PositionElement[0]);
    }

    public Vector getPosition() {
        return position;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public List<PositionElement> getRelativeElements() {
        return relative;
    }

    public int getTeleportId() {
        return teleportId;
    }

    @Override
    public void read(NetInput in) throws IOException {
        double x = in.readDouble();
        double y = in.readDouble();
        double z = in.readDouble();
        position = new Vector(x,y,z);
        yaw = in.readFloat();
        pitch = in.readFloat();
        relative = new ArrayList<PositionElement>();
        int flags = in.readUnsignedByte();
        for(PositionElement element : PositionElement.values()) {
            int bit = 1 << MagicValues.value(Integer.class, element);
            if((flags & bit) == bit) {
                relative.add(element);
            }
        }

        teleportId = in.readVarInt();
    }

    @Override
    public void write(NetOutput out) throws IOException {
        out.writeDouble(position.getX());
        out.writeDouble(position.getY());
        out.writeDouble(position.getZ());
        out.writeFloat(yaw);
        out.writeFloat(pitch);
        int flags = 0;
        for(PositionElement element : relative) {
            flags |= 1 << MagicValues.value(Integer.class, element);
        }

        out.writeByte(flags);
        out.writeVarInt(teleportId);
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