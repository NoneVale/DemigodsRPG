package com.demigodsrpg.game.aspect.demon;

import com.demigodsrpg.game.DGGame;
import com.demigodsrpg.game.ability.Ability;
import com.demigodsrpg.game.ability.AbilityResult;
import com.demigodsrpg.game.aspect.Aspect;
import com.demigodsrpg.game.aspect.Aspects;
import com.demigodsrpg.game.aspect.Groups;
import com.demigodsrpg.game.model.PlayerModel;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class DemonAspectIII implements Aspect {
    @Override
    public Group getGroup() {
        return Groups.DEMON_ASPECT;
    }

    @Override
    public int getId() {
        return -3;
    }

    @Override
    public String getInfo() {
        return "Blood of a demon.";
    }

    @Override
    public Tier getTier() {
        return Tier.I;
    }

    @Ability(name = "Demon Friendlies", info = "Demon monsters will not attack you.", type = Ability.Type.PASSIVE, placeholder = true)
    public void friendlyAbility() {
        // Do nothing, handled directly in the ability listener to save time
    }

    @Ability(name = "Curse", command = "curse", info = "Turns day to night as nothingness corrupts your enemies.", cost = 4000, cooldown = 600000, type = Ability.Type.ULTIMATE)
    public AbilityResult curseAbility(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerModel model = DGGame.PLAYER_R.fromPlayer(player);

        int amt = tartarus(player, model);
        if (amt > 0) {
            player.sendMessage(ChatColor.DARK_RED + "Nothingness" + ChatColor.GRAY + " has corrupted " + amt + " enemies.");
            player.getWorld().setTime(18000);
            return AbilityResult.SUCCESS;
        } else {
            player.sendMessage(ChatColor.YELLOW + "There were no valid targets or the ultimate could not be used.");
        }
        return AbilityResult.OTHER_FAILURE;
    }

    private int tartarus(Player p, PlayerModel m) {
        int range = (int) Math.round(18.83043 * Math.pow(m.getExperience(Aspects.DEMON_ASPECT_III), 0.088637));
        List<LivingEntity> entitylist = new ArrayList<>();
        for (Entity anEntity : p.getNearbyEntities(range, range, range)) {
            if (anEntity instanceof Player && m.getFaction().equals(DGGame.PLAYER_R.fromPlayer((Player) anEntity).getFaction())) {
                continue;
            }
            if (anEntity instanceof LivingEntity) {
                entitylist.add((LivingEntity) anEntity);
            }
        }
        int duration = (int) Math.round(30 * Math.pow(m.getExperience(Aspects.DEMON_ASPECT_III), 0.09)) * 20;
        for (LivingEntity le : entitylist) {
            target(le, duration);
        }
        return entitylist.size();
    }

    private void target(LivingEntity le, int duration) {
        le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 5));
        le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 5));
        le.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, duration, 5));
    }
}