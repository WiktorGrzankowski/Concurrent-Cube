package concurrentcube;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class Cube {
    private final int NUMBER_OF_SIDES = 6;
    private final int top = 0;
    private final int left = 1;
    private final int front = 2;
    private final int right = 3;
    private final int back = 4;
    private final int bottom = 5;
    private final int SHOW_AXIS = 0;
    private final int TOP_BOTTOM_AXIS = -1;
    private final int FRONT_BACK_AXIS = -2;
    private final int LEFT_RIGHT_AXIS = -3;


    private final int size;
    private final int[][][] cube;

    private final Semaphore[] layerSemaphore;
    private final Semaphore rotationsSemaphore;
    private final Semaphore mainSemaphore;

    private final BiConsumer<Integer, Integer> beforeRotation;
    private final BiConsumer<Integer, Integer> afterRotation;
    private final Runnable beforeShowing;
    private final Runnable afterShowing;
    private final AtomicInteger currentAxis;
    private final AtomicInteger begun;


    public Cube(int size,
                BiConsumer<Integer, Integer> beforeRotation,
                BiConsumer<Integer, Integer> afterRotation,
                Runnable beforeShowing,
                Runnable afterShowing) {
        this.currentAxis = new AtomicInteger(0);
        this.begun = new AtomicInteger(0);
        this.size = size;
        this.beforeRotation = beforeRotation;
        this.afterRotation = afterRotation;
        this.beforeShowing = beforeShowing;
        this.afterShowing = afterShowing;
        this.layerSemaphore = new Semaphore[size];
        this.rotationsSemaphore = new Semaphore(0, true);
        this.mainSemaphore = new Semaphore(1, true);
        for (int i = 0; i < size; i++)
            layerSemaphore[i] = new Semaphore(1, true);
        this.cube = new int[NUMBER_OF_SIDES][size][size];
        for (int i = 0; i < NUMBER_OF_SIDES; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    cube[i][j][k] = i;
                }
            }
        }
    }

    private int[] getVerticalRow(int side, int verticalLayer) {
        int[] result = new int[size];
        for (int i = 0; i < size; ++i) {
            result[i] = cube[side][i][verticalLayer];
        }
        return result;
    }

    private int[] getVerticalRowReversed(int side, int verticalLayer) {
        int[] result = new int[size];
        for (int i = 0; i < size; ++i) {
            result[i] = cube[side][size - 1 - i][verticalLayer];
        }
        return result;
    }

    public void setVerticalRow(int side, int verticalLayer, int[] givenRow) {
        for (int i = 0; i < size; i++) {
            cube[side][i][verticalLayer] = givenRow[i];
        }
    }

    public void setVerticalRowReversed(int side, int verticalLayer, int[] givenRow) {
        for (int i = 0; i < size; i++) {
            cube[side][i][verticalLayer] = givenRow[size - 1 - i];
        }
    }

    public int getOppositeSide(int side) { // will probably be used in the concurrent part
        switch (side) {
            case top:
                return bottom;
            case bottom:
                return top;
            case left:
                return right;
            case right:
                return left;
            case front:
                return back;
            default: // back
                return front;
        }
    }

    public int getAxis(int s) {
        switch (s) {
            case top:
            case bottom:
                return TOP_BOTTOM_AXIS;
            case front:
            case back:
                return FRONT_BACK_AXIS;
            default: // left || right
                return LEFT_RIGHT_AXIS;
        }
    }

    public int getSize() {
        return size;
    }

    private void rotateBottom(int layer) {
        // i-th horizontal left->front->right->back->left
        int[] tempLeft = cube[left][size - 1 - layer].clone();
        int[] tempFront = cube[front][size - 1 - layer].clone();
        int[] tempRight = cube[right][size - 1 - layer].clone();
        int[] tempBack = cube[back][size - 1 - layer].clone();
        cube[front][size - 1 - layer] = tempLeft; // left
        cube[right][size - 1 - layer] = tempFront; // front
        cube[back][size - 1 - layer] = tempRight; // right
        cube[left][size - 1 - layer] = tempBack; // back
    }

    private void rotateTop(int layer) {
        int[] tempLeft = cube[left][layer].clone();
        int[] tempFront = cube[front][layer].clone();
        int[] tempRight = cube[right][layer].clone();
        int[] tempBack = cube[back][layer].clone();
        cube[right][layer] = tempBack;
        cube[front][layer] = tempRight;
        cube[left][layer] = tempFront;
        cube[back][layer] = tempLeft;
    }

    private void rotateFront(int layer) {
        int[] tempTop = cube[top][size - 1 - layer].clone();
        int[] tempRightVertical = getVerticalRowReversed(right, layer).clone();
        int[] tempBottom = cube[bottom][layer].clone();
        int[] tempLeftVertical = getVerticalRowReversed(left, size - 1 - layer);
        setVerticalRow(right, layer, tempTop);
        cube[bottom][layer] = tempRightVertical;
        setVerticalRow(left, size - 1 - layer, tempBottom);
        cube[top][size - 1 - layer] = tempLeftVertical;
    }

    private void rotateBack(int layer) {
        int[] tempTop = cube[top][layer].clone();
        int[] tempLeftVertical = getVerticalRow(left, layer);
        int[] tempBottom = cube[bottom][size - 1 - layer].clone();
        int[] tempRightVertical = getVerticalRow(right, size - 1 - layer);
        setVerticalRowReversed(left, layer, tempTop);
        cube[bottom][size - 1 - layer] = tempLeftVertical;
        setVerticalRowReversed(right, size - 1 - layer, tempBottom);
        cube[top][layer] = tempRightVertical;
    }

    private void rotateRight(int layer) {
        int[] tempTopVertical = getVerticalRowReversed(top, size - 1 - layer);
        int[] tempBackVertical = getVerticalRowReversed(back, layer);
        int[] tempBottomVertical = getVerticalRow(bottom, size - 1 - layer);
        int[] tempFrontVertical = getVerticalRow(front, size - 1 - layer);
        setVerticalRow(back, layer, tempTopVertical);
        setVerticalRow(bottom, size - 1 - layer, tempBackVertical);
        setVerticalRow(front, size - 1 - layer, tempBottomVertical);
        setVerticalRow(top, size - 1 - layer, tempFrontVertical);
    }

    private void rotateLeft(int layer) {
        int[] tempTopVertical = getVerticalRow(top, layer);
        int[] tempFrontVertical = getVerticalRow(front, layer);
        int[] tempBottomVertical = getVerticalRowReversed(bottom, layer);
        int[] tempBackVertical = getVerticalRowReversed(back, size - 1 - layer);
        setVerticalRow(front, layer, tempTopVertical);
        setVerticalRow(bottom, layer, tempFrontVertical);
        setVerticalRow(back, size - 1 - layer, tempBottomVertical);
        setVerticalRow(top, layer, tempBackVertical);
    }

    private void rotate90DegreesClockwise(int side) {
        // layer was 0, so we rotate the side given to rotateSide
        int[][] copiedRows = new int[size][size];
        for (int i = 0; i < size; i++) {
            copiedRows[i] = cube[side][i].clone();
        }
        for (int i = 0; i < size; i++) {
            setVerticalRow(side, size - 1 - i, copiedRows[i]);
        }
    }

    private void rotate90DegreesCounterclockwise(int side) {
        int[][] copiedRows = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                copiedRows[i][size - 1 - j] = cube[side][i][j];
            }
        }
        for (int i = 0; i < size; i++) {
            setVerticalRow(side, i, copiedRows[i]);
        }
    }

    private void rotateAroundIfNeccessary(int side, int layer) {
        if (layer == 0)
            rotate90DegreesClockwise(side);
        else if (layer == size - 1)
            rotate90DegreesCounterclockwise(getOppositeSide(side));
    }

    private void acquireMainSemaphore() throws InterruptedException {
        try {
            mainSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new InterruptedException();
        }
    }


    private void waitForCurrentAxisToStopSpinning(int side) throws InterruptedException {
        if (currentAxis.get() != getAxis(side)) {
            try {
                rotationsSemaphore.acquire(begun.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new InterruptedException();
            }
            currentAxis.set(getAxis(side));
            begun.set(0);
        }
    }

    private void endProtocol(int side, int layer, AtomicBoolean beforeAccepted) {
        if (beforeAccepted.get()) {
            afterRotation.accept(side, layer);
        }
        layerSemaphore[layer].release();
        rotationsSemaphore.release();
    }

    private void entryProtocol(int side) throws InterruptedException {
        try {
            acquireMainSemaphore();
            waitForCurrentAxisToStopSpinning(side);
        } catch (InterruptedException e) {
            throw new InterruptedException();
        } finally {
            begun.incrementAndGet();
            mainSemaphore.release();
        }
    }

    private void rotateTopWrapper(int side, int layer) throws InterruptedException {
        AtomicBoolean beforeAccepted = new AtomicBoolean(false);
        entryProtocol(side);
        try {
            layerSemaphore[layer].acquire();
            beforeRotation.accept(side, layer);
            beforeAccepted.set(true);
            if (!Thread.currentThread().isInterrupted()) {
                rotateTop(layer);
                rotateAroundIfNeccessary(side, layer);
            }
        } catch (InterruptedException e) {
            throw new InterruptedException();
        } finally {
            endProtocol(side, layer, beforeAccepted);
        }
    }

    private void rotateBottomWrapper(int side, int layer) throws InterruptedException {
        AtomicBoolean beforeAccepted = new AtomicBoolean(false);
        entryProtocol(side);
        try {
            layerSemaphore[size - 1 - layer].acquire();
            beforeRotation.accept(side, layer);
            beforeAccepted.set(true);
            if (!Thread.currentThread().isInterrupted()) {
                rotateBottom(layer);
                rotateAroundIfNeccessary(side, layer);
            }
        } catch (InterruptedException e) {
            throw new InterruptedException();
        } finally {
            endProtocol(side, size - 1 - layer, beforeAccepted);
        }
    }

    private void rotateFrontWrapper(int side, int layer) throws InterruptedException {
        AtomicBoolean beforeAccepted = new AtomicBoolean(false);
        entryProtocol(side);
        try {
            layerSemaphore[layer].acquire();
            beforeRotation.accept(side, layer);
            beforeAccepted.set(true);
            if (!Thread.currentThread().isInterrupted()) {
                rotateFront(layer);
                rotateAroundIfNeccessary(side, layer);
            }
        } catch (InterruptedException e) {
            throw new InterruptedException();
        } finally {
            endProtocol(side, layer, beforeAccepted);
        }
    }

    private void rotateBackWrapper(int side, int layer) throws InterruptedException {
        AtomicBoolean beforeAccepted = new AtomicBoolean(false);
        entryProtocol(side);
        try {
            layerSemaphore[size - 1 - layer].acquire();
            beforeRotation.accept(side, layer);
            beforeAccepted.set(true);
            if (!Thread.currentThread().isInterrupted()) {
                rotateBack(layer);
                rotateAroundIfNeccessary(side, layer);
            }
        } catch (InterruptedException e) {
            throw new InterruptedException();
        } finally {
            endProtocol(side, size - 1 - layer, beforeAccepted);
        }
    }

    private void rotateLeftWrapper(int side, int layer) throws InterruptedException {
        AtomicBoolean beforeAccepted = new AtomicBoolean(false);
        entryProtocol(side);
        try {
            layerSemaphore[layer].acquire();
            beforeRotation.accept(side, layer);
            beforeAccepted.set(true);
            if (!Thread.currentThread().isInterrupted()) {
                rotateLeft(layer);
                rotateAroundIfNeccessary(side, layer);
            }
        } catch (InterruptedException e) {
            throw new InterruptedException();
        } finally {
            endProtocol(side, layer, beforeAccepted);
        }
    }

    private void rotateRightWrapper(int side, int layer) throws InterruptedException {
        AtomicBoolean beforeAccepted = new AtomicBoolean(false);
        entryProtocol(side);
        try {
            layerSemaphore[size - 1 - layer].acquire();
            beforeRotation.accept(side, layer);
            beforeAccepted.set(true);
            if (!Thread.currentThread().isInterrupted()) {
                rotateRight(layer);
                rotateAroundIfNeccessary(side, layer);
            }
        } catch (InterruptedException e) {
            throw new InterruptedException();
        } finally {
            endProtocol(side, size - 1 - layer, beforeAccepted);
        }
    }

    public void rotate(int side, int layer) throws InterruptedException {
        switch (side) {
            case top:
                rotateTopWrapper(side, layer);
                break;
            case bottom:
                rotateBottomWrapper(side, layer);
                break;
            case front:
                rotateFrontWrapper(side, layer);
                break;
            case back:
                rotateBackWrapper(side, layer);
                break;
            case left:
                rotateLeftWrapper(side, layer);
                break;
            case right:
                rotateRightWrapper(side, layer);
                break;
        }
    }

    public String show() throws InterruptedException {
        try {
            mainSemaphore.acquire();
            if (currentAxis.get() != SHOW_AXIS) {
                rotationsSemaphore.acquire(begun.get());
                begun.set(0);
                currentAxis.set(SHOW_AXIS);
            }
        } catch (InterruptedException e) {
            throw new InterruptedException();
        } finally {
            begun.incrementAndGet();
            mainSemaphore.release();
        }
        beforeShowing.run();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < NUMBER_OF_SIDES; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    result.append(cube[i][j][k]);
                }
            }
        }
        afterShowing.run();
        rotationsSemaphore.release();
        return result.toString();
    }
}
