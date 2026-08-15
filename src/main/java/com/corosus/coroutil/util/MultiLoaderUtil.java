package com.corosus.coroutil.util;

import com.corosus.coroutil.loader.fabric.ModConfigDataFabric;
import com.corosus.modconfig.IConfigCategory;
import com.corosus.modconfig.ModConfigData;

public class MultiLoaderUtil {

    private static final MultiLoaderUtil instance = new MultiLoaderUtil();

    private MultiLoaderUtil() {

    }

    public static synchronized MultiLoaderUtil instance() {
        return instance;
    }

    public synchronized boolean isForge() {
        return false;
    }

    public synchronized boolean isNeoForge() {
        return false;
    }

    public synchronized boolean isFabric() {
        return true;
    }

    public synchronized ModConfigData makeLoaderSpecificConfigData(String savePath, String parStr, Class parClass, IConfigCategory parConfig) {
        return new ModConfigDataFabric(savePath, parStr, parClass, parConfig);
    }
}
