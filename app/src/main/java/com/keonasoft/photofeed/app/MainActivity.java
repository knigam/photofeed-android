package com.keonasoft.photofeed.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.keonasoft.photofeed.app.helper.HttpHelper;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    private String mCurrentPhotoPath;
    private FloatingActionButton cameraBtn;
    private ImageView mImageView;
    private ArrayList<String> drawerListItems;
    private Map<Integer, String> albumUrls;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private PictureAdapter adapter;

    private ArrayList<HashMap<String,String>> pictureListItems;
    private RecyclerView picturesList;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HttpHelper.getInstance().initialize(getApplicationContext());

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        picturesList = (RecyclerView) findViewById(R.id.pictures_list);
        picturesList.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        picturesList.setLayoutManager(mLayoutManager);

        adapter = new PictureAdapter(pictureListItems);
        picturesList.setAdapter(adapter);

        picturesList.setOnScrollListener(new RecyclerView.OnScrollListener() {
            private boolean hidden = false;
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                Animation animation;

                super.onScrolled(recyclerView, dx, dy);
                if (dy >= 0 && !hidden) {
                    animation = new TranslateAnimation(0, 0,0, 300);
                    hidden = true;
                }
                else if (dy < 0 && hidden){
                    animation = new TranslateAnimation(0, 0,300, 0);
                    hidden = false;
                }
                else
                    return;

                animation.setDuration(300);
                animation.setFillAfter(true);
                cameraBtn.startAnimation(animation);
                cameraBtn.setVisibility(View.INVISIBLE);
            }
        });

        updateDrawerListItems();
        populatePicturesList();

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                HttpHelper.getInstance().getClient().get(albumUrls.get(position), new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(responseBody,0, responseBody.length);
                        mImageView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                    }
                });
                mDrawerList.setItemChecked(position, true);
                setTitle(drawerListItems.get(position));
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        });

        cameraBtn = (FloatingActionButton) findViewById(R.id.camera_button);
        //TODO: MOVE NEXT LINE TO NEW ACTIVITY
        // mImageView = (ImageView) findViewById(R.id.thumbnail);

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });


    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            //Todo: send picture to server
            final String URL = getString(R.string.conn) + getString(R.string.picture_create);
            final File image = new File(mCurrentPhotoPath);
            RequestParams params = new RequestParams();
            params.put("text", image.getName());
            try {
                params.put("image", image);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            final String[] IMAGEURL = new String[1];
            HttpHelper.getInstance().getClient().post(URL, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    JSONObject json;
                    final String CONN = getString(R.string.conn);
                    String imageUrl = CONN + "/404.html";
                    try {
                        json = new JSONObject(new String(responseBody));
                        imageUrl = CONN + json.getString("image_url");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    HttpHelper.getInstance().getClient().get(imageUrl, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
//                            Bitmap bitmap = BitmapFactory.decodeByteArray(responseBody,0, responseBody.length);
//                            mImageView.setImageBitmap(bitmap);
//                            setTitle(image.getName());
                            //TODO Start new activity to show picture
                            populatePicturesList();
                            updateDrawerListItems();
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                        }
                    });
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Toast.makeText(MainActivity.this, "Sending failed", Toast.LENGTH_SHORT);
                }
            });


        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File to save the photo
            File photoFile = null;
            try {
                photoFile = createImageFile();
            }
            catch (IOException e) {
                // Error while creating the file
                System.out.println("ERROR CREATING THE FILE");
                return;
            }
            // Continue if there was no error
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void updateDrawerListItems() {
        albumUrls = new HashMap<Integer, String>();
        drawerListItems = new ArrayList<String>();
        HttpHelper.getInstance().getClient().get(getString(R.string.conn) + getString(R.string.picture_index), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                JSONArray json;
                try {
                    json = new JSONArray(new String(responseBody));
                    for (int i = 0; i < json.length(); i++){
                        JSONObject j = json.getJSONObject(i);
                        drawerListItems.add(j.getString("text"));
                        albumUrls.put(i, getString(R.string.conn) + j.getString("original_url"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mDrawerList.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, drawerListItems));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    private void populatePicturesList() {
        pictureListItems = new ArrayList<HashMap<String, String>>();

        HttpHelper.getInstance().getClient().get(getString(R.string.conn) + getString(R.string.picture_index), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                JSONArray json;
                try {
                    json = new JSONArray(new String(responseBody));
                    for (int i = 0; i < json.length(); i++){
                        JSONObject j = json.getJSONObject(i);
                        final HashMap<String,String> item = new HashMap<String, String>();
                        item.put("picture_text", j.getString("text"));
                        item.put("picture_thumb", j.getString("thumb_url"));
                        item.put("url", j.getString("url"));

                        pictureListItems.add(item);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                adapter.refresh(pictureListItems);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    private class PictureAdapter extends RecyclerView.Adapter {
        private List<HashMap<String, String>> data;

        private class ViewHolder extends RecyclerView.ViewHolder {
            TextView tv;
            ImageView img;

            public ViewHolder(View v){
                super(v);
                this.tv = (TextView) v.findViewById(R.id.picture_text);
                this.img = (ImageView) v.findViewById(R.id.picture_thumb);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public PictureAdapter(List<HashMap<String, String>> data) {
            this.data = data;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public PictureAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.picture_item, parent, false);

            // set the view's size, margins, paddings and layout parameters
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, PictureActivity.class);
                    intent.putExtra("url", data.get(picturesList.getChildPosition(v)).get("url"));
                    startActivity(intent);
                }
            });
            ViewHolder vh = new ViewHolder(rowView);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            final ViewHolder mHolder = (ViewHolder)holder;
            mHolder.tv.setText(data.get(position).get("picture_text"));

            HttpHelper.getInstance().getClient().get(getString(R.string.conn) + data.get(position).get("picture_thumb"), new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.length);
                    mHolder.img.setImageBitmap(bitmap);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                }
            });
        }

        public void refresh(List<HashMap<String,String>> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }
    }
}
