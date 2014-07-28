package com.demigodsrpg.demigods.classic;

import com.demigodsrpg.demigods.classic.command.AllianceCommand;
import com.demigodsrpg.demigods.classic.command.BindsCommand;
import com.demigodsrpg.demigods.classic.command.CheckCommand;
import com.demigodsrpg.demigods.classic.command.ValuesCommand;
import com.demigodsrpg.demigods.classic.listener.PlayerListener;
import com.demigodsrpg.demigods.classic.listener.ShrineListener;
import com.demigodsrpg.demigods.classic.listener.TributeListener;
import com.demigodsrpg.demigods.classic.model.PlayerModel;
import com.demigodsrpg.demigods.classic.model.TributeModel;
import com.demigodsrpg.demigods.classic.registry.*;
import com.demigodsrpg.demigods.classic.util.ZoneUtil;
import com.google.common.base.Supplier;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


// FIXME: Massive memory leak.

public class DGClassic extends JavaPlugin {
    // -- PLUGIN RELATED CONSTANTS -- //

    public static DGClassic INST;
    public static Logger CONSOLE;
    public static String SAVE_PATH;


    // -- REGISTRIES -- //

    public static final PlayerRegistry PLAYER_R = new PlayerRegistry();
    public static final ShrineRegistry SHRINE_R = new ShrineRegistry();
    public static final TributeRegistry TRIBUTE_R = new TributeRegistry();
    public static final SpawnRegistry SPAWN_R = new SpawnRegistry();
    public static final BattleRegistry BATTLE_R = new BattleRegistry();
    public static final AbilityRegistry ABILITY_R = new AbilityRegistry();
    public static final ServerDataRegistry SERV_R = new ServerDataRegistry();
    public static final ConcurrentMap<String, TerritoryRegistry> TERR_R = new ConcurrentHashMap<>();

    // -- TEMP DATA -- //

    public static final Table<String, String, Object> TEMP_DATA = Tables.newCustomTable(new ConcurrentHashMap<String, Map<String, Object>>(), new Supplier<Map<String, Object>>() {
        @Override
        public Map<String, Object> get() {
            return new ConcurrentHashMap<>();
        }
    });

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

        // Determine territory registries
        for (World world : Bukkit.getWorlds()) {
            TerritoryRegistry terr_r = new TerritoryRegistry(world);
            TERR_R.put(world.getName(), new TerritoryRegistry(world));
        }

        // Register the abilities
        ABILITY_R.registerAbilities();

        // Start the threads
        startThreads();

        // Register the listeners
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(new PlayerListener(), this);
        manager.registerEvents(new ShrineListener(), this);
        manager.registerEvents(new TributeListener(), this);
        manager.registerEvents(ABILITY_R, this);

        // Register commands
        getCommand("alliance").setExecutor(new AllianceCommand());
        getCommand("check").setExecutor(new CheckCommand());
        getCommand("binds").setExecutor(new BindsCommand());
        getCommand("values").setExecutor(new ValuesCommand());

        // TODO Admin commands

        // Enable ZoneUtil
        ZoneUtil.init();

        // Let the console know
        CONSOLE.info("Enabled and ready for battle.");
    }

    @Override
    public void onDisable() {
        // Ensure that we unregister our commands and tasks
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
    }

    // -- TASK RELATED -- //

    private static final BukkitRunnable SYNC, ASYNC, FAVOR, VALUE;


    static {
        SYNC = new BukkitRunnable() {
            @Override
            public void run() {
                // Update online players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (ZoneUtil.inNoDGCZone(player.getLocation())) continue;
                    PlayerModel model = PLAYER_R.fromPlayer(player);
                    if (model != null) {
                        model.updateCanPvp();
                    }
                }
            }
        };
        ASYNC = new BukkitRunnable() {
            @Override
            public void run() {
                // Update Timed Data
                SERV_R.clearExpired();
            }
        };
        FAVOR = new BukkitRunnable() {
            @Override
            public void run() {
                // Update Favor
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (ZoneUtil.inNoDGCZone(player.getLocation())) continue;
                    PlayerModel model = PLAYER_R.fromPlayer(player);
                    if (model != null) {
                        model.updateFavor();
                    }
                }
            }
        };
        VALUE = new TributeModel.ValueTask();
    }

    @SuppressWarnings("deprecation")
    public void startThreads() {
        BukkitScheduler scheduler = Bukkit.getScheduler();

        // Start sync demigods runnable
        scheduler.scheduleSyncRepeatingTask(this, SYNC, 20, 20);
        CONSOLE.info("Main Demigods SYNC runnable enabled...");

        // Start async demigods runnable
        scheduler.scheduleAsyncRepeatingTask(this, ASYNC, 20, 20);
        CONSOLE.info("Main Demigods ASYNC runnable enabled...");

        // Start async favor runnable
        scheduler.scheduleAsyncRepeatingTask(this, FAVOR, 20, (long) ((double) Setting.FAVOR_REGEN_SECONDS.get() * 20));
        CONSOLE.info("Favor regeneration (" + (TimeUnit.SECONDS.toMillis((long) (double) Setting.FAVOR_REGEN_SECONDS.get())) + ") runnable enabled...");

        // Start async value runnable
        scheduler.scheduleAsyncRepeatingTask(this, VALUE, 60, 200);
        CONSOLE.info("Main Demigods VALUE runnable enabled...");
    }

    public static DGClassic getInst() {
        return INST;
    }
}
