package io.github.origincts;

import java.util.function.Consumer;

/** A failed request must not consume Home; launcher cancellation is an explicit pre-request step. */
final class LaunchSequence {
    static void run(Runnable before, Runnable request, Runnable accepted, Consumer<Throwable> report) {
        if (before != null) before.run();
        request.run();
        if (accepted != null) {
            try { accepted.run(); }
            catch (Throwable failure) { report.accept(failure); }
        }
    }
}
