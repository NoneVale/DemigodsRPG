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

package com.demigodsrpg.game.command.admin;

import com.demigodsrpg.game.DGGame;
import com.demigodsrpg.game.command.type.BaseCommand;
import com.demigodsrpg.game.command.type.CommandResult;
import com.demigodsrpg.game.deity.Faction;
import com.google.common.base.Joiner;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateFactionCommand extends BaseCommand {
    @Override
    protected CommandResult onCommand(CommandSender sender, Command command, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            return CommandResult.PLAYER_ONLY;
        }
        if (DGGame.PLAYER_R.fromPlayer((Player) sender).getAdminMode()) {
            if (args.length < 3) {
                return CommandResult.INVALID_SYNTAX;
            }

            List<String> parts = new ArrayList<>(Arrays.asList(args));
            parts.subList(3, parts.size() - 1); // FIXME IDK if this will work how I want

            String name = args[0];
            String color = ChatColor.translateAlternateColorCodes('&', args[1]);
            String chatSymbol = args[2];
            String welcomeMessage = Joiner.on(" ").join(parts);

            Faction newFaction = new Faction(name, color, chatSymbol, welcomeMessage);
            DGGame.FACTION_R.register(newFaction);

            sender.sendMessage("You've successfully created the " + color + name + " faction!");

            return CommandResult.SUCCESS;
        }
        return CommandResult.NO_PERMISSIONS;
    }
}
