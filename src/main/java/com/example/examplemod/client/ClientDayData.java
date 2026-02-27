package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;

public class ClientDayData {
    private static long currentDay = 0;
    private static long clientTicks = 0;
    private static final long TICKS_PER_DAY = 24000L;
    
    public static void setDay(long day) {
        currentDay = day;
    }
    
    public static long getDay() {
        return currentDay;
    }
    
    public static void tick() {
        clientTicks++;
        if (clientTicks >= TICKS_PER_DAY) {
            clientTicks = 0;
        }
    }
    
    public static long getDayProgress() {
        return clientTicks;
    }
    
    public static void reset() {
        currentDay = 0;
        clientTicks = 0;
    }
}
