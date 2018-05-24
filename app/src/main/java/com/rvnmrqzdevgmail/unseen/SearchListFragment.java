package com.rvnmrqzdevgmail.unseen;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class SearchListFragment extends Fragment {


    Context context;

    public SearchListFragment() {
        // Required empty public constructor
    }


    @Override
    public void onResume() {
        super.onResume();
        //hide main bottom nav
        ((MainActivity)getActivity()).hideShowBottomNav(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((MainActivity)getActivity()).hideShowBottomNav(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search_list, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }
}
