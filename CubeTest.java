package concurrentcube;


import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class CubeTest {
    public static class ConcurrentEqualsSequential implements Runnable {
        private final Cube cube;
        private final int layer;

        ConcurrentEqualsSequential(Cube cube, int layer) {
            this.cube = cube;
            this.layer = layer;
        }

        @Override
        public void run() {
            try {
                if (layer == 0)
                    cube.rotate(1, layer);
                else
                    cube.rotate(0, layer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public static int test(ArrayList<String> results, int gitCount) {
        Cube cube = new Cube(3,
                (x, y) -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                },
                (x, y) -> {
                },
                () -> {
                },
                () -> {
                }
        );

        Thread[] threads = new Thread[3];
        for (int i = 0; i < 3; i++) {
            Runnable r = new ConcurrentEqualsSequential(cube, i);
            threads[i] = new Thread(r);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            if (results.contains(cube.show())) {
                gitCount++;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return gitCount;
    }

    @Tag("correctness")
    @DisplayName("Checks whether concurrent rotations give same result as sequential rotations.")
    @Test
    public void test1() {
        // A list of all possible combinations of rotations (1,0), (0,1), (0,2) on a 3x3 cube.
        ArrayList<String> results = new ArrayList<>();
        results.add("400100400121121022022033333333444445445115121555555232");
        results.add("100100400221221221022033033333444444445115115255355355");
        results.add("100100400221221221022033033333444444445115115255355355");
        results.add("100400400211022211022333033333445444445211115255255355");
        results.add("400400400111022022022333333333445445445111111555555222");
        results.add("400400400111022022022333333333445445445111111555555222");
        int correct = 0;
        for (int i = 0; i < 100; i++) {
            correct = test(results, correct);
        }
        Assertions.assertEquals(100, correct);
    }


    private Semaphore semaphore237 = new Semaphore(237);
    private Semaphore semaphore1 = new Semaphore(1);

    public class ManyAtOnce implements Runnable {
        private final Cube cube;
        private final int layer;

        ManyAtOnce(Cube cube, int layer) {
            this.cube = cube;
            this.layer = layer;
        }

        @Override
        public void run() {
            // Possibility to rotate up to 3 layers at a time.
            try {
                semaphore237.acquire();
                cube.rotate(0, layer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            semaphore237.release();
        }
    }

    public class OneAtATime implements Runnable {
        private final Cube cube;
        private final int layer;

        OneAtATime(Cube cube, int layer) {
            this.cube = cube;
            this.layer = layer;
        }

        @Override
        public void run() {
            // No possibility of rotating concurrently.
            try {
                semaphore1.acquire();
                cube.rotate(0, layer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            semaphore1.release();
        }
    }

    public void testOneAtATime() {
        Cube cube = new Cube(3,
                (x, y) -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                },
                (x, y) -> {
                },
                () -> {
                },
                () -> {
                }
        );

        Thread[] threads = new Thread[20];
        for (int i = 0; i < 20; i++) {
            Runnable r = new OneAtATime(cube, i % 3);
            threads[i] = new Thread(r);
        }
        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void testManyAtOnce() {
        Cube cube = new Cube(3,
                (x, y) -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                },
                (x, y) -> {
                },
                () -> {
                },
                () -> {
                }
        );

        Thread[] threads = new Thread[20];
        for (int i = 0; i < 20; i++) {
            Runnable r = new ManyAtOnce(cube, i % 3);
            threads[i] = new Thread(r);
        }
        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @Test
    @Tag("concurrency")
    @DisplayName("Concurrent rotations on same side are faster than sequential rotations.")
    void test2() {
        long start1 = System.currentTimeMillis();
        testManyAtOnce();
        long finish1 = System.currentTimeMillis();
        long timeElapsed1 = finish1 - start1;
        long start2 = System.currentTimeMillis();
        testOneAtATime();
        long finish2 = System.currentTimeMillis();
        long timeElapsed2 = finish2 - start2;
        Assertions.assertTrue(timeElapsed1 < timeElapsed2);
    }

    static class oneSide implements Runnable {
        private final Cube cube;
        private final int layer;

        oneSide(Cube cube, int layer) {
            this.cube = cube;
            this.layer = layer;
        }

        @Override
        public void run() {
            try {
                cube.rotate(2, layer % 4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @Tag("correctness")
    @DisplayName("Checks whether concurrent rotations give the same result as sequential rotations.")
    void test3() {
        Cube seqcube = new Cube(4,
                (x, y) -> {
                },
                (x, y) -> {
                },
                () -> {
                },
                () -> {
                }
        );

        String expected = "";
        for (int i = 0; i < 20; i++) {
            try {
                seqcube.rotate(2, i % 4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            expected = seqcube.show();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Cube cube = new Cube(4,
                (x, y) -> {
                },
                (x, y) -> {
                },
                () -> {
                },
                () -> {
                }
        );
        Thread[] threads = new Thread[20];
        for (int i = 0; i < 20; i++) {
            Runnable r = new oneSide(cube, i);
            threads[i] = new Thread(r);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            Assertions.assertEquals(expected, cube.show());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Integer> sides = new ArrayList<>();
    private ArrayList<Integer> layers = new ArrayList<>();

    public class RememberingResults implements Runnable {
        private final Cube cube;
        private final int side;
        private final int layer;

        RememberingResults(Cube cube) {
            Random x = new Random();
            this.side = x.nextInt(5);
            this.layer = x.nextInt(7);
            this.cube = cube;
        }

        @Override
        public void run() {
            try {
                cube.rotate(side, layer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public int test(int gitCount) {
        Semaphore mutexForBefore = new Semaphore(1, true);
        Cube cube = new Cube(7,
                (x, y) -> {
                    try {
                        mutexForBefore.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    sides.add(x);
                    layers.add(y);
                    mutexForBefore.release();
                },
                (x, y) -> {
                },
                () -> {
                },
                () -> {
                }
        );

        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            Runnable r = new RememberingResults(cube);
            threads[i] = new Thread(r);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Cube seqcube = new Cube(7,
                (x, y) -> {
                },
                (x, y) -> {
                },
                () -> {
                },
                () -> {
                }
        );
        // Sequential remembered rotations from concurrent spinning of a 7x7 cube.
        for (int i = 0; i < 10; i++) {
            try {
                seqcube.rotate(sides.get(i), layers.get(i));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            if (cube.show().equals(seqcube.show())) {
                gitCount++;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sides.clear();
        layers.clear();
        return gitCount;
    }

    @Test
    @Tag("correctness")
    @DisplayName("Checks whether sequential rotations can mimic previously made concurrent rotations.")
    void test4() {
        int correct = 0;
        for (int i = 0; i < 100; i++) {
            correct = test(correct);
        }
        Assertions.assertEquals(100, correct);
    }


    static class Left1000right1000 implements Runnable {
        private final Cube cube;
        private final int layer;

        Left1000right1000(Cube cube, int layer) {
            this.cube = cube;
            this.layer = layer;
        }

        @Override
        public void run() {
            try {

                if (layer % 2 == 0) {
                    cube.rotate(1, 2);
                } else
                    cube.rotate(3, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @Tag("correctness")
    @DisplayName("Checks whether no more than one rotation is performed at a time on the same layer.")
    void test5() {
        String startingCube = "000000000111111111222222222333333333444444444555555555";
        var rotationsAtOnceCount = new Object() {
            int value = 0;
        };
        Cube cube = new Cube(3,
                (x, y) -> {
                    ++rotationsAtOnceCount.value;
                    Assertions.assertEquals(1, rotationsAtOnceCount.value);
                },
                (x, y) -> {
                    --rotationsAtOnceCount.value;
                },
                () -> {
                },
                () -> {
                }
        );

        Thread[] threads = new Thread[20];
        for (int i = 0; i < 20; i++) {
            Runnable r = new Left1000right1000(cube, i);
            threads[i] = new Thread(r);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            Assertions.assertEquals(cube.show(), startingCube);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static class ColourCount implements Runnable {
        private final Cube cube;
        private final int side;
        private final int layer;

        ColourCount(Cube cube) {
            this.cube = cube;
            Random x = new Random();
            this.side = x.nextInt(5);
            this.layer = x.nextInt(12);
        }

        @Override
        public void run() {
            try {
                cube.rotate(side, layer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void test6Help() {
        Cube cube = new Cube(12,
                (x, y) -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                },
                (x, y) -> {
                },
                () -> {
                },
                () -> {
                }
        );

        Thread[] threads = new Thread[20];
        for (int i = 0; i < 20; i++) {
            Runnable r = new ColourCount(cube);
            threads[i] = new Thread(r);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String result = "";
        try {
            result = cube.show();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int[] colours = new int[6];
        for (int i = 0; i < result.length(); i++) {
            colours[result.charAt(i) - '0']++;
        }
        for (int i : colours) {
            Assertions.assertEquals(144, i);
        }

    }

    @Test
    @Tag("correctness")
    @DisplayName("Counts number of bricks in each colour.")
    void test6() {
        for (int i = 0; i < 100; i++) {
            test6Help();
        }
    }

    static class HowManyAtOnce implements Runnable {
        private final Cube cube;
        private final int layer;

        HowManyAtOnce(Cube cube, int layer) {
            this.cube = cube;
            this.layer = layer;
        }

        @Override
        public void run() {
            try {
                cube.rotate(0, layer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @Tag("concurrency")
    @DisplayName("Checks whether no more than <size> layers are rotated at the same time.")
    void test7() {
        Semaphore mutex = new Semaphore(1, true);
        var rotationsAtOnceCount = new Object() {
            int value = 0;
        };
        Cube cube = new Cube(4,
                (x, y) -> {
                    try {
                        mutex.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ++rotationsAtOnceCount.value;
                    Assertions.assertTrue(rotationsAtOnceCount.value > 0 && rotationsAtOnceCount.value < 5);
                    mutex.release();
                },
                (x, y) -> {
                    try {
                        mutex.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    --rotationsAtOnceCount.value;
                    mutex.release();
                },
                () -> {
                },
                () -> {
                }
        );

        Thread[] threads = new Thread[20];
        for (int i = 0; i < 20; i++) {
            Runnable r = new HowManyAtOnce(cube, i % 4);
            threads[i] = new Thread(r);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class ShowMidRotations implements Runnable {
        private final Cube cube;
        private final int layer;

        ShowMidRotations(Cube cube, int layer) {
            this.cube = cube;
            this.layer = layer;
        }

        @Override
        public void run() {
            if (layer % 7 == 0) {
                try {
                    String result = cube.show();
                    int[] colours = new int[6];
                    for (int i = 0; i < result.length(); i++) {
                        colours[result.charAt(i) - '0']++;
                    }
                    for (int i : colours) {
                        Assertions.assertEquals(16, i);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    cube.rotate(0, layer % 4);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    @Tag("concurrency")
    @Tag("correctness")
    @DisplayName("Checks whether show() called between concurrent rotations displays correct" +
            "number of bricks in each colour")
    void test8() {
        Cube cube = new Cube(4,
                (x, y) -> {
                },
                (x, y) -> {
                },
                () -> {
                },
                () -> {
                }
        );

        Thread[] threads = new Thread[20];
        for (int i = 0; i < 20; i++) {
            Runnable r = new ShowMidRotations(cube, i);
            threads[i] = new Thread(r);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class TheBiggerTheQuicker implements Runnable {
        private final Cube cube;
        private final int layer;

        TheBiggerTheQuicker(Cube cube, int layer) {
            this.cube = cube;
            this.layer = layer;
        }

        @Override
        public void run() {
            for (int i = 0; i < 400; i++) {
                try {
                    cube.rotate(0, (i + layer) % cube.getSize());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    void test9Help(int sizeGiven, long[] timeSpent) {
        long start = System.currentTimeMillis();
        Cube cube = new Cube(sizeGiven,
                (x, y) -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                },
                (x, y) -> {
                },
                () -> {
                },
                () -> {
                }
        );

        Thread[] threads = new Thread[8];
        for (int i = 0; i < 8; i++) {
            Runnable r = new TheBiggerTheQuicker(cube, i);
            threads[i] = new Thread(r);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        timeSpent[sizeGiven - 1] = timeElapsed;
    }

    @Test
    @Tag("concurrency")
    @DisplayName("Same number of rotations can be performed concurrently quicker on a " +
            "bigger cube, because more layers are rotated at a time.")
    void test9() {
        long[] timeSpent = new long[8];
        for (int i = 1; i < 9; i++) {
            test9Help(i, timeSpent);
        }
        for (int i = 0; i < 7; i++) {
            Assertions.assertTrue(timeSpent[i] > timeSpent[i + 1]);
        }

    }

    @Test
    @Tag("interruptions")
    @DisplayName("Interrupts one thread. After that other threads can rotate the same cube.")
    public void test10() {
        Cube seqcube = new Cube(
                3,
                (x, y) -> {
                },
                (x, y) -> {
                },
                () -> {
                },
                () -> {
                }
        );
        String expected = "";
        try {
            seqcube.rotate(0, 1);
            seqcube.rotate(5, 2);
            seqcube.rotate(3, 0);
            expected = seqcube.show();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Cube cube = new Cube(
                3,
                (x, y) -> {
                },
                (x, y) -> {
                },
                () -> {
                },
                () -> {
                }
        );

        Thread thread1 = new Thread(() -> {
            try {
                Thread.sleep(1000);
                cube.rotate(0, 0);
            } catch (InterruptedException e) {
//                e.printStackTrace();
            }
        });
        thread1.start();

        Thread thread2 = new Thread(() -> {
            try {
                cube.rotate(0, 1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread2.start();
        thread1.interrupt();
        try {
            thread1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            cube.rotate(5, 2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            cube.rotate(3, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            Assertions.assertEquals(expected, cube.show());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    @Tag("correctness")
    @DisplayName("Checks whether beforeShowing and afterShowing work properly.")
    public void Test11() {
        AtomicInteger balance = new AtomicInteger(0);

        Cube cube = new Cube(5,
                (x, y) -> {
                },
                (x, y) -> {
                },
                balance::getAndIncrement,
                balance::getAndDecrement
        );

        Thread[] threads = new Thread[20];

        for (int i = 0; i < 20; i++) {
            if (i % 7 == 0) {
                threads[i] = new Thread(() -> {
                    try {
                        cube.rotate(0, 2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            } else {

                threads[i] = new Thread(() -> {
                    try {
                        for (int j = 0; j < 10; j++) {
                            cube.show();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Assertions.assertEquals(0, balance.get());
    }

    @Test
    @Tag("interruptions")
    @DisplayName("Interrupts threads at the very beginning.")
    public void Test12() {
        Cube cube = new Cube(5,
                (x, y) -> {
                },
                (x, y) -> {
                },
                () -> {},
                () -> {}
        );

        Thread[] threads = new Thread[20];

        for (int i = 0; i < 20; i++) {
            if (i % 7 == 0) {
                threads[i] = new Thread(() -> {
                    try {
                        cube.rotate(0, 2);
                    } catch (InterruptedException ignored) {
                    }
                });
            } else {
                threads[i] = new Thread(() -> {
                    try {
                        for (int j = 0; j < 10; j++) {
                            cube.show();
                        }
                    } catch (InterruptedException ignored) {
                    }
                });
            }
        }
        for (Thread thread : threads) {
            thread.start();
            thread.interrupt();
        }
    }
}
