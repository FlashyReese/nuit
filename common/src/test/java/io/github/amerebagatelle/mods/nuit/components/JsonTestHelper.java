package io.github.amerebagatelle.mods.nuit.components;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;

public class JsonTestHelper {
    public static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .create();

    public static JsonObject readJson(String json) {
        return GSON.fromJson(json, JsonObject.class);
    }
}
