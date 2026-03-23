package com.group15.gymtracker.domain;

public class StreakTracker {

    public static class State {
        public final int currentStreak;
        public final int longestStreak;
        public final int freezeTokens;

        public State(int currentStreak, int longestStreak, int freezeTokens) {
            this.currentStreak = currentStreak;
            this.longestStreak = longestStreak;
            this.freezeTokens = freezeTokens;
        }
    }

    public static class Result {
        public final State newState;
        public final boolean shouldLock;
        public final boolean shouldUnlock;
        public final String reason;

        public Result(State newState, boolean shouldLock, boolean shouldUnlock, String reason) {
            this.newState = newState;
            this.shouldLock = shouldLock;
            this.shouldUnlock = shouldUnlock;
            this.reason = reason;
        }
    }

    public Result evaluate(boolean isGymDay, boolean targetMet, State oldState) {
        int current = oldState.currentStreak;
        int longest = oldState.longestStreak;
        int freeze = oldState.freezeTokens;

        if (!isGymDay) {
            return new Result(
                    new State(current, longest, freeze),
                    false,
                    true,
                    "Yesterday was not a gym day"
            );
        }

        if (targetMet) {
            current += 1;
            if (current > longest) {
                longest = current;
            }
            if (current % 5 == 0) {
                freeze += 1;
            }
            return new Result(
                    new State(current, longest, freeze),
                    false,
                    true,
                    "Target met"
            );
        }

        if (freeze > 0) {
            freeze -= 1;
            return new Result(
                    new State(current, longest, freeze),
                    false,
                    true,
                    "Freeze token used"
            );
        }

        current = 0;
        return new Result(
                new State(current, longest, freeze),
                true,
                false,
                "Missed gym day"
        );
    }
}
