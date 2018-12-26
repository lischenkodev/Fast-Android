package ru.stwtforever.fast.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.stwtforever.fast.MainActivity;
import ru.stwtforever.fast.R;

public class FragmentNavDrawer extends BottomSheetDialogFragment {

    private int selected_id = -1;

    public FragmentNavDrawer() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            selected_id = getArguments().getInt("selected_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bottomsheet_navdrawer, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final NavigationView navigationView = getView().findViewById(R.id.navigation_drawer);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).itemClick(item);
                }

                dismiss();
                return true;
            }
        });

        if (selected_id == -1) return;

        navigationView.setCheckedItem(selected_id);
    }
}
