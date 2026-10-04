/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 */
package code.wiosvisual.utils.math.script;

import code.wiosvisual.utils.math.script.ScriptAction;
import code.wiosvisual.utils.math.time.StopWatch;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

public class Script {
    private final StopWatch time = new StopWatch();
    private final List<ScriptStep> scriptSteps = Lists.newCopyOnWriteArrayList();
    private final List<ScriptTickStep> scriptTickSteps = Lists.newCopyOnWriteArrayList();
    private int currentStepIndex;
    private int currentTickStepIndex;
    private boolean interrupt;
    private LoopStrategy loopStrategy = new FiniteLoopStrategy(1);

    public Script() {
        this.cleanup();
    }

    public Script addStep(int delay, ScriptAction action) {
        return this.addStep(delay, action, () -> true, 0);
    }

    public Script addStep(int delay, ScriptAction action, BooleanSupplier condition) {
        return this.addStep(delay, action, condition, 0);
    }

    public Script addStep(int delay, ScriptAction action, int priority) {
        return this.addStep(delay, action, () -> true, priority);
    }

    public Script addStep(int delay, ScriptAction action, BooleanSupplier condition, int priority) {
        this.scriptSteps.add(new ScriptStep(delay, action, condition, priority));
        Collections.sort(this.scriptSteps);
        return this;
    }

    public Script addTickStep(int ticks, ScriptAction action) {
        return this.addTickStep(ticks, action, () -> true, 0);
    }

    public Script addTickStep(int ticks, ScriptAction action, BooleanSupplier condition) {
        return this.addTickStep(ticks, action, condition, 0);
    }

    public Script addTickStep(int ticks, ScriptAction action, int priority) {
        return this.addTickStep(ticks, action, () -> true, priority);
    }

    public Script addTickStep(int ticks, ScriptAction action, BooleanSupplier condition, int priority) {
        this.scriptTickSteps.add(new ScriptTickStep(ticks, action, condition, priority));
        Collections.sort(this.scriptTickSteps);
        return this;
    }

    public void resetTime() {
        this.time.reset();
    }

    public void resetStepIndex() {
        this.currentStepIndex = 0;
        this.currentTickStepIndex = 0;
    }

    public Script cleanupIfFinished() {
        if (this.isFinished()) {
            this.cleanup();
        }
        return this;
    }

    public Script cleanup() {
        this.scriptSteps.clear();
        this.scriptTickSteps.clear();
        this.resetTime();
        this.resetStepIndex();
        return this;
    }

    public void update() {
        if (this.scriptSteps.isEmpty() && this.scriptTickSteps.isEmpty() || this.interrupt) {
            return;
        }
        this.scriptSteps.forEach(step -> {
            ScriptStep currentStep;
            if (this.currentStepIndex < this.scriptSteps.size() && (currentStep = this.scriptSteps.get(this.currentStepIndex)).condition().getAsBoolean() && this.time.finished(currentStep.delay())) {
                currentStep.action().perform();
                ++this.currentStepIndex;
                this.resetTime();
                if (this.loopStrategy.shouldLoop(this.currentStepIndex, this.scriptSteps.size())) {
                    this.resetStepIndex();
                    this.loopStrategy.onLoop();
                }
            }
        });
        this.scriptTickSteps.forEach(step -> {
            if (this.currentTickStepIndex < this.scriptTickSteps.size()) {
                ScriptTickStep currentTickStep = this.scriptTickSteps.get(this.currentTickStepIndex);
                if (currentTickStep.condition().getAsBoolean() && currentTickStep.ticks() <= 0) {
                    currentTickStep.action().perform();
                    ++this.currentTickStepIndex;
                    this.resetTime();
                    if (this.loopStrategy.shouldLoop(this.currentTickStepIndex, this.scriptTickSteps.size())) {
                        this.resetStepIndex();
                        this.loopStrategy.onLoop();
                    }
                }
                currentTickStep.decrementTicks();
            }
        });
        this.currentStepIndex = Math.min(this.currentStepIndex, this.scriptSteps.size());
        this.currentTickStepIndex = Math.min(this.currentTickStepIndex, this.scriptTickSteps.size());
    }

    public Script setLoopStrategy(LoopStrategy loopStrategy) {
        this.loopStrategy = loopStrategy;
        return this;
    }

    public boolean isFinished() {
        return this.currentStepIndex >= this.scriptSteps.size() && this.currentTickStepIndex >= this.scriptTickSteps.size() && !this.interrupt && this.loopStrategy.isFinished();
    }

