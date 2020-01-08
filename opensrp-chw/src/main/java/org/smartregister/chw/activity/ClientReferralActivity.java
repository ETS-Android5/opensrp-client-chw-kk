package org.smartregister.chw.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.adapter.ReferralTypeAdapter;
import org.smartregister.chw.contract.ClientReferralContract;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.model.ReferralTypeModel;
import org.smartregister.chw.util.Constants;
import org.smartregister.util.FormUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class ClientReferralActivity extends AppCompatActivity implements ClientReferralContract.View, View.OnClickListener {

    private ReferralTypeAdapter referralTypeAdapter;
    private FormUtils formUtils;
    private String baseEntityId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_referral);
        referralTypeAdapter = new ReferralTypeAdapter();
        referralTypeAdapter.setOnClickListener(this);
        setUpView();
    }

    @Override
    public void setUpView() {
        List<ReferralTypeModel> referralTypeModels = null;
        RecyclerView referralTypesRecyclerView = findViewById(R.id.referralTypeRecyclerView);

        ImageView closeImageView = findViewById(R.id.close);
        closeImageView.setOnClickListener(this);

        if (getIntent().getExtras() != null) {
            referralTypeModels = getIntent().getParcelableArrayListExtra(Constants.REFERRAL_TYPES);
            baseEntityId = getIntent().getStringExtra(Constants.ENTITY_ID);
        }

        referralTypeAdapter.setReferralTypes(referralTypeModels);
        referralTypesRecyclerView.setAdapter(referralTypeAdapter);
        referralTypesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        referralTypesRecyclerView.setMotionEventSplittingEnabled(false);
    }

    @Override
    public void startReferralForm(JSONObject jsonObject) {
        ReferralRegistrationActivity.startGeneralReferralFormActivityForResults(this, baseEntityId, jsonObject);
    }

    @Override
    public FormUtils getFormUtils() throws Exception {
        if (this.formUtils == null) {
            this.formUtils = new FormUtils(this);
        }
        return this.formUtils;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.close) {
            finish();
        } else if (v.getTag() instanceof ReferralTypeAdapter.ReferralTypeViewHolder && referralTypeAdapter.canStart) {
            referralTypeAdapter.canStart = false;
            ReferralTypeAdapter.ReferralTypeViewHolder referralTypeViewHolder = (ReferralTypeAdapter.ReferralTypeViewHolder) v.getTag();
            ReferralTypeModel referralTypeModel = referralTypeAdapter.getReferralTypes().get(referralTypeViewHolder.getAdapterPosition());
            try {
                if (referralTypeModel.getFormName() == null) {
                    org.smartregister.util.Utils.showShortToast(this, getString(R.string.open_referral_form, referralTypeModel.getReferralType()));
                    referralTypeAdapter.canStart = true; //TODO Remove this necessary evil; necessary since on resume is not revoked again
                }

                JSONObject formJson = getFormUtils().getFormJson(referralTypeModel.getFormName());
                formJson.put(Constants.REFERRAL_TASK_FOCUS, referralTypeModel.getReferralType());
                startReferralForm(formJson);
            } catch (Exception e) {
                Timber.e(e, "ClientReferralActivity --> onActivityResult");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        referralTypeAdapter.canStart = true;
    }
}

