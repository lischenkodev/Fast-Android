package ru.stwtforever.fast.view;

import android.content.*;
import android.support.design.widget.*;
import android.support.v4.view.*;
import android.util.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import ru.stwtforever.fast.util.*;

public class FABBehaviour extends CoordinatorLayout.Behavior<LinearLayout> {
	
    public FABBehaviour(Context context, AttributeSet attrs) {
        super(context, attrs);
		//m = Utils.pxFromDp(68); 
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, LinearLayout child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);

		//if (m == 0) return;
		
        //child -> LinearLayout по сути типо тот тулбар
        if (dyConsumed > 0) {
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
          //  int fab_bottomMargin = layoutParams.bottomMargin;
            //child.animate().translationY(child.getHeight()).setInterpolator(new LinearInterpolator()).start();
        } else if (dyConsumed < 0) {
            //child.animate().translationY(0).setInterpolator(new LinearInterpolator()).start();
			//m = Utils.pxFromDp(68);
        }
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, LinearLayout child, View directTargetChild, View target, int nestedScrollAxes) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    } 
}
