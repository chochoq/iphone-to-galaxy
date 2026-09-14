package com.chocho.ui;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
public final class SaveJobsTests {
    static int checks;
    static void ok(boolean v){checks++;if(!v)throw new AssertionError();}
    static SaveJobs.Result result(String id)throws Exception{
        for(int i=0;i<100;i++){SaveJobs.Job job=SaveJobs.get(id);if(job!=null&&job.result!=null)return job.result;Thread.sleep(10);}
        throw new AssertionError("save timed out");
    }
    public static void main(String[] args)throws Exception{
        AtomicInteger count=new AtomicInteger();CountDownLatch begun=new CountDownLatch(1),release=new CountDownLatch(1);
        String id=SaveJobs.start(()->{count.incrementAndGet();begun.countDown();if(!release.await(2,TimeUnit.SECONDS))throw new IllegalStateException("timeout");return SaveJobs.Result.saved("done");});
        ok(begun.await(1,TimeUnit.SECONDS));ok(SaveJobs.get(id).result==null);
        // Re-attaching a new UI reads the same job, without starting another write.
        SaveJobs.Job reattached=SaveJobs.get(id);release.countDown();ok(result(id).close);ok(reattached.result.message.equals("done"));ok(count.get()==1);
        SaveJobs.consume(id);ok(SaveJobs.get(id)==null);
        String failed=SaveJobs.start(()->SaveJobs.Result.failed("failure"));ok(!result(failed).close);ok(result(failed).message.equals("failure"));SaveJobs.consume(failed);
        String thrown=SaveJobs.start(()->{throw new IllegalStateException("test");});ok(!result(thrown).close);SaveJobs.consume(thrown);
        String missing=SaveJobs.start(()->null);ok(!result(missing).close);SaveJobs.consume(missing);
        for(int i=0;i<35;i++){String completed=SaveJobs.start(()->SaveJobs.Result.saved("orphan"));ok(result(completed).close);}
        System.out.println("PASS SaveJobs "+checks+" checks");
    }
}
