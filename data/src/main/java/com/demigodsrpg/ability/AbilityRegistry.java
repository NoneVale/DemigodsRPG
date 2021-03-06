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

package com.demigodsrpg.ability;

import com.censoredsoftware.library.util.StringUtil2;
import com.demigodsrpg.aspect.Aspect;
import com.demigodsrpg.aspect.Aspects;
import com.demigodsrpg.data.DGData;
import com.demigodsrpg.data.Setting;
import com.demigodsrpg.data.model.PlayerModel;
import com.demigodsrpg.util.ZoneUtil;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class AbilityRegistry implements Listener {
    // FIXME Do we really need two collections for the same data? This is expensive...

    private static final ConcurrentMap<String, AbilityMetaData> REGISTERED_COMMANDS = new ConcurrentHashMap<>();
    private static final Multimap<String, AbilityMetaData> REGISTERED_ABILITIES = Multimaps.newListMultimap(new ConcurrentHashMap<>(), () -> new ArrayList<>(0));

    @EventHandler(priority = EventPriority.LOWEST)
    private void onEvent(PlayerInteractEvent event) {
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK:
            case LEFT_CLICK_AIR:
                return;
        }
        for (AbilityMetaData ability : REGISTERED_ABILITIES.get(event.getClass().getName())) {
            try {
                PlayerModel model = DGData.PLAYER_R.fromPlayer(event.getPlayer());
                if (processAbility1(model, ability)) {
                    Object rawResult = ability.getMethod().invoke(ability.getAspect(), event);
                    processAbility2(event.getPlayer(), model, ability, rawResult);
                    event.setCancelled(true);
                    return;
                }
            } catch (Exception oops) {
                oops.printStackTrace();
            }

        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onEvent(EntityDamageByEntityEvent event) {
        for (AbilityMetaData ability : REGISTERED_ABILITIES.get(event.getClass().getName())) {
            try {
                if (event.getDamager() instanceof Player) {
                    Player player = (Player) event.getDamager();
                    PlayerModel model = DGData.PLAYER_R.fromPlayer(player);
                    if (processAbility1(model, ability)) {
                        Object rawResult = ability.getMethod().invoke(ability.getAspect(), event);
                        processAbility2(player, model, ability, rawResult);
                    }
                }
            } catch (Exception oops) {
                oops.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onEvent(FurnaceSmeltEvent event) {
        for (AbilityMetaData ability : REGISTERED_ABILITIES.get(event.getClass().getName())) {
            for (PlayerModel model : DGData.PLAYER_R.fromAspect(ability.getAspect())) {
                try {
                    if (model.getOnline() && model.getLocation().getWorld().equals(event.getBlock().getWorld()) && model.getLocation().distance(event.getBlock().getLocation()) < (int) Math.round(20 * Math.pow(model.getExperience(Aspects.CRAFTING_ASPECT_I), 0.15))) {
                        if (processAbility1(model, ability)) {
                            Object rawResult = ability.getMethod().invoke(ability.getAspect(), event);
                            processAbility2(null, model, ability, rawResult);
                            return; // TODO
                        }
                    }
                } catch (Exception oops) {
                    oops.printStackTrace();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void bindCommands(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        message = message.substring(1);
        String[] args = message.split("\\s+");
        Player player = event.getPlayer();

        if (!ZoneUtil.inNoDGZone(event.getPlayer().getLocation())) {
            // Process the command
            try {
                if (args.length == 2 && "info".equals(args[1])) {
                    if (abilityInfo(player, args[0].toLowerCase())) {
                        DGData.CONSOLE.info(event.getPlayer().getName() + " used the command: /" + message);
                        event.setCancelled(true);
                        return;
                    }
                }
                if (bindAbility(player, args[0].toLowerCase())) {
                    DGData.CONSOLE.info(event.getPlayer().getName() + " used the command: /" + message);
                    event.setCancelled(true);
                }
            } catch (Exception errored) {
                // Not a command
                errored.printStackTrace();
            }
        }
    }

    boolean abilityInfo(Player player, String command) {
        for (AbilityMetaData ability : REGISTERED_ABILITIES.values()) {
            if (ability.getCommand().equals(command)) {
                player.sendMessage(StringUtil2.chatTitle(ability.getName()));
                player.sendMessage(" - Aspect: " + ability.getAspect().getGroup().getColor() + ability.getAspect().name());
                player.sendMessage(" - Type: " + StringUtil2.beautify(ability.getType().name()));
                if (!ability.getType().equals(Ability.Type.PASSIVE)) {
                    player.sendMessage(" - Cost: " + ability.getCost());
                }
                if (ability.getCooldown() > 0) {
                    player.sendMessage(" - Cooldown (ms): " + ability.getCooldown());
                }
                player.sendMessage(ability.getInfo());
                return true;
            }
        }
        return false;
    }

    boolean bindAbility(Player player, String command) {
        // Is this a correct command?
        if (!REGISTERED_COMMANDS.keySet().contains(command)) {
            return false;
        }

        // Can't bind to air.
        if (player.getItemInHand() == null || Material.AIR.equals(player.getItemInHand().getType())) {
            abilityInfo(player, command);
            return true;
        }

        PlayerModel model = DGData.PLAYER_R.fromPlayer(player);
        Material material = player.getItemInHand().getType();
        AbilityMetaData bound = model.getBound(material);
        if (bound != null) {
            if (!bound.getCommand().equals(command)) {
                player.sendMessage(ChatColor.RED + "This item already has /" + bound.getCommand() + " bound to it.");
                return true;
            } else {
                model.unbind(bound);
                player.sendMessage(ChatColor.YELLOW + bound.getName() + " has been unbound.");
                return true;
            }
        } else {
            AbilityMetaData ability = fromCommand(command);
            if (ability.getCommand().equals(command) && model.getAspects().contains(ability.getAspect().name()) && ability.getCommand().equals(command)) {
                model.bind(ability, material);
                player.sendMessage(ChatColor.YELLOW + ability.getName() + " has been bound to " + StringUtil2.beautify(material.name()) + ".");
                return true;
            }
        }
        return false;
    }

    boolean processAbility1(PlayerModel model, AbilityMetaData ability) {
        if (ZoneUtil.inNoDGZone(model.getLocation())) return false;
        if (!ability.getType().equals(Ability.Type.PASSIVE)) {
            if ((ability.getType().equals(Ability.Type.OFFENSIVE) || ability.getType().equals(Ability.Type.ULTIMATE)) && ZoneUtil.inNoPvpZone(model.getLocation())) {
                return false;
            }
            if (model.getBound(ability) == null) {
                return false;
            }
            if (!model.getOfflinePlayer().getPlayer().getItemInHand().getType().equals(model.getBound(ability))) {
                return false;
            }

            double cost = ability.getCost();
            if (!Setting.NO_COST_ASPECT_MODE && model.getFavor() < cost) {
                model.getOfflinePlayer().getPlayer().sendMessage(ChatColor.YELLOW + ability.getName() + " requires more favor.");
                return false;
            }
            if (DGData.SERVER_R.contains(model.getMojangId(), ability + ":delay")) {
                return false;
            }
            if (DGData.SERVER_R.contains(model.getMojangId(), ability + ":cooldown")) {
                model.getOfflinePlayer().getPlayer().sendMessage(ChatColor.YELLOW + ability.getName() + " is on a cooldown.");
                return false;
            }
        }
        return true;
    }

    void processAbility2(Player player, PlayerModel model, AbilityMetaData ability, @Nullable Object rawResult) {
        // Check for result
        if (rawResult == null) {
            throw new NullPointerException("An ability (" + ability.getName() + ") returned null while casting.");
        }

        try {
            // Process result
            AbilityResult result = (AbilityResult) rawResult;
            switch (result) {
                case SUCCESS: {
                    break;
                }
                case NO_TARGET_FOUND: {
                    player.sendMessage(ChatColor.YELLOW + "No target found.");
                    return;
                }
                case OTHER_FAILURE: {
                    return;
                }
            }

            // Process ability
            double cost = ability.getCost();
            long delay = ability.getDelay();
            long cooldown = ability.getCooldown();

            if (delay > 0) {
                DGData.SERVER_R.put(model.getMojangId(), ability + ":delay", true, delay, TimeUnit.MILLISECONDS);
            }
            if (cooldown > 0) {
                DGData.SERVER_R.put(model.getMojangId(), ability + ":cooldown", true, cooldown, TimeUnit.MILLISECONDS);
            }
            if (!Setting.NO_COST_ASPECT_MODE && cost > 0) {
                model.setFavor(model.getFavor() - cost);
            }
        } catch (Exception ignored) {
        }
    }

    public List<AbilityMetaData> getAbilities(Aspect aspect) {
        Class<? extends Aspect> deityClass = aspect.getClass();
        List<AbilityMetaData> abilityMetaDatas = new ArrayList<>();
        for (Method method : deityClass.getMethods()) {
            if (method.isAnnotationPresent(Ability.class)) {
                Ability ability = method.getAnnotation(Ability.class);
                abilityMetaDatas.add(new AbilityMetaData(aspect, method, ability));
            }
        }
        return abilityMetaDatas;
    }

    public void registerAbilities() {
        for (Aspect aspect : Aspects.values()) {
            Class<? extends Aspect> deityClass = aspect.getClass();
            for (Method method : deityClass.getMethods()) {
                if (method.isAnnotationPresent(Ability.class)) {
                    Ability ability = method.getAnnotation(Ability.class);
                    register(aspect, method, ability);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    void register(Aspect aspect, Method method, Ability ability) {
        if (ability.placeholder()) return;
        Class<?>[] paramaters = method.getParameterTypes();
        try {
            if (paramaters.length < 1) {
                DGData.CONSOLE.severe("An ability (" + ability.name() + ") tried to register without any parameters.");
                return;
            }
            Class<? extends Event> eventClass = (Class<? extends Event>) paramaters[0];
            AbilityMetaData data = new AbilityMetaData(aspect, method, ability);
            REGISTERED_ABILITIES.put(eventClass.getName(), data);
            if (!"".equals(data.getCommand())) {
                REGISTERED_COMMANDS.put(data.getCommand(), data);
            }
        } catch (Exception oops) {
            oops.printStackTrace();
        }
    }

    public static AbilityMetaData fromCommand(String commandName) {
        if (REGISTERED_COMMANDS.containsKey(commandName)) {
            return REGISTERED_COMMANDS.get(commandName);
        }
        return null;
    }
}
