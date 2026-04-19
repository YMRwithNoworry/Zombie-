package alku.zombie_plus;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class DayTracker extends SavedData {
    private static final String DATA_NAME = "zombie_plus_days";
    private long elapsedTicks;
    private int lastSyncDay = -1;

    public DayTracker() {
        this.elapsedTicks = 0;
    }

    public static DayTracker load(CompoundTag tag, HolderLookup.Provider registries) {
        DayTracker tracker = new DayTracker();
        tracker.elapsedTicks = tag.getLong("elapsedTicks");
        return tracker;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("elapsedTicks", elapsedTicks);
        return tag;
    }

    public void tick() {
        elapsedTicks++;
        setDirty();
    }

    public int getCurrentDay() {
        return (int) (elapsedTicks / 24000L) + 1;
    }

    public long getElapsedTicks() {
        return elapsedTicks;
    }

    public void setCurrentDay(int day) {
        this.elapsedTicks = (long) (day - 1) * 24000L;
        this.lastSyncDay = -1;
        setDirty();
    }

    public boolean shouldSync() {
        int currentDay = getCurrentDay();
        if (currentDay != lastSyncDay) {
            lastSyncDay = currentDay;
            return true;
        }
        return false;
    }

    public static DayTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DayTracker::new, DayTracker::load, null),
                DATA_NAME
        );
    }
}
