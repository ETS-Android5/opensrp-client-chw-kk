package org.smartregister.chw.helper;

import android.content.Context;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.util.JsonFormUtils;
import org.smartregister.domain.Alert;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class CCDChildDisciplineActionHelper extends HomeVisitActionHelper {

    private Context context;
    private Alert alert;
    private String correctingChild = "";
    private String jsonPayload;

    public CCDChildDisciplineActionHelper(Context context, Alert alert){
        this.alert = alert;
        this.context = context;
    }

    @Override
    public void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details) {
        this.context = context;
        this.jsonPayload = jsonString;
    }

    @Override
    public String getPreProcessed() {
        return super.getPreProcessed();
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try{
            JSONObject jsonObject = new JSONObject(jsonPayload);
            correctingChild = JsonFormUtils.getCheckBoxValue(jsonObject, "caregiver_child_correction");
        }catch (Exception e){
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {
        return MessageFormat.format("Mother corrects child: {0}", correctingChild);
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (correctingChild.isEmpty())
            return BaseAncHomeVisitAction.Status.PENDING;
        else
            return BaseAncHomeVisitAction.Status.COMPLETED;
    }

}
