package com.tozyx.blocksidesound.client.rain;

import com.tozyx.blocksidesound.client.config.SoundConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RainPlaybackPlanTest {
    private static final Vec3 LISTENER = new Vec3(.5, 65, .5);
    private static final Set<RainSound> AVAILABLE = Set.of(RainSound.WOOD, RainSound.METAL, RainSound.GLASS,
            RainSound.FOLIAGE, RainSound.GRANULAR, RainSound.WATER, RainSound.SOFT, RainSound.PLANT_DRIP);
    private static RainSurface surface(int x, int z, RainMaterial material, boolean enhanced, boolean drip) {
        return new RainSurface(new BlockPos(x,64,z), new Vec3(x+.5,65,z+.5),
                new MaterialResolver.Match(material,enhanced,drip),0);
    }
    private static RainPlaybackPlan.Group group(RainSound kind) { return new RainPlaybackPlan.Group(0,0,kind); }
    private record Voice(RainSound kind, long start, long end) {}

    @Test void aSingleMetalBlockStartsAgainWhileItsPreviousFullTakeIsStillPlaying() {
        var config = new SoundConfig();
        config.rain.materials.get("metal").triggerProbability=0;
        config.rain.materials.get("metal").attemptIntervalTicks=1200;
        var iron=surface(0,0,RainMaterial.METAL,true,false);
        var plan=new RainPlaybackPlan();
        var random=RandomSource.create(1);
        assertEquals(1,plan.poll(0,List.of(iron),config,AVAILABLE,List.of(),1,random,LISTENER).size());
        long second=-1;
        for(long tick=1;tick<50;tick++) {
            var requests=plan.poll(tick,List.of(iron),config,AVAILABLE,List.of(group(RainSound.METAL)),0,random,LISTENER);
            if(!requests.isEmpty()) { second=tick; break; }
        }
        assertTrue(second>=30 && second<=36);
        assertTrue(second<60-16,"The next take must already be playing before the shortest metal take enters its outro");
    }

    @Test void tenMinuteMixedSceneMaintainsMatureOverlappingVoicesForEveryMaterial() {
        var kinds=List.of(RainSound.WOOD,RainSound.METAL,RainSound.GLASS,RainSound.FOLIAGE,
                RainSound.GRANULAR,RainSound.WATER,RainSound.SOFT);
        var surfaces=List.of(surface(0,0,RainMaterial.WOOD,true,false),surface(1,0,RainMaterial.METAL,true,false),
                surface(2,0,RainMaterial.GLASS,true,false),surface(3,0,RainMaterial.FOLIAGE,true,false),
                surface(4,0,RainMaterial.GRANULAR,true,false),surface(5,0,RainMaterial.WATER,true,false),
                surface(6,0,RainMaterial.SOFT,true,false));
        Map<RainSound,Integer> durations=Map.of(RainSound.WOOD,100,RainSound.METAL,60,RainSound.GLASS,128,
                RainSound.FOLIAGE,149,RainSound.GRANULAR,100,RainSound.WATER,100,RainSound.SOFT,142);
        var voices=new ArrayList<Voice>();
        var plan=new RainPlaybackPlan();
        var config=new SoundConfig();
        var random=RandomSource.create(2);
        long lastDispatch=-100;
        int overlappedStarts=0;
        for(long tick=0;tick<12_000;tick++) {
            final long now=tick;
            voices.removeIf(voice->voice.end()<=now);
            var occupied=voices.stream().map(voice->group(voice.kind())).toList();
            var requests=plan.poll(tick,surfaces,config,AVAILABLE,occupied,8-new HashSet<>(occupied).size(),random,LISTENER);
            for(var request:requests) {
                RainSound kind=request.group().sound();
                assertTrue(tick-lastDispatch>=2);
                if(voices.stream().anyMatch(voice->voice.kind()==kind && voice.end()-now>16)) overlappedStarts++;
                voices.add(new Voice(kind,tick,tick+durations.get(kind)));
                lastDispatch=tick;
            }
            assertTrue(voices.size()<=RainPlaybackPlan.MAX_VOICES);
            if(tick<50) continue;
            for(var kind:kinds) {
                double power=0;
                for(var voice:voices) if(voice.kind()==kind) {
                    // Adversarial source envelopes: 400 ms intro and 800 ms fading outro.
                    double amplitude=Math.min(1,(tick-voice.start())/8.0)*Math.min(1,(voice.end()-tick)/16.0);
                    power+=amplitude*amplitude*1.6/(durations.get(kind)/20.0);
                }
                assertTrue(power>.05,"No mature covering voice for "+kind+" at tick "+tick);
            }
        }
        assertTrue(overlappedStarts>2000,"Full recordings should regularly overlap, not only meet at EOF");
    }

    @Test void oneMetalBlockGetsIndependentStartsBesideManyWoodRegions() {
        var surfaces=new ArrayList<RainSurface>();
        for(int i=0;i<20;i++) surfaces.add(surface(i*8,0,RainMaterial.WOOD,true,false));
        surfaces.add(surface(0,2,RainMaterial.METAL,true,false));
        var plan=new RainPlaybackPlan();
        var occupied=new ArrayList<RainPlaybackPlan.Group>();
        var starts=new java.util.EnumMap<RainSound,Integer>(RainSound.class);
        for(long tick=0;tick<100;tick++) {
            // Keep one representative voice per kind; real take lifetimes are checked above.
            for(var request:plan.poll(tick,surfaces,new SoundConfig(),AVAILABLE,occupied,8-occupied.size(),
                    RandomSource.create(tick+4),LISTENER)) {
                starts.merge(request.group().sound(),1,Integer::sum);
                if(!occupied.contains(request.group())) occupied.add(request.group());
            }
        }
        assertTrue(starts.getOrDefault(RainSound.METAL,0)>=3);
        assertTrue(starts.getOrDefault(RainSound.WOOD,0)>=3);
    }

    @Test void perBlockCooldownAndGlobalDispatchSpacingApplyToNewStartsOnly() {
        var wood=surface(0,0,RainMaterial.WOOD,true,false);
        var plan=new RainPlaybackPlan();
        var random=RandomSource.create(8);
        long last=-100;
        int count=0;
        for(long tick=0;tick<200;tick++) {
            var requests=plan.poll(tick,List.of(wood),new SoundConfig(),AVAILABLE,List.of(),8,random,LISTENER);
            if(!requests.isEmpty()) { assertTrue(tick-last>=30);last=tick;count++; }
        }
        assertTrue(count>=5 && count<=7);
    }

    @Test void voiceCapsDoNotStopOrReplaceAlreadyPlayingTakes() {
        var wood=surface(0,0,RainMaterial.WOOD,true,false);
        var occupied=java.util.Collections.nCopies(RainPlaybackPlan.MAX_VOICES_PER_KIND,group(RainSound.WOOD));
        assertTrue(new RainPlaybackPlan().poll(100,List.of(wood),new SoundConfig(),AVAILABLE,occupied,8,
                RandomSource.create(9),LISTENER).isEmpty());
        occupied=java.util.Collections.nCopies(RainPlaybackPlan.MAX_VOICES,group(RainSound.METAL));
        assertTrue(new RainPlaybackPlan().poll(100,List.of(wood),new SoundConfig(),AVAILABLE,occupied,8,
                RandomSource.create(9),LISTENER).isEmpty());
        assertTrue(new RainPlaybackPlan().poll(100,List.of(wood),new SoundConfig(),AVAILABLE,List.of(),0,
                RandomSource.create(9),LISTENER).isEmpty());
    }

    @Test void invalidatingAnOldSurfaceLetsAnotherNearbySurfaceStartPromptly() {
        var left=surface(-2,0,RainMaterial.METAL,true,false);
        var right=surface(2,0,RainMaterial.METAL,true,false);
        var plan=new RainPlaybackPlan();
        var config=new SoundConfig();
        assertEquals(1,plan.poll(0,List.of(left),config,AVAILABLE,List.of(),1,RandomSource.create(10),LISTENER).size());
        plan.retry(group(RainSound.METAL));
        var request=plan.poll(2,List.of(right),config,AVAILABLE,List.of(group(RainSound.METAL)),0,
                RandomSource.create(11),LISTENER).getFirst();
        assertEquals(right.block(),request.surface().block());
    }

    @Test void disabledSilentExcludedAndMissingMaterialEventsStaySilent() {
        var config=new SoundConfig();
        config.rain.materials.get("wood").volume=0;
        config.rain.materials.get("metal").enabled=false;
        var surfaces=List.of(surface(0,0,RainMaterial.WOOD,true,false),surface(1,0,RainMaterial.METAL,true,false),
                surface(2,0,RainMaterial.STONE,true,false),surface(3,0,RainMaterial.GLASS,false,false));
        assertTrue(new RainPlaybackPlan().poll(0,surfaces,config,AVAILABLE,List.of(),8,RandomSource.create(12),LISTENER).isEmpty());
    }

    @Test void absentSurfacesNeverScheduleGhostVoices() {
        assertTrue(new RainPlaybackPlan().poll(100,List.of(),new SoundConfig(),AVAILABLE,
                List.of(group(RainSound.METAL)),8,RandomSource.create(13),LISTENER).isEmpty());
    }

    @Test void plantDripsKeepTheirProbabilityAndIntervalAndRemainIndependent() {
        var plant=surface(0,0,RainMaterial.FOLIAGE,false,true);
        var config=new SoundConfig();
        config.rain.materials.get("foliage").enabled=false;
        config.rain.plantDrips.triggerProbability=1;
        config.rain.plantDrips.attemptIntervalTicks=7;
        var plan=new RainPlaybackPlan();
        var random=RandomSource.create(14);
        int count=0;long last=-100;
        for(long tick=0;tick<70;tick++) {
            var requests=plan.poll(tick,List.of(plant),config,AVAILABLE,List.of(),8,random,LISTENER);
            if(!requests.isEmpty()) { assertEquals(RainSound.PLANT_DRIP,requests.getFirst().group().sound());
                assertTrue(tick-last>=7);last=tick;count++; }
        }
        assertEquals(10,count);
        config.rain.plantDrips.triggerProbability=0;
        assertTrue(plan.poll(100,List.of(plant),config,AVAILABLE,List.of(),8,random,LISTENER).isEmpty());
        config.rain.plantDrips.triggerProbability=1;
        config.rain.plantDrips.enabled=false;
        assertTrue(plan.poll(101,List.of(plant),config,AVAILABLE,List.of(),8,random,LISTENER).isEmpty());
    }

    @Test void newResourceEventsWorkAndClearDropsAllOldCooldowns() {
        var stone=surface(0,0,RainMaterial.STONE,true,false);
        var plan=new RainPlaybackPlan();
        assertTrue(plan.poll(0,List.of(stone),new SoundConfig(),AVAILABLE,List.of(),8,RandomSource.create(15),LISTENER).isEmpty());
        assertEquals(1,plan.poll(1000,List.of(stone),new SoundConfig(),Set.of(RainSound.STONE),List.of(),8,
                RandomSource.create(15),LISTENER).size());
        plan.clear();
        assertEquals(1,plan.poll(0,List.of(stone),new SoundConfig(),Set.of(RainSound.STONE),List.of(),8,
                RandomSource.create(15),LISTENER).size());
    }

    @Test void nearbyRealPositionsAreFavoredWithoutCollapsingSpatialMixing() {
        var near=surface(0,0,RainMaterial.WOOD,true,false);
        var far=surface(7,7,RainMaterial.WOOD,true,false);
        int count=0;var random=RandomSource.create(42);
        for(int i=0;i<1000;i++) if(RainPlaybackPlan.chooseSurface(List.of(near,far),LISTENER,random)==near) count++;
        assertTrue(count>950 && count<1000);
    }
}