    public StopWatch getTime() {
        return this.time;
    }

    public List<ScriptStep> getScriptSteps() {
        return this.scriptSteps;
    }

    public List<ScriptTickStep> getScriptTickSteps() {
        return this.scriptTickSteps;
    }

    public int getCurrentStepIndex() {
        return this.currentStepIndex;
    }

    public int getCurrentTickStepIndex() {
        return this.currentTickStepIndex;
    }

    public boolean isInterrupt() {
        return this.interrupt;
    }

    public LoopStrategy getLoopStrategy() {
        return this.loopStrategy;
    }

    public void setCurrentStepIndex(int currentStepIndex) {
        this.currentStepIndex = currentStepIndex;
    }

    public void setCurrentTickStepIndex(int currentTickStepIndex) {
        this.currentTickStepIndex = currentTickStepIndex;
    }

    public void setInterrupt(boolean interrupt) {
        this.interrupt = interrupt;
    }

    public static class FiniteLoopStrategy
    implements LoopStrategy {
        private final int loopCount;
        private int currentLoop;

        public FiniteLoopStrategy(int loopCount) {
            this.loopCount = loopCount - 1;
        }

        @Override
        public boolean shouldLoop(int currentStepIndex, int totalSteps) {
            return currentStepIndex >= totalSteps && this.currentLoop < this.loopCount;
        }

        @Override
        public void onLoop() {
            ++this.currentLoop;
        }

        @Override
        public boolean isFinished() {
            return this.currentLoop >= this.loopCount;
        }
    }

    public static interface LoopStrategy {
        public boolean shouldLoop(int var1, int var2);

        public void onLoop();

        public boolean isFinished();
    }

    public static final class ScriptStep
    implements Comparable<ScriptStep> {
        private int delay;
        private ScriptAction action;
        private BooleanSupplier condition;
        private int priority;

        public ScriptStep(int delay, ScriptAction action, BooleanSupplier condition, int priority) {
            this.delay = delay;
            this.action = action;
            this.condition = condition;
            this.priority = priority;
        }

        @Override
        public int compareTo(ScriptStep otherStep) {
            return Integer.compare(otherStep.priority(), this.priority());
        }

        public int delay() {
            return this.delay;
        }

        public ScriptAction action() {
            return this.action;
        }

        public BooleanSupplier condition() {
            return this.condition;
        }

        public int priority() {
            return this.priority;
        }

        public ScriptStep delay(int delay) {
            this.delay = delay;
            return this;
        }

        public ScriptStep action(ScriptAction action) {
            this.action = action;
            return this;
        }

        public ScriptStep condition(BooleanSupplier condition) {
            this.condition = condition;
            return this;
        }

        public ScriptStep priority(int priority) {
            this.priority = priority;
            return this;
        }
    }

    public static final class ScriptTickStep
    implements Comparable<ScriptTickStep> {
        private int ticks;
        private ScriptAction action;
        private BooleanSupplier condition;
        private int priority;

        public ScriptTickStep(int ticks, ScriptAction action, BooleanSupplier condition, int priority) {
            this.ticks = ticks;
            this.action = action;
            this.condition = condition;
            this.priority = priority;
        }

        @Override
        public int compareTo(ScriptTickStep otherStep) {
            return Integer.compare(otherStep.priority(), this.priority());
        }

        public void decrementTicks() {
            --this.ticks;
        }

        public int ticks() {
            return this.ticks;
        }

        public ScriptAction action() {
            return this.action;
        }

        public BooleanSupplier condition() {
            return this.condition;
        }

        public int priority() {
            return this.priority;
        }

        public ScriptTickStep ticks(int ticks) {
            this.ticks = ticks;
            return this;
        }

        public ScriptTickStep action(ScriptAction action) {
            this.action = action;
            return this;
        }

        public ScriptTickStep condition(BooleanSupplier condition) {
            this.condition = condition;
            return this;
        }

        public ScriptTickStep priority(int priority) {
            this.priority = priority;
            return this;
        }
    }

    public static class InfiniteLoopStrategy
    implements LoopStrategy {
        @Override
        public boolean shouldLoop(int currentStepIndex, int totalSteps) {
            return currentStepIndex >= totalSteps;
        }

        @Override
        public void onLoop() {
        }

        @Override
        public boolean isFinished() {
            return false;
        }
    }
}
