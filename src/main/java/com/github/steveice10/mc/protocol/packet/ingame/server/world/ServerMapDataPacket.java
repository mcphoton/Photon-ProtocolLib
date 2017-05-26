package com.github.steveice10.mc.protocol.packet.ingame.server.world;

import com.github.steveice10.mc.protocol.data.game.world.map.MapData;
import com.github.steveice10.mc.protocol.data.game.world.map.MapIconType;
import com.github.steveice10.mc.protocol.util.ReflectionToString;
import com.github.steveice10.mc.protocol.data.MagicValues;
import com.github.steveice10.mc.protocol.data.game.world.map.MapIcon;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.packet.Packet;

import java.io.IOException;

public class ServerMapDataPacket implements Packet {

    private int mapId;
    private byte scale;
    private boolean trackingPosition;
    private MapIcon icons[];

    private MapData data;

    @SuppressWarnings("unused")
    private ServerMapDataPacket() {
    }

    public ServerMapDataPacket(int mapId, byte scale, boolean trackingPosition, MapIcon icons[]) {
        this(mapId, scale, trackingPosition, icons, null);
    }

    public ServerMapDataPacket(int mapId, byte scale, boolean trackingPosition, MapIcon icons[], MapData data) {
        this.mapId = mapId;
        this.scale = scale;
        this.trackingPosition = trackingPosition;
        this.icons = icons;
        this.data = data;
    }

    public int getMapId() {
        return mapId;
    }

    public byte getScale() {
        return scale;
    }

    public boolean getTrackingPosition() {
        return trackingPosition;
    }

    public MapIcon[] getIcons() {
        return icons;
    }

    public MapData getData() {
        return data;
    }

    @Override
    public void read(NetInput in) throws IOException {
        mapId = in.readVarInt();
        scale = in.readByte();
        trackingPosition = in.readBoolean();
        icons = new MapIcon[in.readVarInt()];
        for(int index = 0; index < icons.length; index++) {
            int data = in.readUnsignedByte();
            int type = (data >> 4) & 15;
            int rotation = data & 15;
            int x = in.readUnsignedByte();
            int z = in.readUnsignedByte();
            icons[index] = new MapIcon(x, z, MagicValues.value(MapIconType.class, type), rotation);
        }

        int columns = in.readUnsignedByte();
        if(columns > 0) {
            int rows = in.readUnsignedByte();
            int x = in.readUnsignedByte();
            int y = in.readUnsignedByte();
            byte data[] = in.readBytes(in.readVarInt());
            this.data = new MapData(columns, rows, x, y, data);
        }
    }

    @Override
    public void write(NetOutput out) throws IOException {
        out.writeVarInt(mapId);
        out.writeByte(scale);
        out.writeBoolean(trackingPosition);
        out.writeVarInt(icons.length);
        for(int index = 0; index < icons.length; index++) {
            MapIcon icon = icons[index];
            int type = MagicValues.key(Integer.class, icon.getIconType());
            out.writeByte((type & 15) << 4 | icon.getIconRotation() & 15);
            out.writeByte(icon.getCenterX());
            out.writeByte(icon.getCenterZ());
        }

        if(data != null && data.getColumns() != 0) {
            out.writeByte(data.getColumns());
            out.writeByte(data.getRows());
            out.writeByte(data.getX());
            out.writeByte(data.getY());
            out.writeVarInt(data.getData().length);
            out.writeBytes(data.getData());
        } else {
            out.writeByte(0);
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
