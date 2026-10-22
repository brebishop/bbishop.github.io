package com.example.brenabishopweighttracker;

import java.util.List;

/**
 * Analyzes a recent chronological window of weight history and reports whether
 * the user is moving toward the saved goal, away from it, or remaining stable.
 */
public class TrendAnalyzer {

    private static final int DEFAULT_WINDOW_SIZE = 5;
    private static final double STABLE_DISTANCE_THRESHOLD = 0.1;

    public enum TrendDirection {
        TOWARD_GOAL,
        AWAY_FROM_GOAL,
        STABLE,
        GOAL_REACHED,
        INSUFFICIENT_DATA,
        NO_GOAL,
        NO_DATA
    }

    /**
     * Analyzes up to the five most recent entries. The supplied list must be
     * ordered from oldest to newest.
     */
    public TrendResult analyze(List<WeightEntry> history, double goalWeight) {
        if (history == null || history.isEmpty()) {
            return TrendResult.empty(TrendDirection.NO_DATA);
        }

        WeightEntry newestEntry = history.get(history.size() - 1);

        if (goalWeight <= 0) {
            return TrendResult.withLatest(
                    TrendDirection.NO_GOAL,
                    newestEntry.getWeight()
            );
        }

        if (history.size() < 2) {
            TrendDirection direction = newestEntry.getWeight() <= goalWeight
                    ? TrendDirection.GOAL_REACHED
                    : TrendDirection.INSUFFICIENT_DATA;

            return TrendResult.withLatest(direction, newestEntry.getWeight());
        }

        int startIndex = Math.max(0, history.size() - DEFAULT_WINDOW_SIZE);
        int entriesAnalyzed = history.size() - startIndex;

        WeightEntry oldestEntry = history.get(startIndex);
        double totalChange = 0;

        for (int index = startIndex + 1; index < history.size(); index++) {
            double previousWeight = history.get(index - 1).getWeight();
            double currentWeight = history.get(index).getWeight();
            totalChange += currentWeight - previousWeight;
        }

        double averageChange = totalChange / (entriesAnalyzed - 1);
        double oldDistanceFromGoal = Math.abs(oldestEntry.getWeight() - goalWeight);
        double newDistanceFromGoal = Math.abs(newestEntry.getWeight() - goalWeight);
        double distanceImprovement = oldDistanceFromGoal - newDistanceFromGoal;

        TrendDirection direction;

        // The existing Weight Tracker defines its goal as a weight-loss goal.
        if (newestEntry.getWeight() <= goalWeight) {
            direction = TrendDirection.GOAL_REACHED;
        } else if (distanceImprovement > STABLE_DISTANCE_THRESHOLD) {
            direction = TrendDirection.TOWARD_GOAL;
        } else if (distanceImprovement < -STABLE_DISTANCE_THRESHOLD) {
            direction = TrendDirection.AWAY_FROM_GOAL;
        } else {
            direction = TrendDirection.STABLE;
        }

        return new TrendResult(
                direction,
                newestEntry.getWeight(),
                totalChange,
                averageChange,
                Math.abs(distanceImprovement),
                entriesAnalyzed
        );
    }

    public static final class TrendResult {
        private final TrendDirection direction;
        private final double latestWeight;
        private final double totalChange;
        private final double averageChange;
        private final double distanceChange;
        private final int entriesAnalyzed;

        private TrendResult(
                TrendDirection direction,
                double latestWeight,
                double totalChange,
                double averageChange,
                double distanceChange,
                int entriesAnalyzed
        ) {
            this.direction = direction;
            this.latestWeight = latestWeight;
            this.totalChange = totalChange;
            this.averageChange = averageChange;
            this.distanceChange = distanceChange;
            this.entriesAnalyzed = entriesAnalyzed;
        }

        private static TrendResult empty(TrendDirection direction) {
            return new TrendResult(direction, 0, 0, 0, 0, 0);
        }

        private static TrendResult withLatest(TrendDirection direction, double latestWeight) {
            return new TrendResult(direction, latestWeight, 0, 0, 0, 1);
        }

        public TrendDirection getDirection() {
            return direction;
        }

        public double getLatestWeight() {
            return latestWeight;
        }

        public double getTotalChange() {
            return totalChange;
        }

        public double getAverageChange() {
            return averageChange;
        }

        public double getDistanceChange() {
            return distanceChange;
        }

        public int getEntriesAnalyzed() {
            return entriesAnalyzed;
        }
    }
}
