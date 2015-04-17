/*
 * Copyright 2015 Demigods RPG
 * Copyright 2015 Alexander Chauncey
 * Copyright 2015 Alex Bennett
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.demigodsrpg.game;

import com.demigodsrpg.chitchat.Chitchat;
import com.demigodsrpg.game.command.*;
import com.demigodsrpg.game.command.admin.*;
import com.demigodsrpg.game.deity.Faction;
import com.demigodsrpg.game.integration.chitchat.FactionChatTag;
import com.demigodsrpg.game.integration.chitchat.FactionIdTag;
import com.demigodsrpg.game.listener.*;
import com.demigodsrpg.game.model.PlayerModel;
import com.demigodsrpg.game.model.TributeModel;
import com.demigodsrpg.game.registry.*;
import com.demigodsrpg.game.util.ZoneUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class DGGame extends JavaPlugin {
    // -- PLUGIN RELATED CONSTANTS -- //

    private static DGGame INST;
    public static Logger CONSOLE;
    public static String SAVE_PATH;

    // -- REGISTRIES -- //

    public static final PlayerRegistry PLAYER_R = new PlayerRegistry();
    public static final ShrineRegistry SHRINE_R = new ShrineRegistry();
    public static final TributeRegistry TRIBUTE_R = new TributeRegistry();
    public static final SpawnRegistry SPAWN_R = new SpawnRegistry();
    public static final FactionRegistry FACTION_R = new FactionRegistry();
    public static final BattleRegistry BATTLE_R = new BattleRegistry();
    public static final AbilityRegistry ABILITY_R = new AbilityRegistry();
    public static final DeityRegistry DEITY_R = new DeityRegistry();
    public static final ServerDataRegistry SERVER_R = new ServerDataRegistry();
    public static final ConcurrentMap<String, AreaRegistry> AREA_R = new ConcurrentHashMap<>();

    // -- PLUGIN RELATED INSTANCE METHODS -- //

    @Override
    public void onEnable() {
        // Define the instance
        INST = this;

        // Define the logger
        CONSOLE = getLogger();

        // Config
        getConfig().options().copyDefaults(true);
        saveConfig();

        // Define the save path
        SAVE_PATH = getDataFolder().getPath() + "/data/";

        // Get custom factions and deities
        FACTION_R.registerFromFile();
        DEITY_R.registerFromFile();

        // Register default factions
        FACTION_R.register(Faction.NEUTRAL);
        FACTION_R.register(Faction.EXCOMMUNICATED);

        // Debug data
        if (Setting.DEBUG_DATA) {
            // Debug deities
            DEITY_R.register(Demo.D.LOREM);
            DEITY_R.register(Demo.D.IPSUM);
            DEITY_R.register(Demo.D.DOLOR);
            DEITY_R.register(Demo.D.SIT);
            DEITY_R.register(Demo.D.AMET);

            // Debug factions
            FACTION_R.register(Demo.F.KŌHAI);
            FACTION_R.register(Demo.F.SENPAI);
            FACTION_R.register(Demo.F.SENSEI);
        }

        // Determine territory registries
        for (World world : Bukkit.getWorlds()) {
            AreaRegistry area_r = new AreaRegistry(world);
            area_r.registerFromFile();
            AREA_R.put(world.getName(), new AreaRegistry(world));
        }

        // Register the abilities
        ABILITY_R.registerAbilities();

        // Regen shrines
        SHRINE_R.generate();

        // Fill up tribute data
        if (TRIBUTE_R.getRegistered().isEmpty()) {
            TRIBUTE_R.initializeTributeTracking();
        }

        // Start the threads
        startThreads();

        // Register the listeners
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(new InventoryListener(), this);
        manager.registerEvents(new PlayerListener(), this);
        manager.registerEvents(new BattleListener(), this);
        manager.registerEvents(new ShrineListener(), this);
        manager.registerEvents(new TributeListener(), this);
        manager.registerEvents(new AreaListener(), this);
        manager.registerEvents(ABILITY_R, this);

        // Register commands
        getCommand("faction").setExecutor(new FactionCommand());
        getCommand("binds").setExecutor(new BindsCommand());
        getCommand("check").setExecutor(new CheckCommand());
        getCommand("aspect").setExecutor(new AspectCommand());
        getCommand("cleanse").setExecutor(new CleanseCommand());
        getCommand("shrine").setExecutor(new ShrineCommand());
        getCommand("values").setExecutor(new ValuesCommand());

        // Admin commands
        getCommand("adminmode").setExecutor(new AdminModeComand());
        getCommand("selectarea").setExecutor(new SelectAreaCommand());
        getCommand("createfaction").setExecutor(new CreateFactionCommand());
        getCommand("createfactionarea").setExecutor(new CreateFactionAreaCommand());
        getCommand("checkplayer").setExecutor(new CheckPlayerCommand());
        getCommand("adddevotion").setExecutor(new AddDevotionCommand());
        getCommand("removedevotion").setExecutor(new RemoveDevotionCommand());
        getCommand("giveaspect").setExecutor(new GiveAspectCommand());
        getCommand("removeaspect").setExecutor(new RemoveAspectCommand());
        getCommand("setfaction").setExecutor(new SetFactionCommand());

        // Enable ZoneUtil
        ZoneUtil.init();

        // Handle Chitchat integration
        if (manager.isPluginEnabled("Chitchat")) {
            Chitchat.getChatFormat().add(new FactionChatTag());
            Chitchat.getChatFormat().add(new FactionIdTag());
        }

        // Let the console know
        CONSOLE.info("Enabled and ready for battle.");
    }

    @Override
    public void onDisable() {
        // Ensure that we unregister our commands and tasks
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);

        // Clear the cache.
        clearCache();

        // Let the console know
        CONSOLE.info("Disabled successfully.");
    }

    // -- PLUGIN RELATED UTILITY METHODS -- //

    private static void clearCache() {
        PLAYER_R.clearCache();
        SHRINE_R.clearCache();
        FACTION_R.clearCache();
        TRIBUTE_R.clearCache();
        SPAWN_R.clearCache();
        DEITY_R.clearCache();
        SERVER_R.clearCache();

        AREA_R.values().forEach(AreaRegistry::clearCache);
    }

    // -- TASK RELATED -- //

    private static final Runnable SYNC, ASYNC, FIRE_SPREAD, BATTLE, VALUE;

    static {
        SYNC = () -> {
            // Update online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (ZoneUtil.inNoDGZone(player.getLocation())) continue;
                PlayerModel model = PLAYER_R.fromPlayer(player);
                if (model != null) {
                    model.updateCanPvp();
                }
            }
        };
        ASYNC = SERVER_R::clearExpired;
        FIRE_SPREAD = () -> {
            for (World world : Bukkit.getWorlds()) {
                world.getLivingEntities().stream().filter(entity -> entity.getFireTicks() > 0).forEach(entity ->
                                entity.getNearbyEntities(0.5, 0.5, 0.5).stream().filter(nearby -> nearby instanceof LivingEntity && !nearby.equals(entity)).forEach(nearby -> nearby.setFireTicks(100))
                );
            }
        };
        BATTLE = BATTLE_R::endExpired;
        VALUE = new TributeModel.ValueTask();
    }

    @SuppressWarnings("deprecation")
    void startThreads() {
        BukkitScheduler scheduler = Bukkit.getScheduler();

        // Start sync demigods runnable
        scheduler.scheduleSyncRepeatingTask(this, SYNC, 20, 20);
        CONSOLE.info("Main Demigods SYNC thread enabled...");

        // Start async demigods runnable
        scheduler.scheduleAsyncRepeatingTask(this, ASYNC, 20, 20);
        CONSOLE.info("Main Demigods ASYNC thread enabled...");

        // Start sync fire runnable
        scheduler.scheduleSyncRepeatingTask(this, FIRE_SPREAD, 3, 20);
        CONSOLE.info("Demigods FIRE SPREAD task enabled...");

        // Start sync fire runnable
        scheduler.scheduleSyncRepeatingTask(this, BATTLE, 3, 20);
        CONSOLE.info("Demigods BATTLE task enabled...");

        // Start async value runnable
        scheduler.scheduleAsyncRepeatingTask(this, VALUE, 60, 400);
        CONSOLE.info("Demigods VALUE task enabled...");
    }

    public static DGGame getInst() {
        return INST;
    }
}
