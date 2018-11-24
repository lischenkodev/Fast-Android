package ru.stwtforever.fast.fragment.material;

import android.content.*;
import android.net.*;
import android.os.*;
import android.support.annotation.*;
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


public class FragmentVideos extends Fragment {
/*
    private GridView gv;
    private long cid, uid;
    private ArrayList<VideoMaterialItems> items;
    private VideoMaterialAdapter adapter;
    private long id;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        uid = getActivity().getIntent().getExtras().getLong("uid");
        cid = getActivity().getIntent().getExtras().getLong("cid");
        View rootView = inflater.inflate(R.layout.materials_video, container, false);
        gv = (GridView) rootView.findViewById(R.id.gv);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progress);
        gv.setNumColumns(2);
        gv.setVerticalSpacing(5);
        gv.setHorizontalSpacing(5);
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        gv.setVerticalSpacing(4);
        
        
        items = new ArrayList<>();
        adapter = new VideoMaterialAdapter(getActivity(), items);

        new getVideos().execute();

        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                VideoMaterialItems item = (VideoMaterialItems) adapterView.getItemAtPosition(i);
                showAlert(item);
            }
        });
        return rootView;
    }

    private void showAlert(final VideoMaterialItems item) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setMessage("Вы хотите посмотреть видео '" + item.att.video.title + "'?");
        adb.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(item.att.video.getVideoUrl()), "video/mp4");
                getContext().startActivity(intent);
            }
        });
        adb.setNegativeButton("Нет", null);
        AlertDialog a = adb.create();
        a.show();
    }

    private class getVideos extends AsyncTask<Void, Void, Void> {
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

                        ArrayList<VKMessageAttachment> apiMessagesHistory = Api.getHistoryAttachments(id, "video", 0, 200, null);
                        for (VKMessageAttachment message : apiMessagesHistory) {
                            items.add(new VideoMaterialItems(message));
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter = new VideoMaterialAdapter(getActivity(), items);
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
