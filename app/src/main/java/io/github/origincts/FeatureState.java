package io.github.origincts;

/** Persist only public feature flags. Unknown bits can never enable a feature. */
final class FeatureState {
    static final int MASTER = 1, CTS = 2, GEMINI = 4, TRANSLATE = 8, ALL = 15;
    static boolean enabled(int state, int feature) {
        return (state & MASTER) != 0 && (state & feature) == feature && (feature & ~ALL) == 0;
    }
    static int set(int state, int feature, boolean enabled) {
        return (enabled ? state | feature : state & ~feature) & ALL;
    }
}
