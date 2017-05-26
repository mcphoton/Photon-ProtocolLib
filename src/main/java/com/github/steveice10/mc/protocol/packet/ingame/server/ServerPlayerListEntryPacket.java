package com.github.steveice10.mc.protocol.packet.ingame.server;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.util.ReflectionToString;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.MagicValues;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.packet.Packet;

import java.io.IOException;
import java.util.UUID;

public class ServerPlayerListEntryPacket implements Packet {
    private PlayerListEntryAction action;
    private PlayerListEntry entries[];

    @SuppressWarnings("unused")
    private ServerPlayerListEntryPacket() {
    }

    public ServerPlayerListEntryPacket(PlayerListEntryAction action, PlayerListEntry entries[]) {
        this.action = action;
        this.entries = entries;
    }

    public PlayerListEntryAction getAction() {
        return action;
    }

    public PlayerListEntry[] getEntries() {
        return entries;
    }

    @Override
    public void read(NetInput in) throws IOException {
        action = MagicValues.key(PlayerListEntryAction.class, in.readVarInt());
        entries = new PlayerListEntry[in.readVarInt()];
        for(int count = 0; count < entries.length; count++) {
            UUID uuid = in.readUUID();
            GameProfile profile;
            if(action == PlayerListEntryAction.ADD_PLAYER) {
                profile = new GameProfile(uuid, in.readString());
            } else {
                profile = new GameProfile(uuid, null);
            }

            PlayerListEntry entry = null;
            switch(action) {
                case ADD_PLAYER:
                    int properties = in.readVarInt();
                    for(int index = 0; index < properties; index++) {
                        String propertyName = in.readString();
                        String value = in.readString();
                        String signature = null;
                        if(in.readBoolean()) {
                            signature = in.readString();
                        }

                        profile.getProperties().add(new GameProfile.Property(propertyName, value, signature));
                    }

                    int g = in.readVarInt();
                    GameMode gameMode = MagicValues.key(GameMode.class, g < 0 ? 0 : g);
                    int ping = in.readVarInt();
                    Message displayName = null;
                    if(in.readBoolean()) {
                        displayName = Message.fromString(in.readString());
                    }

                    entry = new PlayerListEntry(profile, gameMode, ping, displayName);
                    break;
                case UPDATE_GAMEMODE:
                    g = in.readVarInt();
                    GameMode mode = MagicValues.key(GameMode.class, g < 0 ? 0 : g);
                    entry = new PlayerListEntry(profile, mode);
                    break;
                case UPDATE_LATENCY:
                    int png = in.readVarInt();
                    entry = new PlayerListEntry(profile, png);
                    break;
                case UPDATE_DISPLAY_NAME:
                    Message disp = null;
                    if(in.readBoolean()) {
                        disp = Message.fromString(in.readString());
                    }

                    entry = new PlayerListEntry(profile, disp);
                case REMOVE_PLAYER:
                    entry = new PlayerListEntry(profile);
                    break;
            }

            entries[count] = entry;
        }
    }

    @Override
    public void write(NetOutput out) throws IOException {
        out.writeVarInt(MagicValues.value(Integer.class, action));
        out.writeVarInt(entries.length);
        for(PlayerListEntry entry : entries) {
            out.writeUUID(entry.getProfile().getId());
            switch(action) {
                case ADD_PLAYER:
                    out.writeString(entry.getProfile().getName());
                    out.writeVarInt(entry.getProfile().getProperties().size());
                    for(GameProfile.Property property : entry.getProfile().getProperties()) {
                        out.writeString(property.getName());
                        out.writeString(property.getValue());
                        out.writeBoolean(property.hasSignature());
                        if(property.hasSignature()) {
                            out.writeString(property.getSignature());
                        }
                    }

                    out.writeVarInt(MagicValues.value(Integer.class, entry.getGameMode()));
                    out.writeVarInt(entry.getPing());
                    out.writeBoolean(entry.getDisplayName() != null);
                    if(entry.getDisplayName() != null) {
                        out.writeString(entry.getDisplayName().toJsonString());
                    }

                    break;
                case UPDATE_GAMEMODE:
                    out.writeVarInt(MagicValues.value(Integer.class, entry.getGameMode()));
                    break;
                case UPDATE_LATENCY:
                    out.writeVarInt(entry.getPing());
                    break;
                case UPDATE_DISPLAY_NAME:
                    out.writeBoolean(entry.getDisplayName() != null);
                    if(entry.getDisplayName() != null) {
                        out.writeString(entry.getDisplayName().toJsonString());
                    }

                    break;
                case REMOVE_PLAYER:
                    break;
            }
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
