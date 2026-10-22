package com.example.brenabishopweighttracker;

/**
 * Owns the application rule used to determine whether a weight-loss goal is reached.
 */
public class GoalEvaluator {

    public boolean isGoalReached(double currentWeight, double goalWeight) {
        return goalWeight > 0 && currentWeight <= goalWeight;
    }
}
