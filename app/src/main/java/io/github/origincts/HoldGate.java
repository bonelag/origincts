package io.github.origincts;

/** Routes a candidate hold without letting the ordinary Home recognizer see its DOWN. */
final class HoldGate {
    static final int PASS = 0, BUFFER = 1, REPLAY = 2, DROP = 3;
    private int state = PASS;
    int down(boolean candidate) { state = candidate ? BUFFER : PASS; return state; }
    int move(boolean rejected) {
        if (state == BUFFER && rejected) { state = PASS; return REPLAY; }
        return state;
    }
    int up() {
        int route = state == BUFFER ? REPLAY : state;
        state = PASS;
        return route;
    }
    int cancel() {
        int route = state == PASS ? PASS : DROP;
        state = PASS;
        return route;
    }
    boolean claim() {
        if (state != BUFFER) return false;
        state = DROP;
        return true;
    }
    void release() { state = PASS; }
}
