package com.votinginfoproject.VotingInformationProject.fragments.ballotFragment.ballotFragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.votinginfoproject.VotingInformationProject.R;
import com.votinginfoproject.VotingInformationProject.activities.voterInformationActivity.VoterInformationActivity;
import com.votinginfoproject.VotingInformationProject.activities.voterInformationActivity.VoterInformationView;
import com.votinginfoproject.VotingInformationProject.fragments.BaseFragment;
import com.votinginfoproject.VotingInformationProject.fragments.ballotFragment.BallotRecyclerViewDecorator;
import com.votinginfoproject.VotingInformationProject.fragments.bottomNavigationFragment.BottomNavigationFragment;
import com.votinginfoproject.VotingInformationProject.models.Contest;
import com.votinginfoproject.VotingInformationProject.models.Election;

import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link ContestListListener}
 * interface.
 */
public class ContestListFragment extends BaseFragment<ContestListPresenter> implements BottomNavigationFragment, ContestListView {
    private static final String TAG = ContestListFragment.class.getSimpleName();

    private static final String ARG_ELECTION = "arg_election";
    private static final String ARG_CONTESTS = "arg_contests";

    private ContestListRecyclerViewAdapter mAdapter;
    private ContestListListener mListener;
    private RecyclerView mRecyclerView;

    private View mNoContentContainer;

    public ContestListFragment() {
        //Required Empty Constructor
    }

    public static ContestListFragment newInstance(Election election, ArrayList<Contest> contests) {
        ContestListFragment fragment = new ContestListFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_ELECTION, election);
        args.putParcelableArrayList(ARG_CONTESTS, contests);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            Election election = getArguments().getParcelable(ARG_ELECTION);
            ArrayList<Contest> contests = getArguments().getParcelableArrayList(ARG_CONTESTS);

            ContestListPresenterImpl presenter = new ContestListPresenterImpl(this, election, contests);
            setPresenter(presenter);
            mAdapter = new ContestListRecyclerViewAdapter(presenter);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recycler_view, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.addItemDecoration(new BallotRecyclerViewDecorator(getActivity()));

        // Set the adapter
        Context context = view.getContext();

        mRecyclerView.setLayoutManager(new GridLayoutManager(context, 1));

        if (mAdapter != null) {
            mRecyclerView.setAdapter(mAdapter);
        }

        mNoContentContainer = view.findViewById(R.id.fragment_recycler_container_no_content);

        TextView emptyText = (TextView) mNoContentContainer.findViewById(R.id.fragment_recycler_text_view_no_content);
        emptyText.setText(R.string.contest_label_empty_list);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar mToolbar;
        if (view != null) {
            mToolbar = (Toolbar) view.findViewById(R.id.toolbar);

            if (mToolbar == null) {
                Log.e(TAG, "No toolbar found in class: " + getClass().getSimpleName());
            } else {
                mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                mToolbar.setTitle(R.string.bottom_navigation_title_ballot);
                mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getActivity() instanceof VoterInformationActivity) {
                            ((VoterInformationView) getActivity()).navigateBack();
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof ContestListListener) {
            mListener = (ContestListListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ContestListListener");
        }
    }

    /**
     * Implement deprecated protocol for older devices that will not call onAttach(Context)
     *
     * @param activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof ContestListListener) {
            mListener = (ContestListListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement ContestListListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mAdapter.setPresenter(null);
    }

    /**
     * Override getContext to return activity if context is not available
     * <p/>
     * This is a problem with older devices where getContext is not utilized
     *
     * @return
     */

    @Override
    public Context getContext() {
        try {
            return super.getContext();
        } catch (NoSuchMethodError error) {
            return getActivity();
        }
    }

    @Override
    public void onContestItemClicked(Contest contest) {
        mListener.contestClicked(contest);
    }

    @Override
    public void onReportErrorClicked() {
        mListener.reportErrorClicked();
    }

    @Override
    public void showNoContentView() {
        mNoContentContainer.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void resetView() {
        mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, 0);
    }

    public interface ContestListListener {
        void contestClicked(Contest contest);

        void reportErrorClicked();
    }
}
