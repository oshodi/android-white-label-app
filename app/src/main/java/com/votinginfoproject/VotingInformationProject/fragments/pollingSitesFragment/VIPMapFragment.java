package com.votinginfoproject.VotingInformationProject.fragments.pollingSitesFragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.votinginfoproject.VotingInformationProject.R;
import com.votinginfoproject.VotingInformationProject.activities.voterInformationActivity.VoterInformationActivity;
import com.votinginfoproject.VotingInformationProject.activities.voterInformationActivity.VoterInformationView;
import com.votinginfoproject.VotingInformationProject.adapters.LocationInfoWindow;
import com.votinginfoproject.VotingInformationProject.fragments.bottomNavigationFragment.BottomNavigationFragment;
import com.votinginfoproject.VotingInformationProject.models.CivicApiAddress;
import com.votinginfoproject.VotingInformationProject.models.ElectionAdministrationBody;
import com.votinginfoproject.VotingInformationProject.models.FilterLabels;
import com.votinginfoproject.VotingInformationProject.models.PollingLocation;
import com.votinginfoproject.VotingInformationProject.models.VoterInfo;
import com.votinginfoproject.VotingInformationProject.models.singletons.UserPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VIPMapFragment extends MapFragment implements AdapterView.OnItemSelectedListener, Toolbar.OnMenuItemClickListener, PollingSitesView, BottomNavigationFragment {
    private static final String LOCATION_ID = "location_id";
    private static final String POLYLINE = "polyline";
    private static final String HOME = "home";
    private static final String ARG_CURRENT_SORT = "current_sort";
    private static final String CURRENT_LOCATION = "current";
    private final String TAG = VIPMapFragment.class.getSimpleName();
    VoterInfo voterInfo;
    View mapView;
    RelativeLayout rootView;
    //    LayoutInflater layoutInflater;
    ArrayList<PollingLocation> allLocations;
    GoogleMap map;
    String locationId;
    PollingLocation selectedLocation;
    LatLng thisLocation;
    LatLng homeLocation;
    LatLng currentLocation;
    String homeAddress;
    String currentAddress;
    String encodedPolyline;
    LatLngBounds polylineBounds;
    boolean haveElectionAdminBody;
    HashMap<String, MarkerOptions> markers;
    // track the internally-assigned ID for each marker and map it to the location's key
    HashMap<String, String> markerIds;
    // track which location filter was last selected, and only refresh list if it changed
    long lastSelectedFilterItem = 0; // default to all items, which is first in list
    FilterLabels filterLabels = null;
    boolean showPolling = true;
    boolean showEarly = true;
    boolean showDropBox = true;

    private PollingSitesListFragment.PollingSiteOnClickListener mListener;
    private PollingSitesPresenter mPresenter;

    private Toolbar mToolbar;

    public VIPMapFragment() {
        super();
    }

    /**
     * Default newInstance Method.
     *
     * @param context
     * @param tag
     * @param polyline
     * @return
     */
    public static VIPMapFragment newInstance(Context context, String tag, String polyline) {
        // instantiate with map options
        GoogleMapOptions options = new GoogleMapOptions();
        VIPMapFragment fragment = VIPMapFragment.newInstance(context, options);

        Bundle args = new Bundle();
        args.putString(LOCATION_ID, tag);
        args.putString(POLYLINE, polyline);
        fragment.setArguments(args);

        return fragment;
    }

    public static VIPMapFragment newInstance(Context context, String tag, @LayoutRes int currentSort) {
        // instantiate with map options
        GoogleMapOptions options = new GoogleMapOptions();
        VIPMapFragment fragment = VIPMapFragment.newInstance(context, options);

        Bundle args = new Bundle();
        args.putString(LOCATION_ID, tag);
        args.putInt(ARG_CURRENT_SORT, currentSort);

        fragment.setArguments(args);

        return fragment;
    }

    public static VIPMapFragment newInstance(Context context, GoogleMapOptions options) {
        Bundle args = new Bundle();
        // need to send API key to initialize map
        args.putParcelable(context.getString(R.string.google_api_android_key), options);

        VIPMapFragment fragment = new VIPMapFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (id == lastSelectedFilterItem) {
            return;
        }

        lastSelectedFilterItem = id;

        String selection = (String) parent.getItemAtPosition(position);
        if (selection == filterLabels.ALL) {
            showEarly = showPolling = showDropBox = true;
        } else if (selection.equals(filterLabels.EARLY)) {
            showEarly = true;
            showPolling = showDropBox = false;
        } else if (selection.equals(filterLabels.POLLING)) {
            showPolling = true;
            showEarly = showDropBox = false;
        } else if (selection.equals(filterLabels.DROP_BOX)) {
            showDropBox = true;
            showEarly = showPolling = false;
        } else {
            Log.e(TAG, "Selected item " + selection + "isn't recognized!");
            showEarly = showPolling = showDropBox = true;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof PollingSitesListFragment.PollingSiteOnClickListener) {
            mListener = (PollingSitesListFragment.PollingSiteOnClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // required method implementation
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // programmatically add map view, so filter drop-down appears on top
        mapView = super.onCreateView(inflater, container, savedInstanceState);
        rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_map, container, false);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.BELOW, R.id.toolbar);
        rootView.addView(mapView, layoutParams);

        @LayoutRes int selectedSort = R.id.sort_all;
        if (getArguments() != null) {
            selectedSort = getArguments().getInt(ARG_CURRENT_SORT);
        }

        mPresenter = new PollingSitesPresenterImpl(this, selectedSort);

        voterInfo = UserPreferences.getVoterInfo();
        allLocations = voterInfo.getAllLocations();
//        homeLocation = mActivity.getHomeLatLng();
//        currentLocation = mActivity.getUserLocation();
//        currentAddress = mActivity.getUserLocationAddress();
        homeAddress = voterInfo.normalizedInput.toGeocodeString();

//        polylineBounds = mActivity.getPolylineBounds();

        // check if this map view is for an election administration body
        if (locationId.equals(ElectionAdministrationBody.AdminBody.STATE) ||
                locationId.equals(ElectionAdministrationBody.AdminBody.LOCAL)) {
            haveElectionAdminBody = true;
        } else {
            haveElectionAdminBody = false;
        }

        // set selected location to zoom to
        if (locationId.equals(HOME)) {
            thisLocation = homeLocation;
        } else if (haveElectionAdminBody) {
            thisLocation = voterInfo.getAdminBodyLatLng(locationId);
        } else {
            Log.d(TAG, "Have location ID: " + locationId);
            selectedLocation = voterInfo.getLocationForId(locationId);
            CivicApiAddress address = selectedLocation.address;
            thisLocation = new LatLng(address.latitude, address.longitude);
        }

        // check if already instantiated
        if (map == null) {
            map = getMap();
            map.setMyLocationEnabled(true);
            map.setInfoWindowAdapter(new LocationInfoWindow(inflater));

            // start asynchronous task to add markers to map
            new AddMarkersTask().execute(locationId);

            // wait for map layout to occur before zooming to location
            final ViewTreeObserver observer = mapView.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (observer.isAlive()) {

                        // deal with SDK compatibility
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            //noinspection deprecation
                            observer.removeGlobalOnLayoutListener(this);
                        } else {
                            observer.removeOnGlobalLayoutListener(this);
                        }
                    }

                    addNonPollingToMap();

                    if (polylineBounds != null) {
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(polylineBounds, 60));
                    } else if (thisLocation != null) {
                        // zoom to selected location
                        if (thisLocation == homeLocation) {
                            // zoom out further when viewing general map centered on home
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(thisLocation, 8));
                        } else {
                            // zom to specific polling location or other point of interest
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(thisLocation, 15));
                        }
                    }
                }
            });
        } else {
            map.clear();
        }

        // get labels for dropdown
