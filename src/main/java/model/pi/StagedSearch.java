package model.pi;

import controller.StoreController;
import model.ycd.YCD_SeqProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/** Two-stage search of a single inclusive candidate range. */
public final class StagedSearch {
    private final List<File> files;
    private final int unitLength, threads, batches;
    private final double survivalPercent;
    private final BooleanSupplier stopRequested;

    public StagedSearch(List<File> files, int unitLength, int threads, int batches, double survivalPercent) {
        this(files, unitLength, threads, batches, survivalPercent, () -> false);
    }

    public StagedSearch(List<File> files, int unitLength, int threads, int batches, double survivalPercent,
            BooleanSupplier stopRequested) {
        if (unitLength <= 0 || threads <= 0 || batches <= 0 || !Double.isFinite(survivalPercent)
                || survivalPercent <= 0 || survivalPercent > 100) throw new IllegalArgumentException("Invalid staged search settings");
        this.files = files;
        this.unitLength = unitLength;
        this.threads = threads;
        this.batches = batches;
        this.survivalPercent = survivalPercent;
        this.stopRequested = stopRequested != null ? stopRequested : () -> false;
    }

    /** Returns null when the available input cannot resolve every candidate. */
    public SurvivalResult search(int length, long start, long end, SurvivalList combined) throws Exception {
        if (combined == null) {
            throw new IllegalArgumentException("combined SurvivalList must not be null");
        }
        if (length < 1 || length > SurvivalList.MAX_LENGTH || combined.getLength() != length) {
            throw new IllegalArgumentException("Invalid or mismatched candidate length");
        }
        long maxValue = SurvivalList.pow10(length) - 1L;
        if (start < 0 || end < start || end > maxValue) {
            throw new IllegalArgumentException("Candidate range out of bounds for length " + length);
        }
        long expectedCount = end - start + 1L;
        if (combined.getDiscoveredCount() != 0
                || combined.size() != expectedCount
                || Long.parseLong(combined.get(0)) != start
                || Long.parseLong(combined.get(combined.size() - 1)) != end) {
            throw new IllegalArgumentException("combined SurvivalList must be an undiscovered list matching [start, end]");
        }
        StageOnePartition partition = new StageOnePartition(start, end, threads, batches);
        java.util.concurrent.atomic.AtomicInteger workerNumber = new java.util.concurrent.atomic.AtomicInteger();
        ExecutorService workers = Executors.newFixedThreadPool(Math.min(threads, partition.groupCount()), task ->
                new Thread(task, "staged-search-worker-" + workerNumber.incrementAndGet()));
        SurvivalResult deepest = new SurvivalResult("", -1L);
        try {
            checkStop();
            List<SurvivalList> pilot = groups(partition, length, 0);
            System.out.println("  -> Stage 1: パイロット測定中...");
            long depth = scan(pilot, length, Long.MAX_VALUE, true, workers, null, deepest);
            System.out.printf("  -> Stage 1: パイロット完了 (基準深さ: %,d 桁)%n", depth);
            StoreController.survivalProgressMap.put("STAGE1_DEPTH", depth);
            StoreController.survivalProgressMap.put("STAGE1_BATCHES", partition.batchCount());
            BiConsumer<String, Long> record = (target, position) -> {
                synchronized (combined) { combined.discover(target, position); }
            };
            for (int batch = 0; batch < partition.batchCount(); batch++) {
                checkStop();
                StoreController.survivalProgressMap.put("STAGE1_BATCH", batch + 1);
                StoreController.survivalProgressMap.put("STAGE1_PROGRESS", 100.0 * batch / partition.batchCount());
                scan(groups(partition, length, batch), length, depth, false, workers, record, deepest);
                System.out.printf("  -> Stage 1: バッチ %d/%d 処理完了 (残り候補: %,d)%n",
                        batch + 1, partition.batchCount(), combined.size());
            }
            StoreController.survivalProgressMap.put("STAGE1_PROGRESS", 100.0);
            System.out.printf("  -> Stage 1: 全バッチ完了 (Stage 2 へ引き継ぐ候補: %,d 件)%n", combined.size());
            if (!combined.isEmpty()) {
                checkStop();
                StoreController.survivalProgressMap.put("SEARCH_STAGE", 2);
                System.out.printf("  -> Stage 2: 残り候補 %,d 件の最終確定走査中...%n", combined.size());
                List<SurvivalList> all = new ArrayList<>();
                all.add(combined);
                scanFrom(all, length, depth, Long.MAX_VALUE, workers, null, deepest);
            }
            return combined.isEmpty() ? deepest : null;
        } finally {
            workers.shutdownNow();
            boolean interrupted = Thread.interrupted();
            for (;;) {
                try {
                    if (workers.awaitTermination(1, TimeUnit.SECONDS)) break;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private void checkStop() throws InterruptedException {
        if (Thread.currentThread().isInterrupted() || stopRequested.getAsBoolean()) {
            throw new InterruptedException();
        }
    }

    private List<SurvivalList> groups(StageOnePartition partition, int length, int batch) {
        List<SurvivalList> result = new ArrayList<>();
        int first = batch * threads;
        for (int i = first; i < Math.min((long) first + threads, partition.groupCount()); i++)
            result.add(new SurvivalList(length, partition.start(i), partition.end(i)));
        return result;
    }

    private long scan(List<SurvivalList> lists, int length, long end, boolean pilot,
            ExecutorService workers, BiConsumer<String, Long> listener, SurvivalResult deepest) throws Exception {
        return scanInternal(lists, length, 0, end, pilot, workers, listener, deepest);
    }

    private long scanFrom(List<SurvivalList> lists, int length, long from, long end,
            ExecutorService workers, BiConsumer<String, Long> listener, SurvivalResult deepest) throws Exception {
        return scanInternal(lists, length, from, end, false, workers, listener, deepest);
    }

    private long scanInternal(List<SurvivalList> lists, int length, long from, long end, boolean pilot,
            ExecutorService workers, BiConsumer<String, Long> listener, SurvivalResult deepest) throws Exception {
        int[] thresholds = new int[lists.size()];
        long started = System.nanoTime();
        long lastLog = started;
        List<Integer> initialRemaining = new ArrayList<>();
        for (int i = 0; i < lists.size(); i++) {
            thresholds[i] = (int) Math.floor(lists.get(i).size() * survivalPercent / 100.0);
            initialRemaining.add(lists.get(i).size());
        }
        StoreController.survivalProgressMap.put("STAGE1_WORKERS", initialRemaining);
        if (pilot) {
            StoreController.survivalProgressMap.put("NOW_SURVIVAL_DEPTH", 0L);
            StoreController.survivalProgressMap.put("STAGE1_SPEED", 0.0);
        }
        long availableEnd = 0;
        try (YCD_SeqProvider provider = new YCD_SeqProvider(5, files, length, unitLength)) {
            while (provider.hasNext()) {
                checkStop();
                YCD_SeqProvider.Unit unit = provider.next();
                checkStop();
                long unitStart = unit.getStartDigit() - 1;
                long unitEnd = unitStart + Math.max(0, unit.getData().length() - length + 1);
                availableEnd = Math.max(availableEnd, unitEnd);
                if (unitStart >= end) break;
                if (unitEnd <= from) continue;
                List<SearchThread> tasks = new ArrayList<>();
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < lists.size(); i++) {
                    SearchThread task = new SearchThread(lists.get(i), unit, from, end, pilot ? thresholds[i] : -1);
                    task.setDiscoveryListener(listener);
                    tasks.add(task);
                    futures.add(workers.submit(task::run));
                }
                for (Future<?> future : futures) future.get();
                checkStop();
                long now = System.nanoTime();
                if (now - lastLog >= TimeUnit.SECONDS.toNanos(5)) {
                    long remaining = 0;
                    for (SurvivalList list : lists) remaining += list.size();
                    System.out.printf("     ... 走査中 (現在深さ: %,d 桁 / このフェーズの残り候補: %,d)%n",
                            Math.min(availableEnd, end), remaining);
                    lastLog = now;
                }
                List<Integer> remaining = new ArrayList<>();
                for (SurvivalList list : lists) remaining.add(list.size());
                StoreController.survivalProgressMap.put("STAGE1_WORKERS", remaining);
                double seconds = (now - started) / 1_000_000_000.0;
                long currentPos = pilot ? availableEnd : Math.min(availableEnd, end);
                StoreController.survivalProgressMap.put("NOW_SURVIVAL_DEPTH", currentPos);
                StoreController.survivalProgressMap.put("STAGE1_SPEED", seconds > 0 ? Math.max(0L, currentPos - from) / seconds : 0.0);
                if (!pilot && listener != null) {
                    Object batch = StoreController.survivalProgressMap.get("STAGE1_BATCH");
                    Object count = StoreController.survivalProgressMap.get("STAGE1_BATCHES");
                    if (end > 0 && batch instanceof Integer && count instanceof Integer) {
                        double fraction = Math.min(1.0, (double) availableEnd / end);
                        StoreController.survivalProgressMap.put("STAGE1_PROGRESS",
                                100.0 * ((Integer) batch - 1 + fraction) / (Integer) count);
                    }
                }
                long reached = Long.MAX_VALUE;
                for (SearchThread task : tasks) {
                    if (!pilot && task.getResult().compareTo(deepest) > 0) deepest.update(task.getResult());
                    if (task.getThresholdWindow() >= 0) reached = Math.min(reached, task.getThresholdWindow());
                }
                if (pilot && reached != Long.MAX_VALUE) {
                    long resolvedDepth = reached + 1;
                    StoreController.survivalProgressMap.put("NOW_SURVIVAL_DEPTH", resolvedDepth);
                    return resolvedDepth;
                }
                boolean empty = true;
                for (SurvivalList list : lists) empty &= list.isEmpty();
                if (empty || availableEnd >= end) break;
            }
        }
        return Math.min(availableEnd, end);
    }
}
