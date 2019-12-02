package org.smartregister.chw.form_data;

import android.content.Context;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;

import java.util.List;
import java.util.Map;

public interface DataLoader {

    List<String> getFormTables(Context context, String eventName);

    @Nullable
    Map<String, Object> getValues(Context context, String tableName, String baseEntityID);

    CommonPersonObjectClient getClient(Context context, String baseEntityID);

    String getValue(Context context, String baseEntityID, JSONObject jsonObject, Map<String, Map<String, Object>> dbData) throws JSONException;

}