//        filterLabels = mActivity.getFilterLabels();

        // build filter dropdown list, and initialize with all locations
//        ArrayList<String> filterOptions = new ArrayList<>(4);
//        // always show 'all sites' option
//        filterOptions.add(filterLabels.ALL);
//
//        // show the other three options if there are any
//        if (!voterInfo.getOpenEarlyVoteSites().isEmpty()) {
//            filterOptions.add(filterLabels.EARLY);
//        }
//
//        if (!voterInfo.getPollingLocations().isEmpty()) {
//            filterOptions.add(filterLabels.POLLING);
//        }
//
//        if (!voterInfo.getOpenDropOffLocations().isEmpty()) {
//            filterOptions.add(filterLabels.DROP_BOX);
//        }

//        Spinner filterSpinner = (Spinner) rootView.findViewById(R.id.locations_map_spinner);
//        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(mActivity,
//                R.layout.location_spinner_item, filterOptions);
//        spinnerAdapter.setDropDownViewResource(R.layout.locations_spinner_view);
//        filterSpinner.setAdapter(spinnerAdapter);
//        filterSpinner.setOnItemSelectedListener(this);
//        filterSpinner.setSelection(0); // all locations by default

        // set click handler for info window (to go to directions list)
        // info window is just a bitmap, so can't listen for clicks on elements within it.
        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                // get location key for this marker's ID
                String key = markerIds.get(marker.getId());

                // do nothing for taps on user address or current location info window
                if (key.equals(HOME) || key.equals((CURRENT_LOCATION))) {
                    return;
                }

                Log.d(TAG, "Clicked marker for " + key);
