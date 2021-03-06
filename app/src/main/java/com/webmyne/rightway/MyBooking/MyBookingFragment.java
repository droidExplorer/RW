package com.webmyne.rightway.MyBooking;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.webmyne.rightway.Bookings.Trip;
import com.webmyne.rightway.CustomComponents.CallWebService;
import com.webmyne.rightway.CustomComponents.CircleDialog;
import com.webmyne.rightway.CustomComponents.ComplexPreferences;
import com.webmyne.rightway.Registration.Customer;
import com.webmyne.rightway.Model.AppConstants;
import com.webmyne.rightway.CustomComponents.PagerSlidingTabStrip;
import com.webmyne.rightway.Model.SharedPreferenceNotification;
import com.webmyne.rightway.Model.SharedPreferenceTrips;
import com.webmyne.rightway.MyNotifications.CustomerNotification;
import com.webmyne.rightway.R;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class MyBookingFragment extends Fragment {
    private SharedPreferenceTrips sharedPreferenceTrips;
    private SharedPreferenceNotification sharedPreferenceNotification;
    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private int badgeValue=0;
    private MyPagerAdapter adapter;
    public ArrayList<Trip> tripArrayList=new ArrayList<Trip>();
//    private ProgressDialog progressDialog;
    private CircleDialog circleDialog;
    private Customer customerDetails;
    private ArrayList<CustomerNotification> notificationList;

    public static MyBookingFragment newInstance(String param1, String param2) {
        MyBookingFragment fragment = new MyBookingFragment();
        return fragment;
    }

    public MyBookingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View convertView= inflater.inflate(R.layout.fragment_my_booking, container, false);
        tabs = (PagerSlidingTabStrip)convertView.findViewById(R.id.my_order_tabs);
        pager = (ViewPager) convertView.findViewById(R.id.pager);

        return convertView;
    }

    public  boolean isConnected() {

        ConnectivityManager cm =(ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return  isConnected;
    }


    @Override
    public void onResume() {
        super.onResume();
        if(isConnected()==true) {
            getTripList();
        } else {
            Toast.makeText(getActivity(), "Internet Connection Unavailable", Toast.LENGTH_SHORT).show();
        }
        sharedPreferenceTrips=new SharedPreferenceTrips();
        sharedPreferenceNotification=new SharedPreferenceNotification();
    }

    public void getTripList() {

        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(getActivity(), "customer_data", 0);
        customerDetails =complexPreferences.getObject("customer_data", Customer.class);
        circleDialog=new CircleDialog(getActivity(),0);
        circleDialog.setCancelable(true);
        circleDialog.show();
//        Log.e("Customer trip url: ",AppConstants.getTripList+customerDetails.CustomerID+"");
        new CallWebService(AppConstants.getTripList+customerDetails.CustomerID , CallWebService.TYPE_JSONARRAY) {

            @Override
            public void response(String response) {

                Type listType=new TypeToken<List<Trip>>(){
                }.getType();
                tripArrayList = new GsonBuilder().create().fromJson(response, listType);

                sharedPreferenceTrips.clearTrip(getActivity());
                for(int i=0;i<tripArrayList.size();i++){
                    sharedPreferenceTrips.saveTrip(getActivity(),tripArrayList.get(i));

                }

                adapter = new MyPagerAdapter(getActivity().getSupportFragmentManager());
                pager.setAdapter(adapter);
                final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
                pager.setPageMargin(pageMargin);
                tabs.setViewPager(pager);
                getNotificationList();

            }

            @Override
            public void error(VolleyError error) {
                Log.e("error: ",error+"");

            }
        }.start();
    }

    public void getNotificationList() {
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(getActivity(), "customer_data", 0);
        customerDetails =complexPreferences.getObject("customer_data", Customer.class);

        new CallWebService(AppConstants.GetCustomerNotifications+customerDetails.CustomerID , CallWebService.TYPE_JSONARRAY) {

            @Override
            public void response(String response) {

                Type listType=new TypeToken<List<CustomerNotification>>(){
                }.getType();

                notificationList = new GsonBuilder().create().fromJson(response, listType);
//                Log.e("notification list size:",notificationList.size()+"");

                if(notificationList != null) {
                    sharedPreferenceNotification.clearNotification(getActivity());
                    for (int i = 0; i < notificationList.size(); i++) {
                        if(notificationList.get(i).Status.equalsIgnoreCase("false")){
                            badgeValue=badgeValue+1;
                        }
                        sharedPreferenceNotification.saveNotification(getActivity(), notificationList.get(i));
                    }
                }

//                Log.e("Badge value: ",badgeValue+"");
                SharedPreferences preferencesTimeInterval = getActivity().getSharedPreferences("badge_value",getActivity().MODE_PRIVATE);
                SharedPreferences.Editor editor=preferencesTimeInterval.edit();
                editor.putString("badge_value",badgeValue+"");
                editor.commit();

                circleDialog.dismiss();
            }

            @Override
            public void error(VolleyError error) {

                Log.e("error: ",error+"");

            }
        }.start();

    }

    public class MyPagerAdapter extends FragmentStatePagerAdapter {

        private final String[] TITLES = { "Current", "History", "Cancelled"};
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment=null;
            if(i==0) {
                fragment=CurrentOrdersFragment.newInstance("","");
            } else if(i==1) {
                fragment=OrdersHistoryFragment.newInstance("","");
            }else if(i==2) {
                fragment=CanceledOrdersFragment.newInstance("","");
            }
            return fragment;
        }
    }
}
