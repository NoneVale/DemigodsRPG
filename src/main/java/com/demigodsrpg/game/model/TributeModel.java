package com.demigodsrpg.game.model;

import com.censoredsoftware.library.util.MapUtil2;
import com.demigodsrpg.game.DGGame;
import com.demigodsrpg.game.registry.TributeRegistry;
import com.demigodsrpg.game.util.JsonSection;
import org.spongepowered.api.item.ItemType;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class TributeModel extends AbstractPersistentModel<String> {
    private static final double VALUE_K = 14.286;
    private static double OFFSET = 1.0;

    private final ItemType material;
    private List<Double> tributeTimes;
    private int fitness;
    private final TributeRegistry.Category category;
    private double lastKnownValue;

    public TributeModel(ItemType material, JsonSection conf) {
        this.material = material;
        tributeTimes = conf.getDoubleList("tribute_times");
        fitness = conf.getInt("fitness");
        category = DGGame.TRIBUTE_R.getCategory(material);
        lastKnownValue = conf.getDouble("last_known_value");
    }

    public TributeModel(ItemType material, int fitness) {
        this.material = material;
        tributeTimes = new ArrayList<>();
        this.fitness = fitness;
        category = DGGame.TRIBUTE_R.getCategory(material);
        lastKnownValue = 1.0;
    }

    public ItemType getMaterial() {
        return material;
    }

    List<Double> getTributeTimes() {
        long twoWeeksAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14);
        List<Double> tributeTimesClone = new ArrayList<>();
        tributeTimesClone.addAll(this.tributeTimes);
        tributeTimesClone.stream().filter(time -> time < twoWeeksAgo).forEach(this.tributeTimes::remove);
        return tributeTimes;
    }

    public void addTributeTime() {
        tributeTimes.add((double) System.currentTimeMillis());
    }

    public int getFitness() {
        return fitness;
    }

    public void setFitness(int amount) {
        this.fitness = amount;
        tributeTimes.add((double) System.currentTimeMillis());
        DGGame.TRIBUTE_R.register(this);
    }

    public TributeRegistry.Category getCategory() {
        return category;
    }

    double getFrequency() {
        return getTributeTimes().size() / 336;
    }

    public double getLastKnownValue() {
        return lastKnownValue;
    }

    private void updateValue() {
        if (getCategory().equals(TributeRegistry.Category.WORTHLESS)) {
            lastKnownValue = 0.0;
        } else if (getCategory().equals(TributeRegistry.Category.CHEATING)) {
            lastKnownValue = -3000.0;
        } else {
            lastKnownValue = (getValuePercentage() / OFFSET) * VALUE_K * DGGame.TRIBUTE_R.getRegistered().size();
        }
        DGGame.TRIBUTE_R.register(this);
    }

    private double getValuePercentage() {
        Collection<TributeModel> allInCat = DGGame.TRIBUTE_R.find(getCategory());
        int size = allInCat.size();
        if (size < 2) {
            size = 2;
        }
        Map<TributeModel, Double> map = new HashMap<>();
        for (TributeModel model : allInCat) {
            map.put(model, model.getFrequency());
        }
        int count = 1;
        double rankInCategory = 1.0;
        for (TributeModel model : MapUtil2.sortByValue(map, false).keySet()) {
            if (model.equals(this)) {
                rankInCategory = (double) count;
                break;
            }
        }
        double fractionOfCategory = rankInCategory / size;
        double categoryFitness = DGGame.TRIBUTE_R.getTributesForCategory(getCategory());
        double fractionOfTotal = 1 - (categoryFitness / DGGame.TRIBUTE_R.getTotalTributes());
        return fractionOfTotal * fractionOfCategory;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("tribute_times", tributeTimes);
        map.put("fitness", fitness);
        map.put("last_known_value", lastKnownValue);
        return map;
    }

    @Override
    public String getPersistentId() {
        return getMaterial().getId();
    }

    public static class ValueTask implements Runnable {
        @Override
        public void run() {
            OFFSET = 1.0;
            for (TributeModel model : DGGame.TRIBUTE_R.getRegistered()) {
                OFFSET += model.getValuePercentage();
            }

            for (TributeModel model : DGGame.TRIBUTE_R.getRegistered()) {
                // Trim the tribute times
                if (model.tributeTimes.size() > 300) {
                    model.tributeTimes = model.tributeTimes.subList(model.tributeTimes.size() - 31, model.tributeTimes.size() - 1);
                }

                // Update the value
                model.updateValue();
            }
        }
    }
}
