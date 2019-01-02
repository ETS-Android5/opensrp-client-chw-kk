package org.smartgresiter.wcaro.activity;

import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONObject;
import org.opensrp.api.constants.Gender;
import org.smartgresiter.wcaro.R;
import org.smartgresiter.wcaro.contract.ChildProfileContract;
import org.smartgresiter.wcaro.contract.ChildRegisterContract;
import org.smartgresiter.wcaro.custom_view.IndividualMemberFloatingMenu;
import org.smartgresiter.wcaro.listener.OnClickFloatingMenu;
import org.smartgresiter.wcaro.model.ChildProfileModel;
import org.smartgresiter.wcaro.presenter.ChildProfilePresenter;
import org.smartregister.domain.FetchStatus;
import org.smartregister.family.util.Constants;
import org.smartregister.helper.ImageRenderHelper;
import org.smartregister.view.activity.BaseProfileActivity;


public class ChildProfileActivity extends BaseProfileActivity implements ChildProfileContract.View, ChildRegisterContract.InteractorCallBack {

    private boolean appBarTitleIsShown = true;
    private int appBarLayoutScrollRange = -1;
    private String childBaseEntityId;
    private TextView textViewTitle, textViewParentName, textViewChildName, textViewGender, textViewAddress, textViewId, textViewRecord, textViewVisitNot;
    private ImageView imageViewProfile;
    private String gender;
    private OnClickFloatingMenu onClickFloatingMenu = new OnClickFloatingMenu() {
        @Override
        public void onClickMenu(int viewId) {
            switch (viewId) {
                case R.id.call_layout:
                    break;
                case R.id.registration_layout:
                    break;
                case R.id.remove_member_layout:
                    break;
            }

        }
    };

    @Override
    protected void onCreation() {
        setContentView(R.layout.activity_child_profile);
        ((IndividualMemberFloatingMenu) findViewById(R.id.individual_floating_menu)).setClickListener(onClickFloatingMenu);
        Toolbar toolbar = findViewById(R.id.collapsing_toolbar);
        textViewTitle = toolbar.findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        appBarLayout = findViewById(org.smartregister.R.id.collapsing_toolbar_appbarlayout);
        appBarLayout.addOnOffsetChangedListener(this);

        imageRenderHelper = new ImageRenderHelper(this);

        initializePresenter();
        setupViews();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

        if (appBarLayoutScrollRange == -1) {
            appBarLayoutScrollRange = appBarLayout.getTotalScrollRange();
        }
        if (appBarLayoutScrollRange + verticalOffset == 0) {

            textViewTitle.setText(patientName);
            appBarTitleIsShown = true;
        } else if (appBarTitleIsShown) {
            textViewTitle.setText(getString(R.string.return_to_all_children));
            appBarTitleIsShown = false;
        }

    }

    @Override
    protected void setupViews() {

        textViewParentName = findViewById(R.id.textview_parent_name);
        textViewChildName = findViewById(R.id.textview_name_age);
        textViewGender = findViewById(R.id.textview_gender);
        textViewAddress = findViewById(R.id.textview_address);
        textViewId = findViewById(R.id.textview_id);
        imageViewProfile = findViewById(R.id.imageview_profile);
        textViewRecord = findViewById(R.id.textview_record_visit);
        textViewVisitNot = findViewById(R.id.textview_visit_not);
        textViewRecord.setOnClickListener(this);
        textViewVisitNot.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        switch (view.getId()) {
            case R.id.textview_record_visit:
                break;
            case R.id.textview_visit_not:
                break;
        }
    }

    private void updateTopbar() {
        if (gender.equalsIgnoreCase(Gender.MALE.toString())) {
            appBarLayout.setBackgroundColor(getResources().getColor(R.color.light_blue));
        } else if (gender.equalsIgnoreCase(Gender.FEMALE.toString())) {
            appBarLayout.setBackgroundColor(getResources().getColor(R.color.light_pink));
        }

    }

    @Override
    protected void initializePresenter() {
        childBaseEntityId = getIntent().getStringExtra(Constants.INTENT_KEY.BASE_ENTITY_ID);

        presenter = new ChildProfilePresenter(this, new ChildProfileModel(), childBaseEntityId);
        fetchProfileData();
    }

    @Override
    protected ViewPager setupViewPager(ViewPager viewPager) {
        return null;
    }

    @Override
    protected void fetchProfileData() {
        presenter().fetchProfileData();
    }

    @Override
    public void startFormActivity(JSONObject form) {

    }

    @Override
    public void refreshMemberList(FetchStatus fetchStatus) {

    }

    @Override
    public void displayShortToast(int resourceId) {

    }

    @Override
    public void setProfileImage(String baseEntityId) {
        int defaultImage = gender.equalsIgnoreCase(Gender.MALE.toString()) ? R.drawable.row_boy : R.drawable.row_girl;
        imageRenderHelper.refreshProfileImage(baseEntityId, imageViewProfile, defaultImage);


    }

    @Override
    public void setParentName(String parentName) {
        textViewParentName.setText(parentName);

    }

    @Override
    public void setGender(String gender) {
        this.gender = gender;
        textViewGender.setText(gender);
        updateTopbar();

    }

    @Override
    public void setAddress(String address) {
        textViewAddress.setText(address);

    }

    @Override
    public void setId(String id) {
        textViewId.setText(id);

    }

    @Override
    public void setProfileName(String fullName) {
        patientName = fullName;
        textViewChildName.setText(fullName);

    }

    @Override
    public void setAge(String age) {
        textViewChildName.append("," + age);

    }

    @Override
    public ChildProfileContract.Presenter presenter() {
        return (ChildProfileContract.Presenter) presenter;
    }

    @Override
    public void onNoUniqueId() {

    }

    @Override
    public void onUniqueIdFetched(Triple<String, String, String> triple, String entityId, String familyId) {

    }

    @Override
    public void onRegistrationSaved(boolean isEdit) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

}