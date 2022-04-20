package org.smartregister.chw.actionhelper;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.actionhelper.HomeVisitActionHelper;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.util.JsonFormUtils;

import java.text.MessageFormat;

import timber.log.Timber;

/**
 * Created by Kassim Sheghembe on 2022-04-19
 */
public class PncDangerSignsActionHelper extends HomeVisitActionHelper {

    private String signs_present;

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            signs_present = JsonFormUtils.getCheckBoxValue(jsonObject, "danger_signs_screening_mama");
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public String evaluateSubTitle() {

        return MessageFormat.format("{0}: {1}", context.getString(R.string.anc_home_visit_danger_signs), signs_present) +
                "\n";
    }

    @Override
    public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(signs_present)) {
            return BaseAncHomeVisitAction.Status.PENDING;
        }

        return BaseAncHomeVisitAction.Status.COMPLETED;
    }
}