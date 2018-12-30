package ru.stwtforever.fast.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.navigation.NavigationView
import ru.stwtforever.fast.MainActivity
import ru.stwtforever.fast.R

class FragmentNavDrawer : BottomSheetDialogFragment() {

    private var selectedId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            selectedId = arguments!!.getInt("selected_id")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_bottomsheet_navdrawer, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val navigationView = view!!.findViewById<NavigationView>(R.id.navigation_drawer)
        navigationView.setNavigationItemSelectedListener { item ->
            if (activity is MainActivity) {
                (activity as MainActivity).itemClick(item)
            }

            dismiss()
            true
        }

        if (selectedId == -1) return

        navigationView.setCheckedItem(selectedId)
    }
}
