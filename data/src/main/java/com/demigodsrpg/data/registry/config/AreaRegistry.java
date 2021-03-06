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

package com.demigodsrpg.data.registry.config;


import com.demigodsrpg.data.area.Area;
import com.demigodsrpg.data.area.ClaimRoom;
import com.demigodsrpg.data.area.FactionTerritory;
import com.demigodsrpg.util.DataSection;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.stream.Collectors;

public class AreaRegistry extends AbstractConfigRegistry<Area> {
    private final World WORLD;
    private final String FILE_NAME;

    public AreaRegistry(World world) {
        WORLD = world;
        FILE_NAME = world.getName() + ".areas";
    }

    public World getWorld() {
        return WORLD;
    }

    @Override
    public Area valueFromData(String stringKey, DataSection data) {
        String areaType = stringKey.split("\\$")[0];
        if ("faction".equals(areaType)) {
            return new FactionTerritory(stringKey, data);
        } else if ("claimroom".equals(areaType)) {
            return new ClaimRoom(stringKey, data);
        }
        throw new NullPointerException("There is no area of type \"" + areaType + ".\"");
    }

    @Override
    public String getName() {
        return FILE_NAME;
    }

    public List<Area> fromLocation(final Location location) {
        return getRegistered().parallelStream().filter(area -> area.contains(location)).collect(Collectors.toList());
    }

    @Override
    protected boolean isPretty() {
        return true;
    }
}

