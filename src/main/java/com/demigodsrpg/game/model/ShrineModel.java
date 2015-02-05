package com.demigodsrpg.game.model;

import com.censoredsoftware.library.schematic.Point;
import com.demigodsrpg.game.DGGame;
import com.demigodsrpg.game.aspect.Aspect;
import com.demigodsrpg.game.deity.Faction;
import com.demigodsrpg.game.shrine.Shrine;
import com.demigodsrpg.game.shrine.ShrineWorld;
import com.demigodsrpg.game.util.JsonSection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ShrineModel extends AbstractPersistentModel<String> {
    private final String shrineId;
    private final String ownerMojangId;
    private final Aspect aspect;
    private final Shrine shrine;
    private final Point point;
    private final Location location;

    public ShrineModel(String name, Player player, Aspect aspect, Shrine shrine, final Location location) {
        shrineId = name;
        ownerMojangId = player.getUniqueId().toString();
        this.aspect = aspect;
        this.shrine = shrine;
        this.point = new Point(location.getBlockX(), location.getBlockY(), location.getBlockZ(), new ShrineWorld(location.getWorld()));
        this.location = location;
    }

    public ShrineModel(String shrineId, JsonSection conf) {
        this.shrineId = shrineId;
        ownerMojangId = conf.getString("owner_id");
        aspect = Aspect.valueOf(conf.getString("deity"));
        shrine = Shrine.valueOf(conf.getString("type"));

        World world = Bukkit.getWorld(conf.getString("world_name"));
        if (world != null) {
            int x = conf.getInt("x");
            int y = conf.getInt("y");
            int z = conf.getInt("z");
            point = new Point(x, y, z, new ShrineWorld(world));
            location = new Location(world, x, y, z);
        } else {
            throw new NullPointerException("World not found for a shrine location.");
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("owner_id", ownerMojangId);
        map.put("deity", aspect.name());
        map.put("type", shrine.name());
        map.put("world_name", point.getWorld().getName());
        map.put("x", point.getX());
        map.put("y", point.getY());
        map.put("z", point.getZ());
        return map;
    }

    public Faction getFaction() {
        return DGGame.PLAYER_R.fromId(ownerMojangId).getFaction();
    }

    public String getOwnerMojangId() {
        return ownerMojangId;
    }

    public Aspect getAspect() {
        return aspect;
    }

    public Shrine getShrineType() {
        return shrine;
    }

    public Location getLocation() {
        return location;
    }

    public Point getPoint() {
        return point;
    }

    public Location getClickable() {
        return getShrineType().getClickable(location);
    }

    public Location getSafeTeleport() {
        return getShrineType().getSafeTeleport(location);
    }

    @Override
    public String getPersistentId() {
        return shrineId;
    }
}