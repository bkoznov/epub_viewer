package com.jideguru.epub_viewer;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.folioreader.Config;
import com.folioreader.FolioReader;
import com.folioreader.model.HighLight;
import com.folioreader.model.locators.ReadLocator;
import com.folioreader.ui.base.OnSaveHighlight;
import com.folioreader.util.AppUtil;
import com.folioreader.util.OnHighlightListener;
import com.folioreader.util.ReadLocatorListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;

public class Reader implements OnHighlightListener, ReadLocatorListener, FolioReader.OnClosedListener{

    private ReaderConfig readerConfig;
    public FolioReader folioReader;
    private Context context;
    public MethodChannel.Result result;
    public EventChannel.EventSink pageEventSink;
    private BinaryMessenger globalMessenger;
    private ReadLocator  read_locator;
    private static final String PAGE_CHANNEL = "epub_viewer/page";
    private EventChannel e;

    public EventChannel.StreamHandler xyz = new EventChannel.StreamHandler() {

        @Override
        public void onListen(Object o, EventChannel.EventSink eventSink) {
            Log.i("readLocator", "setting page event sink");

            pageEventSink = eventSink;
        }

        @Override
        public void onCancel(Object o) {
            Log.i("readLocator", "canceling page event sink");


        }
    };

    Reader(Context context, BinaryMessenger readerMessenger,ReaderConfig config){
        Log.i("readLocator", "about to set messenger 1" + readerMessenger.toString());
        globalMessenger = readerMessenger;
        this.context = context;
        readerConfig = config;
        getHighlightsAndSave();

        folioReader = FolioReader.get()
                .setOnHighlightListener(this)
                .setReadLocatorListener(this)
                .setOnClosedListener(this);

        Log.i("readLocator", "about to set paged handler");

        setPageHandler();
        Log.i("readLocator", "about to set paged handler4");

    }

    public void open(String bookPath, String lastLocation){
        final String path = bookPath;
        final String location = lastLocation;
        new Thread(new Runnable() {
            @Override
            public void run() {
               try {
                   Log.i("SavedLocation", "-> savedLocation -> " + location);
                   if(location != null && !location.isEmpty()){
                       ReadLocator readLocator = ReadLocator.fromJson(location);
                       folioReader.setReadLocator(readLocator);
                   }
                   folioReader.setConfig(readerConfig.config, true)
                           .openBook(path);  
               } catch (Exception e) {
                   e.printStackTrace();
               }
            }
        }).start();
       
    }

    public void close(){
        folioReader.close();
    }

    private void setPageHandler(){
//        final MethodChannel channel = new MethodChannel(registrar.pageHandlerMessenger(), "page");
//        channel.setMethodCallHandler(new EpubKittyPlugin());
        Log.i("readLocator", "about to set pageHandlerMessenger " + globalMessenger.toString() + " and page channel" + PAGE_CHANNEL);

        e = new EventChannel(globalMessenger,PAGE_CHANNEL);
        e.setStreamHandler(xyz);
        Log.i("readLocator", "just set paged handle 3"  + e.toString() + " and xyz" + xyz.toString());

    }

    private void getHighlightsAndSave() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<HighLight> highlightList = null;
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    highlightList = objectMapper.readValue(
                            loadAssetTextAsString("highlights/highlights_data.json"),
                            new TypeReference<List<HighlightData>>() {
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (highlightList == null) {
                    folioReader.saveReceivedHighLights(highlightList, new OnSaveHighlight() {
                        @Override
                        public void onFinished() {
                            //You can do anything on successful saving highlight list
                        }
                    });
                }
            }
        }).start();
    }


    private String loadAssetTextAsString(String name) {
        BufferedReader in = null;
        try {
            StringBuilder buf = new StringBuilder();
            InputStream is = context.getAssets().open(name);
            in = new BufferedReader(new InputStreamReader(is));

            String str;
            boolean isFirst = true;
            while ((str = in.readLine()) != null) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append('\n');
                buf.append(str);
            }
            return buf.toString();
        } catch (IOException e) {
            Log.e("Reader", "Error opening asset " + name);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e("Reader", "Error closing asset " + name);
                }
            }
        }
        return null;
    }

    @Override
    public void onFolioReaderClosed() {
        Log.i("readLocator", "-> saveReadLocator -> " + read_locator.toJson());

        if (pageEventSink != null){

            //todo
            Log.i("readLocator", " success !");
            pageEventSink.success(read_locator.toJson());
        } else {

//            setPageHandler();
//            pageEventSink.success(read_locator.toJson());

            Log.i("readLocator", "page event sink is null!!!!!");
        }
    }

    @Override
    public void onHighlight(HighLight highlight, HighLight.HighLightAction type) {

    }

    @Override
    public void saveReadLocator(ReadLocator readLocator) {
        read_locator = readLocator;
    }



}
