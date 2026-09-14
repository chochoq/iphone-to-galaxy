package com.chocho.ui;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;
import java.util.UUID;

/** Process-local work survives Activity recreation, but never claims to survive process death. */
public final class SaveJobs {
    public static final class Result {
        public final boolean close;
        public final String message;
        public Result(boolean close,String message){this.close=close;this.message=message;}
        public static Result saved(String message){return new Result(true,message);}
        public static Result failed(String message){return new Result(false,message);}
    }
    public static final class Job { public volatile Result result; }
    private static final ConcurrentHashMap<String,Job> JOBS=new ConcurrentHashMap<>();
    private SaveJobs(){}
    public static String start(Callable<Result> work){
        // Completed orphaned jobs have no listeners or Context references; keep the map bounded.
        if(JOBS.size()>32)JOBS.entrySet().removeIf(e->e.getValue().result!=null);
        String id=UUID.randomUUID().toString();Job job=new Job();JOBS.put(id,job);
        new Thread(()->{
            try{job.result=work.call();if(job.result==null)throw new IllegalStateException("Missing save result");}
            catch(Exception e){job.result=Result.failed("저장을 확인하지 못했어요. 저장된 값을 다시 확인해 주세요.");}
        },"SettingsEditorSave").start();
        return id;
    }
    public static Job get(String id){return JOBS.get(id);}
    public static void consume(String id){JOBS.remove(id);}
}
