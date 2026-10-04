/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.interactions.interact;

import code.wiosvisual.events.player.RotationUpdateEvent;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.client.managers.event.impl.EventUpdate;
import java.util.LinkedList;
import java.util.Queue;

public class ScriptManager {
    private final Queue<ScriptTask> tasks = new LinkedList<ScriptTask>();

    public ScriptManager() {
        EventManager.register(this);
    }

    public void tick(Object event) {
        ScriptTask currentTask = this.tasks.peek();
        if (currentTask == null) {
            return;
        }
        boolean finished = currentTask.tryTick(event);
        if (finished) {
            this.tasks.poll();
        }
    }

    @EventHandler
    public void rotateTick(RotationUpdateEvent tickRotate) {
        this.tick(tickRotate);
    }

    @EventHandler(value=0)
    public void updateTick(EventUpdate tickUpdate) {
        this.tick(tickUpdate);
    }

    public void addTask(ScriptTask task) {
        this.tasks.add(task);
    }

    public boolean isFinished() {
        return this.tasks.isEmpty();
    }

    public static class ScriptTask {
        private final Queue<Step<?>> steps = new LinkedList();
        private int idleTicks = 0;
        private int maxIdleTicks = 400;

        public ScriptTask withMaxIdleTicks(int maxIdleTicks) {
            this.maxIdleTicks = Math.max(1, maxIdleTicks);
            return this;
        }

        public <E> ScriptTask schedule(Class<E> eventClass, StepTask<E> action) {
            this.steps.add(new Step<E>(eventClass, action));
            return this;
        }

        public boolean tryTick(Object event) {
            boolean stepDone;
            Step<?> nextStep = this.steps.peek();
            if (nextStep == null) {
                return true;
            }
            boolean progressed = false;
            if (nextStep.eventClass.isInstance(event) && (stepDone = nextStep.execute(event))) {
                this.steps.poll();
                progressed = true;
            }
            if (progressed) {
                this.idleTicks = 0;
                return this.steps.isEmpty();
            }
            ++this.idleTicks;
            if (this.idleTicks > this.maxIdleTicks) {
                this.steps.clear();
                return true;
            }
            return false;
        }

        public boolean isCompleted() {
            return this.steps.isEmpty();
        }

        private static class Step<E> {
            private final Class<E> eventClass;
            private final StepTask<E> action;

            Step(Class<E> eventClass, StepTask<E> action) {
                this.eventClass = eventClass;
                this.action = action;
            }

            boolean execute(Object event) {
                return this.action.accept(this.eventClass.cast(event));
            }
        }

        @FunctionalInterface
        public static interface StepTask<E> {
            public boolean accept(E var1);
        }
    }
}
