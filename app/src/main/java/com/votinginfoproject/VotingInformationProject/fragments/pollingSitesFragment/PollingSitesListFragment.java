package com.votinginfoproject.VotingInformationProject.fragments.pollingSitesFragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.votinginfoproject.VotingInformationProject.R;
import com.votinginfoproject.VotingInformationProject.activities.voterInformationActivity.VoterInformationActivity;
import com.votinginfoproject.VotingInformationProject.activities.voterInformationActivity.VoterInformationView;
import com.votinginfoproject.VotingInformationProject.fragments.BaseFragment;
import com.votinginfoproject.VotingInformationProject.fragments.bottomNavigationFragment.BottomNavigationFragment;
import com.votinginfoproject.VotingInformationProject.models.PollingLocation;
import com.votinginfoproject.VotingInformationProject.views.viewHolders.DividerItemDecoration;

import java.util.ArrayList;

public class PollingSitesListFragment extends BaseFragment<PollingSitesPresenter> implements BottomNavigationFragment, Toolbar.OnMenuItemClickListener, PollingSitesView {

    private static final String TAG = PollingSitesListFragment.class.getSimpleName();

    private static final String ARG_CURRENT_SORT = "current_sort";

    private PollingSitesListener mListener;

    private PollingSiteItemRecyclerViewAdapter mAdapter;

    private RecyclerView mRecyclerView;

    private View mNoContentContainer;

    private Toolbar mToolbar;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PollingSitesListFragment() {
    }

    public static PollingSitesListFragment newInstance() {
        return new PollingSitesListFragment();
    }

    public static PollingSitesListFragment newInstance(@LayoutRes int currentSort) {
        PollingSitesListFragment fragment = new PollingSitesListFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_CURRENT_SORT, currentSort);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getArguments() != null) {
            setPresenter(new PollingSitesPresenterImpl(this, getArguments().getInt(ARG_CURRENT_SORT, R.id.sort_all)));
        } else {
            setPresenter(new PollingSitesPresenterImpl(this));
        }

        View view = inflater.inflate(R.layout.fragment_recycler_view, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        mNoContentContainer = view.findViewById(R.id.fragment_recycler_container_no_content);

        TextView emptyText = (TextView) mNoContentContainer.findViewById(R.id.fragment_recycler_text_view_no_content);
        emptyText.setText(R.string.locations_label_empty_list);

        Context context = view.getContext();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));

        mAdapter = new PollingSiteItemRecyclerViewAdapter(context, getPresenter().getElection(), getPresenter().getSortedLocations(), mListener);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.invalidate();

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof PollingSitesListener) {
            mListener = (PollingSitesListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof PollingSitesListener) {
            mListener = (PollingSitesListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (view != null) {
            mToolbar = (Toolbar) view.findViewById(R.id.toolbar);

            if (mToolbar == null) {
                Log.e(TAG, "No toolbar found in class: " + getClass().getSimpleName());
            } else {
                mToolbar.inflateMenu(R.menu.polling_sites_list);

                mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                mToolbar.setTitle(R.string.bottom_navigation_title_polls);
                mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getActivity() instanceof VoterInformationActivity) {
                            ((VoterInformationView) getActivity()).navigateBack();
                        }
                    }
                });

                mToolbar.setOnMenuItemClickListener(this);

                if (!getPresenter().hasPollingLocations()) {
                    mToolbar.getMenu().removeItem(R.id.sort_polling_locations);
                }

                if (!getPresenter().hasEarlyVotingLocations()) {
                    mToolbar.getMenu().removeItem(R.id.sort_early_vote);
                }

                if (!getPresenter().hasDropBoxLocations()) {
                    mToolbar.getMenu().removeItem(R.id.sort_drop_boxes);
                }

                //If there aren't any polling locations
                if (!getPresenter().hasPollingLocations() &&
                        !getPresenter().hasEarlyVotingLocations() &&
                        !getPresenter().hasDropBoxLocations()) {
                    mToolbar.getMenu().removeGroup(R.id.filter_polling_sites);
                } else {
                    mToolbar.getMenu().findItem(getPresenter().getCurrentSort()).setChecked(true);
                }
            }
        }
    }

    @Override
    public void resetView() {
        mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, 0);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        getPresenter().menuItemClicked(item.getItemId());
        item.setChecked(true);

        return true;
    }

    @Override
    public void navigateToDirections(PollingLocation pollingLocation) {
        mListener.navigateToDirections(pollingLocation);
    }

    @Override
    public void navigateToMap(@LayoutRes int currentSort) {
        mListener.mapButtonClicked(currentSort);
    }

    @Override
    public void navigateToList(@LayoutRes int currentSort) {
        //Not implemented
    }

    @Override
    public void updateList(ArrayList<PollingLocation> locations) {
        mAdapter.updatePollingLocations(locations);
        mRecyclerView.invalidate();
        mRecyclerView.invalidateItemDecorations();
    }

    @Override
    public void showLocationCard(PollingLocation location) {
        //Not Implemented
    }

    @Override
    public void showNoContentView() {
        mNoContentContainer.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
        mToolbar.getMenu().removeItem(R.id.map_view);
    }

    /**
     * Recycler View Interaction methods
     * <p/>
     * Must be implemented by the Activity
     */
    public interface PollingSitesListener {
        void mapButtonClicked(@LayoutRes int currentSort);

        void listButtonClicked(@LayoutRes int currentSort);

        void navigateToDirections(PollingLocation location);

        void reportErrorClicked();

        void startPollingLocation();
    }
}
