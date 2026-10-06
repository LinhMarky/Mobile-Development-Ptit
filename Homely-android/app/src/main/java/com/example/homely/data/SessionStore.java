package com.example.homely.data;
import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import android.util.Log;
import org.json.JSONObject;
import java.security.KeyStore;
import java.util.UUID;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

public class SessionStore {
    private final android.content.SharedPreferences prefs;
    public SessionStore(Context context){prefs=context.getSharedPreferences("homely_session",Context.MODE_PRIVATE);}
    private javax.crypto.SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(!ks.containsAlias("homely_tokens")) {
            KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder("homely_tokens",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            gen.generateKey();
        }
        return (javax.crypto.SecretKey)ks.getKey("homely_tokens",null);
    }
    public synchronized JSONObject session(){
        try {
            String raw=prefs.getString("session","");
            if(raw.isEmpty()) return new JSONObject();
            String[] parts=raw.split(":");
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
            return new JSONObject(new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8));
        } catch(Exception e){clear();return new JSONObject();}
    }
    public synchronized void save(JSONObject value) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        String raw=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(
                cipher.doFinal(value.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP);
        if(!prefs.edit().putString("session",raw).commit()) throw new java.io.IOException("Không lưu được phiên đăng nhập");
    }
    // Clear synchronously before logout/login returns; never log token or encrypted payload.
    public synchronized void clear(){
        if(!prefs.edit().remove("session").commit()) Log.w("SessionStore","Could not persist cleared session");
    }
    public String token(){return session().optString("access_token");}
    public String userId(){JSONObject u=session().optJSONObject("user");return u==null?"":u.optString("id");}
    // Serialize creation so concurrent callers use the same installation identity.
    public synchronized String installation(){
        String id=prefs.getString("installation","");
        if(id.isEmpty()){
            id=UUID.randomUUID().toString();
            if(!prefs.edit().putString("installation",id).commit()) Log.w("SessionStore","Could not persist installation identity");
        }
        return id;
    }
    public boolean loggedIn(){return !token().isEmpty();}
}
