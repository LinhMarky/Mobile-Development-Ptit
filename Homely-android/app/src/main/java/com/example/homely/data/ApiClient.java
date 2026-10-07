package com.example.homely.data;
import okhttp3.*;
import org.json.*;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.example.homely.BuildConfig;

public class ApiClient {
    public final SessionStore session;
    public final OkHttpClient http=new OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS)
            .readTimeout(25,TimeUnit.SECONDS).callTimeout(40,TimeUnit.SECONDS).build();
    public ApiClient(SessionStore session){this.session=session;}
    public static JSONObject json(Object... pairs){
        JSONObject result=new JSONObject();
        try {for(int i=0;i<pairs.length;i+=2) result.put(pairs[i].toString(),pairs[i+1]);}catch(JSONException e){throw new IllegalArgumentException(e);}
        return result;
    }
    public synchronized JSONObject call(String method,String path,JSONObject body,String key) throws Exception {
        return request(method,path,body,key,true);
    }
    private JSONObject request(String method,String path,JSONObject body,String key,boolean refresh) throws Exception {
        Request.Builder req=new Request.Builder().url(BuildConfig.API_URL+path);
        String token=session.token();
        if(!token.isEmpty()&&!java.util.Set.of("/auth/login","/auth/refresh","/auth/register","/auth/verify-email").contains(path))
            req.header("Authorization","Bearer "+token);
        if(key!=null)req.header("Idempotency-Key",key);
        RequestBody content=body==null?RequestBody.create("",MediaType.get("application/json")):
                RequestBody.create(body.toString(),MediaType.get("application/json"));
        req.method(method,method.equals("GET")||method.equals("DELETE")?null:content);
        try(Response response=http.newCall(req.build()).execute()){
            String raw=response.body()==null?"":response.body().string();
            if(response.code()==401 && refresh && !path.equals("/auth/refresh") && !path.equals("/auth/login") && !session.session().optString("refresh_token").isEmpty()){
                JSONObject next=request("POST","/auth/refresh",json("refresh_token",session.session().optString("refresh_token"),
                        "installation_id",session.installation(),"device_name",android.os.Build.MODEL),null,false);
                session.save(next);
                return request(method,path,body,key,false);
            }
            if(!response.isSuccessful()){
                String detail=ApiEnvelope.errorMessage(raw,response.code(),"Không thực hiện được yêu cầu ("+response.code()+").");
                if(response.code()==401)session.clear();
                throw new ApiException(response.code(),detail);
            }
            return ApiEnvelope.data(raw,response.code());
        } catch(IOException e){throw new IOException("Không kết nối được máy chủ. Kiểm tra mạng và thử lại.",e);}
    }
    public void login(String email,String password) throws Exception {
        session.clear();
        session.save(call("POST","/auth/login",json("email",email,"password",password,
                "installation_id",session.installation(),"device_name",android.os.Build.MODEL),null));
    }
    public void logout() throws Exception {
        try {
            if(session.loggedIn())call("POST","/auth/logout",json("refresh_token",session.session().optString("refresh_token"),
                    "installation_id",session.installation()),null);
        } finally { session.clear(); }
    }
    public void refreshSession() throws Exception {
        session.save(call("POST","/auth/refresh",json("refresh_token",session.session().optString("refresh_token"),
                "installation_id",session.installation(),"device_name",android.os.Build.MODEL),null));
    }
    public void registerPush(String token) throws Exception {
        if(session.loggedIn())call("PUT","/profile/device-token",json("token",token,"platform","ANDROID",
                "device_name",android.os.Build.MODEL,"installation_id",session.installation()),null);
    }
    public synchronized JSONObject uploadPhoto(byte[] bytes,String type) throws Exception {
        if(bytes.length>10*1024*1024)throw new IOException("Ảnh tối đa 10 MB.");
        RequestBody form=new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("purpose","ROOM_PHOTO")
                .addFormDataPart("file","room-photo",RequestBody.create(bytes,MediaType.get(type))).build();
        for(int attempt=0;attempt<2;attempt++){
            Request req=new Request.Builder().url(BuildConfig.API_URL+"/media/upload")
                    .header("Authorization","Bearer "+session.token()).post(form).build();
            try(Response response=http.newCall(req).execute()){
                String raw=response.body()==null?"{}":response.body().string();
                if(response.code()==401&&attempt==0){refreshSession();continue;}
                if(!response.isSuccessful())throw new ApiException(response.code(),ApiEnvelope.errorMessage(raw,response.code(),"Không tải được ảnh."));
                return ApiEnvelope.data(raw,response.code());
            }
        }
        throw new IOException("Phiên đăng nhập đã hết hạn.");
    }
    public static class ApiException extends Exception {
        public final int status;
        public ApiException(int status,String message){super(message);this.status=status;}
    }
}
