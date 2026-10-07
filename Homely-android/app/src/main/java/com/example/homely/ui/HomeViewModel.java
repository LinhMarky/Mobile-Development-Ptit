package com.example.homely.ui;
import android.app.Application;
import androidx.lifecycle.*;
import com.example.homely.data.*;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;

public class HomeViewModel extends AndroidViewModel {
    public final ApiClient api;
    public final MutableLiveData<Result> result=new MutableLiveData<>();
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Map<String,String> pendingKeys=new ConcurrentHashMap<>();
    public String screen="home";
    public long selectedId;
    public String pendingMessageId,pendingContent;
    public String roomDraftKey=UUID.randomUUID().toString();
    public long photoRoomId;
    private volatile long generation;
    public static class Result {
        public final String operation;public final JSONObject data;public final String error;public final boolean loading;
        Result(String op,JSONObject data,String error,boolean loading){this.operation=op;this.data=data;this.error=error;this.loading=loading;}
    }
    public HomeViewModel(Application app){super(app);api=new ApiClient(new SessionStore(app));}
    public void request(String op,String method,String path,JSONObject body){
        String fingerprint=method+" "+path+" "+(body==null?"":body.toString());
        String key=method.equals("POST")?pendingKeys.computeIfAbsent(fingerprint,x->UUID.randomUUID().toString()):null;
        run(op,()->{
            JSONObject data=api.call(method,path,body,key);
            synchronized(pendingKeys){pendingKeys.remove(fingerprint);}
            return data;
        });
    }
    public void run(String op,Callable<JSONObject> work){
        long current=++generation;result.setValue(new Result(op,null,null,true));
        executor.execute(()->{
            try {JSONObject data=work.call();if(current==generation)result.postValue(new Result(op,data,null,false));}
            catch(Exception e){if(current==generation)result.postValue(new Result(op,null,e.getMessage(),false));}
        });
    }
    public void invalidate(){generation++;}
    @Override protected void onCleared(){generation++;executor.shutdownNow();api.http.dispatcher().cancelAll();}
}
