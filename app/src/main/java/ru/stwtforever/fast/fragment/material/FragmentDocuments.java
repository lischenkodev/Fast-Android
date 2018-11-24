package ru.stwtforever.fast.fragment.material;

import android.*;
import android.os.*;
import android.support.annotation.*;
import android.support.v4.app.*;
import android.view.*;
import android.widget.*;
import ru.stwtforever.fast.R;


public class FragmentDocuments extends Fragment {


    private ListView lv;
    private long uid, cid;
  //  private ArrayList<DocsMaterialItems> items;
   // private DocsMaterialAdapter adapter;
    private long id;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        uid = getActivity().getIntent().getExtras().getLong("uid");
        cid = getActivity().getIntent().getExtras().getLong("cid");
        View rootView = null; //inflater.inflate(R.layout.materials_docs, container, false);
        lv = (ListView) rootView.findViewById(R.id.lv);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progress);
        
      //  items = new ArrayList<>();
       // adapter = new DocsMaterialAdapter(getActivity(), items);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //DocsMaterialItems item = (DocsMaterialItems) adapterView.getItemAtPosition(i);
                //showDialog(item);
            }
        });
       // new getAudios().execute();
        return rootView;
    }
/*
    private void showDialog(final DocsMaterialItems item) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setMessage("Вы хотите скачать документ '" + item.att.doc.title + "'?");
        adb.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.att.doc.url)));
            }
        });
        adb.setNegativeButton("Нет", null);
        AlertDialog ad = adb.create();
        ad.show();
    }

    private class getAudios extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            lv.setVisibility(View.GONE);
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

                        ArrayList<VKMessageAttachment> apiMessagesHistory = Api.getHistoryAttachments(id, "doc", 0, 200, null);
                        for (VKMessageAttachment message : apiMessagesHistory) {
                            items.add(new DocsMaterialItems(message));
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter = new DocsMaterialAdapter(getActivity(), items);
                                lv.setAdapter(adapter);
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
    }*/
}
