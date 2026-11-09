package com.amabeat.control;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final String HOST="http://192.168.4.1";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ArrayList<String> wavs=new ArrayList<>();
    private JSONArray patches=new JSONArray();
    private Spinner patchSpinner,wavSpinner;
    private TextView status,stats,selected;
    private final Button[] padButtons=new Button[8];
    private int pad=0,patch=0;
    private LinearLayout root;

    @Override public void onCreate(Bundle state){super.onCreate(state);buildUi();refresh();}
    private TextView text(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(size);v.setPadding(8,10,8,10);return v;}
    private Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->action.run());return b;}
    private void buildUi(){
        ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(14,19,27));root=new LinearLayout(this);root.setOrientation(1);root.setPadding(16,18,16,30);sc.addView(root);setContentView(sc);
        root.addView(text("AMABEAT S3 CONTROL",24));root.addView(text("Connect phone Wi-Fi to AMABEAT_UPLOAD (password: 12345678). Stay connected even if Android says no internet.",13));
        LinearLayout top=new LinearLayout(this);top.setOrientation(0);top.addView(button("Refresh Library",this::refresh),new LinearLayout.LayoutParams(0,-2,1));top.addView(button("Choose Tone Folder",this::chooseFolder),new LinearLayout.LayoutParams(0,-2,1));root.addView(top);
        stats=text("Not connected",14);root.addView(stats);
        root.addView(text("PATCH",16));patchSpinner=new Spinner(this);root.addView(patchSpinner);
        patchSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){patch=pos;renderPads();}});
        root.addView(text("8 PADS",16));
        for(int row=0;row<2;row++){LinearLayout line=new LinearLayout(this);for(int col=0;col<4;col++){final int idx=row*4+col;Button b=button("PAD "+(idx+1),()->{pad=idx;renderPads();});padButtons[idx]=b;line.addView(b,new LinearLayout.LayoutParams(0,100,1));}root.addView(line);}
        selected=text("Select a pad",13);root.addView(selected);
        root.addView(text("WAV LIBRARY",16));wavSpinner=new Spinner(this);root.addView(wavSpinner);
        root.addView(button("Assign WAV to Selected Pad",()->runRequest("/api/assign?patch="+patch+"&pad="+(pad+1)+"&path="+enc(selectedWav()),"Assigned WAV")));
        root.addView(button("Preview Selected Pad",()->runRequest("/api/preview?patch="+patch+"&pad="+(pad+1),"Preview requested")));
        root.addView(button("Auto Assign 8 Pads",()->runRequest("/api/auto?patch="+patch,"Auto assigned")));
        root.addView(button("Upload WAV Folder",this::chooseFolder));
        status=text("Ready",14);root.addView(status);
    }
    private String selectedWav(){int i=wavSpinner.getSelectedItemPosition();return i>=0&&i<wavs.size()?wavs.get(i):"";}
    private String enc(String s){try{return java.net.URLEncoder.encode(s,"UTF-8");}catch(Exception e){return s;}}
    private void message(String s){runOnUiThread(()->status.setText(s));}
    private void runRequest(String url,String ok){if(url.contains("path=")&&selectedWav().isEmpty()){message("No WAV files found. Upload a folder first.");return;}worker.execute(()->{try{request("POST",url,null);message(ok);refresh();}catch(Exception e){message("Error: "+e.getMessage());}});}
    private String request(String method,String endpoint,byte[] body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(HOST+endpoint).openConnection();c.setRequestMethod(method);c.setConnectTimeout(7000);c.setReadTimeout(20000);
        if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","multipart/form-data; boundary=AMABEATBOUNDARY");c.setFixedLengthStreamingMode(body.length);try(OutputStream out=c.getOutputStream()){out.write(body);}}
        int code=c.getResponseCode();InputStream in=code<400?c.getInputStream():c.getErrorStream();String response="";if(in!=null){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);response=out.toString("UTF-8");in.close();}c.disconnect();if(code>=400)throw new IOException("HTTP "+code+": "+response);return response;
    }
    private void refresh(){worker.execute(()->{try{String json=request("GET","/api/data",null);JSONObject data=new JSONObject(json);JSONArray ps=data.getJSONArray("patches"),fs=data.getJSONArray("files");ArrayList<String> names=new ArrayList<>(),files=new ArrayList<>();for(int i=0;i<ps.length();i++)names.add(ps.getJSONObject(i).getString("name"));for(int i=0;i<fs.length();i++)files.add(fs.getString(i));long free=data.optLong("free");runOnUiThread(()->{patches=ps;wavs.clear();wavs.addAll(files);patchSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));wavSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,files));stats.setText(names.size()+" patches • "+files.size()+" WAV files • "+(free/1024/1024)+" MB free");if(patch<names.size())patchSpinner.setSelection(patch);renderPads();status.setText("Library refreshed");});}catch(Exception e){message("Connection failed: Connect to AMABEAT_UPLOAD Wi-Fi. "+e.getMessage());}});}
    private void renderPads(){for(int i=0;i<8;i++){String name="No tone";try{if(patch<patches.length()){String s=patches.getJSONObject(patch).getJSONArray("pads").optString(i,"");if(!s.isEmpty())name=s.substring(s.lastIndexOf('/')+1);}}catch(Exception ignored){}padButtons[i].setText("PAD "+(i+1)+"\n"+(name.length()>12?name.substring(0,12):name));padButtons[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(i==pad?Color.rgb(17,143,105):Color.rgb(53,67,86)));}selected.setText("Selected: PAD "+(pad+1));}
    private void chooseFolder(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,42);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==42&&result==RESULT_OK&&data!=null){Uri uri=data.getData();new AlertDialog.Builder(this).setTitle("Upload WAV folder?").setMessage("Select the main tone-pack folder or one patch folder. Existing WAV files with the same name may be replaced. Upload can take time.").setPositiveButton("Upload",(d,w)->uploadTree(uri)).setNegativeButton("Cancel",null).show();}}
    private static class Tone {Uri uri;String path;long size;Tone(Uri u,String p,long n){uri=u;path=p;size=n;}}
    private void scan(Uri dir,String prefix,List<Tone> tones)throws Exception{
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(dir,DocumentsContract.getDocumentId(dir));
        try(Cursor cur=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE},null,null,null)){
            if(cur==null)return;while(cur.moveToNext()){String id=cur.getString(0),name=cur.getString(1),type=cur.getString(2);long size=cur.isNull(3)?0:cur.getLong(3);Uri child=DocumentsContract.buildDocumentUriUsingTree(dir,id);
                if(DocumentsContract.Document.MIME_TYPE_DIR.equals(type))scan(child,prefix+name+"/",tones);
                else if(name.toLowerCase(Locale.ROOT).endsWith(".wav"))tones.add(new Tone(child,prefix+name,size));
            }
        }
    }
    private void uploadTree(Uri tree){worker.execute(()->{try{
        String rootName="Patch";try(Cursor c=getContentResolver().query(tree,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())rootName=c.getString(0);}
        List<Tone> tones=new ArrayList<>();scan(tree,"",tones);if(tones.isEmpty()){message("No WAV files in selected folder");return;}
        boolean nested=false;for(Tone t:tones)if(t.path.contains("/")){nested=true;break;}
        long needed=0;for(Tone t:tones)needed+=t.size;
        JSONObject current=new JSONObject(request("GET","/api/data",null));if(needed>current.optLong("free",0)){message("Not enough FFat space: need "+(needed/1024)+" KB");return;}
        int done=0;for(Tone t:tones){String rel=nested?t.path:rootName+"/"+t.path;String path="/tones/"+rel;String boundary="AMABEATBOUNDARY";
            ByteArrayOutputStream body=new ByteArrayOutputStream();body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+t.path.substring(t.path.lastIndexOf('/')+1).replace("\"","")+"\"\r\nContent-Type: audio/wav\r\n\r\n").getBytes("UTF-8"));
            try(InputStream in=getContentResolver().openInputStream(t.uri)){if(in==null)throw new IOException("Cannot open "+t.path);byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)body.write(buffer,0,n);}
            body.write(("\r\n--"+boundary+"--\r\n").getBytes("UTF-8"));request("POST","/upload?path="+enc(path),body.toByteArray());done++;message("Uploaded "+done+" / "+tones.size()+" WAV files");
        }
        request("POST","/api/refresh",null);message("Upload complete: "+done+" WAV files");refresh();
    }catch(Exception e){message("Upload stopped: "+e.getMessage());}});}
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
