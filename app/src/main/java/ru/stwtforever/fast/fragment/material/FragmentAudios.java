package ru.stwtforever.fast.fragment.material;


import android.*;
import android.os.*;
import android.support.v4.app.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import ru.stwtforever.fast.api.*;
import ru.stwtforever.fast.api.model.*;
import java.util.*;
import ru.stwtforever.fast.R;

public class FragmentAudios extends Fragment {

    private ListView lv;
    private long uid, cid;
  //  private AudioMaterialAdapter adapter;
    private long id;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        uid = getActivity().getIntent().getExtras().getLong("uid");
        cid = getActivity().getIntent().getExtras().getLong("cid");
        View rootView = null;// inflater.inflate(R.layout.materials_audio, container, false);
        lv = (ListView) rootView.findViewById(R.id.lv);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progress);
        
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
               // AudioMaterialItems item = (AudioMaterialItems) adapterView.getItemAtPosition(i);
              //  Toast.makeText(getActivity(), "Простите, но аудиозапись '" + item.attachment.audio.title + "' недоступна для прослушивания.", Toast.LENGTH_SHORT).show();
            }
        });
        new getAudios().execute();
        return rootView;
    }

    private class getAudios extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            lv.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                       // final ArrayList<AudioMaterialItems> items = new ArrayList<>();
                        if (cid == 0)
                            id = uid;
                        else id = 2000000000 + cid;

                        ArrayList<VKAttachments> apiMessagesHistory = VKApi.messages().getHistoryAttachments().peerId(id).mediaType("audio").execute(VKAttachments.class);
                        for (VKAttachments message : apiMessagesHistory) {
                            //items.add(new AudioMaterialItems(message));
                         //   Log.e("Links", message.audio.url);
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                               // adapter = new AudioMaterialAdapter(getActivity(), items);
                               // lv.setAdapter(adapter);
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
            lv.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            super.onPostExecute(aVoid);
        }
    }
}
