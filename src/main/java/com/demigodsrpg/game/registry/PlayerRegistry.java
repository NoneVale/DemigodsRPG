package com.demigodsrpg.game.registry;

import com.demigodsrpg.game.DGGame;
import com.demigodsrpg.game.aspect.Aspect;
import com.demigodsrpg.game.deity.Deity;
import com.demigodsrpg.game.deity.Faction;
import com.demigodsrpg.game.model.PlayerModel;
import com.demigodsrpg.game.util.JsonSection;
import org.spongepowered.api.entity.player.Player;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PlayerRegistry extends AbstractRegistry<PlayerModel> {
    private static final String FILE_NAME = "players.dgdat";

    @Deprecated
    public PlayerModel fromName(final String name) {
        Optional<PlayerModel> player = getRegistered().stream().filter(model -> model.getLastKnownName().equalsIgnoreCase(name)).findAny();
        if (player.isPresent()) {
            return player.get();
        }
        return null;
    }

    public PlayerModel fromPlayer(Player player) {
        PlayerModel found = fromId(player.getUniqueId().toString());
        if (found == null) {
            found = new PlayerModel(player);
            register(found);
        }
        return found;
    }

    public Set<Player> getOfflinePlayers() {
        return getRegistered().stream().map(PlayerModel::getPlayer).collect(Collectors.toSet());
    }

    public Set<PlayerModel> getOnlineInAlliance(Faction alliance) {
        Set<PlayerModel> players = new HashSet<>();
        for (Player player : DGGame.SERVER.getOnlinePlayers()) {
            PlayerModel model = fromPlayer(player);
            if (model.getFaction().equals(alliance)) {
                players.add(model);
            }
        }
        return players;
    }

    public Set<PlayerModel> fromDeity(Deity deity) {
        return getRegistered().parallelStream().filter(model -> model.hasDeity(deity)).collect(Collectors.toSet());
    }

    public Collection<PlayerModel> fromAspect(final Aspect aspect) {
        return getRegistered().stream().filter(model -> model.getAspects().contains(aspect.getGroup() + " " + aspect.getTier().name())).collect(Collectors.toList());
    }

    public List<String> getNameStartsWith(final String name) {
        return getRegistered().stream().filter(model -> TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - model.getLastLoginTime()) < 3
                && model.getLastKnownName().toLowerCase().startsWith(name.toLowerCase())).map(PlayerModel::getLastKnownName).collect(Collectors.toList());
    }

    @Override
    public String getFileName() {
        return FILE_NAME;
    }

    @Override
    public PlayerModel valueFromData(String stringKey, JsonSection data) {
        return new PlayerModel(stringKey, data);
    }
}
