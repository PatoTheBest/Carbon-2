package com.lastabyss.carbon.utils;

import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketListener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.spigotmc.SneakyThrow;

import io.netty.buffer.ByteBuf;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Navid
 */
public class Utils {

    /**
     * Registers a bukkit command without the need for a plugin.yml entry.
     *
     * @param fallbackPrefix
     * @param cmd
     */
    public static void registerBukkitCommand(String fallbackPrefix, Command cmd) {
        CommandMap cmap = getFieldValue(CraftServer.class, "commandMap", Bukkit.getServer());
        cmap.register(fallbackPrefix, cmd);
    }

    /**
     * Adds entity type to bukkit entity types enum and returns it
     *
     * @param name - name of the entitytype
     * @param id - id of the entitytype
     * @param entityClass - entity class
     * @return
     */
    public static EntityType addEntity(String name, int id, Class<? extends Entity> entityClass) {
        EntityType entityType = DynamicEnumType.addEnum(EntityType.class, name, new Class[]{String.class, entityClass.getClass(), Integer.TYPE}, new Object[]{name, entityClass.getClass(), id});
        Utils.<Map<String, EntityType>>getFieldValue(EntityType.class, "NAME_MAP", null).put(name, entityType);
        Utils.<Map<Short, EntityType>>getFieldValue(EntityType.class, "ID_MAP", null).put((short) id, entityType);
        return entityType;
    }

    /**
     * Adds material to bukkit material enum end returns it
     *
     * @param name - name of the material
     * @param id - id of the material
     * @return
     */
    public static Material addMaterial(String name, int id) {
        Material material = DynamicEnumType.addEnum(Material.class, name, new Class[]{Integer.TYPE}, new Object[]{id});
        Utils.<Map<String, Material>>getFieldValue(Material.class, "BY_NAME", null).put(name, material);
        Material[] byId = getFieldValue(Material.class, "byId", null);
        byId[id] = material;
        setFieldValue(Material.class, "byId", null, byId);
        return material;
    }

    /**
     * Adds material with data to bukkit material enum end returns it
     *
     * @param name
     * @param id
     * @param data
     * @return
     */
    public static Material addMaterial(String name, int id, short data) {
        Material material = DynamicEnumType.addEnum(Material.class, name, new Class[]{Integer.TYPE}, new Object[]{id});
        Utils.<Map<String, Material>>getFieldValue(Material.class, "BY_NAME", null).put(name, material);
        Material[] byId = getFieldValue(Material.class, "byId", null);
        byId[id] = material;
        setFieldValue(Material.class, "byId", null, byId);
        Material[] durability = getFieldValue(Material.class, "durability", null);
        durability[data] = material;
        setFieldValue(Material.class, "durability", null, byId);
        return material;
    }

    /**
     * Returns 0 if there's an error accessing the "strength" field in the minecraft server Blocks class, otherwise, returns the block's given strength.
     *
     * @param b
     * @return
     */
    public static float getBlockStrength(net.minecraft.server.v1_8_R3.Block b) {
        return getFieldValue(b.getClass(), "strength", b);
    }

    /**
     * Returns 0 if there's an error accessing the "durability" field in the minecraft server Blocks class, otherwise, returns the block's given strength.
     *
     * @param b
     * @return
     */
    public static float getBlockDurability(net.minecraft.server.v1_8_R3.Block b) {
        return getFieldValue(b.getClass(), "durability", b);
    }

    /**
     * Returns all adjacent blocks to a specified block. Returns an empty list if none were found. Air is not included.
     *
     * @param source
     * @return
     */
    public static List<Block> getAllAdjacentBlocks(Block source) {
        List<Block> list = new ArrayList<Block>();
        for (BlockFace f : BlockFace.values()) {
            Block rel = source.getRelative(f);
            if (rel.getType() != Material.AIR) {
                list.add(rel);
            }
        }
        return list;
    }

    /**
     * Sends packet to a player
     *
     * @param player
     * @param packet
     */
    public static void sendPacket(Player player, Packet<? extends PacketListener> packet) {
        EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        nmsPlayer.playerConnection.sendPacket(packet);
    }

    /**
     * Sets accessibleobject accessible state an returns this object
     *
     * @param <T>
     * @param object
     * @return
     */
    public static <T extends AccessibleObject> T setAccessible(T object) {
        object.setAccessible(true);
        return object;
    }

    /**
     * Sets final field to the provided value
     *
     * @param field - the field which should be modified
     * @param obj - the object whose field should be modified
     * @param newValue - the new value for the field of obj being modified
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static void setFinalField(Field field, Object obj, Object newValue) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        setAccessible(Field.class.getDeclaredField("modifiers")).setInt(field, field.getModifiers() & ~Modifier.FINAL);
        setAccessible(Field.class.getDeclaredField("root")).set(field, null);
        setAccessible(Field.class.getDeclaredField("overrideFieldAccessor")).set(field, null);
        setAccessible(field).set(obj, newValue);
    }

    /**
     * Returns ByteBuf contents as byte array
     * 
     * @param buf
     * @return
     */
    public static byte[] toArray(ByteBuf buf) {
        byte[] result = new byte[buf.readableBytes()];
        buf.readBytes(result);
        return result;
    }

    /**
     * Gets field reflectively
     * 
     * @param clazz
     * @param fieldName
     * @param obj
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Class<?> clazz, String fieldName, Object obj) {
        try {
            return (T) setAccessible(clazz.getDeclaredField(fieldName)).get(obj);
        } catch (Throwable t) {
            SneakyThrow.sneaky(t);
        }
        return null;
    }

    /**
     * Sets field reflectively
     * 
     * @param clazz
     * @param fieldName
     * @param obj
     * @param value
     */
    public static void setFieldValue(Class<?> clazz, String fieldName, Object obj, Object value) {
        try {
            setAccessible(clazz.getDeclaredField(fieldName)).set(obj, value);
        } catch (Throwable t) {
            SneakyThrow.sneaky(t);
        }
    }

}
