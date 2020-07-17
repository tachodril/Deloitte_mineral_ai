package com.example.deloittescanimage;

import android.content.Context;
import android.content.res.Resources;

import java.util.HashMap;

public class Description {
    public static HashMap<String, String> mDescriptionMap = new HashMap<>();

    public static void initMap(Context context) {
        mDescriptionMap.put("biotite", context.getString(R.string.biotite_det));
        mDescriptionMap.put("bornite", context.getString(R.string.bornite_det));
        mDescriptionMap.put("chrysocolla", context.getString(R.string.chrysocolla_det));
        mDescriptionMap.put("malachite", context.getString(R.string.malachite_det));
        mDescriptionMap.put("muscovite", context.getString(R.string.muscovite_det));
        mDescriptionMap.put("pyrite", context.getString(R.string.pyrite_det));
        mDescriptionMap.put("quartz", context.getString(R.string.quartz_det));
    }
}
