package org.smartregister.chw.interactor;

import android.annotation.SuppressLint;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.actionhelper.CareGiverResponsivenessActionHelper;
import org.smartregister.chw.actionhelper.ImmunizationsHelper;
import org.smartregister.chw.actionhelper.MalariaPreventionActionHelper;
import org.smartregister.chw.actionhelper.KMCSkinToSkinCounsellingHelper;
import org.smartregister.chw.actionhelper.NeonatalDangerSignsActionHelper;
import org.smartregister.chw.actionhelper.NewBornCareBreastfeedingHelper;
import org.smartregister.chw.actionhelper.NewBornCareIntroductionHelper;
import org.smartregister.chw.actionhelper.NewbornCordCareActionHelper;
import org.smartregister.chw.actionhelper.PlayAssessmentCounselingActionHelper;
import org.smartregister.chw.actionhelper.ProblemSolvingActionHelper;
import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.helper.CCDChildDisciplineActionHelper;
import org.smartregister.chw.helper.CCDCommunicationAssessmentAction;
import org.smartregister.chw.helper.CCDDevelopmentScreeningAction;
import org.smartregister.chw.helper.CCDIntroductionAction;
import org.smartregister.chw.helper.ChildSafetyActionHelper;
import org.smartregister.chw.helper.ComplimentaryFeedingActionHelper;
import org.smartregister.chw.anc.util.VisitUtils;
import org.smartregister.chw.application.ChwApplication;
import org.smartregister.chw.helper.ToddlerDangerSignAction;
import org.smartregister.chw.util.Constants;
import org.smartregister.chw.util.KKCoreConstants;
import org.smartregister.chw.util.KkConstants;
import org.smartregister.domain.Alert;
import org.smartregister.domain.AlertStatus;
import org.smartregister.domain.Immunizations;
import org.smartregister.immunization.domain.ServiceWrapper;
import org.smartregister.util.DateUtil;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class ChildHomeVisitInteractorFlv extends DefaultChildHomeVisitInteractorFlv {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

    @Override
    public LinkedHashMap<String, BaseAncHomeVisitAction> calculateActions(BaseAncHomeVisitContract.View view, MemberObject memberObject, BaseAncHomeVisitContract.InteractorCallBack callBack) throws BaseAncHomeVisitAction.ValidationException {
        actionList = new LinkedHashMap<>();
        context = view.getContext();
        this.memberObject = memberObject;
        editMode = view.getEditMode();
        try {
            this.dob = DateTime.parse(memberObject.getDob()).toDate(); //ChildDao.getChild(memberObject.getBaseEntityId()).getDateOfBirth();
        } catch (Exception e) {
            Timber.e(e);
        }
        this.view = view;
        // get the preloaded data
        if (view.getEditMode()) {
            Visit lastVisit = getVisitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EventType.CHILD_HOME_VISIT);
            if (lastVisit != null) {
                details = VisitUtils.getVisitGroups(getVisitDetailsRepository().getVisits(lastVisit.getVisitId()));
            }
        }

        Map<String, ServiceWrapper> serviceWrapperMap = getServices();

        try {
            Constants.JSON_FORM.setLocaleAndAssetManager(ChwApplication.getCurrentLocale(), ChwApplication.getInstance().getApplicationContext().getAssets());
        } catch (Exception e) {
            Timber.e(e);
        }

        bindEvents(serviceWrapperMap);

        return actionList;
    }


    @Override
    protected void bindEvents(Map<String, ServiceWrapper> serviceWrapperMap) throws BaseAncHomeVisitAction.ValidationException {

        try {

            String childAge = DateUtil.getDuration(new DateTime(dob));
            int childAgeInMonth = -1;
            //Get the month part
            if (!childAge.contains("y")){
                //The child is less than one year
                if (childAge.contains("m")) {
                    String childMonth = childAge.substring(0, childAge.indexOf("m"));
                    childAgeInMonth =  Integer.parseInt(childMonth);
                }
            }

            evaluateToddlerDangerSign(serviceWrapperMap);
            evaluateNewBornCareIntro(serviceWrapperMap);
            evaluateBreastFeeding(serviceWrapperMap);
            evaluateNeonatalDangerSigns(serviceWrapperMap);
            evaluateMalariaPrevention(serviceWrapperMap);
            evaluateChildPlayAssessmentCounseling(serviceWrapperMap);
            evaluateProblemSolving(serviceWrapperMap);
            evaluateCareGiverResponsiveness(serviceWrapperMap);
            evaluateNewbornCordCare(serviceWrapperMap);
            evaluateCCDIntro(serviceWrapperMap);
            evaluateCCDDevelopmentScreening(serviceWrapperMap);
            evaluateCCDCommunicationAssessment(serviceWrapperMap, childAgeInMonth);
            evaluateCCDChildDiscipline(serviceWrapperMap);
            evaluateCCDChildSafety(serviceWrapperMap);
            evaluateComplementaryFeeding(serviceWrapperMap);
            evaluateImmunizations(serviceWrapperMap);

            evaluateKMCSkinToSkinCounselling(serviceWrapperMap);
        } catch (BaseAncHomeVisitAction.ValidationException e) {
            throw (e);
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    protected void evaluateToddlerDangerSign(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Toddler danger sign");

        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);

        String title = context.getString(R.string.toddler_danger_sign_month, serviceIteration);

        // alert if overdue after 14 days
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        ToddlerDangerSignAction helper = new ToddlerDangerSignAction(context, alert);

        Map<String, List<VisitDetail>> details = getDetails(KKCoreConstants.ChildVisitEvents.TODDLER_DANGER_SIGN);

        BaseAncHomeVisitAction toddler_ds_action = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.toddler_danger_sign_month, serviceIteration))
                .withOptional(false)
                .withDetails(details)
                .withFormName("child_hv_toddler_danger_sign")
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .withHelper(helper)
                .build();

        actionList.put(title, toddler_ds_action);
    }

    protected void evaluateBreastFeeding(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {

        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Essential care breastfeeding");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceName = serviceWrapper.getName();

        // Check if it is a dummy -5 weeks service that is there to re-set milestone to 0 before start 1 months recurring
        if ("Essential care breastfeeding -5 weeks".equalsIgnoreCase(serviceName)) return;

        // Get the very first breastfeeding visit
        boolean firstBreastFeedingHappened;
        List<Visit> breastFeedingServiceVisits = getVisitRepository().getVisits(memberObject.getBaseEntityId(), "Essential New Born Care: Breastfeeding");

        firstBreastFeedingHappened = breastFeedingServiceVisits.size() > 0;

        String title = getBreastfeedingServiceTittle(serviceWrapper.getName());

        NewBornCareBreastfeedingHelper helper = new NewBornCareBreastfeedingHelper(context, alert, firstBreastFeedingHappened, serviceWrapper);
        JSONObject jsonObject = getFormJson(KkConstants.KKJSON_FORM_CONSTANT.KKCHILD_HOME_VISIT.getChildHvBreastfeeding(), memberObject.getBaseEntityId());

        BaseAncHomeVisitAction action = getBuilder(title)
                .withHelper(helper)
                .withDetails(details)
                .withOptional(false)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withFormName(KkConstants.KKJSON_FORM_CONSTANT.KKCHILD_HOME_VISIT.getChildHvBreastfeeding())
                .withPayloadDetails(serviceWrapper.getName())
                .build();

        actionList.put(title, action);

    }

    private void evaluateNewBornCareIntro(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Essential New Born Care: Introduction");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);

        // Get the very first visit
        List<Visit> introductionVisits = getVisitRepository().getVisits(memberObject.getBaseEntityId(), KkConstants.EventType.ESSENTIAL_NEW_BORN_CARE_INTRO);
        boolean firstVisitDone = introductionVisits.size() > 0;

        boolean isOverdue = alert.status().equals(AlertStatus.urgent);
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        NewBornCareIntroductionHelper newBornIntroHelper = new NewBornCareIntroductionHelper(context, firstVisitDone);
        Map<String, List<VisitDetail>> details = getDetails(KkConstants.EventType.ESSENTIAL_NEW_BORN_CARE_INTRO);

        BaseAncHomeVisitAction newBornCareIntroAction = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.new_born_care_introduction_month, serviceIteration))
                .withOptional(false)
                .withDetails(details)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.SEPARATE)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withFormName(KkConstants.KKJSON_FORM_CONSTANT.KKCHILD_HOME_VISIT.getChildHvEssentialCareIntroductionForm())
                .withScheduleStatus(BaseAncHomeVisitAction.ScheduleStatus.DUE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .withHelper(newBornIntroHelper)
                .build();

        actionList.put(context.getString(R.string.new_born_care_introduction_month, serviceIteration), newBornCareIntroAction);
    }

    private void evaluateNeonatalDangerSigns(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Neonatal Danger Signs");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);

        // Todo -> Compute overdue
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        NeonatalDangerSignsActionHelper neonatalDangerSignsActionHelper = new NeonatalDangerSignsActionHelper(context, alert);
        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.CHILD_HOME_VISIT);

        BaseAncHomeVisitAction neoNatalDangerSignsAction = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.neonatal_danger_signs_month, serviceIteration))
                .withOptional(false)
                .withDetails(details)
                .withFormName("child_hv_neonatal_danger_signs")
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .withHelper(neonatalDangerSignsActionHelper)
                .build();

        actionList.put(context.getString(R.string.neonatal_danger_signs_month, serviceIteration), neoNatalDangerSignsAction);
    }

    private void evaluateKMCSkinToSkinCounselling(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("KMC skin-to-skin counselling");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);

        // Todo -> Compute overdue
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        KMCSkinToSkinCounsellingHelper skinToSkinCounsellingHelper = new KMCSkinToSkinCounsellingHelper(alert);
        Map<String, List<VisitDetail>> details = getDetails(Constants.EventType.CHILD_HOME_VISIT);

        BaseAncHomeVisitAction skinToSkinCounsellingAction = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.kmc_skin_to_skin_counselling_month, serviceIteration))
                .withOptional(false)
                .withDetails(details)
                .withFormName("child_hv_kmc_skin_to_skin_counselling")
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .withHelper(skinToSkinCounsellingHelper)
                .build();

        actionList.put(context.getString(R.string.kmc_skin_to_skin_counselling_month, serviceIteration), skinToSkinCounsellingAction);
    }

    private void evaluateCCDIntro(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("CCD introduction");

        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);
        String title = context.getString(R.string.ccd_introduction, serviceIteration);

        // alert if overdue after 14 days
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        CCDIntroductionAction helper = new CCDIntroductionAction(context, alert);

        Map<String, List<VisitDetail>> details = getDetails(KKCoreConstants.ChildVisitEvents.CCD_INTRODUCTION);

        BaseAncHomeVisitAction ccd_intro_action = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.ccd_introduction, serviceIteration))
                .withOptional(false)
                .withDetails(details)
                .withFormName("child_hv_ccd_introduction")
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .withHelper(helper)
                .build();

        actionList.put(title, ccd_intro_action);

    }

    private void evaluateCCDDevelopmentScreening(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("CCD development screening");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);
        String title = context.getString(R.string.ccd_development_screening, serviceIteration);

        // alert if overdue after 14 days
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        CCDDevelopmentScreeningAction ccdDevelopmentScreeningAction = new CCDDevelopmentScreeningAction(context, alert);

        Map<String, List<VisitDetail>> details = getDetails(KKCoreConstants.ChildVisitEvents.CCD_DEVELOPMENT_SCREENING);

        BaseAncHomeVisitAction ccd_development_screening_action = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.ccd_development_screening, serviceIteration))
                .withOptional(false)
                .withDetails(details)
                .withFormName("child_hv_ccd_development_screening")
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .withHelper(ccdDevelopmentScreeningAction)
                .build();

        actionList.put(title, ccd_development_screening_action);

    }

    private void evaluateCCDCommunicationAssessment(Map<String, ServiceWrapper> serviceWrapperMap, int childAge) throws Exception {

        ServiceWrapper serviceWrapper = serviceWrapperMap.get("CCD communication assessment");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);
        String title = context.getString(R.string.ccd_communication_assessment, serviceIteration);

        // alert if overdue after 14 days
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        CCDCommunicationAssessmentAction ccdCommunicationAssessmentAction = new CCDCommunicationAssessmentAction(context, alert, childAge);

        Map<String, List<VisitDetail>> details = getDetails(KKCoreConstants.ChildVisitEvents.CCD_DEVELOPMENT_SCREENING);

        BaseAncHomeVisitAction ccd_communication_assessment = new BaseAncHomeVisitAction.Builder(context, title)
                .withOptional(false)
                .withDetails(details)
                .withFormName("child_hv_ccd_communication_assessment")
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .withHelper(ccdCommunicationAssessmentAction)
                .build();

        actionList.put(title, ccd_communication_assessment);

    }

    private void evaluateCCDChildDiscipline(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {

        ServiceWrapper serviceWrapper = serviceWrapperMap.get("CCD child discipline");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);
        String title = context.getString(R.string.ccd_child_discipline, serviceIteration);

        // alert if overdue after 14 days
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        CCDChildDisciplineActionHelper ccdChildDisciplineAction = new CCDChildDisciplineActionHelper(context, alert);

        Map<String, List<VisitDetail>> details = getDetails(KKCoreConstants.ChildVisitEvents.CCD_CHILD_DISCIPLINE);

        BaseAncHomeVisitAction ccd_child_discipline_action = new BaseAncHomeVisitAction.Builder(context, title)
                .withOptional(false)
                .withDetails(details)
                .withFormName("child_hv_ccd_child_discipline")
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .withHelper(ccdChildDisciplineAction)
                .build();

        actionList.put(title, ccd_child_discipline_action);

    }

    private void evaluateCCDChildSafety(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {

        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Child safety");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);
        String title = context.getString(R.string.child_safety, serviceIteration);

        // alert if overdue after 14 days
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        ChildSafetyActionHelper childSafetyActionHelper = new ChildSafetyActionHelper(context, alert);

        Map<String, List<VisitDetail>> details = getDetails(KKCoreConstants.ChildVisitEvents.CHILD_SAFETY);

        BaseAncHomeVisitAction child_safety_action = new BaseAncHomeVisitAction.Builder(context, title)
                .withOptional(false)
                .withDetails(details)
                .withFormName("child_hv_child_safety")
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .withHelper(childSafetyActionHelper)
                .build();

        actionList.put(title, child_safety_action);

    }

    private void evaluateComplementaryFeeding(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {

        ServiceWrapper serviceWrapper = serviceWrapperMap.get("CCD communication assessment");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceIteration = serviceWrapper.getName().substring(serviceWrapper.getName().length() - 1);
        String title = context.getString(R.string.complimentary_feeding, serviceIteration);

        // alert if overdue after 14 days
        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        ComplimentaryFeedingActionHelper complimentaryFeedingActionHelper = new ComplimentaryFeedingActionHelper(context, alert);

        Map<String, List<VisitDetail>> details = getDetails(KKCoreConstants.ChildVisitEvents.COMPLIMENTARY_FEEDING);

        BaseAncHomeVisitAction complementary_feeding = new BaseAncHomeVisitAction.Builder(context, title)
                .withOptional(false)
                .withDetails(details)
                .withFormName("child_hv_complimentary_feeding")
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.DUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .withHelper(complimentaryFeedingActionHelper)
                .build();

        actionList.put(title, complementary_feeding);

    }

    protected void evaluateMalariaPrevention(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {

        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Malaria Prevention");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceName = serviceWrapper.getName();

        String title = getMalariaPreventionServiceTittle(serviceWrapper.getName());

        MalariaPreventionActionHelper helper = new MalariaPreventionActionHelper(context, alert);

        BaseAncHomeVisitAction action = getBuilder(title)
                .withHelper(helper)
                .withDetails(details)
                .withOptional(false)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withFormName(KkConstants.KKJSON_FORM_CONSTANT.KKCHILD_HOME_VISIT.getChildHvMalariaPrevention())
                .withPayloadDetails(serviceWrapper.getName())
                .build();

        actionList.put(title, action);

    }

    protected void evaluateChildPlayAssessmentCounseling(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {

        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Play Assessment and Counselling");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceName = serviceWrapper.getName();

        // Check if it is a dummy -5 weeks service that is there to re-set milestone to 0 before start 1 months recurring
        if ("Play Assessment and Counselling -5 weeks".equalsIgnoreCase(serviceName)) return;

        String[] serviceNameSplit = serviceName.split(" ");
        String period = serviceNameSplit[serviceNameSplit.length - 2];
        String periodNoun = serviceNameSplit[serviceNameSplit.length - 1];

        String title = context.getString(R.string.child_play_and_assessment_counselling) + getTranslatedPeriod(period, periodNoun);

        PlayAssessmentCounselingActionHelper helper = new PlayAssessmentCounselingActionHelper(serviceWrapper);

        BaseAncHomeVisitAction action = getBuilder(title)
                .withHelper(helper)
                .withDetails(details)
                .withOptional(false)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withFormName(KkConstants.KKJSON_FORM_CONSTANT.KKCHILD_HOME_VISIT.getChildHvPlayAssessmentCounselling())
                .withPayloadDetails(serviceWrapper.getName())
                .build();

        actionList.put(title, action);

    }

    private void evaluateProblemSolving(Map<String, ServiceWrapper> serviceWrapperMap) throws BaseAncHomeVisitAction.ValidationException{

        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Problem Solving");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        ProblemSolvingActionHelper actionHelper = new ProblemSolvingActionHelper(alert, context);

        String title = context.getString(R.string.ccd_problem_solving);
        BaseAncHomeVisitAction action = getBuilder(title)
                .withHelper(actionHelper)
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.OVERDUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withDetails(details)
                .withOptional(false)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .withFormName(KkConstants.KKJSON_FORM_CONSTANT.KKCHILD_HOME_VISIT.getChildHvProblemSolving())
                .build();

        actionList.put(title, action);
    }

    protected void evaluateCareGiverResponsiveness(Map<String, ServiceWrapper> serviceWrapperMap) throws BaseAncHomeVisitAction.ValidationException {

        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Caregiver Responsiveness");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        boolean isOverdue = new LocalDate().isAfter(new LocalDate(alert.startDate()).plusDays(14));
        String dueState = !isOverdue ? context.getString(R.string.due) : context.getString(R.string.overdue);

        CareGiverResponsivenessActionHelper actionHelper = new CareGiverResponsivenessActionHelper(alert);

        String title = context.getString(R.string.ccd_caregiver_responsiveness);

        BaseAncHomeVisitAction action = getBuilder(title)
                .withHelper(actionHelper)
                .withScheduleStatus(!isOverdue ? BaseAncHomeVisitAction.ScheduleStatus.OVERDUE : BaseAncHomeVisitAction.ScheduleStatus.OVERDUE)
                .withDetails(details)
                .withOptional(false)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withSubtitle(MessageFormat.format("{0}{1}", dueState, DateTimeFormat.forPattern("dd MMM yyyy").print(new DateTime(serviceWrapper.getVaccineDate()))))
                .withFormName(KkConstants.KKJSON_FORM_CONSTANT.KKCHILD_HOME_VISIT.getChildHvCaregiverResponsiveness())
                .build();

        actionList.put(title, action);

    }

    protected void evaluateNewbornCordCare(Map<String, ServiceWrapper> serviceWrapperMap) throws BaseAncHomeVisitAction.ValidationException {

        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Newborn Cord Care");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        NewbornCordCareActionHelper actionHelper = new NewbornCordCareActionHelper();

        String title = context.getString(R.string.newborn_care_cord_care);

        BaseAncHomeVisitAction action = getBuilder(title)
                .withHelper(actionHelper)
                .withDetails(details)
                .withOptional(false)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withFormName(KkConstants.KKJSON_FORM_CONSTANT.KKCHILD_HOME_VISIT.getChildHvNewbornCordCare())
                .build();

        actionList.put(title, action);

    }

    private void evaluateImmunizations(Map<String, ServiceWrapper> serviceWrapperMap) throws Exception {
        ServiceWrapper serviceWrapper = serviceWrapperMap.get("Immunizations");
        if (serviceWrapper == null) return;

        Alert alert = serviceWrapper.getAlert();
        if (alert == null || new LocalDate().isBefore(new LocalDate(alert.startDate()))) return;

        final String serviceName = serviceWrapper.getName();

        String[] serviceNameSplit = serviceName.split(" ");
        String period = serviceNameSplit[serviceNameSplit.length - 2];
        String periodNoun = serviceNameSplit[serviceNameSplit.length - 1];

        String immunizationsTitle = context.getString(R.string.immunizations) + getTranslatedPeriod(period, periodNoun);
        Map<String, List<VisitDetail>> details = getDetails(KkConstants.EventType.IMMUNIZATIONS);

        ImmunizationsHelper immunizationsHelper = new ImmunizationsHelper(serviceWrapper);

        BaseAncHomeVisitAction immunizationsAction = getBuilder(immunizationsTitle)
                .withHelper(immunizationsHelper)
                .withDetails(details)
                .withOptional(false)
                .withBaseEntityID(memberObject.getBaseEntityId())
                .withProcessingMode(BaseAncHomeVisitAction.ProcessingMode.COMBINED)
                .withPayloadType(BaseAncHomeVisitAction.PayloadType.SERVICE)
                .withFormName(KkConstants.KKJSON_FORM_CONSTANT.KKCHILD_HOME_VISIT.getChildHvImmunizationsForm())
                .build();

        actionList.put(immunizationsTitle, immunizationsAction);
    }

    private String getBreastfeedingServiceTittle(String serviceName) {

        String[] serviceNameSplit = serviceName.split(" ");
        String period = serviceNameSplit[serviceNameSplit.length - 2];
        String periodNoun = serviceNameSplit[serviceNameSplit.length - 1];

        return context.getString(R.string.essential_newborn_care_breastfeeding) + getTranslatedPeriod(period, periodNoun);
    }

    private String getMalariaPreventionServiceTittle(String serviceName) {

        String[] serviceNameSplit = serviceName.split(" ");
        String period = serviceNameSplit[serviceNameSplit.length - 2];
        String periodNoun = serviceNameSplit[serviceNameSplit.length - 1];

        return context.getString(R.string.malaria_prevention_service) + getTranslatedPeriod(period, periodNoun);
    }

    private String getTranslatedPeriod(String period, String periodNoun) {
        String translatedText = "";

        if ("hours".equalsIgnoreCase(periodNoun)) {
            translatedText = context.getString(R.string.hour_of, period);
        } else if ("days".equalsIgnoreCase(periodNoun)) {
            translatedText = context.getString(R.string.day_of, period);
        } else if ("weeks".equalsIgnoreCase(periodNoun)) {
            translatedText = context.getString(R.string.week_of, period);
        } else {
            translatedText = context.getString(R.string.month_of, period);
        }
        return translatedText;
    }
}
