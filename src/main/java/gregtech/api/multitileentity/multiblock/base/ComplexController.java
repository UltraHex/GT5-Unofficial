package gregtech.api.multitileentity.multiblock.base;

import gregtech.api.logic.interfaces.PollutionLogicHost;

public abstract class ComplexController<T extends ComplexController<T>> extends PowerController<T> {

    protected int maxComplexParallels = 0;
    protected int currentComplexParallels = 0;
    protected long[] maxProgressTimes = new long[0];
    protected long[] progressTimes = new long[0];

    protected void setMaxComplexParallels(int parallel) {
        this.maxComplexParallels = parallel;
        maxProgressTimes = new long[parallel];
        progressTimes = new long[parallel];
    }

    @Override
    protected void runMachine(long tick) {
        if (acceptsFuel() && isActive()) {
            if (!consumeFuel()) {
                stopMachine(true);
                return;
            }
        }

        if (hasThingsToDo()) {
            markDirty();
            runningTick(tick);
        }
        if ((tick % TICKS_BETWEEN_RECIPE_CHECKS == 0 || hasWorkJustBeenEnabled() || hasInventoryBeenModified())
            && maxComplexParallels != currentComplexParallels) {
            if (isAllowedToWork()) {
                wasEnabled = false;
                boolean started = false;
                for (int i = 0; i < maxComplexParallels; i++) {
                    if (checkRecipe(i)) {
                        currentComplexParallels++;
                        started = true;
                    }
                }
                if (started) {
                    setActive(true);
                    updateSlots();
                    markDirty();
                    issueClientUpdate();
                }
            }
        }
    }

    @Override
    protected void runningTick(long tick) {
        consumeEnergy();
        boolean allStopped = true;
        for (int i = 0; i < maxComplexParallels; i++) {
            if (maxProgressTimes[i] > 0 && ++progressTimes[i] >= maxProgressTimes[i]) {
                progressTimes[i] = 0;
                maxProgressTimes[i] = 0;
                outputItems(i);
                outputFluids(i);
                if (isAllowedToWork()) {
                    if (checkRecipe(i)) {
                        allStopped = false;
                    } else {
                        currentComplexParallels--;
                    }
                }
                updateSlots();
            }
        }
        if (allStopped) {
            setActive(false);
            issueClientUpdate();
        }

        if (this instanceof PollutionLogicHost && tick % POLLUTION_TICK == 0) {
            doPollution();
        }
        emitEnergy();
    }

    protected abstract boolean checkRecipe(int index);

    protected void outputItems(int index) {
        outputItems();
    }

    protected void outputFluids(int index) {
        outputFluids();
    }
}
