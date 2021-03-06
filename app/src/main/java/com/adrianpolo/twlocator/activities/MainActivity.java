package com.adrianpolo.twlocator.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.adrianpolo.twlocator.R;
import com.adrianpolo.twlocator.model.City;
import com.adrianpolo.twlocator.model.Tweet;
import com.adrianpolo.twlocator.model.db.DBConstants;
import com.adrianpolo.twlocator.model.db.DBHelper;
import com.adrianpolo.twlocator.model.db.dao.CityDAO;
import com.adrianpolo.twlocator.model.db.dao.TweetDAO;
import com.adrianpolo.twlocator.util.NetworkHelper;
import com.adrianpolo.twlocator.util.location.GeoCoderHelper;
import com.adrianpolo.twlocator.util.location.LocationHelper;
import com.adrianpolo.twlocator.util.location.LocationHelper.LocationHelperListener;
import com.adrianpolo.twlocator.util.twitter.ConnectTwitterTask;
import com.adrianpolo.twlocator.util.twitter.TwitterHelper;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscriber;
import twitter4j.AccountSettings;
import twitter4j.AsyncTwitter;
import twitter4j.Category;
import twitter4j.DirectMessage;
import twitter4j.Friendship;
import twitter4j.GeoLocation;
import twitter4j.IDs;
import twitter4j.Location;
import twitter4j.OEmbed;
import twitter4j.PagableResponseList;
import twitter4j.Place;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.RateLimitStatus;
import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.SavedSearch;
import twitter4j.Status;
import twitter4j.Trends;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.TwitterException;
import twitter4j.TwitterListener;
import twitter4j.TwitterMethod;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.api.HelpResources;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuth2Token;
import twitter4j.auth.RequestToken;


