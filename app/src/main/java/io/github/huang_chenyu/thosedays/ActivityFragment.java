package io.github.huang_chenyu.thosedays;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import io.github.huang_chenyu.thosedays.events.DateChangedEvent;
import io.github.huang_chenyu.thosedays.events.StartDetailActivityEvent;


/**
 * A simple {@link Fragment} subclass.
 */
public class ActivityFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private HumanActivityListAdapter humanActivityListAdapter;
    private Database2 db;

    public ActivityFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_activity, container, false);

        db = new Database2();
        db.openDB(getActivity());

        recyclerView = rootView.findViewById(R.id.activity_list);

        //this setting to improve performance if you know that changes in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // Below is the right format for DB queries.
//        String date = "10/12/2017";
        getAndRenderListOfActivities();

//        List<HumanActivity> activities = db.queryByDate(date);
////        List<HumanActivity> activities = Algorithm.process(getContext());
//
//        humanActivityListAdapter = new HumanActivityListAdapter(getContext(), activities);
//
//        layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
//        recyclerView.setLayoutManager(layoutManager);
//        recyclerView.setAdapter(humanActivityListAdapter);
//
//
//        humanActivityListAdapter.setOnItemClickListener(new HumanActivityListAdapter.OnItemClickListener() {
//            @Override
//            public void onItemClick(View itemView, int position) {
//                HumanActivity activity = humanActivityListAdapter.getQueue().get(position);
//                EventBus.getDefault().post(new StartDetailActivityEvent(activity));
//            }
//        });

        return rootView;
    }

    private void getAndRenderListOfActivities() {
        //todo change date to a Date object, and set it to today
        String date = "2017-12-2";
        List<HumanActivity> activities = db.queryByDate(date);
//        List<HumanActivity> activities = Algorithm.process(getContext());

        humanActivityListAdapter = new HumanActivityListAdapter(getContext(), activities);

        layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(humanActivityListAdapter);

        humanActivityListAdapter.setOnItemClickListener(new HumanActivityListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                HumanActivity activity = humanActivityListAdapter.getQueue().get(position);
                EventBus.getDefault().post(new StartDetailActivityEvent(activity));
            }
        });
    }

    private void getAndRenderListOfActivities(String date) {
        List<HumanActivity> activities = db.queryByDate(date);
//        List<HumanActivity> activities = Algorithm.process(getContext());

        humanActivityListAdapter = new HumanActivityListAdapter(getContext(), activities);

        layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(humanActivityListAdapter);

        humanActivityListAdapter.setOnItemClickListener(new HumanActivityListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                HumanActivity activity = humanActivityListAdapter.getQueue().get(position);
                EventBus.getDefault().post(new StartDetailActivityEvent(activity));
            }
        });
    }

    @Subscribe
    public void onEvent(DateChangedEvent event){
        String date = event.date.getYear() + "-" + (event.date.getMonth() + 1) + "-" + event.date.getDate();
        getAndRenderListOfActivities(date);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        db.closeDB();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
