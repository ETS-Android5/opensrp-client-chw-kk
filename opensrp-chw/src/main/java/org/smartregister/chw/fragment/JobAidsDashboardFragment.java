package org.smartregister.chw.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.smartregister.chw.R;
import org.smartregister.chw.presenter.JobAidsDashboardFragmentPresenter;
import org.smartregister.chw.util.DashboardUtil;
import org.smartregister.reporting.contract.ReportContract;
import org.smartregister.reporting.domain.IndicatorTally;
import org.smartregister.reporting.domain.NumericIndicatorVisualization;
import org.smartregister.reporting.view.NumericDisplayFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobAidsDashboardFragment extends Fragment implements ReportContract.View, LoaderManager.LoaderCallbacks<List<Map<String, IndicatorTally>>> {

    private ViewGroup visualizationsViewGroup;
    private View childrenU5View;
    private View deceased_0_11_View;
    private View deceased_12_59_View;
    private static ReportContract.Presenter presenter;
    private List<Map<String, IndicatorTally>> indicatorTallies;

    public JobAidsDashboardFragment() {
        // Required empty public constructor
    }

    public static JobAidsDashboardFragment newInstance() {
        JobAidsDashboardFragment fragment = new JobAidsDashboardFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fetch Indicator data
        presenter = new JobAidsDashboardFragmentPresenter(this);
        getLoaderManager().initLoader(0, null, this).forceLoad();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_job_aids_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        visualizationsViewGroup = getView().findViewById(R.id.dashboard_content);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void buildVisualisations() {
        if (indicatorTallies == null || indicatorTallies.isEmpty()) {
            return;
        }
        // Aggregate values for display
        Map<String, IndicatorTally> childrenU5NumericMap = new HashMap<>();
        Map<String, IndicatorTally> deceased0_11_NumericMap = new HashMap<>();
        Map<String, IndicatorTally> deceased12_59_NumericMap = new HashMap<>();

        for (Map<String, IndicatorTally> indicatorTallyMap : indicatorTallies) {
            if (indicatorTallyMap.containsKey(DashboardUtil.countOfChildrenUnder5)) {
                updateTotalTally(indicatorTallyMap, childrenU5NumericMap, DashboardUtil.countOfChildrenUnder5);
            }
            if (indicatorTallyMap.containsKey(DashboardUtil.deceasedChildren0_11Months)) {
                updateTotalTally(deceased0_11_NumericMap, deceased0_11_NumericMap, DashboardUtil.deceasedChildren0_11Months);
            }
            if (indicatorTallyMap.containsKey(DashboardUtil.deceasedChildren12_59Months)) {
                updateTotalTally(deceased12_59_NumericMap, deceased0_11_NumericMap, DashboardUtil.deceasedChildren12_59Months);
            }
        }
        // Generate numeric indicator visualization
        NumericIndicatorVisualization numericIndicatorData;
        NumericDisplayFactory numericIndicatorFactory = new NumericDisplayFactory();

        numericIndicatorData = new NumericIndicatorVisualization(getResources().getString(R.string.total_under_5_children_label),
                childrenU5NumericMap.get(DashboardUtil.countOfChildrenUnder5).getCount());
        childrenU5View = numericIndicatorFactory.getIndicatorView(numericIndicatorData, getContext());

        numericIndicatorData = new NumericIndicatorVisualization(getResources().getString(R.string.deceased_children_0_11_months),
                deceased0_11_NumericMap.get(DashboardUtil.deceasedChildren0_11Months).getCount());
        deceased_0_11_View = numericIndicatorFactory.getIndicatorView(numericIndicatorData, getContext());


        numericIndicatorData = new NumericIndicatorVisualization(getResources().getString(R.string.deceased_children_12_59_months),
                deceased12_59_NumericMap.get(DashboardUtil.deceasedChildren12_59Months).getCount());
        deceased_12_59_View = numericIndicatorFactory.getIndicatorView(numericIndicatorData, getContext());

        visualizationsViewGroup.addView(childrenU5View);
        visualizationsViewGroup.addView(deceased_0_11_View);
        visualizationsViewGroup.addView(deceased_12_59_View);

    }

    private void updateTotalTally(Map<String, IndicatorTally> indicatorTallyMap, Map<String, IndicatorTally> currentIndicatorValueMap, String indicatorKey) {
        int count, currentValue;
        count = indicatorTallyMap.get(indicatorKey).getCount();
        if (currentIndicatorValueMap.get(indicatorKey) == null) {
            currentIndicatorValueMap.put(indicatorKey, new IndicatorTally(null, count, indicatorKey, null));
            return;
        }
        currentValue = currentIndicatorValueMap.get(indicatorKey).getCount();
        currentIndicatorValueMap.get(indicatorKey).setCount(count + currentValue);
    }

    @NonNull
    @Override
    public Loader<List<Map<String, IndicatorTally>>> onCreateLoader(int i, @Nullable Bundle bundle) {
        return new ReportIndicatorsLoader(getContext());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Map<String, IndicatorTally>>> loader, List<Map<String, IndicatorTally>> indicatorTallies) {
        this.indicatorTallies = indicatorTallies;
        refreshUI();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Map<String, IndicatorTally>>> loader) {

    }

    @Override
    public void refreshUI() {
        buildVisualisations();
    }

    private static class ReportIndicatorsLoader extends AsyncTaskLoader<List<Map<String, IndicatorTally>>> {

        public ReportIndicatorsLoader(Context context) {
            super(context);
        }

        @Nullable
        @Override
        public List<Map<String, IndicatorTally>> loadInBackground() {
            return presenter.fetchIndicatorsDailytallies();
        }
    }

}
