package model.pi;

import controller.StoreController;
import model.ycd.YCD_SeqProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/** Two-stage search of a single inclusive candidate range. */
public final class StagedSearch {
    private final List<File> files;
    private final int unitLength, threads, batches;
    private final double survivalPercent;

    public StagedSearch(List<File> files, int unitLength, int threads, int batches, double survivalPercent) {
        if (unitLength <= 0 || threads <= 0 || batches <= 0 || !Double.isFinite(survivalPercent)
                || survivalPercent <= 0 || survivalPercent > 100) throw new IllegalArgumentException("Invalid staged search settings");
        this.files = files;
        this.unitLength = unitLength;
        this.threads = threads;
        this.batches = batches;
        this.survivalPercent = survivalPercent;
    }

    /** Returns null when the available input cannot resolve every candidate. */
    public SurvivalResult search(int length, long start, long end, SurvivalList combined) throws Exception {
        StageOnePartition partition = new StageOnePartition(start, end, threads, batches);
        java.util.concurrent.atomic.AtomicInteger workerNumber = new java.util.concurrent.atomic.AtomicInteger();
        ExecutorService workers = Executors.newFixedThreadPool(Math.min(threads, partition.groupCount()), task ->
                new Thread(task, "staged-search-worker-" + workerNumber.incrementAndGet()));
        SurvivalResult deepest = new SurvivalResult("", -1L);
        try {
            List<SurvivalList> pilot = groups(partition, length, 0);
            long depth = scan(pilot, length, Long.MAX_VALUE, true, workers, null, deepest);
            StoreController.survivalProgressMap.put("STAGE1_DEPTH", depth);
            StoreController.survivalProgressMap.put("STAGE1_BATCHES", partition.batchCount());
            BiConsumer<String, Long> record = (target, position) -> {
                synchronized (combined) { combined.discover(target, position); }
            };
            for (int batch = 0; batch < partition.batchCount(); batch++) {
                StoreController.survivalProgressMap.put("STAGE1_BATCH", batch + 1);
                StoreController.survivalProgressMap.put("STAGE1_PROGRESS", 100.0 * batch / partition.batchCount());
                scan(groups(partition, length, batch), length, depth, false, workers, record, deepest);
            }
            StoreController.survivalProgressMap.put("STAGE1_PROGRESS", 100.0);
            if (!combined.isEmpty()) {
                StoreController.survivalProgressMap.put("SEARCH_STAGE", 2);
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
        for (int i = 0; i < lists.size(); i++) thresholds[i] = (int) Math.floor(lists.get(i).size() * survivalPercent / 100.0);
        long availableEnd = 0;
        try (YCD_SeqProvider provider = new YCD_SeqProvider(5, files, length, unitLength)) {
            while (provider.hasNext()) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                YCD_SeqProvider.Unit unit = provider.next();
                long unitStart = unit.getStartDigit() - 1;
                availableEnd = Math.max(availableEnd, unitStart + Math.max(0, unit.getData().length() - length + 1));
                if (unitStart >= end) break;
                List<SearchThread> tasks = new ArrayList<>();
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < lists.size(); i++) {
                    SearchThread task = new SearchThread(lists.get(i), unit, from, end, pilot ? thresholds[i] : -1);
                    task.setDiscoveryListener(listener);
                    tasks.add(task);
                    futures.add(workers.submit(task::run));
                }
                for (Future<?> future : futures) future.get();
                if (pilot) {
                    List<Integer> remaining = new ArrayList<>();
                    for (SurvivalList list : lists) remaining.add(list.size());
                    StoreController.survivalProgressMap.put("STAGE1_WORKERS", remaining);
                    StoreController.survivalProgressMap.put("NOW_SURVIVAL_DEPTH", availableEnd);
                    double seconds = (System.nanoTime() - started) / 1_000_000_000.0;
                    StoreController.survivalProgressMap.put("STAGE1_SPEED", seconds > 0 ? availableEnd / seconds : 0.0);
                }
                if (!pilot && listener != null) {
                    List<Integer> remaining = new ArrayList<>();
                    for (SurvivalList list : lists) remaining.add(list.size());
                    StoreController.survivalProgressMap.put("STAGE1_WORKERS", remaining);
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
                if (pilot && reached != Long.MAX_VALUE) return reached + 1;
                if (!pilot) StoreController.survivalProgressMap.put("NOW_SURVIVAL_DEPTH", Math.min(availableEnd, end));
                boolean empty = true;
                for (SurvivalList list : lists) empty &= list.isEmpty();
                if (empty || availableEnd >= end) break;
            }
        }
        return Math.min(availableEnd, end);
    }
}
