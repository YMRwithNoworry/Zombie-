package alku.zombie_plus.client;

public class ClientData {
    private static long elapsedTicks;
    private static int totalDays = 100;

    public static void update(long ticks, int days) {
        elapsedTicks = ticks;
        totalDays = days;
    }

    public static int getCurrentDay() {
        return (int) (elapsedTicks / 24000L) + 1;
    }

    public static int getTotalDays() {
        return totalDays;
    }

    public static int getEvolutionStage() {
        if (totalDays <= 0) return 0;
        int currentDay = getCurrentDay();
        int stage = (currentDay * 9) / totalDays;
        return Math.min(stage, 9);
    }
}
