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

package com.demigodsrpg.game.listener;

import com.demigodsrpg.aspect.Aspect;
import com.demigodsrpg.aspect.Groups;
import com.demigodsrpg.data.DGData;
import com.demigodsrpg.data.Setting;
import com.demigodsrpg.data.area.Area;
import com.demigodsrpg.data.area.ClaimRoom;
import com.demigodsrpg.data.area.FactionTerritory;
import com.demigodsrpg.data.deity.Deity;
import com.demigodsrpg.data.deity.DeityType;
import com.demigodsrpg.data.deity.Faction;
import com.demigodsrpg.data.model.PlayerModel;
import com.demigodsrpg.data.registry.config.AreaRegistry;
import com.demigodsrpg.util.ZoneUtil;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AreaListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        // Get the world
        World world = event.getTo().getWorld();

        // If it isn't a DG world, don't listen anymore
        if (!ZoneUtil.isNoDGWorld(world)) {
            AreaRegistry area_r = DGData.AREA_R.get(world.getName());

            // Get the possible areas
            List<Area> areasFrom = area_r.fromLocation(event.getFrom());
            List<Area> areasTo = area_r.fromLocation(event.getTo());

            // Make sure the areas exist
            if (!areasTo.isEmpty()) {

                // Remove all overlapping areas
                areasTo.removeAll(areasFrom);

                // Iterate over all new areas
                for (Area area : areasTo) {
                    // Faction territories
                    if (area instanceof FactionTerritory) {
                        // Handle the faction territory, check if it should cancel the event
                        FactionTerritory factionArea = (FactionTerritory) area;
                        if (!handleFactionAreas(factionArea, event.getPlayer(), event.getTo(), !(event instanceof PlayerTeleportEvent))) {
                            // Cancel the event
                            event.setCancelled(true);

                            // Bounce back
                            Vector victor = event.getPlayer().getVelocity();
                            victor.multiply(-8); // TODO This is wonky
                            victor.setY(0.5);
                            event.getPlayer().setVelocity(victor);
                        }
                    }

                    // Claim rooms
                    else if (area instanceof ClaimRoom) {
                        ClaimRoom claimRoom = (ClaimRoom) area;
                        handleClaimAreas(claimRoom, event.getPlayer());
                    }
                }
            }
        }
    }

    // -- HELPER METHODS -- //

    private boolean handleFactionAreas(FactionTerritory area, Player player, Location forward, boolean block) {
        // Important info
        PlayerModel model = DGData.PLAYER_R.fromPlayer(player);
        Faction faction = area.getFaction();

        // Check to make sure the player is in the right faction
        if (!faction.equals(model.getFaction()) && !model.getAdminMode()) {
            // Throttle the warning message
            if (!DGData.SERVER_R.contains(model.getMojangId(), "faction-area")) {
                player.sendMessage(ChatColor.RED + "You are not a member of the " + faction.getColor() + faction.getName() + org.bukkit.ChatColor.RED + " faction.");
                DGData.SERVER_R.put(model.getMojangId(), "faction-area", false, 4, TimeUnit.SECONDS);
            }

            // Send a fake invisible wall to prevent the player from moving forward
            if (block) {
                Material wall = Setting.DEBUG_INVISIBLE_WALLS ? Material.BRICK : Material.BARRIER;
                Location forwardTop = forward.clone().add(0, 1, 0);
                player.sendBlockChange(forward, wall, (byte) 0);
                player.sendBlockChange(forwardTop, wall, (byte) 0);
            }

            // Cancel the event
            return false;
        }

        // Throttle the welcome message
        if (!DGData.SERVER_R.contains(model.getMojangId(), "faction-area")) {
            player.sendMessage(faction.getColor() + faction.getWelcomeMessage());
            DGData.SERVER_R.put(model.getMojangId(), "faction-area", true, 4, TimeUnit.SECONDS);
        }

        // Don't cancel the event
        return true;
    }

    private void handleClaimAreas(ClaimRoom area, Player player) {
        // Important info
        PlayerModel model = DGData.PLAYER_R.fromPlayer(player);
        Deity deity = area.getDeity();

        String endMessage = ChatColor.YELLOW + "You have chosen ";

        // Set the correct type (and potentially faction if the deity is a hero)
        switch (deity.getDeityType()) {
            case HERO:
                model.setHero(deity);
                endMessage += deity.getFactions().get(0).getColor() + deity.getName() + ChatColor.YELLOW + " as your parent Hero.";
                break;
            case GOD:
                model.setGod(deity);
                String color = ChatColor.WHITE.toString();
                if (model.getHero().isPresent()) {
                    color = model.getHero().get().getFactions().get(0).getColor();
                }
                endMessage += color + deity.getName() + ChatColor.YELLOW + " as your parent God.";
                break;
        }

        // Send the appropriate messages
        player.sendMessage(endMessage);

        // Add starting aspects
        for (Aspect.Group group : deity.getAspectGroups()) {
            List<Aspect> inGroup = Groups.aspectsInGroup(group);
            for (Aspect aspect : inGroup) {
                // Hero aspect
                if (DeityType.HERO.equals(deity.getDeityType()) && Aspect.Tier.HERO.equals(aspect.getTier())) {
                    model.giveHeroAspect(deity, aspect);
                    player.sendMessage(ChatColor.YELLOW + StringUtils.capitalize(deity.getPronouns()[0]) + " has placed you in the " + deity.getFactions().get(0).getColor() + deity.getFactions().get(0).getName() + ChatColor.YELLOW + " faction.");
                    break;
                }

                // God tier I aspect
                else if (DeityType.GOD.equals(deity.getDeityType()) && Aspect.Tier.I.equals(aspect.getTier())) {
                    model.giveAspect(aspect);
                }
            }
        }

        // If there is a next location, teleport the player to it
        if (area.getNextLocation() != null) {
            player.teleport(area.getNextLocation());
        }

        // Save the model
        DGData.PLAYER_R.register(model);
    }
}