//                mActivity.showDirections(key);
            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (view != null) {
            mToolbar = (Toolbar) view.findViewById(R.id.toolbar);

            if (mToolbar == null) {
                Log.e(TAG, "No toolbar found in class: " + getClass().getSimpleName());
            } else {
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
                mToolbar.inflateMenu(R.menu.polling_sites_map);
                mToolbar.getMenu().findItem(mPresenter.getCurrentSort()).setChecked(true);

                if (!mPresenter.hasPollingLocations()) {
                    mToolbar.getMenu().removeItem(R.id.sort_polling_locations);
                }

                if (!mPresenter.hasEarlyVotingLocations()) {
                    mToolbar.getMenu().removeItem(R.id.sort_early_vote);
                }

                if (!mPresenter.hasDropBoxLocations()) {
                    mToolbar.getMenu().removeItem(R.id.sort_drop_boxes);
                }
            }
        }
    }

    /**
     * Helper function to add everything that isn't a polling site to the map
     */
    private void addNonPollingToMap() {
        // add marker for user-entered address
        if (homeLocation != null) {
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(homeLocation)
                    .title(getContext().getString(R.string.locations_map_label_user_address))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_home_map))
            );

            markerIds.put(marker.getId(), HOME);
        }

        if (currentLocation != null) {
            // add marker for current user location (used for directions)
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .title(getContext().getString(R.string.locations_map_label_user_location))
                    .snippet(currentAddress)
                    .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_mylocation))
            );

            markerIds.put(marker.getId(), CURRENT_LOCATION);
        }

        if (haveElectionAdminBody) {
            // add marker for state or local election administration body
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(thisLocation)
                    .title(getContext().getString(R.string.locations_map_label_election_administration_body))
                    .snippet(voterInfo.getAdminAddress(locationId).toString())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_leg_body_map))
            );

            marker.showInfoWindow();
            // allow for getting directions from election admin body location
            markerIds.put(marker.getId(), locationId);
        }

        if (encodedPolyline != null && !encodedPolyline.isEmpty()) {
            // show directions line on map
            PolylineOptions polylineOptions = new PolylineOptions();
            List<LatLng> pts = PolyUtil.decode(encodedPolyline);
            polylineOptions.addAll(pts);
            polylineOptions.color(getContext().getResources().getColor(R.color.brand));
            map.addPolyline(polylineOptions);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            locationId = args.getString(LOCATION_ID);
            encodedPolyline = args.getString(POLYLINE);
        }
    }

    private MarkerOptions createMarkerOptions(PollingLocation location, float color) {
        String showTitle = location.name;

        if (showTitle == null || showTitle.isEmpty()) {
            showTitle = location.address.locationName;
        }

        StringBuilder showSnippet = new StringBuilder(location.address.toGeocodeString());

        // show date range for when early vote sites are open
        if (location.startDate != null && !location.startDate.isEmpty()
                && location.endDate != null && !location.endDate.isEmpty()) {

            showSnippet.append("\n\n");
            showSnippet.append(location.startDate);
            showSnippet.append(" - ");
            showSnippet.append(location.endDate);
        }

        if (location.pollingHours != null && !location.pollingHours.isEmpty()) {
            showSnippet.append("\n\n");
            showSnippet.append(getContext().getString(R.string.locations_map_label_polling_location_hours));
            showSnippet.append("\n");
            showSnippet.append(location.pollingHours);
        } else {
            // display placeholder for no hours
            showSnippet.append("\n\n");
            showSnippet.append(getContext().getString(R.string.locations_map_error_polling_location_hours_not_found));
        }

        return new MarkerOptions()
                .position(new LatLng(location.address.latitude, location.address.longitude))
                .title(showTitle)
                .snippet(showSnippet.toString())
                .icon(BitmapDescriptorFactory.defaultMarker(color));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        mPresenter.menuItemClicked(item.getItemId());
        item.setChecked(true);

        return false;
    }

    @Override
    public void navigateToDirections(PollingLocation pollingLocation) {

    }

    @Override
    public void navigateToErrorForm() {

    }

    @Override
    public void navigateToMap(@LayoutRes int currentSort) {
        //Not implemented
    }

    @Override
    public void navigateToList(@LayoutRes int currentSort) {
        mListener.listButtonClicked(currentSort);
    }

    @Override
    public void updateList(ArrayList<PollingLocation> locations) {

    }

    @Override
    public void resetView() {

    }

    private class AddMarkersTask extends AsyncTask<String, Integer, String> {

        /**
         * Helper function to add collection of polling locations to map.
         *
         * @param locations        list of PollingLocations to add
         * @param bitmapDescriptor BitmapDescriptorFactory value for marker color
         */
        private void addLocationsToMap(List<PollingLocation> locations, float bitmapDescriptor) {
            for (PollingLocation location : locations) {
                if (location.address.latitude == 0) {
                    Log.d(TAG, "Skipping adding to map location " + location.name);
                    continue;
                }

                markers.put(location.address.toGeocodeString(), createMarkerOptions(location, bitmapDescriptor));
            }
        }

        @Override
        protected String doInBackground(String... select_locations) {
            markers = new HashMap<>(allLocations.size());
            markerIds = new HashMap<>(allLocations.size());

            // use red markers for early voting sites
            if (showEarly) {
                addLocationsToMap(voterInfo.getOpenEarlyVoteSites(), BitmapDescriptorFactory.HUE_RED);
            }

            // use blue markers for polling locations
            if (!voterInfo.getPollingLocations().isEmpty() && showPolling) {
                addLocationsToMap(voterInfo.getPollingLocations(), BitmapDescriptorFactory.HUE_AZURE);
            }

            // use green markers for drop boxes
            if (showDropBox) {
                addLocationsToMap(voterInfo.getOpenDropOffLocations(), BitmapDescriptorFactory.HUE_GREEN);
            }

            return locationId;
        }

        @Override
        protected void onPostExecute(String checkId) {
            for (String key : markers.keySet()) {
                Marker marker = map.addMarker(markers.get(key));
                markerIds.put(marker.getId(), key);

                if (key.equals(locationId)) {
                    // show popup for marker at selected location
                    marker.showInfoWindow();
                }
            }
        }
    }
}