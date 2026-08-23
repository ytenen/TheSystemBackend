package org.smorodin.infrastructure.persistence;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.smorodin.core.model.Stats;


@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Data
public class StatsEmbedded {
    private int strength;
    private int agility;
    private int vitality;
    private int intelligence;
    private int perception;
    private int will;

    public static StatsEmbedded of(Stats stats){
        return new StatsEmbedded(stats.getStrength(), stats.getAgility(),
                stats.getVitality(), stats.getIntelligence(),
                stats.getPerception(), stats.getWill());
    }

    public Stats toDomain(){
        return new Stats.Builder()
                .strength(this.strength)
                .agility(this.agility)
                .vitality(this.vitality)
                .intelligence(this.intelligence)
                .perception(this.perception)
                .will(this.will).build();
    }
}
