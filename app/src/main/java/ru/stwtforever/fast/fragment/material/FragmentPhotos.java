package ru.stwtforever.fast.fragment.material;


import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.support.v7.app.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import ru.stwtforever.fast.adapter.*;
import ru.stwtforever.fast.util.*;
import ru.stwtforever.fast.api.*;
import ru.stwtforever.fast.api.model.*;
import ru.stwtforever.fast.*;
import java.util.*;


public class FragmentPhotos extends Fragment {

/*
    private GridView gv;
    private long cid, uid;
    private ArrayList<PhotoMaterialsItems> items;
    private PhotoMaterialsAdapter adapter;
    private long id;
    private ProgressBar progressBar;

    public FragmentPhoto() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        uid = getActivity().getIntent().getExtras().getLong("uid");
        cid = getActivity().getIntent().getExtras().getLong("cid");
        View rootView = inflater.inflate(R.layout.materials_photo, container, false);
        gv = (GridView) rootView.findViewById(R.id.gv);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progress);
        gv.setNumColumns(3);
        gv.setVerticalSpacing(5);
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        gv.setColumnWidth(metrics.widthPixels / 3);
        
      
        items = new ArrayList<>();
        adapter = new PhotoMaterialsAdapter(getActivity(), items);

        new PhotoGetter().execute();

        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                PhotoMaterialsItems item = (PhotoMaterialsItems) adapterView.getItemAtPosition(i);
                showDialog(item);
            }
        });
        return rootView;
    }

    private void showDialog(final PhotoMaterialsItems item) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setMessage("Вы хотите посмотреть данную фотографию?");
        adb.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getActivity(), "Мне похуй. Хоти дальше.", Toast.LENGTH_SHORT).show();
            }
        });
        adb.setNegativeButton("Нет", null);
        AlertDialog a = adb.create();
        a.show();
    }

    private class PhotoGetter extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            gv.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        items = new ArrayList<>();
                        if (cid == 0)
                            id = uid;
                        else id = 2000000000 + cid;

                        ArrayList<VKMessageAttachment> apiMessagesHistory = Api.getHistoryAttachments(id, "photo", 0, 200, null);
                        for (VKMessageAttachment message : apiMessagesHistory) {
                            items.add(new PhotoMaterialsItems(message));
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter = new PhotoMaterialsAdapter(getActivity(), items);
                                gv.setAdapter(adapter);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            gv.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            super.onPostExecute(aVoid);
        }
    }*/
}