public class MainActivity extends AppCompatActivity implements
        ConnectTwitterTask.OnConnectTwitterListener, LocationHelperListener, OnMapReadyCallback,
        SearchView.OnQueryTextListener {

    ConnectTwitterTask twitterTask;
    LocationHelper mLocationHelper;
    GeoCoderHelper mGeoCoderHelper;
    private int mMaxTweetsForLocation = 5;
    private GoogleMap mMap;
    private static final int URL_LOADER = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure Database
        DBHelper.configure(DBConstants.DB_NAME, getApplicationContext());

        ButterKnife.bind(this);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLocationHelper = new LocationHelper(this, this);
        mGeoCoderHelper = new GeoCoderHelper(getApplicationContext(), Locale.getDefault(), 5);
        if (NetworkHelper.isNetworkConnectionOK(new WeakReference<>(getApplication()))) {
            twitterTask = new ConnectTwitterTask(this);
            twitterTask.setListener(this);

            twitterTask.execute();
        } else {
            Toast.makeText(this, getString(R.string.error_network), Toast.LENGTH_LONG).show();

        }
    }


    private void launchTwitter(final City city, final double latitude, final double longitude, long idMaxId, final int importedElements) {
        final AsyncTwitter twitter = new TwitterHelper(this).getAsyncTwitter();
        twitter.addListener(new TwitterListener() {
            @Override
            public void gotMentions(ResponseList<Status> statuses) {

            }

            @Override
            public void gotHomeTimeline(ResponseList<Status> statuses) {

            }

            @Override
            public void gotUserTimeline(ResponseList<Status> statuses) {

            }

            @Override
            public void gotRetweetsOfMe(ResponseList<Status> statuses) {

            }

            @Override
            public void gotRetweets(ResponseList<Status> retweets) {

            }

            @Override
            public void gotShowStatus(Status status) {

            }

            @Override
            public void destroyedStatus(Status destroyedStatus) {

            }

            @Override
            public void updatedStatus(Status status) {

            }

            @Override
            public void retweetedStatus(Status retweetedStatus) {

            }

            @Override
            public void gotOEmbed(OEmbed oembed) {

            }

            @Override
            public void lookedup(ResponseList<Status> statuses) {

            }

            @Override
            public void searched(QueryResult queryResult) {
                long idMaxId = -1;
                int maxProccessed = importedElements;
                TweetDAO tweetDAO = new TweetDAO();
                for (Status tweet : queryResult.getTweets()) {
                    if (idMaxId == -1 || idMaxId > tweet.getId()) {
                        idMaxId = tweet.getId();
                    }
                    if (tweet.getGeoLocation() != null) {
                        if (tweetDAO.tweetForIdTwitter(tweet.getId()) == null) {
                            maxProccessed += 1;
                            Tweet mTweet = new Tweet(tweet.getUser().getName(),
                                    tweet.getText(), tweet.getUser().getProfileImageURL(),
                                    tweet.getGeoLocation().getLatitude(),
                                    tweet.getGeoLocation().getLongitude(), city.getId(), tweet.getId());
                            tweetDAO.insert(mTweet);
                        }
                    }
                }
                updateMap(city);
                if (maxProccessed < mMaxTweetsForLocation) {
                    launchTwitter(city, latitude, longitude, idMaxId, maxProccessed);
                }
            }

            @Override
            public void gotDirectMessages(ResponseList<DirectMessage> messages) {

            }

            @Override
            public void gotSentDirectMessages(ResponseList<DirectMessage> messages) {

            }

            @Override
            public void gotDirectMessage(DirectMessage message) {

            }

            @Override
            public void destroyedDirectMessage(DirectMessage message) {

            }

            @Override
            public void sentDirectMessage(DirectMessage message) {

            }

            @Override
            public void gotFriendsIDs(IDs ids) {

            }

            @Override
            public void gotFollowersIDs(IDs ids) {

            }

            @Override
            public void lookedUpFriendships(ResponseList<Friendship> friendships) {

            }

            @Override
            public void gotIncomingFriendships(IDs ids) {

            }

            @Override
            public void gotOutgoingFriendships(IDs ids) {

            }

            @Override
            public void createdFriendship(User user) {

            }

            @Override
            public void destroyedFriendship(User user) {

            }

            @Override
            public void updatedFriendship(Relationship relationship) {

            }

            @Override
            public void gotShowFriendship(Relationship relationship) {

            }

            @Override
            public void gotFriendsList(PagableResponseList<User> users) {

            }

            @Override
            public void gotFollowersList(PagableResponseList<User> users) {

            }

            @Override
            public void gotAccountSettings(AccountSettings settings) {

            }

            @Override
            public void verifiedCredentials(User user) {

            }

            @Override
            public void updatedAccountSettings(AccountSettings settings) {

            }

            @Override
            public void updatedProfile(User user) {

            }

            @Override
            public void updatedProfileBackgroundImage(User user) {

            }

            @Override
            public void updatedProfileColors(User user) {

            }

            @Override
            public void updatedProfileImage(User user) {

            }

            @Override
            public void gotBlocksList(ResponseList<User> blockingUsers) {

            }

            @Override
            public void gotBlockIDs(IDs blockingUsersIDs) {

            }

            @Override
            public void createdBlock(User user) {

            }

            @Override
            public void destroyedBlock(User user) {

            }

            @Override
            public void lookedupUsers(ResponseList<User> users) {

            }

            @Override
            public void gotUserDetail(User user) {

            }

            @Override
            public void searchedUser(ResponseList<User> userList) {

            }

            @Override
            public void gotContributees(ResponseList<User> users) {

            }

            @Override
            public void gotContributors(ResponseList<User> users) {

            }

            @Override
            public void removedProfileBanner() {

            }

            @Override
            public void updatedProfileBanner() {

            }

            @Override
            public void gotMutesList(ResponseList<User> blockingUsers) {

            }

            @Override
            public void gotMuteIDs(IDs blockingUsersIDs) {

            }

            @Override
            public void createdMute(User user) {

            }

            @Override
            public void destroyedMute(User user) {

            }

            @Override
            public void gotUserSuggestions(ResponseList<User> users) {

            }

            @Override
            public void gotSuggestedUserCategories(ResponseList<Category> category) {

            }

            @Override
            public void gotMemberSuggestions(ResponseList<User> users) {

            }

            @Override
            public void gotFavorites(ResponseList<Status> statuses) {

            }

            @Override
            public void createdFavorite(Status status) {

            }

            @Override
            public void destroyedFavorite(Status status) {

            }

            @Override
            public void gotUserLists(ResponseList<UserList> userLists) {

            }

            @Override
            public void gotUserListStatuses(ResponseList<Status> statuses) {

            }

            @Override
            public void destroyedUserListMember(UserList userList) {

            }

            @Override
            public void gotUserListMemberships(PagableResponseList<UserList> userLists) {

            }

            @Override
            public void gotUserListSubscribers(PagableResponseList<User> users) {

            }

            @Override
            public void subscribedUserList(UserList userList) {

            }

            @Override
            public void checkedUserListSubscription(User user) {

            }

            @Override
            public void unsubscribedUserList(UserList userList) {

            }

            @Override
            public void createdUserListMembers(UserList userList) {

            }

            @Override
            public void checkedUserListMembership(User users) {

            }

            @Override
            public void createdUserListMember(UserList userList) {

            }

            @Override
            public void destroyedUserList(UserList userList) {

            }

            @Override
            public void updatedUserList(UserList userList) {

            }

            @Override
            public void createdUserList(UserList userList) {

            }

            @Override
            public void gotShowUserList(UserList userList) {

            }

            @Override
            public void gotUserListSubscriptions(PagableResponseList<UserList> userLists) {

            }

            @Override
            public void gotUserListMembers(PagableResponseList<User> users) {

            }

            @Override
            public void gotSavedSearches(ResponseList<SavedSearch> savedSearches) {

            }

            @Override
            public void gotSavedSearch(SavedSearch savedSearch) {

            }

            @Override
            public void createdSavedSearch(SavedSearch savedSearch) {

            }

            @Override
            public void destroyedSavedSearch(SavedSearch savedSearch) {

            }

            @Override
            public void gotGeoDetails(Place place) {

            }

            @Override
            public void gotReverseGeoCode(ResponseList<Place> places) {

            }

            @Override
            public void searchedPlaces(ResponseList<Place> places) {

            }

            @Override
            public void gotSimilarPlaces(ResponseList<Place> places) {

            }

            @Override
            public void gotPlaceTrends(Trends trends) {

            }

            @Override
            public void gotAvailableTrends(ResponseList<Location> locations) {

            }

            @Override
            public void gotClosestTrends(ResponseList<Location> locations) {

            }

            @Override
            public void reportedSpam(User reportedSpammer) {

            }

            @Override
            public void gotOAuthRequestToken(RequestToken token) {

            }

            @Override
            public void gotOAuthAccessToken(AccessToken token) {

            }

            @Override
            public void gotOAuth2Token(OAuth2Token token) {

            }

            @Override
            public void gotAPIConfiguration(TwitterAPIConfiguration conf) {

            }

            @Override
            public void gotLanguages(ResponseList<HelpResources.Language> languages) {

            }

            @Override
            public void gotPrivacyPolicy(String privacyPolicy) {

            }

            @Override
            public void gotTermsOfService(String tof) {

            }

            @Override
            public void gotRateLimitStatus(Map<String, RateLimitStatus> rateLimitStatus) {

            }

            @Override
            public void onException(TwitterException te, TwitterMethod method) {

            }
        });

        Query query = new Query().geoCode(new GeoLocation(latitude, longitude), 10, Query.KILOMETERS.toString());
        query.count(100);
        if (idMaxId != -1) {
            Log.d("Twitter Query", "Executing query with MaxId" + idMaxId);
            query.setMaxId(idMaxId);
        }
        twitter.search(query);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSearchRequested() {
        Bundle appData = new Bundle();
        startSearch(null, false, appData, false);
        return true;
    }

    @Override
    protected void onStop() {
        mLocationHelper.disconnect();
        super.onStop();

    }

    @Override
    protected void onStart() {
        mLocationHelper.connect();
        super.onStart();
    }

    @Override
    public void twitterConnectionFinished() {
        Snackbar.make(findViewById(android.R.id.content), getString(R.string.twitter_auth_ok),
                Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onLocation(double longitude, double latitude) {
        Log.d("OnLocation", "Longitude: " + longitude + " Latitude " + latitude);
        if (mMap != null) {
            LatLng latLng = new LatLng(latitude, longitude);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
            mMap.animateCamera(cameraUpdate);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
            @Override
            public void onInfoWindowClose(Marker marker) {
                marker.remove();
            }
        });
        getObservableMaps().debounce(3, TimeUnit.SECONDS).subscribe(new Subscriber<LatLng>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(LatLng latLng) {
                Log.d("Camera Changed", "Longitude:" + latLng.longitude +
                        " Latitude:" + latLng.latitude);
                if (NetworkHelper.isNetworkConnectionOK(new WeakReference<>(getApplication()))) {
                    String data = mGeoCoderHelper.getInfoForPosition(latLng);
                    if (data != null && !data.isEmpty() ) {
                        CityDAO cityDAO = new CityDAO();
                        City city = cityDAO.findByName(data.trim().toUpperCase());
                        if (city == null) {

                            city = new City(data.trim().toUpperCase(), latLng.latitude, latLng.longitude);
                            cityDAO.insert(city);
                        }else{
                            updateMap(city);
                        }

                        launchTwitter(city, latLng.latitude, latLng.longitude, -1, 0);
                    }
                }
            }
        });
    }

    public void updateMap(final City city) {
        TweetDAO tweetDAO = new TweetDAO();
        for (final Tweet tweet : tweetDAO.tweetsForCity(city.getId())) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    final BitmapDescriptor bitmapDefault = BitmapDescriptorFactory.fromResource(R.drawable.hue);
                    final LatLng latitem = new LatLng(tweet.getLatitude(), tweet.getLongitude());

                    try {
                        URL url;
                        if (NetworkHelper.isNetworkConnectionOK(new WeakReference<>(getApplication()))) {
                            url = new URL(tweet.getUrlImage());
                            final Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                            final BitmapDescriptor bitmapUrl = BitmapDescriptorFactory.fromBitmap(bmp);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mMap.addMarker(new MarkerOptions()
                                            .title(tweet.getText())
                                            .snippet(tweet.getAuthor())
                                            .position(latitem)
                                            .icon(bitmapUrl)
                                    );
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mMap.addMarker(new MarkerOptions()
                                            .title(tweet.getText())
                                            .snippet(tweet.getAuthor())
                                            .position(latitem)
                                            .icon(bitmapDefault)
                                    );
                                }
                            });
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }

    public Observable<LatLng> getObservableMaps() {
        return Observable.create(new Observable.OnSubscribe<LatLng>() {
            @Override
            public void call(final Subscriber<? super LatLng> subscriber) {
                try {
                    if (!subscriber.isUnsubscribed()) {
                        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                            @Override
                            public void onCameraChange(CameraPosition cameraPosition) {
                                subscriber.onNext(cameraPosition.target);
                            }
                        });
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }


    public void moveToCity(final String nameCity) {
        mMap.clear();

        City city = new CityDAO().findByName(nameCity.trim().toUpperCase());
        if (city == null) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    final LatLng latLng = mGeoCoderHelper.getLatLngFromName(nameCity);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (latLng != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
                            } else {
                                Snackbar.make(findViewById(android.R.id.content), getString(R.string.city_not_found),
                                        Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            });
            thread.start();
        } else {
            LatLng latlng = new LatLng(city.getLatitude(), city.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 10));
            updateMap(city);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        moveToCity(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }
}

