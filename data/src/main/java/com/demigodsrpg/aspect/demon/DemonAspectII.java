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

package com.demigodsrpg.aspect.demon;

import com.censoredsoftware.library.bukkitutil.ItemUtil;
import com.demigodsrpg.ability.Ability;
import com.demigodsrpg.ability.AbilityResult;
import com.demigodsrpg.aspect.Aspect;
import com.demigodsrpg.aspect.Aspects;
import com.demigodsrpg.aspect.Groups;
import com.demigodsrpg.data.DGData;
import com.demigodsrpg.data.model.PlayerModel;
import com.demigodsrpg.util.TargetingUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;

public class DemonAspectII implements Aspect {
    @Override
    public Group getGroup() {
        return Groups.DEMON_ASPECT;
    }

    @Override
    public ItemStack getItem() {
        return ItemUtil.create(Material.OBSIDIAN, name(), Collections.singletonList(getInfo()), null);
    }

    @Override
    public int getId() {
        return -2;
    }

    @Override
    public String getInfo() {
        return "Blood of a demon.";
    }

    @Override
    public Tier getTier() {
        return Tier.I;
    }

    @Override
    public String name() {
        return "Soulless";
    }

    @Ability(name = "Entomb", command = "entomb", info = "Entomb an entity in obsidian.", cost = 470, cooldown = 20000)
    public AbilityResult entombAbility(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerModel model = DGData.PLAYER_R.fromPlayer(player);

        LivingEntity le = TargetingUtil.autoTarget(player);
        if (le == null) return AbilityResult.NO_TARGET_FOUND;
        int duration = (int) Math.round(2.18678 * Math.pow(model.getExperience(Aspects.DEMON_ASPECT_II), 0.24723)); // seconds
        final ArrayList<Block> tochange = new ArrayList<Block>();
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block block = player.getWorld().getBlockAt(le.getLocation().getBlockX() + x, le.getLocation().getBlockY() + y, le.getLocation().getBlockZ() + z);
                    if ((block.getLocation().distance(le.getLocation()) > 2) && (block.getLocation().distance(le.getLocation()) < 3.5))
                        if ((block.getType() == Material.AIR) || (block.getType() == Material.WATER) || (block.getType() == Material.LAVA)) {
                            block.setType(Material.OBSIDIAN);
                            tochange.add(block);
                        }
                }
            }
        }

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(DGData.PLUGIN, () -> {
            tochange.stream().filter(b -> b.getType() == Material.OBSIDIAN).forEach(b -> b.setType(Material.AIR));
        }, duration * 20);

        return AbilityResult.SUCCESS;
    }
}