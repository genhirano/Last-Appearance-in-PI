package model.pi;

/** Partition one inclusive candidate range into non-empty, balanced groups. */
public final class StageOnePartition {
    private final long first;
    private final int groups;
    private final int threads;
    private final long quotient;
    private final long remainder;

    public StageOnePartition(long first, long last, int threads, int batches) {
        if (first < 0 || last < first || threads <= 0 || batches <= 0) {
            throw new IllegalArgumentException("Invalid candidate range or parallelism");
        }
        long count = Math.addExact(Math.subtractExact(last, first), 1L);
        long maximum = (long) threads * batches;
        long groupCount = Math.min(count, maximum);
        if (groupCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Too many groups");
        }
        this.first = first;
        this.groups = (int) groupCount;
        this.threads = threads;
        this.quotient = count / groups;
        this.remainder = count % groups;
    }

    public int groupCount() { return groups; }
    public int batchCount() { return (int) (((long) groups + threads - 1) / threads); }

    public long start(int index) {
        check(index);
        return first + index * quotient + Math.min((long) index, remainder);
    }

    public long end(int index) {
        return start(index) + quotient + (index < remainder ? 1 : 0) - 1;
    }

    private void check(int index) {
        if (index < 0 || index >= groups) throw new IndexOutOfBoundsException();
    }
}
